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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Listens for changes to the RADIUS server's configuration.
 */
public class ConfigChangeListener implements ServiceListener {

    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Blocking queue used to communicate with RuntimeServiceController that handlerConfig changes were made and the
     * service should restart or adjust accordingly. Putting an item in this queue indicates that we need to refresh our
     * configuration. A queue was selected for the rare event of an admin changing one field, hitting save, then
     * changing another and hitting save and the loading steps could potentially miss the latter change. Whereas we can
     * reload multiple times and adjust services accordingly without difficulty. So we queue each call to the change
     * listener and reload for each event. However, since we are comparing processing time with user input we don't need
     * lots of slots since processing should beat the user. But if loading took long enough we might get another change
     * queued up. And if it was taking that long then I don't care if we lose any further change events since there are
     * at least two in the queue that will force reloading. Hence why the queue isn't longer.
     */
    private final BlockingQueue<String> configChangedQueue = new ArrayBlockingQueue<String>(2);

    private final ServiceConfigManager svcConfigMgr;

    /**
     * Construct a ConfigChangeListener.
     *
     * @param serviceConfigurationManager - a ServiceConfigurationManager for the RADIUS service.
     */
    @Inject
    public ConfigChangeListener(@Named("RadiusServer") ServiceConfigManager serviceConfigurationManager) {
        this.svcConfigMgr = serviceConfigurationManager;
        // Put an initial item on the queue to cause intial config loading.
        configChangedQueue.add("Loading RADIUS config.");
    }

    /**
     * Method to allow creators of the ConfigChangeListener to have it start listening to config changes.
     */
    public void startListening() {
        logger.message("Entering ConfigChangeListener.startListening()");
        // Register this object as a listener of the ServiceConfigManger.
        // ** NOTE ** it may be tempting to move this to the constructor and save the call to start listening,
        // but we _could_ end up with a config change notification being received before this object has been
        // fully constructed then.
        this.svcConfigMgr.addListener(this);
        logger.message("Exiting ConfigChangeListener.startListening");
    }

    /**
     * Blocking call. Will return when a config change notification arrives or if one is queued.
     *
     * @return a string that may be logged, or null if the calling thread is interrupted.
     * @throws InterruptedException
     *             if the calling thread has been interrupted.
     */
    public String waitForConfigChange() throws InterruptedException {
        logger.message("Entering ConfigChangeListener.waitForConfigChange()");
        String msg = null;
        try {
            msg = configChangedQueue.take();
        } catch (final InterruptedException e) {
            logger.warning("Thread waiting for config change has been interrupted.");
            throw e;
        }
        logger.message("Exiting ConfigChangeListener.waitForConfigChange, returning " + msg);
        return msg;
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // ignore for now.
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
            int type) {
        logger.message("Entering globalConfigChange()");
        final boolean accepted = configChangedQueue.offer("RADIUS Config Changed. Loading...");

        if (!accepted) {
            logger.message("RADIUS Client handlerConfig change event ignored.");
        }
        logger.message("Leaving globalConfigChange()");
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        // ignore since we don't have any explicit realm related data in our handlerConfig
    }
}
