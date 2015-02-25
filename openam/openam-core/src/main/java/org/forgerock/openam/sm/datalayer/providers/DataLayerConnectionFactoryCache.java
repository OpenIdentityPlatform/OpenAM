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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.providers;

import com.iplanet.dpro.session.service.SessionConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;

/**
 * Abstraction for the ConnectionFactory provider implementations.
 *
 * Manages details around the {@link ConnectionFactory} instances. In particular
 * ensures that only one ConnectionFactory will be generated for each
 * associated {@link ConnectionType}. Also ensures that each ConnectionFactory issued
 * will automatically respond to the system wide shutdown signal.
 *
 * This cache needs to be {@link Singleton} because it will establish connections to
 * the LDAP server. Multiple instances would be inappropriate.
 */
@Singleton
public class DataLayerConnectionFactoryCache implements ConnectionFactoryProvider, ShutdownListener {
    // Injected
    private final ConnectionFactoryProvider connectionFactoryProvider;
    private final Debug debug;

    private final Map<ConnectionType, ConnectionFactory> factories =
            new EnumMap<ConnectionType, ConnectionFactory>(ConnectionType.class);

    private boolean shutdown;

    /**
     * Creates an instance of this ConnectionFactoryProvider.
     *
     * @param shutdownManager Required to monitor shutdown process.
     * @param debug Non null.
     */
    @Inject
    public DataLayerConnectionFactoryCache(ShutdownManager shutdownManager,
                                           DataLayerConnectionFactoryProvider connectionFactoryProvider,
                                           @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        shutdownManager.addShutdownListener(this);
        this.debug = debug;
    }

    /**
     * Get the requested ConnectionFactory. If this factory has not been previously
     * requested it will be created on this call.
     *
     * @param type The type of ConnectionFactory to return.
     *
     * @return Non null connection factory.
     *
     * @throws IllegalStateException If this method is called after the system
     * has started shutdown.
     *
     * @throws InvalidConfigurationException If there was a problem with the configuration.
     */
    public synchronized ConnectionFactory createFactory(ConnectionType type) throws InvalidConfigurationException {
        if (shutdown) {
            throw new IllegalStateException("Shutdown requested");
        }

        debug("Requesting ConnectionFactory for type {0}", type.name());
        if (!factories.containsKey(type)) {
            debug("Creating factory for type {0}", type.name());
            factories.put(type, connectionFactoryProvider.createFactory(type));
        }
        return factories.get(type);
    }

    /**
     * Shuts down all established connection factories.
     */
    @Override
    public synchronized void shutdown() {
        shutdown = true;
        debug("Shutdown triggered");
        for (ConnectionFactory factory : factories.values()) {
            try {
                factory.close();
            } catch (RuntimeException e) {
                debug.error("Error whilst shutting down a Connection Factory", e);
            }
        }
    }

    private void debug(String format, String... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(format, args));
        }
    }
}
