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
package org.forgerock.openam.cts.impl.queue.config;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.QueueSelector;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.text.MessageFormat;

/**
 * The CTS asynchronous feature has a number of configuration properties which allow an
 * administrator to size and adjust the asynchronous queue and queue processors.
 *
 * QueueConfiguration brings this configuration together into a central point which can be
 * then used throughout the CTS asynchronous code.
 */
@Singleton
public class QueueConfiguration {
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
    public QueueConfiguration(ConnectionConfigFactory dataLayerConfig,
                              ConnectionCount connectionCount,
                              @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
        this.dataLayerConfig = dataLayerConfig;
        this.connectionCount = connectionCount;
        this.debug = debug;
    }

    /**
     * The maximum duration the caller should wait to place their asynchronous
     * task on a work queue, and the maximum duration the caller should wait to
     * retrieve the result of processing from the queue.
     *
     * @return A positive integer in seconds to wait. Default is {@link #DEFAULT_TIMEOUT}
     */
    public int getQueueTimeout() {
        int timeout = SystemProperties.getAsInt(CoreTokenConstants.CTS_ASYNC_QUEUE_TIMEOUT, DEFAULT_TIMEOUT);
        if (timeout <= 0) {
            return DEFAULT_TIMEOUT;
        }
        return timeout;
    }

    /**
     * The size of each work queue that is used by the asynchronous work queue mechanism.
     * This will control how many items can be queued up in the CTS before this causes
     * the caller to block.
     *
     * @return A positive integer for the size of the queue, default is {@link #DEFAULT_QUEUE_SIZE}
     */
    public int getQueueSize() {
        int queueSize = SystemProperties.getAsInt(CoreTokenConstants.CTS_ASYNC_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
        if (queueSize <= 0) {
            debug("Queue size {0} was invalid, using default {1}", queueSize, DEFAULT_QUEUE_SIZE);
            return DEFAULT_QUEUE_SIZE;
        }
        debug("Queue Size: {0}", queueSize);
        return queueSize;
    }

    /**
     * The number of CTS asynchronous Task Processors that should be initialised.
     * This value is based on the number of connections available to the CTS.
     *
     * @see org.forgerock.openam.sm.ConnectionConfigFactory
     * @see ConnectionCount
     * @see QueueSelector
     *
     * @throws CoreTokenException If there was any issue resolving the configuration of the processors.
     * @return A positive number of processors to initialise.
     */
    public int getProcessors() throws CoreTokenException {
        try {
            int max = dataLayerConfig.getConfig(ConnectionType.CTS_ASYNC).getMaxConnections();
            return connectionCount.getConnectionCount(max, ConnectionType.CTS_ASYNC);
        } catch (IllegalArgumentException e) {
            throw new CoreTokenException("Number of connections too low", e);
        } catch (InvalidConfigurationException e) {
            throw new CoreTokenException("Configuration was invalid", e);
        }
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
