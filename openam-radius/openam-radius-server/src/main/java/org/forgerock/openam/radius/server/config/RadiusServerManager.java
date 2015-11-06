/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.config;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.radius.server.RadiusLifecycleException;
import org.forgerock.openam.radius.server.RadiusRequestListener;
import org.forgerock.openam.radius.server.RequestListenerFactory;

import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is a singleton instance acting as the entry point both for starting up the RADIUS server feature in OpenAM
 * and for tearing it down upon container shutdown. This class is also responsible for ensuring that the radius
 * configuration pages are installed in OpenAM and installing them if they are not. Its daemon defers to the
 * {@link org.forgerock.openam.radius.server.config.StartupCoordinator} to ensure that OpenAM is ready for business,
 * uses the {@link org.forgerock.openam.radius.server.config.ConfigLoader} to watch for and share changes in
 * configuration in the admin console, and starts up or shuts down the radius
 * {@link org.forgerock.openam.radius.server.RadiusRequestListener} accordingly.
 * <p/>
 */

public final class RadiusServerManager implements Runnable, RadiusServer {

    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The thread that we launch to run this Runnable. Reference is needed in the event we need to shut down.
     */
    private volatile Thread coordinatingThread = null;

    /**
     * Loads config from openam.
     */
    private final ConfigLoader configLoader;

    /**
     * We can make a blocking call to the configChangeListener to wait for a config change.
     */
    private final ConfigChangeListener configChangeListener;


    /**
     * A factory from which <code>RadiusRequestListener</code> objects may be obtained.
     */
    private final RequestListenerFactory requestListenerFactory;


