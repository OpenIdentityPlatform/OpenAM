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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.sm.datalayer.providers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.text.MessageFormat;

/**
 * Abstraction for the ConnectionFactory provider implementations.
 *
 * Manages details around the {@link ConnectionFactory} instances. In particular
 * ensures that only one ConnectionFactory will be generated for each
 * associated {@link ConnectionType}. Also ensures that each ConnectionFactory issued
 * will automatically respond to the system wide shutdown signal.
 *
 * This cache needs to be {@link Singleton} because it will establish connections to
 * the database server. Multiple instances would be inappropriate.
 */
@Singleton
public class DataLayerConnectionFactoryCache implements Provider<ConnectionFactory>, ShutdownListener {
    // Injected
    private final ConnectionFactoryProvider connectionFactoryProvider;
    private final Debug debug;
    private final ConnectionType connectionType;

    private ConnectionFactory factory = null;

    private boolean shutdown;

    /**
     * Creates an instance of this ConnectionFactoryProvider.
     *
     * @param shutdownManager Required to monitor shutdown process.
     * @param debug Non null.
     */
    @Inject
    public DataLayerConnectionFactoryCache(ConnectionType connectionType,
            ShutdownManager shutdownManager,
            ConnectionFactoryProvider connectionFactoryProvider,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
        this.connectionType = connectionType;
        this.connectionFactoryProvider = connectionFactoryProvider;
        shutdownManager.addShutdownListener(this);
        this.debug = debug;
    }

    /**
     * Get the requested ConnectionFactory. If this factory has not been previously
     * requested it will be created on this call.
     *
     * @return Non null connection factory.
     *
     * @throws IllegalStateException If this method is called after the system
     * has started shutdown.
     *
     * @throws InvalidConfigurationException If there was a problem with the configuration.
     */
    public synchronized ConnectionFactory get() throws InvalidConfigurationException {
        if (shutdown) {
            throw new IllegalStateException("Shutdown requested");
        }

        debug("Requesting ConnectionFactory for type {0}", connectionType.name());
        if (factory == null) {
            factory = connectionFactoryProvider.createFactory();
        }
        return factory;
    }

    /**
     * Shuts down all established connection factories.
     */
    @Override
    public synchronized void shutdown() {
        shutdown = true;
        debug("Shutdown triggered");
        try {
            factory.close();
        } catch (RuntimeException e) {
            debug.error("Error whilst shutting down a Connection Factory", e);
        }
    }

    private void debug(String format, String... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(format, args));
        }
    }
}
