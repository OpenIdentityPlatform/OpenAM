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

import org.forgerock.openam.cts.exceptions.CoreTokenException;

/**
 * ResultHandler is responsible for providing a mechanism of allowing access to the result
 * of an asynchronous operation.
 *
 * In particular, this interface is generic to allow it to handle the particular return types
 * of the potentially synchronous operations of the CTS.
 *
 * This ResultHandler is intended to only handle a single result. Therefore a new instance
 * should be created for each request.
 *
 * @see org.forgerock.openam.cts.impl.queue.ResultHandlerFactory
 *
 * @param <T> The processed result that will be passed to the implementation and returned
 *           in the {@link #getResults()} method.
 */
public interface ResultHandler<T> {
    /**
     * Get the results from the ResultHandler.
     *
     * @return A possibly null, result of type T.
     *
     * @throws CoreTokenException If processing the task caused an error, then this error will be thrown.
     * Also thrown if the caller is blocked for too long waiting for this task.
     */
    public T getResults() throws CoreTokenException;

    /**
     * @param result The result to store in this ResultHandler.
     */
    public void processResults(T result);

    /**
     * @param error The error to store in this result handler.
     */
    public void processError(CoreTokenException error);
}
