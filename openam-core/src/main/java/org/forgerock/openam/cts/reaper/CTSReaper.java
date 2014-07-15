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
package org.forgerock.openam.cts.reaper;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.time.StopWatch;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.reaper.ReaperQuery;
import org.forgerock.openam.cts.impl.query.reaper.ReaperQueryFactory;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Responsible for the scheduled deletion of expired Tokens.
 *
 * This implementation makes use of an LDAP specific concept of a paged query (that is,
 * one where results are returned in pages) which is performed against the persistent store
 * and the results are scheduled for deletion using the SDK provided asynchronous call.
 *
 * The LDAP SDK is responsible for managing the scheduling around asynchronous tasks and
 * as such simplifies the implementation to one that queries the results and delegates the
 * responsibility of deletion to the SDK.
 *
 * Once the search is complete, we need to wait for all asynchronous delete operations to
 * complete before we close the connection to the Directory. Otherwise we risk closing a
 * connection that has pending operations on it.
 *
 * This class is not responsible for scheduling and is expected to be scheduled according
 * to system configuration.
 *
 * Thread Policy: This runnable will respond to Thread interrupts and will exit cleanly
 * when interrupted.
 */
public class CTSReaper implements Runnable {
    // Injected
    private final TokenDeletion tokenDeletion;
    private final ReaperQueryFactory queryFactory;
    private final CTSReaperMonitoringStore monitoringStore;
    private final Debug debug;


    /**
     * Create an instance, but do not schedule the instance for execution.
     *
     * @param tokenDeletion Required for deleting tokens.
     * @param monitoringStore Required for monitoring reaper runs.
     * @param debug Required for debugging.
     */
    @Inject
    public CTSReaper(final ReaperQueryFactory queryFactory, final TokenDeletion tokenDeletion,
                     final CTSReaperMonitoringStore monitoringStore,
                     @Named(CoreTokenConstants.CTS_REAPER_DEBUG) final Debug debug) {
        this.queryFactory = queryFactory;
        this.tokenDeletion = tokenDeletion;
        this.monitoringStore = monitoringStore;
        this.debug = debug;
    }

    /**
     * Performs the query against the directory by selecting the Token IDs for all Tokens
     * that have expired. These Token IDs are then scheduled for deletion. The task will
     * not complete until all of the delete operations have returned.
     */
    public void run() {

        debug("Reaper starting");

        // Timers for debugging
        StopWatch query = new StopWatch();
        StopWatch waiting = new StopWatch();

        // Latches will track deletion of each page of results
        List<CountDownLatch> latches = new ArrayList<CountDownLatch>();

        ReaperQuery reaperQuery = queryFactory.getQuery();

        try {
            long total = 0;
            query.start();
            for (Collection<String> ids = reaperQuery.nextPage(); ids != null; ids = reaperQuery.nextPage()) {
                // If the thread has been interrupted, exit all processing.
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    debug("Interrupted, returning");
                    return;
                }

                total += ids.size();
                debug("Queried {0} tokens", Long.toString(total));

                // Latch will track the deletions of the page
                latches.add(tokenDeletion.deleteBatch(ids));
            }

            query.stop();
            waiting.start();

            debug("Expired Token Query Time: {0}ms", Long.toString(query.getTime()));

            // Wait stage
            for (CountDownLatch latch : latches) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            waiting.stop();
            monitoringStore.addReaperRun(query.getStartTime(), query.getTime() + waiting.getTime(), total);

            debug("Worker threads Time: {0}ms", Long.toString(waiting.getTime()));
        } catch (CoreTokenException e) {
            debug.error("CTS Reaper failed", e);
        }

        debug("Reaper complete");
    }

    private void debug(String msg, String... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER + "Reaper: " + msg,
                    args));
        }
    }
}
