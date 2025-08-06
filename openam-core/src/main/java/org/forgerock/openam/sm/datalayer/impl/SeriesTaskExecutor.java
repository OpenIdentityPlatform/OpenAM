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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.sm.datalayer.impl;

import java.text.MessageFormat;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.audit.context.AbstractAuditRequestContextPropagatingDecorator;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.queue.QueueSelector;
import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.QueueTimeoutException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;

import com.sun.identity.shared.debug.Debug;

/**
 * The SeriesTaskExecutor is an executor that allows parallel processing of tasks, while guaranteeing that tasks on a
 * single {@link org.forgerock.openam.cts.api.tokens.Token} are always processed in order via a queuing mechanism.
 *
 * This task queue has some specific behaviours which allow for consistent
 * operations under certain conditions.
 *
 * The executor will create a pre-defined number of {@link org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThread}
 * threads which are responsible for processing tasks provided to this class. The
 * {@link Task}s themselves are keyed by the Token ID of the request.
 *
 * A {@link QueueSelector} algorithm is used to ensure that tasks for the same
 * TokenID are processed by the same TaskProcessor within this instance of
 * OpenAM. The intention here is to prevent unpredictable ordering of operations
 * against the same Token ID which could cause concurrent modification errors
 * at the storage layer.
 *
 * Each TaskProcessor is assigned a FIFO {@link BlockingQueue} instance which will
 * provide the predictable processing order. The implication of this design ensures
 * that when this queue is full, the caller is required to block.
 *
 * This provides an automatic throttling function for the CTS. The queues provide a
 * buffer for the CTS in the event that more come in than can be handled. If however
 * this situation persists for an extended duration, then the CTS queues will
 * throttle the caller until the CTS has had time to catch up.
 *
 * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
 */
public class SeriesTaskExecutor implements TaskExecutor {
    private static final Random random = new Random();
    private final Debug debug;
    static private BlockingQueue<Task> taskQueue=null;
    private int processors;
    private boolean initialised = false;
    private final SeriesTaskExecutorThreadFactory processorFactory;
    private final ThreadMonitor monitor;
    private final CTSQueueConfiguration configuration;
    private final ExecutorService poolService;

    /**
     * Create a default instance of the SeriesTaskExecutor.
     *
     * @param poolService Required to scheduled worker threads.
     * @param processorFactory Required to create worker thread instances.
     * @param monitor Required to ensure threads are restarted.
     * @param configuration Required to determine runtime configuration options.
     * @param debug Required for debugging.
     */
    @Inject
    public SeriesTaskExecutor(
            ExecutorService poolService,
            SeriesTaskExecutorThreadFactory processorFactory,
            ThreadMonitor monitor,
            CTSQueueConfiguration configuration,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
        this.debug = debug;
        this.monitor = monitor;
        this.configuration = configuration;
        this.processorFactory = processorFactory;
        this.poolService = poolService;
    }

    @Override
    public void execute(String tokenId, Task task) throws DataLayerException {
        BlockingQueue<Task> queue = getQueue(tokenId);
        offer(queue, task);
    }

    /**
     * Create TaskProcessor threads for all configured connections.
     * Ensure each thread is monitored by {@link ThreadMonitor}.
     *
     * Synchronized to ensure that only one set of threads are initialised.
     */
    @Override
    public synchronized void start() {
        if (initialised) {
            return;
        }

        try {
            processors = configuration.getProcessors();
        } catch (DataLayerException e) {
            throw new RuntimeException(e);
        }

        taskQueue = new LinkedBlockingQueue<>(configuration.getQueueSize());
        
        for (int ii = 0; ii < processors; ii++) {
            SeriesTaskExecutorThread processor = processorFactory.create(taskQueue);
            monitor.watchThread(poolService, processor);
        }
        debug("Created {0} Task Processors", processors);

        initialised = true;    }

    /**
     * Select a random queue to use for the query operation.
     * @return Non null.
     */
    private BlockingQueue<Task> getQueueForQuery() {
        return taskQueue;
    }

    /**
     * Select the appropriate queue based on the given Token ID.
     *
     * The QueueSelector algorithm used will be consistent against the Token ID.
     *
     * @param tokenId Non null Token ID.
     * @return A non null Queue to assign the task to.
     */
    private BlockingQueue<Task> getQueue(String tokenId) {
        if (tokenId == null) {
            return getQueueForQuery();
        }
        return taskQueue;
    }

    /**
     * Assign the task to the queue with a known timeout.
     * @param queue Non null BlockingQueue.
     * @param task Task to add.
     * @throws org.forgerock.openam.sm.datalayer.api.QueueTimeoutException If the timeout expired before the Task was added.
     */
    private void offer(BlockingQueue<Task> queue, Task task) throws QueueTimeoutException {
        try {
            debug("Queuing Task {0}", task.toString());
            if (!queue.offer(wrap(task), configuration.getQueueTimeout(), TimeUnit.SECONDS)) {
                throw new QueueTimeoutException(task);
            }
        } catch (InterruptedException e) {
            throw new QueueTimeoutException(task, e);
        }

    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }

    Task wrap(Task task) {
        return new AuditRequestContextPropagatingTask(task);
    }

    /**
     * <code>Task</code> Decorator that propagates thread local {@link AuditRequestContext} to worker thread.
     */
    // As there may be another implementation of TaskExecutor that transfers tasks between threads,
    // it may be a better idea to put this wrapper logic into DataLayerConnectionModule#configure
    // by creating a decorator for TaskExecutor that wraps Tasks submitted to TaskExecutor#execute
    static class AuditRequestContextPropagatingTask
            extends AbstractAuditRequestContextPropagatingDecorator implements Task {

        private final Task delegate;

        AuditRequestContextPropagatingTask(Task delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(TokenStorageAdapter adapter) throws DataLayerException {
            setContext();
            try {
                delegate.execute(adapter);
            } finally {
                revertContext();
            }
        }

        @Override
        public void processError(DataLayerException error) {
            delegate.processError(error);
        }
    }

}
