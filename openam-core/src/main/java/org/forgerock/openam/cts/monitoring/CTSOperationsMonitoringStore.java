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

package org.forgerock.openam.cts.monitoring;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;

/**
 * A data structure for storing monitoring information about CTS operations.
 * <br/>
 * The CTS will use an instance of this data structure to store information about operations as and when they occur
 * and then the CTS monitoring framework will us the same instance to pull information out to send to clients
 * as monitoring requests are made.
 *
 * @since 12.0.0
 */
public interface CTSOperationsMonitoringStore {

    /**
     * Adds a Token operation into the monitoring store.
     * <br/>
     * The operation is mapped to the type of the token. The operations per configurable period and cumulative count
     * will be updated for the token type and operation.
     * <br/>
     * Passing in <code>null</code> as the token will mean that the operation is not mapped to a particular token
     * type, this is for operations such as delete and list as the type of token cannot be determined.
     * The operations per configurable period and cumulative count will be updated for the operation.
     *
     * @param token The Token the operation was performed on.
     * @param operation The operation performed.
     * @param success Whether the operation was successful or not.
     */
    void addTokenOperation(Token token, CTSOperation operation, boolean success);

    /**
     * Gets the average rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     * <br/>
     * Passing in <code>null</code> as the operation will mean that the method will return the number of the specified
     * operation made on tokens, which the type cannot be determined, i.e. Delete and List operations.
     *
     * @param type The type of token to get the operation average rate for.
     * @param operation The operation to get the average rate for.
     * @return The average number of operations made on the type token in a given period.
     */
    double getAverageOperationsPerPeriod(TokenType type, CTSOperation operation);

    /**
     * Gets the minimum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     * <br/>
     * Passing in <code>null</code> as the operation will mean that the method will return the number of the specified
     * operation made on tokens, which the type cannot be determined, i.e. Delete and List operations.
     *
     * @param type The type of token to get the operation minimum rate for.
     * @param operation The operation to get the minimum rate for.
     * @return The minimum number of operations made on the type token in a given period.
     */
    long getMinimumOperationsPerPeriod(TokenType type, CTSOperation operation);

    /**
     * Gets the maximum rate of operations made in a given period.
     * <br/>
     * Will return the number of the specified operation made on the specified type token in a given
     * (configurable) period.
     * <br/>
     * Passing in <code>null</code> as the operation will mean that the method will return the number of the specified
     * operation made on tokens, which the type cannot be determined, i.e. Delete and List operations.
     *
     * @param type The type of token to get the operation maximum rate for.
     * @param operation The operation to get the maximum rate for.
     * @return The maximum number of operations made on the type token in a given period.
     */
    long getMaximumOperationsPerPeriod(TokenType type, CTSOperation operation);

    /**
     * Gets the cumulative count of operations made since server start up.
     * <br/>
     * Will return the total number of the specified operation made on the specified type of token since the server
     * stared.
     * <br/>
     * Passing in <code>null</code> as the operation will mean that the method will return the total number of
     * the specified operation made on tokens, which the type cannot be determined, i.e. Delete and List operations.
     *
     * @param type The type of token to get the cumulative count for.
     * @param operation The operation to get the cumulative count for.
     * @return The total number of operations made on the type of token since server start up.
     */
    long getOperationsCumulativeCount(TokenType type, CTSOperation operation);

    /**
     * Gets the cumulative count of failures of this operation type since server startup.
     * <br/>
     * Note that failure counts are not distinguished by token type as in most failure cases this information is not
     * available.
     *
     * @param operation The operation to get the failure count for.
     * @return The total number of failed operations of this type since server startup.
     */
    long getOperationFailuresCumulativeCount(CTSOperation operation);

    /**
     * Gets the average failure rate for the given operation in the current period.
     *
     * @param operation the operation to get the failure rate for.
     * @return the average failure rate of the given operation in the current monitoring period.
     */
    double getAverageOperationFailuresPerPeriod(CTSOperation operation);

    /**
     * Gets the minimum observed failure rate for the given operation in the current period.
     *
     * @param operation the operation to get the failure rate for.
     * @return the minimum observed failure rate of the given operation in the current monitoring period.
     */
    long getMinimumOperationFailuresPerPeriod(CTSOperation operation);

    /**
     * Gets the maximum observed failure rate for the given operation in the current period.
     *
     * @param operation the operation to get the failure rate for.
     * @return the maximum observed failure rate of the given operation in the current monitoring period.
     */
    long getMaximumOperationFailuresPerPeriod(CTSOperation operation);
}
