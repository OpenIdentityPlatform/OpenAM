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

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.config.QueueConfiguration;

import java.lang.IllegalStateException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * AsyncResultHandler is an asynchronous implementation of the ResultHandler.
 *
 * It specifically is aware of the threading and timing concerns around asynchronous
 * operations and mitigates that with the use of a BlockingQueue.
 *
 * The processed result is placed on the queue when available. At which point either
 * the caller is already waiting, or can begin to wait for the result. The queue
 * is sized to one to enforce the usage of this ResultHandler as being used only
 * once.
 *
 * @see TaskDispatcher#read(String, ResultHandler)
 * @see TaskDispatcher#query(TokenFilter, ResultHandler)
 *
 * @param <T> {@inheritDoc}
 */
public class AsyncResultHandler<T> implements ResultHandler<T> {
    private final BlockingQueue<Object> syncQueue;
    private static final String NULL_SIGNAL = "--NULL--";
    private final QueueConfiguration config;

    public AsyncResultHandler(QueueConfiguration config) {
        this(config, new ArrayBlockingQueue<Object>(1));
    }

    AsyncResultHandler(QueueConfiguration config, BlockingQueue<Object> queue) {
        this.config = config;
        this.syncQueue = queue;
    }

    /**
     * Blocking call to wait for the results of processing.
     *
     * @return {@inheritDoc}
     * @throws CoreTokenException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T getResults() throws CoreTokenException {
        try {
            Object value = syncQueue.poll(config.getQueueTimeout(), TimeUnit.SECONDS);
            if (value == null) {
                throw new CoreTokenException("Timed out whilst waiting for result");
            }

            // In case the value was null
            if (value.equals(NULL_SIGNAL)) {
                return null;
            }
            // In case there was an error
            if (value instanceof CoreTokenException) {
                throw (CoreTokenException) value;
            }

            return (T) value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreTokenException("Interrupted whilst waiting for a async operation", e);
        }
    }

    /**
     * @param result The result to store in this ResultHandler. May be null.
     * @throws IllegalStateException If there is already a value on the queue.
     */
    @Override
    public void processResults(T result) {
        Object addition = result == null ? NULL_SIGNAL : result;

        if (syncQueue.offer(addition)) {
            return;
        }
        throw new IllegalStateException("Cannot add multiple times.");
    }

    /**
     * @param error The error to store in this result handler.
     */
    public void processError(CoreTokenException error) {
        syncQueue.offer(error);
    }
}
