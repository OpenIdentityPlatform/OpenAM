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
package org.forgerock.openam.cts.monitoring.impl.queue;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;

/**
 * A monitoring based handler suitable for monitoring token based operations.
 */
public class TokenMonitoringResultHandler implements ResultHandler<Token> {
    private final ResultHandler<Token> handler;
    private final CTSOperationsMonitoringStore store;
    private final CTSOperation operation;

    /**
     * @param handler Non null handler to delegate to.
     * @param store Non null store to report operations to.
     * @param operation Non null operation type to signal to the store.
     */
    public TokenMonitoringResultHandler(ResultHandler<Token> handler, CTSOperationsMonitoringStore store,
                                        CTSOperation operation) {
        this.handler = handler;
        this.store = store;
        this.operation = operation;
    }

    /**
     * @return Delegates to wrapped handler.
     * @throws CoreTokenException {@inheritDoc}
     */
    @Override
    public Token getResults() throws CoreTokenException {
        return handler.getResults();
    }

    /**
     * @param result The result to log in the operation store, then delegates to wrapped handler.
     */
    @Override
    public void processResults(Token result) {
        store.addTokenOperation(result, operation, true);
        handler.processResults(result);
    }

    /**
     * @param error The error to log in the monitoring store, then delegate to the wrapped handler.
     */
    @Override
    public void processError(CoreTokenException error) {
        store.addTokenOperation(null, operation, false);
        handler.processError(error);
    }
}
