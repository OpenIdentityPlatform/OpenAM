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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring.impl.operations;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * An internal data structure for the CTSOperationsMonitoringStore to store monitoring information about operations of
 * tokens.
 * <br/>
 * The token operation monitoring information is split per token type and both a cumulative count and rate is calculated
 * and maintained.
 *
 * @since 12.0.0
 */
@Singleton
public class TokenOperationsStore {

    private final OperationStoreFactory operationStoreFactory;
    private final Map<TokenType, OperationStore> tokenOperations;
    private final OperationStore operationStore;
    private final OperationStore operationFailureStore;


    /**
     * Constructs a new instance of the TokenOperationsStore.
     */
    public TokenOperationsStore() {
        this(new OperationStoreFactory(), new HashMap<TokenType, OperationStore>(), new OperationStore(), new OperationStore());
    }

    /**
     * Constructs a new instance of the TokenOperationsStore, for test use.
     *
     * @param operationStoreFactory An instance of the OperationStoreFactory.
     * @param tokenOperations An instance of a Map of TokenType and OperationStore to store token operations.
     * @param operationStore An instance of the OperationsStore to collect overall operation statistics.
     * @param operationFailureStore An OperationStore used to collect statistics on operation failures.
     */
    TokenOperationsStore(final OperationStoreFactory operationStoreFactory,
            final Map<TokenType, OperationStore> tokenOperations,
            final OperationStore operationStore,
            final OperationStore operationFailureStore) {
        this.operationStoreFactory = operationStoreFactory;
        this.tokenOperations = tokenOperations;
        this.operationStore = operationStore;
        this.operationFailureStore = operationFailureStore;
    }

    /**
     * Adds a Token operation into the monitoring store.
     * <br/>
     * The operation is mapped to the type of the token. The operations per configurable period and cumulative count
     * will be updated for the token type and operation.
     *
     * @param type The type of token the operation was performed on.
     * @param operation The operation performed.
     * @param success Whether the operation was successful or not.
     */
    public void addTokenOperation(TokenType type, CTSOperation operation, boolean success) {

        // Update per-type operation count
        OperationStore operationStoreForTokenType = tokenOperations.get(type);
        if (operationStoreForTokenType == null) {
            operationStoreForTokenType = operationStoreFactory.createOperationStore();
            tokenOperations.put(type, operationStoreForTokenType);
        }
        operationStoreForTokenType.add(operation);

        // Update overall operation and failure counts as well
        addTokenOperation(operation, success);
    }

    /**
     * Adds a Token operation into the monitoring store.
     * <br/>
     * The operation is not mapped to a particular token type, this is for operations such as delete and list as
     * the type of token cannot be determined.
     * The operations per configurable period and cumulative count will be updated for the operation.
     *
     * @param operation The operation performed.
     * @param success Whether the operation was successful or not.
     */
    public void addTokenOperation(CTSOperation operation, boolean success) {
        // Always add to overall operation rate store
        operationStore.add(operation);

        // Record failures in an additional store
        if (!success) {
            operationFailureStore.add(operation);
        }
    }

