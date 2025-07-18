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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.worker.process;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.worker.CTSWorkerFilter;
import org.forgerock.openam.cts.worker.CTSWorkerTask;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.CountDownHandler;
import org.forgerock.openam.tokens.CoreTokenField;

import com.sun.identity.shared.debug.Debug;

/**
 * A process which defines the deletion of tokens returned from a CTS worker query, having applied the provided
 * filter to them. This process is monitored, and will add reaper run information to the monitoring store as
 * appropriate.
 */
public class CTSWorkerDeleteProcess extends CTSWorkerBaseProcess {

    private TokenDeletion tokenDeletion;
    private CTSReaperMonitoringStore monitoringStore;
    private Debug debug;

    /**
     * Generates a new {@link CTSWorkerDeleteProcess} which can be used across multiple {@link CTSWorkerTask},
     * with various {@link CTSWorkerQuery} and {@link CTSWorkerFilter}.
     *
     * @param tokenDeletion Batch deletion of tokens utility.
     * @param monitoringStore Utility to record monitoring information.
     * @param debug Debug output.
     */
    @Inject
    public CTSWorkerDeleteProcess(TokenDeletion tokenDeletion,
                                  CTSReaperMonitoringStore monitoringStore,
                                  @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.tokenDeletion = tokenDeletion;
        this.monitoringStore = monitoringStore;
        this.debug = debug;
    }

    @Override
    protected CountDownLatch handleBatch(final Collection<PartialToken> batch) throws CoreTokenException {
        return tokenDeletion.deleteBatch(batch);
    }

    @Override
    protected void handleSucceeded(final StopWatch queryStopWatch, final StopWatch waitingStopWatch, final long total) {
        monitoringStore.addReaperRun(queryStopWatch.getStartTime(),
                queryStopWatch.getTime() + waitingStopWatch.getTime(), total);
        debug.message("Worker threads Time: {0}ms", Long.toString(waitingStopWatch.getTime()));
    }

    @Override
    protected void handleFailed(final Exception exception) {
        debug.error("Reaper Delete Process failed", exception);
    }

    /**
     * Deletes batches of Token IDs from the persistence layer.
     *
     * This class manages the detail of both the triggering the deletes and also collecting
     * up the responses to ensure that the operation has been processed asynchronously.
     */
    public static class TokenDeletion {

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
         * @param tokens PartialToken objects containing the IDs of the tokens to delete.
         *
         * @return CountDownLatch A CountDownLatch which can be blocked on to ensure that
         * the delete tasks have been completed.
         *
         * @throws CoreTokenException If there was any problem queuing the delete operation.
         */
        public CountDownLatch deleteBatch(Collection<PartialToken> tokens) throws CoreTokenException {
            CountDownLatch latch = new CountDownLatch(tokens.size());
            ResultHandler<PartialToken, CoreTokenException> handler = new CountDownHandler<>(latch);
            for (PartialToken token : tokens) {
                String tokenId = token.getValue(CoreTokenField.TOKEN_ID);
                queue.delete(tokenId, handler);
            }
            return latch;
        }
    }
}
