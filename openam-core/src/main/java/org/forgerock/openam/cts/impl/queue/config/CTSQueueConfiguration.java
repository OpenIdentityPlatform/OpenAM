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
 */
package org.forgerock.openam.cts.impl.queue.config;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;

/**
 * The CTS asynchronous feature has a number of configuration properties which allow an
 * administrator to size and adjust the asynchronous queue and queue processors.
 *
 * QueueConfiguration brings this configuration together into a central point which can be
 * then used throughout the CTS asynchronous code.
 */
@Singleton
public class CTSQueueConfiguration implements QueueConfiguration {
    public static final int DEFAULT_TIMEOUT = 120;
    public static final int DEFAULT_QUEUE_SIZE = 5000;

    private final ConnectionConfigFactory dataLayerConfig;
    private final ConnectionCount connectionCount;
    private final Debug debug;

    /**
     * @param dataLayerConfig Required for resolving the number of connections available.
     * @param connectionCount
     * @param debug Required for debugging.
     */
    @Inject
    public CTSQueueConfiguration(ConnectionConfigFactory dataLayerConfig,
            ConnectionCount connectionCount,
            @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
        this.dataLayerConfig = dataLayerConfig;
        this.connectionCount = connectionCount;
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc} Default is {@link #DEFAULT_TIMEOUT}
     */
    @Override
    public int getQueueTimeout() {
        int timeout = SystemProperties.getAsInt(CoreTokenConstants.CTS_ASYNC_QUEUE_TIMEOUT, DEFAULT_TIMEOUT);
        if (timeout <= 0) {
            debug("Timeout {0} was invalid, using default {1}", timeout, DEFAULT_TIMEOUT);
            return DEFAULT_TIMEOUT;
        }
        return timeout;
    }

    /**
     * @return {@inheritDoc} Default is {@link #DEFAULT_QUEUE_SIZE}.
     */
    @Override
    public int getQueueSize() {
        int queueSize = SystemProperties.getAsInt(CoreTokenConstants.CTS_ASYNC_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
        if (queueSize <= 0) {
            debug("Queue size {0} was invalid, using default {1}", queueSize, DEFAULT_QUEUE_SIZE);
            return DEFAULT_QUEUE_SIZE;
        }
        debug("Queue Size: {0}", queueSize);
        return queueSize;
    }

    @Override
    public int getProcessors() throws DataLayerException {
        try {
            int max = dataLayerConfig.getConfig().getMaxConnections();
            return connectionCount.getConnectionCount(max, ConnectionType.CTS_ASYNC);
        } catch (IllegalArgumentException e) {
            throw new DataLayerException("Number of connections too low", e);
        } catch (InvalidConfigurationException e) {
            throw new DataLayerException("Configuration was invalid", e);
        }
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
