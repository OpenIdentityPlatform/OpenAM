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

package org.forgerock.openam.cts.monitoring;

/**
 * A data structure for storing monitoring information about CTS connections.
 * <br/>
 * The CTS will use an instance of this data structure to store information about connection attempts
 * and the CTS monitoring framework will us the same instance to pull information out to send to clients
 * as monitoring requests are made.
 *
 */
public interface CTSConnectionMonitoringStore {

    /**
     * Adds a connection attempt event into the monitoring store.
     *
     * @param success True to increment number of successful connections made through this pool, false for failure
     */
    void addConnection(boolean success);

    /**
     * Gets the average number of connections requested from the pool in a given interval.
     *
     * @param success True to get average number of successful connections, false for average number of connection
     *                requests rejected
     * @return The average number of connections in a given period.
     */
    double getAverageConnectionsPerPeriod(boolean success);

    /**
     * Gets the minimum number of connections requested from the pool in a given interval.
     *
     *
     * @param success True to get minimum number of successful connections, false for minimum number of connection
     *                requests rejected
     * @return The minimum number of connections in a given period.
     */
    double getMinimumOperationsPerPeriod(boolean success);

    /**
     * Gets the maximum number of connections requested from the pool in a given interval.
     *
     *
     * @param success True to get maximum number of successful connections, false for maximum number of connection
     *                requests rejected
     * @return The maximum number of connections in a given period.
     */
    double getMaximumOperationsPerPeriod(boolean success);


    /**
     * Gets the total count of connections requested from the pool in a given interval.
     *
     * @param success True to get total count of successful connections, false for total count of connection
     *                requests rejected
     * @return The total count of connections in a given period.
     */
    double getConnectionsCumulativeCount(boolean success);
}
