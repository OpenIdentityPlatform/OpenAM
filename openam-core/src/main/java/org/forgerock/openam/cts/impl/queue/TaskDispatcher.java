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
package org.forgerock.openam.cts.impl.queue;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.QueueTimeoutException;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.impl.queue.config.QueueConfiguration;
import org.forgerock.openam.cts.impl.task.PartialQueryTask;
import org.forgerock.openam.cts.impl.task.QueryTask;
import org.forgerock.openam.cts.impl.task.Task;
import org.forgerock.openam.cts.impl.task.TaskFactory;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TaskDispatcher operates as the coordinator of asynchronous task processing in the
 * CTS persistence layer. The intention is to decouple the caller from the storage
 * mechanism to ensure high throughput and independence from the storage layer.
 *
 * This task queue has some specific behaviours which allow for consistent
 * operations under certain conditions.
 *
 * The {@link TaskDispatcher} will create a pre-defined number of {@link TaskProcessor}
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
 * @see QueueConfiguration#getQueueTimeout()
 * @see Task
 */
@Singleton
public class TaskDispatcher {
    private final ExecutorService poolService;
    private final TaskFactory taskFactory;

    private BlockingQueue<Task>[] taskQueues;
    private int processors;
    private boolean initialised;

    private final TaskProcessorFactory processorFactory;
    private final ThreadMonitor monitor;
    private final QueueConfiguration configuration;
    private static final Random random = new Random();
    private final Debug debug;

    /**
     * Create a default instance of the TaskDispatcher.
     *
     * @param poolService Required to scheduled worker threads.
     * @param taskFactory Required to create Task instances.
     * @param processorFactory Required to create worker thread instances.
     * @param monitor Required to ensure threads are restarted.
     * @param configuration Required to determine runtime configuration options.
     * @param debug Required for debugging.
     */
    @Inject
    public TaskDispatcher(@Named(CoreTokenConstants.CTS_WORKER_POOL) ExecutorService poolService,
                          TaskFactory taskFactory,
                          TaskProcessorFactory processorFactory,
                          ThreadMonitor monitor,
                          QueueConfiguration configuration,
                          @Named(CoreTokenConstants.CTS_ASYNC_DEBUG) Debug debug) {
        this.poolService = poolService;
        this.taskFactory = taskFactory;
        this.processorFactory = processorFactory;
        this.monitor = monitor;
        this.configuration = configuration;
        this.debug = debug;
        initialised = false;
    }

    /**
     * Create TaskProcessor threads for all configured connections.
     * Ensure each thread is monitored by {@link ThreadMonitor}.
     *
     * Synchronized to ensure that only one set of threads are initialised.
     */
    public synchronized void startDispatcher() {
        if (initialised) {
            return;
        }

        try {
            processors = configuration.getProcessors();
        } catch (CoreTokenException e) {
            throw new RuntimeException(e);
        }

        taskQueues = new BlockingQueue[processors];
        for (int ii = 0; ii < processors; ii++) {
            taskQueues[ii] = new LinkedBlockingQueue<Task>(configuration.getQueueSize());
        }

        for (int ii = 0; ii < processors; ii++) {
            TaskProcessor processor = processorFactory.create(taskQueues[ii]);
            monitor.watchThread(poolService, processor);
        }
        debug("Created {0} Task Processors", processors);

        initialised = true;
    }

    /**
     * The CTS Token to create in the persistent store.
     *
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param token Non null token to create.
     * @param handler Non null ResultHandler to notify.
     *
     */
    public void create(Token token, ResultHandler<Token> handler) throws CoreTokenException {
        Reject.ifNull(token);
        String tokenId = token.getTokenId();
        BlockingQueue<Task> queue = getQueue(tokenId);
        offer(queue, taskFactory.create(token, handler));
    }

    /**
     * The CTS Token to read from the persistent store.
     *
     * The provided ResultHandler will be notified when the read has been
     * completed.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param tokenId Non null Token ID.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void read(String tokenId, ResultHandler handler) throws CoreTokenException {
        Reject.ifNull(tokenId, handler);
        BlockingQueue<Task> queue = getQueue(tokenId);
        offer(queue, taskFactory.read(tokenId, handler));
    }

    /**
     * The CTS Token to update in the persistent store.
     *
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param token Non null Token.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void update(Token token, ResultHandler<Token> handler) throws CoreTokenException {
        Reject.ifNull(token);
        String tokenId = token.getTokenId();
        BlockingQueue<Task> queue = getQueue(tokenId);
        offer(queue, taskFactory.update(token, handler));
    }

    /**
     * The Token ID to delete from the persistent store.
     *
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param tokenId Non null Token ID.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was an unexpected error during processing.
     * @throws IllegalArgumentException If tokenId was null.
     */
    public void delete(String tokenId, ResultHandler<String> handler) throws CoreTokenException {
        Reject.ifNull(tokenId);
        BlockingQueue<Task> queue = getQueue(tokenId);
        offer(queue, taskFactory.delete(tokenId, handler));
    }

    /**
     * Perform a query against the persistent store and signal the results to the
     * provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select
     * a random queue to place the {@link QueryTask} on. There is no guarantee that
     * multiple query operations will be performed by the same {@link TaskProcessor}.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param tokenFilter Non null TokenFilter.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void query(TokenFilter tokenFilter, ResultHandler<Collection<Token>> handler) throws CoreTokenException {
        Reject.ifNull(tokenFilter, handler);
        offer(getQueueForQuery(), taskFactory.query(tokenFilter, handler));
    }

    /**
     * Perform a query against the persistent store and signal the results to the
     * provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select
     * a random queue to place the {@link PartialQueryTask} on. There is no guarantee that
     * multiple query operations will be performed by the same {@link TaskProcessor}.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see QueueConfiguration#getQueueTimeout()
     *
     * @param tokenFilter Non null TokenFilter.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void partialQuery(TokenFilter tokenFilter, ResultHandler<Collection<PartialToken>> handler) throws CoreTokenException {
        Reject.ifNull(tokenFilter, handler);
        offer(getQueueForQuery(), taskFactory.partialQuery(tokenFilter, handler));
    }

    /**
     * Select a random queue to use for the query operation.
     * @return Non null.
     */
    private BlockingQueue<Task> getQueueForQuery() {
        String key = Integer.toString(random.nextInt());
        return getQueue(key);
    }

    /**
     * Assign the task to the queue with a known timeout.
     * @param queue Non null BlockingQueue.
     * @param task Task to add.
     * @throws QueueTimeoutException If the timeout expired before the Task was added.
     */
    private void offer(BlockingQueue<Task> queue, Task task) throws QueueTimeoutException {
        try {
            debug("Queuing Task {0}", task.toString());
            if (!queue.offer(task, configuration.getQueueTimeout(), TimeUnit.SECONDS)) {
                throw new QueueTimeoutException(task);
            }
        } catch (InterruptedException e) {
            throw new QueueTimeoutException(task, e);
        }

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
        int select = QueueSelector.select(tokenId, processors);
        debug("Select Queue: Token ID {0} - Queue {1}", tokenId, select);
        return taskQueues[select];
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
