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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl;

import java.text.MessageFormat;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;

import com.sun.identity.shared.debug.Debug;

/**
 * This is a thread-safe, synchronous {@link TaskExecutor} that is implemented as a pool of
 * {@link SimpleTaskExecutor} instances.
 * <p>
 * On calling {@link #execute}, a thread will be blocked until an executor from the pool becomes
 * available. Executors are allocated from the provided {@link Semaphore}, which is provided by
 * the {@link org.forgerock.openam.sm.datalayer.api.DataLayerConnectionModule.SemaphoreProvider}.
 */
public class PooledTaskExecutor implements TaskExecutor {

    private static final String DEBUG_PREFIX = " :: PooledTaskExecutor :: ";
    public static final String SEMAPHORE = "PooledTaskExecutorSemaphore";

    private final int maximumPoolSize;
    private final Provider<SimpleTaskExecutor> simpleTaskExecutorProvider;
    private final Debug debug;
    private final Queue<SimpleTaskExecutor> pool = new ConcurrentLinkedQueue<SimpleTaskExecutor>();
    private final AtomicInteger executorsCount = new AtomicInteger(0);
    private final Semaphore semaphore;

    /**
     * Creates a new Executor pool.
     * @param simpleTaskExecutorProvider Creates new {@link SimpleTaskExecutor} instances for the pool.
     * @param debug Used for providing log output.
     * @param connectionType The type of connection this executor is for. This is used to find the
     *                       maximum pool size from the {@link ConnectionCount}.
     * @param connectionCount Used to deduce the connections for this connection type given the maximum
     *                        available to the underlying database.
     * @param connectionConfig Provides the maximum number of connections to the underlying database.
     * @param semaphore Controls access to the pool.
     */
    @Inject
    public PooledTaskExecutor(Provider<SimpleTaskExecutor> simpleTaskExecutorProvider,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug, ConnectionType connectionType,
            ConnectionCount connectionCount, ConnectionConfigFactory connectionConfig,
            @Named(SEMAPHORE) Semaphore semaphore) {
        this.simpleTaskExecutorProvider = simpleTaskExecutorProvider;
        this.debug = debug;
        this.semaphore = semaphore;

        int max = connectionConfig.getConfig().getMaxConnections();
        this.maximumPoolSize = connectionCount.getConnectionCount(max, connectionType);
        if (maximumPoolSize < 1) {
            throw new IllegalStateException("No connections allocated for " + connectionType);
        }
        if (maximumPoolSize != semaphore.availablePermits()) {
            throw new IllegalArgumentException("Configuration error - mismatch in pool sizes");
        }
    }

    @Override
    public void execute(String tokenId, Task task) throws DataLayerException {
        SimpleTaskExecutor executor = null;
        while (executor == null) {
            try {
                debug("Polling pool for an executor");
                semaphore.acquire();
                if (pool.isEmpty() && executorsCount.get() < maximumPoolSize) {
                    debug("There's room in the pool - will try and add an executor. Executors: {0}, Max: {1}",
                            executorsCount, maximumPoolSize);
                    addExecutor();
                }
                executor = pool.remove();
            } catch (InterruptedException e) {
                // Interrupted - loop again to try and get semaphore
            }
        }
        try {
            debug("Got an executor - executing task");
            executor.execute(tokenId, task);
        } finally {
            debug("Returning executor to the pool");
            pool.add(executor);
            semaphore.release();
        }
    }

    private synchronized void addExecutor() throws DataLayerException {
        if (executorsCount.get() < maximumPoolSize) {
            SimpleTaskExecutor executor = simpleTaskExecutorProvider.get();
            executor.start();
            pool.add(executor);
            executorsCount.incrementAndGet();
            debug("Added executor to pool - now got {0}", executorsCount);
        }
    }

    @Override
    public void start() throws DataLayerException {
    }

    private void debug(String message, Object... parameters) {
        if (debug.messageEnabled()) {
            debug.message(debug.getName() + " " + Thread.currentThread() + DEBUG_PREFIX + MessageFormat.format(message, parameters));
        }
    }
}
