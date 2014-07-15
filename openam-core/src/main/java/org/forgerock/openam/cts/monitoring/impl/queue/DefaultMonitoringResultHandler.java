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
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;

/**
 * A ResultHandler implementation intended for use with CTS monitored operations
 * that do not specifically have access to the Token in the operation. This
 * includes Delete, and both Query operations.
 */
public class DefaultMonitoringResultHandler<T> implements ResultHandler<T> {
    private final ResultHandler<T> handler;
    private final CTSOperationsMonitoringStore store;
    private final CTSOperation operation;

    /**
     * @param handler The result handler being wrapped.
     * @param store The monitoring store to notify.
     * @param operation The CTS Operation to report to the store.
     */
    public DefaultMonitoringResultHandler(ResultHandler<T> handler, CTSOperationsMonitoringStore store, CTSOperation operation) {
        this.handler = handler;
        this.store = store;
        this.operation = operation;
    }

    /**
     * Defers to wrapped handler.
     * @return {@inheritDoc}
     * @throws CoreTokenException {@inheritDoc}
     */
    @Override
    public T getResults() throws CoreTokenException {
        return handler.getResults();
    }

    /**
     * Logs the option then delegates to the wrapped implementation.
     * @param result {@inheritDoc}
     */
    @Override
    public void processResults(T result) {
        store.addTokenOperation(null, operation, true);
        handler.processResults(result);
    }

    /**
     * Logs the option then delegates to the wrapped implementation.
     * @param error {@inheritDoc}
     */
    @Override
    public void processError(CoreTokenException error) {
        store.addTokenOperation(null, operation, false);
        handler.processError(error);
    }
}
