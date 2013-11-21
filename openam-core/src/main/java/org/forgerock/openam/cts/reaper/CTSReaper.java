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
import org.forgerock.openam.utils.IOUtils;
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

/**
 * Responsible for the scheduled deletion of expired Tokens.
 *
 * This implementation consists a paged query (that is, one where results are returned
 * in pages) which is performed against the Directory and the results are scheduled for
 * deletion using the SDK provided asynchronous call.
 *
 * The LDAP SDK is responsible for managing the scheduling around asynchronous tasks and
 * as such simplifies the implementation to one that queries the results and delegates the
 * responsibility of deletion to the SDK.
 *
 * Once the search is complete, we need to wait for all asynchronous delete operations to
 * complete before we close the connection to the Directory. Otherwise we risk closing a
 * connection that has pending operations on it.
 *
 * This class is not responsible for scheduling. See the {@link CTSReaperWatchDog} instead.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CTSReaper implements Runnable {

    // Injected
    private final QueryFactory factory;
    private final CoreTokenConfig config;
    private final TokenDeletion tokenDeletion;
    private final Debug debug;

    private final Calendar calendar = Calendar.getInstance();

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

        try {
            // Iterate over the result pages
            while (iterator.hasNext()) {
                Collection<Entry> entries = iterator.next();
                total += entries.size();

                // If the thread has been interrupted, exit all processing.
                if (Thread.interrupted()) {
                    return;
                }

                // Latch will track the deletions of the page
                CountDownLatch latch = new CountDownLatch(entries.size());
                DeleteComplete complete = new DeleteComplete(latch);
                latches.add(latch);

                // Delete the tokens.
                try {
                    tokenDeletion.deleteBatch(entries, complete);
                } catch (ErrorResultException e) {
                    debug.error("Failed to get a connection, will retry later", e);
                    return;
                }

                if (debug.messageEnabled()) {
                    debug.message(MessageFormat.format(
                            CoreTokenConstants.DEBUG_HEADER +
                            "Reaper: Queried {0} Tokens",
                            total));
                }
            }

            query.stop();
            waiting.start();

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Reaper: Expired Token Query Time: {0}ms",
                        query.getTime()));
            }

            // Wait stage
            for (CountDownLatch latch : latches) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            waiting.stop();

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                                "Reaper: Worker threads Time: {0}ms",
                        waiting.getTime()));
            }
        } finally {
            // Once all latches are complete, close the TokenDeletion
            IOUtils.closeIfNotNull(tokenDeletion);
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
