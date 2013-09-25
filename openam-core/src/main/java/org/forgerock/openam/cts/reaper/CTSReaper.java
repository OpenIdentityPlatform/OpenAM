/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.reaper;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.time.StopWatch;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryPageIterator;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.responses.Result;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for managing the scheduled deletion of expired Tokens.
 *
 * This implementation consists of a runnable task which can be scheduled to run at a regular
 * interval. A paged query (that is, one where results are returned in pages) is performed
 * against the Directory and the results are scheduled for deletion using the SDK provided
 * asynchronous call.
 *
 * The LDAP SDK is responsible for managing the scheduling around asynchronous tasks and
 * as such simplifies the implementation to one that queries the results and queues them
 * for deletion.
 *
 * The final stage in the search is to wait to ensure that the results of the deletion process
 * have completed before allowing the runnable task to complete. This ensures that the next
 * time this query runs, it will cover the next valid set of expired tokens.
 *
 * This scheduled task can be shutdown using the shutdown command.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CTSReaper implements Runnable {
    public static final String CTS_SCHEDULED_SERVICE = "CTSScheduledService";

    // Injected
    private final QueryFactory factory;
    private final CoreTokenConfig config;
    private final TokenDeletion tokenDeletion;
    private final Debug debug;

    private final Calendar calendar = Calendar.getInstance();
    private ScheduledFuture<?> scheduledFuture;

    /**
     * Create an instance, but do not schedule the instance for execution.
     *
     * @param factory Required for generating queries against the directory.
     * @param config Required for providing runtime configuration.
     * @param tokenDeletion Required for deleting tokens.
     */
    @Inject
    public CTSReaper(QueryFactory factory, CoreTokenConfig config,
                     TokenDeletion tokenDeletion, @Named(CoreTokenConstants.CTS_REAPER_DEBUG) Debug debug) {
        this.factory = factory;
        this.config = config;
        this.tokenDeletion = tokenDeletion;
        this.debug = debug;
    }

    /**
     * Schedule the runnable task with the executor service.
     *
     * @param service Non null ScheduledExecutorService.
     */
    public void startup(ScheduledExecutorService service) {
        if (scheduledFuture != null) {
            throw new IllegalStateException("Previous scheduling has not been shutdown.");
        }

        scheduledFuture = service.scheduleAtFixedRate(this, 0, config.getRunPeriod(), TimeUnit.MILLISECONDS);
    }

    /**
     * Signal that the running task should be cancelled. If it is running, it will be interrupted.
     */
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    /**
     * Performs the query against the directory by selecting the Token IDs for all Tokens
     * that have expired. These Token IDs are then scheduled for deletion. The task will
     * not complete until all of the delete operations have returned.
     */
    public void run() {
        // Timers for debugging
        StopWatch query = new StopWatch();
        StopWatch waiting = new StopWatch();

        // Latches will track each page of results
        List<CountDownLatch> latches = new ArrayList<CountDownLatch>();

        // Create the query against the directory
        calendar.setTimeInMillis(System.currentTimeMillis());
        Filter expired = factory.createFilter().and().beforeDate(calendar).build();
        QueryBuilder queryBuilder = factory.createInstance()
                .withFilter(expired)
                .returnTheseAttributes(CoreTokenField.TOKEN_ID);
        QueryPageIterator iterator = new QueryPageIterator(queryBuilder, config.getCleanupPageSize());

        query.start();
        long total = 0;

        // Iterate over the result pages
        while (iterator.hasNext()) {
            Collection<Entry> entries = iterator.next();
            total += entries.size();

            // If the thread has been interrupted, exit all processing.
            if (Thread.interrupted()) return;

            // Latch will track the deletions of the page
            CountDownLatch latch = new CountDownLatch(entries.size());
            DeleteComplete complete = new DeleteComplete(latch);
            latches.add(latch);

            // Delete the tokens.
            tokenDeletion.deleteBatch(entries, complete);

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Reaper2: Queried {0} Tokens",
                        total));
            }
        }

        query.stop();
        waiting.start();

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Reaper2: Expired Token Query Time: {0}ms",
                    query.getTime()));
        }

        // Wait stage
        while (!latches.isEmpty()) {
            CountDownLatch latch = latches.remove(0);
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        waiting.stop();

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Reaper2: Worker threads Time: {0}ms",
                    waiting.getTime()));
        }
    }

    /**
     * DeleteComplete implements the standard LDAP ResultHandler and will indicate that the
     * request has completed or failed. In either case we are only interested in decrementing
     * the provided CountDownLatch.
     */
    private class DeleteComplete implements ResultHandler<Result> {
        private final CountDownLatch latch;

        private DeleteComplete(CountDownLatch latch) {
            this.latch = latch;
        }

        public void handleErrorResult(ErrorResultException e) {
            debug.error("Failed to delete Token", e);
            latch.countDown();
        }

        public void handleResult(Result result) {
            latch.countDown();
        }
    }
}
