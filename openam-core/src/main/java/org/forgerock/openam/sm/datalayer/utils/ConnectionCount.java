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
package org.forgerock.openam.sm.datalayer.utils;

import org.forgerock.openam.cts.impl.queue.QueueSelector;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.util.Reject;

import javax.inject.Inject;

/**
 * Logic to resolve the number of connections used by the three main users of the Service Management layer.
 *
 * @see ConnectionType
 */
public class ConnectionCount {
    static final int MINIMUM_CONNECTIONS = 6;
    private final StoreMode storeMode;

    /**
     * Guice initialised constructor.
     *
     * @param storeMode Non null required for calculating connections.
     */
    @Inject
    public ConnectionCount(StoreMode storeMode) {
        this.storeMode = storeMode;
    }

    /**
     * Returns the number of connections that should be allocated to for each ConnectionFactory type.
     *
     * When used in embedded mode, all three types are applicable. When used in External mode only
     * the CTS Async and CTS Reaper modes are applicable.
     *
     * @param max Non negative maximum number of connections allowed.
     * @param type The non null type of ConnectionFactory to be created.
     *
     * @return A non negative integer.
     *
     * @throws IllegalArgumentException If the maximum is less than {@link #MINIMUM_CONNECTIONS}.
     * @throws IllegalStateException If the type was unknown.
     */
    public int getConnectionCount(int max, ConnectionType type) {
        Reject.ifTrue(max < MINIMUM_CONNECTIONS);
        switch (type) {
            case CTS_ASYNC:
                if (storeMode == StoreMode.DEFAULT) {
                    max = max / 2;
                } else {
                    max = max - 2;
                }
                return findPowerOfTwo(max);
            case CTS_REAPER:
                return 1;
            case DATA_LAYER:
                /**
                  * Ensure that the DATA_LAYER connection type fits into the available
                  * connection space alongside CTS_REAPER and CTS_ASYNC
                  */
                int async = getConnectionCount(max, ConnectionType.CTS_ASYNC);
                int reaper = getConnectionCount(max, ConnectionType.CTS_REAPER);
                return max - (async + reaper);
            default:
                throw new IllegalStateException();
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
}
