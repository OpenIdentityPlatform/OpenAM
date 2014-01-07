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

package org.forgerock.openam.cts.monitoring.impl.connections;

import javax.inject.Inject;

/**
 * A data structure that stores the cumulative count and rate for CTS connections.
 *
 * Periods can be customized through setting of the appropriate ConnectionMonitor values.
 *
 * @since 12.0.0
 */
public class ConnectionStore {

    private final ConnectionMonitor failureConnectionMonitor;
    private final ConnectionMonitor connectionMonitor;

    @Inject
    public ConnectionStore(ConnectionMonitor successMonitor, ConnectionMonitor failureMonitor) {
        connectionMonitor = successMonitor;
        failureConnectionMonitor = failureMonitor;
    }

    /**
     * Increments the counter for a connection. Boolean indicator controls
     * whether we increment the successful or unsuccessful connection counter.
     *
     * @param success true if the connection attempt was successful, false otherwise
     */
    public void addConnection(boolean success) {
        if (success) {
            connectionMonitor.add();
        } else {
            failureConnectionMonitor.add();
        }
    }

    /**
     * Returns the average connections made in the customisable period. Boolean
     * indicator controls whether we return the successful or unsuccessful
     * connection average rate.
     *
     * @param success true if the connection attempt was successful, false otherwise
     * @return the average connection rate made within the given period
     */
    public double getAverageConnectionsPerPeriod(boolean success) {
        if (success) {
            return connectionMonitor.getAverageRate();
        } else {
            return failureConnectionMonitor.getAverageRate();
        }
    }

    /**
     * Returns the minimum connections made in the customisable period. Boolean
     * indicator controls whether we return the successful or unsuccessful
     * connection average rate.
     *
     * @param success true if the connection attempt was successful, false otherwise
     * @return the minimum connection rate made within the given period
     */
    public double getMinimumOperationsPerPeriod(boolean success) {
        if (success) {
            return connectionMonitor.getMinimumRate();
        } else {
            return failureConnectionMonitor.getMinimumRate();
        }
    }

    /**
     * Returns the maximum connections made in the customisable period. Boolean
     * indicator controls whether we return the successful or unsuccessful
     * connection average rate.
     *
     * @param success true if the connection attempt was successful, false otherwise
     * @return the maximum connection rate made within the given period
     */
    public double getMaximumOperationsPerPeriod(boolean success) {
        if (success) {
            return connectionMonitor.getMaximumRate();
        } else {
            return failureConnectionMonitor.getMaximumRate();
        }
    }

    /**
     * Returns the total number of connections made in the customisable period. Boolean
     * indicator controls whether we return the successful or unsuccessful
     * connection average rate.
     *
     * @param success true if the connection attempt was successful, false otherwise
     * @return the total number of connections made since server startup
     */
    public double getConnectionsCumulativeCount(boolean success) {
        if (success) {
            return connectionMonitor.getCumulativeCount();
        } else {
            return failureConnectionMonitor.getCumulativeCount();
        }
    }
}
