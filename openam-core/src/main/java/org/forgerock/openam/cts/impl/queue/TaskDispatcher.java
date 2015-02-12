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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.queue;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.PartialQueryTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.QueryTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.util.Reject;

/**
 * TaskDispatcher operates as the coordinator of asynchronous task processing in the
 * CTS persistence layer. The intention is to decouple the caller from the storage
 * mechanism to ensure high throughput and independence from the storage layer.
 *
 * The TaskDispatcher uses a {@link SeriesTaskExecutor} to ensure token actions are
 * performed in series for each token.
 *
 * @see SeriesTaskExecutor
 * @see Task
 */
@Singleton
public class TaskDispatcher {
    private final TaskFactory taskFactory;
    private final TaskExecutor taskExecutor;

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
    }

    /**
     * Start the dispatcher. Synchronized to ensure that the taskExecutor is not
     * started multiple times in parallel.
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
     * @param handler Non null ResultHandler to notify.
     *
     */
    public void create(Token token, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(token);
        try {
            taskExecutor.execute(token.getTokenId(), taskFactory.create(token, handler));
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
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void read(String tokenId, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenId, handler);
        try {
            taskExecutor.execute(tokenId, taskFactory.read(tokenId, handler));
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
     * @param handler Non null ResultHandler to notify.
     *
     * @throws CoreTokenException If there was a problem adding the task to the queue.
     */
    public void update(Token token, ResultHandler<Token, ?> handler) throws CoreTokenException {
        Reject.ifNull(token);
        try {
            taskExecutor.execute(token.getTokenId(), taskFactory.update(token, handler));
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
    public void delete(String tokenId, ResultHandler<String, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenId);
        try {
            taskExecutor.execute(tokenId, taskFactory.delete(tokenId, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

    /**
     * Perform a query against the persistent store and signal the results to the
     * provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select
     * a random queue to place the {@link QueryTask} on. There is no guarantee that
     * multiple query operations will be performed by the same {@link org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThread}.
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
     * Perform a query against the persistent store and signal the results to the
     * provided ResultHandler.
     *
     * Note: Because a query has no associated Token ID, this function will select
     * a random queue to place the {@link PartialQueryTask} on. There is no guarantee that
     * multiple query operations will be performed by the same {@link org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThread}.
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
    public void partialQuery(TokenFilter tokenFilter, ResultHandler<Collection<PartialToken>, ?> handler) throws CoreTokenException {
        Reject.ifNull(tokenFilter, handler);
        try {
            taskExecutor.execute(null, taskFactory.partialQuery(tokenFilter, handler));
        } catch (DataLayerException e) {
            throw new CoreTokenException("Error in data layer", e);
        }
    }

}
