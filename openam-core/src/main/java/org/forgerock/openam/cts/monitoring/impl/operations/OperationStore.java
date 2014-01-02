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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring.impl.operations;

import org.forgerock.openam.cts.CTSOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * A data structure that stores the cumulative count and rate for CTS operations.
 *
 * @since 12.0.0
 */
class OperationStore {

    private final OperationRateFactory operationRateFactory;
    private transient Map<CTSOperation, OperationMonitor> operationRate;

    /**
     * Constructs a new instance of the OperationStore.
     */
    public OperationStore() {
        this.operationRateFactory = new OperationRateFactory();
        operationRate = new HashMap<CTSOperation, OperationMonitor>();
    }

    /**
     * Constructs a new instance of the OperationStore, for test use.
     *
     * @param operationRateFactory An instance of the OperationRateFactory.
     * @param operationRate An instance of a Map of CTSOperation and OperationMonitor to store token operation rates.
     */
    OperationStore(final OperationRateFactory operationRateFactory,
            final Map<CTSOperation, OperationMonitor> operationRate) {
        this.operationRateFactory = operationRateFactory;
        this.operationRate = operationRate;
    }

    /**
     * Adds a Token operation into the monitoring store.
     * <br/>
     * The operations per configurable period and cumulative count will be updated for the operation.
     *
     * @param operation The operation performed.
     */
    void add(CTSOperation operation) {
        OperationMonitor rate = operationRate.get(operation);
        if (rate == null) {
            synchronized (this) {
                rate = operationRate.get(operation);
                if (rate == null) {
                    rate = operationRateFactory.createOperationRate();
                    operationRate.put(operation, rate);
                }
            }
        }
        rate.increment();
    }

    /**
     * Gets the average rate of the specified operation that has been made in a given period.
     *
     * @param operation The operation to now the average rate for.
     * @return The average number of operations made in a given period.
     */
    double getAverageRate(CTSOperation operation) {
        if (!operationRate.containsKey(operation)) {
            return 0.0;
        }
        return operationRate.get(operation).getAverageRate();
    }

    /**
     * Gets the minimum rate of the specified operation that has been made in a given period.
     *
     * @param operation The operation to now the minimum rate for.
     * @return The minimum number of operations made in a given period.
     */
    long getMinRate(CTSOperation operation) {
        if (!operationRate.containsKey(operation)) {
            return 0L;
        }
        return operationRate.get(operation).getMinRate();
    }

    /**
     * Gets the maximum rate of the specified operation that has been made in a given period.
     *
     * @param operation The operation to now the maximum rate for.
     * @return The maximum number of operations made in a given period.
     */
    long getMaxRate(CTSOperation operation) {
        if (!operationRate.containsKey(operation)) {
            return 0L;
        }
        return operationRate.get(operation).getMaxRate();
    }

    /**
     * Gets the cumulative count of the number of times specified operation has been made, since server start up.
     *
     * @param operation The operation to now the cumulative count for.
     * @return The total number of operations made since server start up.
     */
    long getCount(CTSOperation operation) {
        if (!operationRate.containsKey(operation)) {
            return 0L;
        }
        return operationRate.get(operation).getCount();
    }

    /**
     * Factory for getting new instances of the OperationMonitor.
     */
    static class OperationRateFactory {

        /**
         * Returns a new instance of the OperationMonitor.
         *
         * @return A new OperationMonitor instance.
         */
        OperationMonitor createOperationRate() {
            return new OperationMonitor();
        }
    }
}
