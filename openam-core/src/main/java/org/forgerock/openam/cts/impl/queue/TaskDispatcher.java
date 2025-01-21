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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.impl.queue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThread;
import org.forgerock.openam.sm.datalayer.impl.tasks.ContinuousQueryTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.PartialQueryTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.QueryTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * TaskDispatcher operates as the coordinator of asynchronous task processing in the CTS persistence layer.
 * It does so by mapping the creation of tasks to be performed to a method of processing those tasks.
 *
 * The intention is to decouple the caller from the storage mechanism to ensure high throughput and independence
 * from the storage layer.
 *
 * The TaskDispatcher is unaware of the {@link TaskExecutor} implementation that will be used to dispatch tasks to
 * perform.
 *
 * @see TaskExecutor
 * @see Task
 */
@Singleton
public class TaskDispatcher {

    private final TaskFactory taskFactory;
    private final TaskExecutor taskExecutor;

    /**
     * The usage of Promise here allows access to the result of the Task as it is executed.
     */
    private final ConcurrentMap<TokenFilter, Promise<ContinuousQuery, NeverThrowsException>> continuousQueries;

    /**
     * Create a default instance of the TaskDispatcher.
     *
     * @param taskFactory Required to create Task instances.
     * @param taskExecutor Required for execution of the tasks.
     */
    @Inject
    public TaskDispatcher(@DataLayer(ConnectionType.CTS_ASYNC) TaskFactory taskFactory,
            @DataLayer(ConnectionType.CTS_ASYNC) TaskExecutor taskExecutor) {
        this.taskFactory = taskFactory;
        this.taskExecutor = taskExecutor;
        this.continuousQueries = new ConcurrentHashMap<>();
    }

    /**
     * Start the dispatcher. Synchronized to ensure that the taskExecutor is not started multiple times in parallel.
     */
    public synchronized void startDispatcher() {
        try {
            taskExecutor.start();
        } catch (DataLayerException e) {
            throw new IllegalStateException("Could not start task executor", e);
        }
    }

