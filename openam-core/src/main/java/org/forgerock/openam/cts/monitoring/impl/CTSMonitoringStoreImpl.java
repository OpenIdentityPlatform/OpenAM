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

package org.forgerock.openam.cts.monitoring.impl;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.operations.TokenOperationsStore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * An implementation of the CTSOperationsMonitoringStore that stores the CTS monitoring information
 * in internal data structure based on the type of monitoring information being stored.
 *
 * @since 12.0.0
 */
@Singleton
public class CTSMonitoringStoreImpl implements CTSOperationsMonitoringStore {

    private Debug debug;

    /**
     * Constant for binding an Executor for the CTS monitoring store to store CTS runtime data.
     */
    public static final String EXECUTOR_BINDING_NAME = "MONITORING_EXECUTOR";

    private final TokenOperationsStore tokenOperationsStore;
    private final ExecutorService executorService;

    /**
     * Constructs an instance of the CTSMonitoringStoreImpl.
     *
     * @param tokenOperationsStore An instance of the TokenOperationsStore.
     * @param executorService An instance of an ExecutorService.
     * @param debug An instance of the debug logger.
     */
    @Inject
    public CTSMonitoringStoreImpl(TokenOperationsStore tokenOperationsStore,
            @Named(EXECUTOR_BINDING_NAME) ExecutorService executorService,
            @com.google.inject.name.Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.tokenOperationsStore = tokenOperationsStore;
        this.executorService = executorService;
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTokenOperation(final Token token, final CTSOperation operation) {
        if (token == null) {
            addTokenOperation(operation);
            return;
        }

        try {
            executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    tokenOperationsStore.addTokenOperation(token.getType(), operation);
                    return null;
                }
            });
        } catch (RejectedExecutionException e) {
            debug.error("Token Operation " + operation.name() + " on " + token.getTokenId() + " could not be recorded",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    private void addTokenOperation(final CTSOperation operation) {
        try {
            executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    tokenOperationsStore.addTokenOperation(operation);
                    return null;
                }
            });
        } catch (RejectedExecutionException e) {
            debug.error("Token Operation " + operation.name() + " could not be recorded", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getAverageOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (type != null) {
            return tokenOperationsStore.getAverageOperationsPerPeriod(type, operation);
        } else {
            return tokenOperationsStore.getAverageOperationsPerPeriod(operation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMinimumOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (type != null) {
            return tokenOperationsStore.getMinimumOperationsPerPeriod(type, operation);
        } else {
            return tokenOperationsStore.getMinimumOperationsPerPeriod(operation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaximumOperationsPerPeriod(TokenType type, CTSOperation operation) {
        if (type != null) {
            return tokenOperationsStore.getMaximumOperationsPerPeriod(type, operation);
        } else {
            return tokenOperationsStore.getMaximumOperationsPerPeriod(operation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOperationsCumulativeCount(TokenType type, CTSOperation operation) {
        if (type != null) {
            return tokenOperationsStore.getOperationsCumulativeCount(type, operation);
        } else {
            return tokenOperationsStore.getOperationsCumulativeCount(operation);
        }
    }
}
