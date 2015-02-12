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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

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
 * This is a thread-safe {@link TaskExecutor} that is implemented as a pool of {@link SimpleTaskExecutor} instances.
 */
public class PooledTaskExecutor implements TaskExecutor {

    private static final String DEBUG_PREFIX = " :: PooledTaskExecutor :: ";

    private final int maximumPoolSize;
    private final ConnectionFactory connectionFactory;
    private final Debug debug;
    private final TokenStorageAdapter adapter;
    private final BlockingQueue<SimpleTaskExecutor> pool = new LinkedBlockingQueue<SimpleTaskExecutor>();
    private final AtomicInteger executorsCount = new AtomicInteger(0);

    @Inject
    public PooledTaskExecutor(ConnectionFactory connectionFactory,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
            TokenStorageAdapter adapter, ConnectionType connectionType, ConnectionCount connectionCount,
            ConnectionConfigFactory connectionConfig) {
        int max = connectionConfig.getConfig().getMaxConnections();
        this.maximumPoolSize = connectionCount.getConnectionCount(max, connectionType);
        this.connectionFactory = connectionFactory;
        this.debug = debug;
        this.adapter = adapter;
    }

    @Override
    public void execute(String tokenId, Task task) throws DataLayerException {
        SimpleTaskExecutor executor = null;
        while (executor == null) {
            if (pool.size() == 0 && executorsCount.get() < maximumPoolSize) {
                debug("There's room in the pool - will try and add an executor. Executors: {0}, Max: {1}",
                        executorsCount, maximumPoolSize);
                addExecutor();
            }
            try {
                debug("Polling pool for an executor");
                executor = pool.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                debug.message("Interrupted while waiting for an executor", e);
            }
        }
        try {
            debug("Got an executor - executing task");
            executor.execute(tokenId, task);
        } finally {
            debug("Returning executor to the pool");
            try {
                pool.put(executor);
            } catch (InterruptedException e) {
                debug.message("Could not return executor to the queue. Closing.", e);
                executor.close();
                executorsCount.decrementAndGet();
            }
        }
    }

    private synchronized void addExecutor() throws DataLayerException {
        if (executorsCount.get() < maximumPoolSize) {
            SimpleTaskExecutor executor = new SimpleTaskExecutor(connectionFactory, debug, adapter);
            executor.start();
            if (pool.offer(executor)) {
                executorsCount.incrementAndGet();
                debug("Added executor to pool - now got {0}", executorsCount);
            } else {
                executor.close();
                debug("Could not add executor to pool, so closing again. Executor count: {0}", executorsCount);
            }
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
