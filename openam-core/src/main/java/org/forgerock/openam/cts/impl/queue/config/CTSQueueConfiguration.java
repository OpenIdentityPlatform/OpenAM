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
package org.forgerock.openam.cts.impl.queue.config;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.queue.QueueSelector;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;

/**
 * The CTS asynchronous feature has a number of configuration properties which allow an
 * administrator to size and adjust the asynchronous queue and queue processors.
 *
 * QueueConfiguration brings this configuration together into a central point which can be
 * then used throughout the CTS asynchronous code.
 */
@Singleton
public class CTSQueueConfiguration implements QueueConfiguration {
    public static final int DEFAULT_TIMEOUT = 15;
    public static final int DEFAULT_QUEUE_SIZE = 16000;

    private final ConnectionConfigFactory dataLayerConfig;
    private final Debug debug;

    /**
     * @param dataLayerConfig Required for resolving the number of connections available.
     * @param debug Required for debugging.
     */
    @Inject
    public CTSQueueConfiguration(ConnectionConfigFactory dataLayerConfig,
            @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
        this.dataLayerConfig = dataLayerConfig;
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
            int max = dataLayerConfig.getConfig(ConnectionType.CTS_ASYNC).getMaxConnections();
            return findPowerOfTwo(max - 1);
        } catch (IllegalArgumentException e) {
            throw new DataLayerException("Number of connections too low", e);
        } catch (InvalidConfigurationException e) {
            throw new DataLayerException("Configuration was invalid", e);
        }
    }

    /**
     * Not every even number is a power of two.
     * @see <a href="http://en.wikipedia.org/wiki/Power_of_two#Fast_algorithm_to_check_if_a_positive_number_is_a_power_of_two">Wikipedia</a>
     *
     * @return true if the integer is a power of two.
     */
    private static boolean isPowerOfTwo(int value) {
        return (value & (value - 1)) == 0;
    }

    /**
     * Locate a power of two that is less than or equal to the given number.
     *
     * @see QueueSelector#select(String, int)
     *
     * @param value Starting value, must be positive.
     * @return A valid power of two less than or equal to the given value. Not negative.
     * @throws IllegalArgumentException If no power of two was found.
     */
    public static int findPowerOfTwo(int value) {
        for (int ii = value; ii > 0; ii--) {
            if (isPowerOfTwo(ii)) {
                return ii;
            }
        }
        throw new IllegalArgumentException("No power of two found.");
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args);
        }
    }
}
