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
package org.forgerock.openam.cts.worker.process.deletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.time.StopWatch;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.worker.CTSWorkerFilter;
import org.forgerock.openam.cts.worker.CTSWorkerProcess;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

/**
 * A process which defines the deletion of tokens returned from a reaper query, having applied the provided
 * filter to them. This process is monitored, and will add reaper run information to the monitoring store as
 * appropriate.
 */
public class CTSWorkerDeleteProcess implements CTSWorkerProcess {

    private TokenDeletion tokenDeletion;
    private CTSReaperMonitoringStore monitoringStore;
    private Debug debug;

    /**
     * Generates a new {@link CTSWorkerDeleteProcess} which can be used across multiple ReaperTasks, with various
     * ReaperQueries and ReaperFilters.
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
    public void handle(CTSWorkerQuery query, CTSWorkerFilter filter) {
        // Timers for debugging
        StopWatch queryStopWatch = new StopWatch();
        StopWatch waitingStopWatch = new StopWatch();

        // Latches will track deletion of each page of results
        List<CountDownLatch> latches = new ArrayList<>();

        try {
            long total = 0;
            queryStopWatch.start();
            for (Collection<PartialToken> tokens = query.nextPage(); tokens != null; tokens = query.nextPage()) {

                 Collection<PartialToken> filteredTokens = filter.filter(tokens);

                // If the thread has been interrupted, exit all processing.
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }

                total += filteredTokens.size();

                // Latch will track the deletions of the page
                latches.add(tokenDeletion.deleteBatch(filteredTokens));
            }

            queryStopWatch.stop();
            waitingStopWatch.start();

            // Wait stage
            for (CountDownLatch latch : latches) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            waitingStopWatch.stop();
            monitoringStore.addReaperRun(queryStopWatch.getStartTime(),
                    queryStopWatch.getTime() + waitingStopWatch.getTime(), total);

            debug.message("Worker threads Time: {0}ms", Long.toString(waitingStopWatch.getTime()));
        } catch (CoreTokenException e) {
            debug.error("Reaper Delete Process failed", e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

}
