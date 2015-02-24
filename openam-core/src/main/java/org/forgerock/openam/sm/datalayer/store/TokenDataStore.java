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

package org.forgerock.openam.sm.datalayer.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.PooledTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;

/**
 * A generic token store that can read an write a java bean, {@code T}, that has annotations to support conversion
 * to and from a Token.
 * @param <T> The object type being stored.
 */
public class TokenDataStore<T> {

    private final Debug debug;
    private final JavaBeanAdapter<T> adapter;
    private final TaskExecutor taskExecutor;
    private final TaskFactory taskFactory;

    /**
     * Create a new TokenDataStore. This could be called from an extension class, or constructed as a raw store.
     * @param adapter The Java bean token adapter for the type of bean being stored as tokens.
     * @param taskExecutor The data layer task executor, for executing operations on the data store. Should be a
     *                     {@link org.forgerock.openam.sm.datalayer.impl.SimpleTaskExecutor}.
     * @param taskFactory The task factory for creating data store operations.
     */
    public TokenDataStore(JavaBeanAdapter<T> adapter, TaskExecutor taskExecutor, TaskFactory taskFactory) {
        this(adapter, taskExecutor, taskFactory,
                InjectorHolder.getInstance(Key.get(Debug.class, Names.named(DataLayerConstants.DATA_LAYER_DEBUG))));
    }

    @VisibleForTesting
    TokenDataStore(JavaBeanAdapter<T> adapter, TaskExecutor taskExecutor, TaskFactory taskFactory, Debug debug) {
        Reject.ifFalse(taskExecutor instanceof PooledTaskExecutor, "Task Executor must be a pool");
        this.adapter = adapter;
        this.taskExecutor = taskExecutor;
        this.taskFactory = taskFactory;
        this.debug = debug;
    }

    /**
     * Create an object. The id field will be populated with the resulting identifier.
     *
     * @param obj The object being created.
     * @throws ServerException When an error occurs during creation.
     */
    public void create(T obj) throws ServerException {
        Token token = adapter.toToken(obj);
        SyncResultHandler<Token> handler = new SyncResultHandler<Token>();
        try {
            taskExecutor.execute(token.getTokenId(), taskFactory.create(token, handler));
            handler.getResults();
        } catch (ServerException e) {
            throw e;
        } catch (DataLayerException e) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to create token corresponding", e);
            }
            throw new ServerException("Could not create token in token data store: " + e.getMessage());
        }
    }

    /**
     * Reads a {@code T} out of the store using its OpenAM Unique ID.
     *
     * @param id The OpenAM Unique ID assigned to the object.
     * @return The object, T.
     * @throws NotFoundException If the object is not found.
     * @throws ServerException When the object cannot be loaded.
     */
    public T read(String id) throws NotFoundException, ServerException {
        try {
            if (id == null) {
                throw new NotFoundException("Object not found");
            }
            SyncResultHandler<Token> handler = new SyncResultHandler<Token>();
            taskExecutor.execute(id, taskFactory.read(id, handler));
            Token token = handler.getResults();
            if (token == null) {
                throw new NotFoundException("Object not found with id: " + id);
            }
            return adapter.fromToken(token);
        } catch (NotFoundException e) {
            throw e;
        } catch (ServerException e) {
            throw e;
        } catch (DataLayerException e) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to read token corresponding to id: " + id, e);
            }
            throw new ServerException("Could not read token from token data store: " + e.getMessage());
        }
    }

    /**
     * Update a given instance.
     *
     * @param obj The object being updated.
     * @throws ServerException When the object cannot be found, or an error occurs during update.
     */
    public void update(T obj) throws NotFoundException, ServerException {
        SyncResultHandler<Token> handler = new SyncResultHandler<Token>();
        Token token = adapter.toToken(obj);
        try {
            // Check it exists
            read(token.getTokenId());
            // Update it
            taskExecutor.execute(token.getTokenId(), taskFactory.update(token, handler));
            handler.getResults();
        } catch (ServerException e) {
            throw e;
        } catch (NotFoundException e) {
            throw e;
        } catch (DataLayerException e) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to create token corresponding", e);
            }
            throw new ServerException("Could not create token in token data store: " + e.getMessage());
        }
    }

    /**
     * Remove an object with the given ID from the store.
     *
     * @param id The identifier of the object being removed.
     * @throws ServerException When an error occurs during removal.
     */
    public void delete(String id) throws NotFoundException, ServerException {
        SyncResultHandler<String> handler = new SyncResultHandler<String>();
        try {
            taskExecutor.execute(id, taskFactory.delete(id, handler));
            handler.getResults();
        } catch (ServerException e) {
            throw e;
        } catch (DataLayerException e) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to create token corresponding", e);
            }
            throw new ServerException("Could not create token in token data store: " + e.getMessage());
        }
    }

    /**
     * Query the store for instances.
     *
     * @param query The criteria of the query, using {@code T} bean property names as fields.
     * @return A set of all matching objects.
     * @throws ServerException When an error occurs when querying the store.
     */
    public Set<T> query(QueryFilter<String> query) throws ServerException {
        SyncResultHandler<Collection<Token>> handler = new SyncResultHandler<Collection<Token>>();
        try {
            Task task = taskFactory.query(adapter.toTokenQuery(query), handler);
            taskExecutor.execute(null, task);
            return convertResults(handler.getResults());
        } catch (ServerException e) {
            throw e;
        } catch (DataLayerException e) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to read objects corresponding to query: " + query, e);
            }
            throw new ServerException("Could not query tokens from data store: " + e.getMessage());
        }
    }

    /**
     * Internal conversion function to handle the query result.
     *
     * @param tokens A non null, but possibly empty collection of tokens.
     * @return A set of {@code T} objects expected by the caller.
     */
    private Set<T> convertResults(Collection<Token> tokens) {
        Set<T> results = new HashSet<T>();

        for (Token token : tokens) {
            results.add(adapter.fromToken(token));
        }

        return results;
    }

    /**
     * Different ways to combine criteria in a filter.
     *
     * @since 13.0.0
     */
    public static enum FilterType {
        AND, OR
    }

    /**
     * Because we're using the SimpleTaskExecutor, we can expect all tasks to have been completed before returning
     * for the task executor.
     * @param <R>
     */
    private class SyncResultHandler<R> implements ResultHandler<R, ServerException> {
        private boolean processed = false;
        private R result;
        private ServerException error;

        @Override
        public R getResults() throws ServerException {
            if (!processed) {
                throw new IllegalStateException("Synchronous result handler hasn't been processed");
            }
            if (error != null) {
                throw error;
            }
            return result;
        }

        @Override
        public void processResults(R result) {
            processed = true;
            this.result = result;
        }

        @Override
        public void processError(Exception error) {
            processed = true;
            this.error = new ServerException("Exception from data layer", error);
        }
    }
}