    /**
     * The CTS Token to create in the persistent store.
     *
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param token Non null token to create.
     * @param options Non null Options for the operation.
     * @param handler Non null ResultHandler to notify.
     *
     */
    public void create(Token token, Options options, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(token, options, handler);
        try {
            taskExecutor.execute(token.getTokenId(), taskFactory.create(token, options, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * The CTS Token to read from the persistent store.
     *
     * The provided ResultHandler will be notified when the read has been
     * completed.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param tokenId Non null Token ID.
     * @param options Non null Options for the operation.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void read(String tokenId, Options options, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenId, options, handler);
        try {
            taskExecutor.execute(tokenId, taskFactory.read(tokenId, options, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * The CTS Token to update in the persistent store.
     *
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param token Non null Token.
     * @param options Non null Options for the operation.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void update(Token token, Options options, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(token, options, handler);
        try {
            taskExecutor.execute(token.getTokenId(), taskFactory.update(token, options, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * The Token ID to delete from the persistent store.
     *
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param tokenId Non null Token ID.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was an unexpected error during processing.
     * @throws IllegalArgumentException If tokenId was null.
     */
    public void delete(String tokenId, ResultHandler<PartialToken, ?> handler) throws CoreTokenException {
        delete(tokenId, Options.defaultOptions(), handler);
    }

    /**
     * The Token ID, for a specific revision of the token, to delete from the persistent store.
     *
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param tokenId Non null Token ID.
     * @param options Non null Options for the operation.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was an unexpected error during processing.
     * @throws IllegalArgumentException If tokenId was null.
     */
    public void delete(String tokenId, Options options, ResultHandler<PartialToken, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenId, options, handler);
        try {
            taskExecutor.execute(tokenId, taskFactory.delete(tokenId, options, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * Perform a query against the persistent store and signal the results to the provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select a random queue to place the
     * {@link QueryTask} on. There is no guarantee that multiple query operations will be performed by the same
     * {@link SeriesTaskExecutorThread}.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param tokenFilter Non null TokenFilter.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void query(TokenFilter tokenFilter, ResultHandler<Collection<Token>, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenFilter, handler);
        try {
            taskExecutor.execute(null, taskFactory.query(tokenFilter, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * Perform a query against the persistent store and signal the results to the provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select a random queue to place the
     * {@link PartialQueryTask} on. There is no guarantee that multiple query operations will be performed by the same
     * {@link SeriesTaskExecutorThread}.
     *
     * @see ResultHandler
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param tokenFilter Non null TokenFilter.
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void partialQuery(TokenFilter tokenFilter, ResultHandler<Collection<PartialToken>, ?> handler)
            throws CoreTokenException {
        Reject.ifNull(tokenFilter, handler);
        try {
            taskExecutor.execute(null, taskFactory.partialQuery(tokenFilter, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * Perform a continuous query against the persistent store and signal the results to the provided
     * {@link ContinuousQueryListener}.
     *
     * If a {@link ContinuousQuery} already exists for the provided {@link TokenFilter} this method will simply add
     * the listener to that query.
     *
     * Note: Because a continuous query has no associated Token ID, this function will select a random queue to place
     * the {@link QueryTask} on. There is no guarantee that multiple continuous query operations will be performed by
     * the same {@link SeriesTaskExecutorThread}.
     *
     * @see ContinuousQueryListener
     * @see TaskDispatcher
     * @see org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration#getQueueTimeout()
     *
     * @param listener Non null ResultHandler to notify.
     * @param tokenFilter Non null TokenFilter.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void continuousQuery(final ContinuousQueryListener listener, TokenFilter tokenFilter)
            throws CoreTokenException {
        Reject.ifNull(tokenFilter, listener);
        try {
            synchronized (continuousQueries) {
                if (continuousQueries.containsKey(tokenFilter)) {
                    continuousQueries.get(tokenFilter).then(
                        new Function<ContinuousQuery, Void, NeverThrowsException>() {
                            @Override
                            public Void apply(ContinuousQuery value) throws NeverThrowsException {
                                value.addContinuousQueryListener(listener);
                                return null;
                            }
                        }
                    );
                } else {
                    ContinuousQueryTask task = taskFactory.continuousQuery(tokenFilter, listener);
                    taskExecutor.execute(null, task);
                    continuousQueries.put(tokenFilter, task.getQuery());
                }
            }
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * Removes the supplied {@link ContinuousQueryListener} from the query which is operating the
     * supplied {@link TokenFilter}.
     *
     * A continuous query that has no listeners will NOT be stopped, and may have further listeners added to it later.
     *
     * @see ContinuousQueryListener
     * @see TaskDispatcher
     *
     * @param listener Non null ResultHandler to notify.
     * @param tokenFilter Non null TokenFilter.
     */
    public void removeContinuousQueryListener(final ContinuousQueryListener listener, TokenFilter tokenFilter) {
        Reject.ifNull(tokenFilter, listener);

        if (continuousQueries.containsKey(tokenFilter)) {
            synchronized (continuousQueries) {
                if (continuousQueries.containsKey(tokenFilter)) {
                    continuousQueries.get(tokenFilter).then(
                        new Function<ContinuousQuery, Void, NeverThrowsException>() {
                            @Override
                            public Void apply(ContinuousQuery value) throws NeverThrowsException {
                                value.removeContinuousQueryListener(listener);
                                return null;
                            }
                        }
                    );
                }
            }
        }
    }

    /**
     * Stops a {@link ContinuousQuery} having removed all its {@link ContinuousQueryListener}s. If no
     * query exists for the filter this method performs no action.
     *
     * @param tokenFilter Non null TokenFilter.
     */
    public void stopContinuousQuery(TokenFilter tokenFilter) {
        Reject.ifNull(tokenFilter);

        if (continuousQueries.containsKey(tokenFilter)) {
            synchronized (continuousQueries) {
                if (continuousQueries.containsKey(tokenFilter)) {
                    continuousQueries.get(tokenFilter).then(
                        new Function<ContinuousQuery, Void, NeverThrowsException>() {
                            @Override
                            public Void apply(ContinuousQuery value) throws NeverThrowsException {
                                value.stopQuery();
                                return null;
                            }
                        }
                    );
                    continuousQueries.remove(tokenFilter);
                }
            }
        }
    }

}
