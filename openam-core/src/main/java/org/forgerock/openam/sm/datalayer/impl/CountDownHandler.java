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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.sm.datalayer.impl;

import java.util.concurrent.CountDownLatch;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;

/**
 * {@link ResultHandler} which decrements a {@link CountDownLatch} so that a thread can await completion
 * of a batch of results.
 *
 * @param <T> The type of results passed to {@link #processResults}.
 */
public class CountDownHandler<T> implements ResultHandler<T, CoreTokenException> {

    private final CountDownLatch latch;

    /**
     * Constructs a new {@link CountDownHandler}.
     *
     * @param latch The {@link CountDownLatch} to update as results and errors are received.
     */
    public CountDownHandler(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public T getResults() throws CoreTokenException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processResults(T result) {
        latch.countDown();
    }

    @Override
    public void processError(Exception error) {
        latch.countDown();
    }

}
