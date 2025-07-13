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
 * Portions Copyrighted 2025 3A Systems, LLC.
 */
package org.forgerock.openam.cts.worker.process;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.time.StopWatch;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.worker.CTSWorkerFilter;
import org.forgerock.openam.cts.worker.CTSWorkerProcess;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

/**
 * {@link CTSWorkerProcess} base class handling repeated steps such as paging through query results,
 * thread interruption and waiting for each page of results to be processed.
 */
public abstract class CTSWorkerBaseProcess implements CTSWorkerProcess {

    @Override
    public final void handle(CTSWorkerQuery workerQuery, CTSWorkerFilter filter) {
        // Timers for debugging
        StopWatch queryStopWatch = new StopWatch();
        StopWatch waitingStopWatch = new StopWatch();

        long total = 0;
        waitingStopWatch.start();
        waitingStopWatch.suspend();
        queryStopWatch.start();

        try (CTSWorkerQuery query = workerQuery) {
            for (Collection<PartialToken> tokens = query.nextPage(); tokens != null; tokens = query.nextPage()) {

                // If the thread has been interrupted, exit all processing
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // filter and count results from this page
                Collection<PartialToken> filteredTokens = filter.filter(tokens);
                total += filteredTokens.size();
                queryStopWatch.suspend();

                // process the results; as handleBatch is an asynchronous call, await its completion
                // - retrieving and processing all results pages may cause an OutOfMemory error
                waitingStopWatch.resume();
                CountDownLatch latch = handleBatch(filteredTokens);
                latch.await();
                waitingStopWatch.suspend();

                queryStopWatch.resume();
            }
            queryStopWatch.stop();
            waitingStopWatch.stop();

            handleSucceeded(queryStopWatch, waitingStopWatch, total);
        } catch (CoreTokenException e) {
            handleFailed(e);
        } catch (InterruptedException e) {
            handleFailed(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Hook method allowing subclasses to define the actual work to be carried out by this {@link CTSWorkerProcess}.
     *
     * @param batch the filtered page of query results to be processed.
     * @return CountDownLatch which will open once the {@code batch} has been processed.
     * @throws CoreTokenException if an exception occurs while handling the batch.
     */
    protected abstract CountDownLatch handleBatch(Collection<PartialToken> batch) throws CoreTokenException;

    /**
     * Hook method called when {@link #handle} completes successfully.
     * <p>
     * This method can be overridden by subclasses for monitoring or debug logging.
     *
     * @param queryStopWatch timing of the query and task spawning step.
     * @param waitingStopWatch timing of the task completion wait time.
     * @param total number of query results which matched the filter and were processed.
     */
    protected abstract void handleSucceeded(StopWatch queryStopWatch, StopWatch waitingStopWatch, long total);

    /**
     * Hook method called when {@link #handle} fails to complete due to an error.
     * <p>
     * This method can be overridden by subclasses for debug logging.
     *
     * @param exception the exception which prevented the call to {@link #handle} from completing successfully.
     */
    protected abstract void handleFailed(Exception exception);

    @Override
    public String toString() {
        return getClass().getName();
    }

}