    /**
     * Gets the average rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     *
     * @param type The type of token to now the operation average rate for.
     * @param operation The operation to now the average rate for.
     * @return The average number of operations made on the type token in a given period.
     */
    public double getAverageOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (!tokenOperations.containsKey(type)) {
            return 0L;
        }
        return tokenOperations.get(type).getAverageRate(operation);
    }

    /**
     * Gets the minimum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     *
     * @param type The type of token to now the operation rate for.
     * @param operation The operation to now the minimum rate for.
     * @return The minimum number of operations made on the type token in a given period.
     */
    public long getMinimumOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (!tokenOperations.containsKey(type)) {
            return 0L;
        }
        return tokenOperations.get(type).getMinRate(operation);
    }

    /**
     * Gets the maximum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     *
     * @param type The type of token to now the operation maximum rate for.
     * @param operation The operation to now the maximum rate for.
     * @return The maximum number of operations made on the type token in a given period.
     */
    public long getMaximumOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (!tokenOperations.containsKey(type)) {
            return 0L;
        }
        return tokenOperations.get(type).getMaxRate(operation);
    }

    /**
     * Gets the average rate of operations made in a given period.
     * <br/>
     * Returns the overall rate of calls to the given operation for all token types.
     *
     * @param operation The operation to now the average rate for.
     * @return The average number of operations made in a given period.
     */
    public double getAverageOperationsPerPeriod(CTSOperation operation) {
        return operationStore.getAverageRate(operation);
    }

    /**
     * Gets the average (mean) rate of operation failures in the current collection period for all token types.
     *
     * @param operation the operation to get an average failure rate for.
     * @return the average operation failure rate for this operation for all token types.
     */
    public double getAverageOperationFailuresPerPeriod(CTSOperation operation) {
        return operationFailureStore.getAverageRate(operation);
    }

    /**
     * Gets the minimum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on tokens, which the type cannot be determined,
     * i.e. Delete and List operations.
     *
     * @param operation The operation to now the minimum rate for.
     * @return The minimum number of operations made in a given period.
     */
    public long getMinimumOperationsPerPeriod(CTSOperation operation) {
        return operationStore.getMinRate(operation);
    }

    /**
     * Gets the minimum rate of operation failures seen in the current collection period.
     *
     * @param operation the operation to get the minimum failure rate for.
     * @return the minimum observed failure rate for the given operation.
     */
    public long getMinimumOperationFailuresPerPeriod(CTSOperation operation) {
        return operationFailureStore.getMinRate(operation);
    }

    /**
     * Gets the maximum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on tokens, which the type cannot be determined,
     * i.e. Delete and List operations.
     *
     * @param operation The operation to now the maximum rate for.
     * @return The maximum number of operations made in a given period.
     */
    public long getMaximumOperationsPerPeriod(CTSOperation operation) {
        return operationStore.getMaxRate(operation);
    }

    /**
     * Gets the maximum observed failure rate of an operation in the current period.
     *
     * @param operation the operation to get the maximum failure rate for.
     * @return the maximum observed failure rate for the given operation.
     */
    public long getMaximumOperationFailuresPerPeriod(CTSOperation operation) {
        return operationFailureStore.getMaxRate(operation);
    }

    /**
     * Gets the cumulative count of operations made since server start up.
     * <br/>
     * Will return the total number of the specified operation made on the specified type of token since the server
     * stared.
     *
     * @param type The type of token to now the cumulative count for.
     * @param operation The operation to now the cumulative count for.
     * @return The total number of operations made on the type of token since server start up.
     */
    public long getOperationsCumulativeCount(TokenType type, CTSOperation operation) {
        if (!tokenOperations.containsKey(type)) {
            return 0L;
        }
        return tokenOperations.get(type).getCount(operation);
    }

    /**
     * Gets the cumulative count of operations made since server start up.
     * <br/>
     * Will return the total number of the specified operation made on tokens, which the type cannot be determined,
     * i.e. Delete and List operations.
     *
     * @param operation The operation to know the cumulative count for.
     * @return The total number of operations made since server start up.
     */
    public long getOperationsCumulativeCount(CTSOperation operation) {
        return operationStore.getCount(operation);
    }

    /**
     * Gets the cumulative count of failures of this operation since server startup.
     *
     * @param operation The operation to get the cumulative failure count for.
     * @return The total number of failures of this operation since server startup.
     */
    public long getOperationFailuresCumulativeCount(CTSOperation operation) {
        return operationFailureStore.getCount(operation);
    }

    /**
     * Factory for getting new instances of the OperationStore.
     */
    static class OperationStoreFactory {

        /**
         * Returns a new instance of the OperationStore.
         *
         * @return A new OperationStore instance.
         */
        OperationStore createOperationStore() {
            return new OperationStore();
        }
    }
}
