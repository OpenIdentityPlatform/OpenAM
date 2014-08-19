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
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.config.QueueConfiguration;

import java.text.MessageFormat;
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
    private static final String NULL_SIGNAL = "--NULL--";

    private final BlockingQueue<Object> syncQueue;
    private final QueueConfiguration config;
    private final Debug debug;

    /**
     * Creates an instance of the {@link ResultHandler} with a default queue.
     *
     * @param config Non null configuration required for timeout configuration.
     * @param debug Non null.
     */
    public AsyncResultHandler(QueueConfiguration config, Debug debug) {
        this(config, new ArrayBlockingQueue<Object>(1), debug);
    }

    /**
     * Test only constructor.
     *
     * @param config Non null configuration required for timeout configuration.
     * @param queue Custom queue implementation if required.
     * @param debug Non null.
     */
    AsyncResultHandler(QueueConfiguration config, BlockingQueue<Object> queue, Debug debug) {
        this.config = config;
        this.syncQueue = queue;
        this.debug = debug;
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
                debug("Results: <null>");
                return null;
            }
            // In case there was an error
            if (value instanceof CoreTokenException) {
                throw (CoreTokenException) value;
            }

            debug("Results: {0}", value.toString());
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
        debug("Received: results: {0}", result == null ? "<null>" : result.toString());
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
        debug("Received: Error {0}", error.getMessage());
        syncQueue.offer(error);
    }

    private void debug(String format, String... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
