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

    private final AsyncProcessorCount connectionCount;
    private final Debug debug;

    /**
     * @param connectionCount Required to determine available connections.
     * @param debug Required for debugging.
     */
    @Inject
    public QueueConfiguration(AsyncProcessorCount connectionCount,
                              @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
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
     * Importantly: This value must be a power of two because of the queue selection
     * algorithm used by the CTS async queue.
     *
     * @see CTSConnectionCount
     * @see org.forgerock.openam.cts.impl.queue.QueueSelector
     *
     * @return A positive number of processors to initialise.
     * @throws CoreTokenException If the number of connections was too low to make a
     * number to the power of two, or there was a problem parsing the External CTS
     * configuration.
     */
    public int getProcessors() throws CoreTokenException {
        int count = connectionCount.getProcessorCount();
        if (count > 0) {
            return count;
        }

        throw new CoreTokenException("Too few connections allocated to the CTS");
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
