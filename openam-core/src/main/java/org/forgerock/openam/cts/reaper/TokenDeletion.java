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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.reaper;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * Deletes batches of Token IDs from the persistence layer.
 *
 * This class manages the detail of both the triggering the deletes and also collecting
 * up the responses to ensure that the operation has been processed asynchronously.
 */
public class TokenDeletion {
    // Injected
    private final TaskDispatcher queue;

    @Inject
    public TokenDeletion(TaskDispatcher queue) {
        this.queue = queue;
    }

    /**
     * Performs a delete against a batch of Token IDs in the search results.
     *
     * This function will defer to the {@link TaskDispatcher} for deletion requests.
     *
     * @param tokens The Token ID's to delete.
     *
     * @return CountDownLatch A CountDownLatch which can be blocked on to ensure that
     * the delete tasks have been completed.
     *
     * @throws CoreTokenException If there was any problem queuing the delete operation.
     */
    public CountDownLatch deleteBatch(Collection<String> tokens) throws CoreTokenException {
        CountDownLatch latch = new CountDownLatch(tokens.size());
        ResultHandler<String> handler = new CountdownHandler(latch);
        for (String token : tokens) {
            queue.delete(token, handler);
        }
        return latch;
    }

    /**
     * Internal implementation to ensure that delete operations count down the latch.
     */
    private static class CountdownHandler implements ResultHandler<String> {
        private final CountDownLatch latch;
        public CountdownHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String getResults() throws CoreTokenException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void processResults(String result) {
            latch.countDown();
        }

        @Override
        public void processError(CoreTokenException error) {
            latch.countDown();
        }
    }
}
