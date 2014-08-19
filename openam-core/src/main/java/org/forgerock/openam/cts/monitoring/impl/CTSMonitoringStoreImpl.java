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

package org.forgerock.openam.cts.monitoring.impl;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.connections.ConnectionStore;
import org.forgerock.openam.cts.monitoring.impl.operations.TokenOperationsStore;
import org.forgerock.openam.cts.monitoring.impl.reaper.ReaperMonitor;

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
public class CTSMonitoringStoreImpl implements CTSOperationsMonitoringStore, CTSReaperMonitoringStore,
        CTSConnectionMonitoringStore {

    /**
     * Constant for binding an Executor for the CTS monitoring store to store CTS runtime data.
     */
    public static final String EXECUTOR_BINDING_NAME = "MONITORING_EXECUTOR";

    private final Debug debug;

    private final TokenOperationsStore tokenOperationsStore;
    private final ExecutorService executorService;
    private final ReaperMonitor reaperMonitor;
    private final ConnectionStore connectionStore;

    /**
     * Constructs an instance of the CTSMonitoringStoreImpl.
     *
     * @param debug An instance of the debug logger.
     * @param executorService An instance of an ExecutorService.
     * @param tokenOperationsStore An instance of the TokenOperationsStore.
     * @param reaperMonitor An instance of the ReaperMonitor.
     */
    @Inject
    public CTSMonitoringStoreImpl(@Named(EXECUTOR_BINDING_NAME) final ExecutorService executorService,
                                  final TokenOperationsStore tokenOperationsStore,
                                  final ReaperMonitor reaperMonitor,
                                  final ConnectionStore connectionStore,
                                  @Named(CoreTokenConstants.CTS_DEBUG) final Debug debug) {
        this.debug = debug;
        this.executorService = executorService;
        this.tokenOperationsStore = tokenOperationsStore;
        this.reaperMonitor = reaperMonitor;
        this.connectionStore = connectionStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTokenOperation(final Token token, final CTSOperation operation, final boolean success) {
        if (token == null) {
            addTokenOperation(operation, success);
            return;
        }

        try {
            executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    tokenOperationsStore.addTokenOperation(token.getType(), operation, success);
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
    private void addTokenOperation(final CTSOperation operation, final boolean success) {
        try {
            executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    tokenOperationsStore.addTokenOperation(operation, success);
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

    /**
     * {@inheritDoc}
     *
     * Uses the {@link TokenOperationsStore} to return the cumulative failure count.
     */
    @Override
    public long getOperationFailuresCumulativeCount(CTSOperation operation) {
        return tokenOperationsStore.getOperationFailuresCumulativeCount(operation);
    }

    @Override
    public double getAverageOperationFailuresPerPeriod(CTSOperation operation) {
        return tokenOperationsStore.getAverageOperationFailuresPerPeriod(operation);
    }

    @Override
    public long getMinimumOperationFailuresPerPeriod(CTSOperation operation) {
        return tokenOperationsStore.getMinimumOperationFailuresPerPeriod(operation);
    }

    @Override
    public long getMaximumOperationFailuresPerPeriod(CTSOperation operation) {
        return tokenOperationsStore.getMaximumOperationFailuresPerPeriod(operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReaperRun(long startTime, long runTime, long numberOfDeletedSessions) {
        reaperMonitor.add(startTime, runTime, numberOfDeletedSessions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRateOfDeletedSessions() {
        return reaperMonitor.getRateOfDeletion();
    }

    @Override
    public void addConnection(boolean success) {
        connectionStore.addConnection(success);
    }

    @Override
    public double getAverageConnectionsPerPeriod(boolean success) {
        return connectionStore.getAverageConnectionsPerPeriod(success);
    }

    @Override
    public double getMinimumOperationsPerPeriod(boolean success) {
        return connectionStore.getMinimumOperationsPerPeriod(success);
    }

    @Override
    public double getMaximumOperationsPerPeriod(boolean success) {
        return connectionStore.getMaximumOperationsPerPeriod(success);
    }

    @Override
    public double getConnectionsCumulativeCount(boolean success) {
        return connectionStore.getConnectionsCumulativeCount(success);
    }
}