    /**
     * Creates the instance of the starter.
     *
     * @param configLoader - An object used to load and read Radius config from.
     * @param configChangeListener - An object responsible for listening to config changes. Can make blocking calls to
     *            this class that will only return when new config is available.
     * @param requestListenerFactory - a factory from which a request listener may be obtained.
     */
    @Inject
    @Singleton
    public RadiusServerManager(ConfigLoader configLoader, ConfigChangeListener configChangeListener,
            RequestListenerFactory requestListenerFactory) {
        logger.message("Constructing RadiusServiceStarter");
        this.configLoader = configLoader;
        this.configChangeListener = configChangeListener;
        this.requestListenerFactory = requestListenerFactory;
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.config.RadiusService#shutdown()
     */
    @Override
    public void shutdown() {
        logger.message(this.getClass().getSimpleName() + "#shutdown() method called.");
        final Thread coordinator = coordinatingThread;

        if (coordinator != null) {
            final String name = coordinator.getName();
            logger.message(this.getClass().getSimpleName() + " interrupting " + name);
            coordinator.interrupt();

            while (coordinatingThread != null) {
                logger.message("Waiting for " + name + " to exit.");
                try {
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    logger.message("Thread sleep was interupted.");
                    Thread.currentThread().interrupt();
                }
            }
            logger.message("Thread " + name + " server shutdown.");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.config.RadiusService#startUp()
     */
    @Override
    public synchronized void startUp() {
        if (coordinatingThread == null) {

            configChangeListener.startListening();

            final Thread t = new Thread(this);
            t.setName(MessageFormat.format(RadiusServerConstants.COORDINATION_THREAD_NAME, this.getClass()
                    .getSimpleName()));
            t.setDaemon(true);
            t.start();
            coordinatingThread = t;
        } else {
            logger.warning(this.getClass().getSimpleName() + ".setServletConfig() called again. Service "
                    + "already started. Ignoring.");
        }
    }

    @Override
    public void run() {
        // If AMSetupServlet has determined that there is no current valid configuration, then it is
        // likely an upgrade in process. It is not helpful for the RADIUS server to be running during
        // upgrade so exit.
        final boolean noValidConfig = AMSetupServlet.isCurrentConfigurationValid();
        if (!noValidConfig) {
            logger.error("RADIUS Service Unavailable. No configuration available. This may be due to configuration "
                    + "after installation or upgrade in progress.");
            this.coordinatingThread = null;
            return;
        }

        // kick off config loading and registration of change listener
        String changeMsg = null;

        RadiusRequestListener listener = null;

        // The current handlerConfig loaded from openAM's admin console constructs. When handlerConfig changes are
        // detected we reload and compare the new to the old and adjust accordingly.
        RadiusServiceConfig currentCfg = new RadiusServiceConfig(false, RadiusServerConstants.RADIUS_AUTHN_PORT, null);

        try {

            while (true) {
                // wait until we see a handlerConfig change
                changeMsg = configChangeListener.waitForConfigChange();
                // wait for changes to take effect
                Thread.sleep(1000);
                // load our handlerConfig
                logger.message(changeMsg);

                final RadiusServiceConfig cfg = configLoader.loadConfig();
                if (cfg != null) {
                    logger.message("New RADIUS configuration loaded." + cfg);
                } else {
                    logger.warning("Unable to load new RADIUS configuration. Ignoring change.");
                    // nothing to be done. lets wait for another handlerConfig event and maybe it will be loadable then
                    continue;
                }

                // Config has changed. If there is a listener already running, and there are changes to the
                // RADIUS server config settings then the listener will need to be stopped, and then a new
                // new listener created in the new configuration.
                //
                // If only client config has changed, the listener may be updated in place.
                if (listener == null) {
                    // at startup or after service has been turned off
                    if (cfg.isEnabled()) {
                        try {
                            listener = requestListenerFactory.getRadiusRequestListener(cfg);
                        } catch (final RadiusLifecycleException e) {
                            logger.error("RADIUS server could not be started. Failed to create Request Listener. "
                                    + "Ignoring change.", e);
                            continue;
                        }
                    } else {
                        logger.warning("RADIUS service is not enabled.");
                    }
                } else {
                    // so we already have a listener running
                    if (onlyClientSetChanged(cfg, currentCfg)) {
                        logger.message("Updating client configs.");
                        listener.updateConfig(cfg);
                    } else {
                        // all other changes (port, thread pool values, enabledState) require restart of listener
                        logger.message("RADIUS server config has changed. Radius server will be terminated and"
                                + " restarted in new configuration.");
                        listener.terminate();
                        listener = null;
                        if (cfg.isEnabled()) {
                            try {
                                listener = requestListenerFactory.getRadiusRequestListener(cfg);
                            } catch (final RadiusLifecycleException e) {
                                logger.error("RADIUS server could not be started. Failed to create Request Listener. "
                                        + "Ignoring change.", e);
                                continue;
                            }
                        } else {
                            logger.warning("RADIUS service is not enabled.");
                        }
                    }
                }

                currentCfg = cfg;
            } // End of while loop

        } catch (final InterruptedException e) {
            logger.warning(Thread.currentThread().getName() + " interrupted. Exiting.");
            // Clear the interrupted flag. This thread will now exit as we'll drop out of the
            // while loop - i.e. we've handled the interruption.
            Thread.interrupted();
        }
        // shutting down so terminate the listener and thread pools
        final RadiusRequestListener l = listener;

        if (l != null) {
            l.terminate();
        }

        logger.warning(Thread.currentThread().getName() + " exited.");
        this.coordinatingThread = null;
    }

    /**
     * Returns true if the only changes made are the addition, changing, or removal of the defined set of clients.
     *
     * @param cfg
     * @param currentCfg
     * @return
     */
    private boolean onlyClientSetChanged(RadiusServiceConfig cfg, RadiusServiceConfig currentCfg) {

        return cfg.getPort() == currentCfg.getPort() && cfg.isEnabled() == currentCfg.isEnabled()
                && cfg.getThreadPoolConfig() != null
                && cfg.getThreadPoolConfig().equals(currentCfg.getThreadPoolConfig());
    }
}
