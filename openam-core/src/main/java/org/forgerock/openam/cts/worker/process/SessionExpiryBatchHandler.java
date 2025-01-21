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

import static org.forgerock.openam.session.SessionEventType.IDLE_TIMEOUT;
import static org.forgerock.openam.session.SessionEventType.MAX_TIMEOUT;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.inject.Module;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.sm.datalayer.api.OptimisticConcurrencyCheckFailedException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.CountDownHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionConstraint;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.session.util.SessionUtilsWrapper;
import com.sun.identity.shared.debug.Debug;

/**
 * Delegate for {@link SessionIdleTimeExpiredProcess} and {@link MaxSessionTimeExpiredProcess}
 * which handles the two stage process of attempting to delete the session CTS token and then
 * processing the logout only if this low-level operation succeeds.
 */
class SessionExpiryBatchHandler {

    private final TaskDispatcher queue;
    private final SessionEventType sessionEventType;
    private final StateChangeResultHandlerFactory stateChangeResultHandlerFactory;

    @Inject
    public SessionExpiryBatchHandler(
            TaskDispatcher queue,
            SessionEventType sessionEventType,
            StateChangeResultHandlerFactory stateChangeResultHandlerFactory) {
        this.queue = queue;
        this.sessionEventType = sessionEventType;
        this.stateChangeResultHandlerFactory = stateChangeResultHandlerFactory;
    }

    /**
     * Performs a timeout against a batch of Token IDs in the search results.
     *
     * This function will defer to the {@link TaskDispatcher} for update requests.
     *
     * @param tokenIds PartialToken objects containing the IDs of the tokens to timeout.
     *
     * @return CountDownLatch A CountDownLatch which can be blocked on to ensure that
     * the tasks have been completed.
     *
     * @throws CoreTokenException If there was any problem queuing the update operation.
     */
    CountDownLatch timeoutBatch(Collection<PartialToken> tokenIds) throws CoreTokenException {
        CountDownLatch latch = new CountDownLatch(tokenIds.size());
        StateChangeResultHandler stateChangeResultHandler = stateChangeResultHandlerFactory.create(sessionEventType,
                latch);

        for (PartialToken partialToken : tokenIds) {
            // Ensure session only gets timed out once by first attempting to delete the CTS token
            String tokenId = partialToken.getValue(CoreTokenField.TOKEN_ID);
            Options options = Options.defaultOptions().set(CTSOptions.PRE_DELETE_READ_OPTION, CoreTokenField.values());
            if (sessionEventType == IDLE_TIMEOUT) {
                // Allow idle timeout to be aborted if another process updates the token
                String etag = partialToken.getValue(CoreTokenField.ETAG);
                options.set(CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION, etag);
            }
            queue.delete(tokenId, options, stateChangeResultHandler);
        }
        return latch;
    }

    /**
     * {@link ResultHandler} which publishes session timeout {@link SessionEventType}.
     * <p>
     * Also, decrements a {@link CountDownLatch} as processing of each token completes
     * so that another thread can await completion of a batch of results.
     */
    static class StateChangeResultHandler implements ResultHandler<PartialToken, CoreTokenException> {

        private final Debug debug;
        private final SessionAdapter sessionAdapter;
        private final LocalOperations localOperations;
        private final SessionEventType sessionEventType;
        private final CountDownLatch countDownLatch;
        private final SessionService sessionService;
        private final SessionServiceConfig sessionServiceConfig;
        private final InternalSessionEventBroker internalSessionEventBroker;
        private final SessionUtilsWrapper sessionUtilsWrapper;
        private final SessionConstraint sessionConstraint;

        /**
         * Constructs a new {@link CountDownHandler}.
         *
         * @param debug Required for debug logging
         * @param sessionAdapter Required for adapting CTS {@link Token} to its {@link InternalSession} object.
         * @param localOperations Required for notifying the system that the {@link InternalSession} has timed out.
         * @param sessionEventType Identifies the type of timeout that has occurred.
         * @param countDownLatch The {@link CountDownLatch} to update as results and errors are received.
         * @param sessionService transitive dependency required by {@link InternalSession}.
         * @param sessionServiceConfig transitive dependency required by {@link InternalSession}.
         * @param internalSessionEventBroker transitive dependency required by {@link InternalSession}.
         * @param sessionUtilsWrapper transitive dependency required by {@link InternalSession}.
         * @param sessionConstraint transitive dependency required by {@link InternalSession}.
         */
        @Inject
        public StateChangeResultHandler(
                @Named(CoreTokenConstants.CTS_DEBUG) Debug debug,
                SessionAdapter sessionAdapter,
                LocalOperations localOperations,
                @Assisted final SessionEventType sessionEventType,
                @Assisted final CountDownLatch countDownLatch,
                SessionService sessionService,
                SessionServiceConfig sessionServiceConfig,
                InternalSessionEventBroker internalSessionEventBroker,
                SessionUtilsWrapper sessionUtilsWrapper,
                SessionConstraint sessionConstraint) {

            Reject.ifFalse(sessionEventType == IDLE_TIMEOUT || sessionEventType == MAX_TIMEOUT);

            this.debug = debug;
            this.sessionAdapter = sessionAdapter;
            this.localOperations = localOperations;
            this.sessionEventType = sessionEventType;
            this.countDownLatch = countDownLatch;
            this.sessionService = sessionService;
            this.sessionServiceConfig = sessionServiceConfig;
            this.internalSessionEventBroker = internalSessionEventBroker;
            this.sessionUtilsWrapper = sessionUtilsWrapper;
            this.sessionConstraint = sessionConstraint;
        }

        @Override
        public PartialToken getResults() throws CoreTokenException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void processResults(PartialToken result) {
            try {
                if (result==null || !result.canConvertToToken()) {
                    debug.message("Failed to delete token. Already deleted.");
                    return;
                }
                Token token = result.toToken();
                InternalSession internalSession = sessionAdapter.fromToken(token);
                internalSession.setSessionServiceDependencies(
                        sessionService, sessionServiceConfig, internalSessionEventBroker, sessionUtilsWrapper,
                        sessionConstraint, debug);
                localOperations.timeout(internalSession, sessionEventType);
            } finally {
                countDownLatch.countDown();
            }
        }

        @Override
        public void processError(Exception error) {
            countDownLatch.countDown();
            if (error instanceof OptimisticConcurrencyCheckFailedException) {
                debug.message("Failed to delete token with expired timeout. {}", error.getMessage(), error);
            } else {
                debug.error("Failed to delete token with expired timeout. {}", error.getMessage(), error);
            }
        }

    }

    /**
     * Factory interface to backed by Guice provided implementation.
     * <p>
     * Guice will inject all other constructor parameters for {@link StateChangeResultHandler} and
     * use the provided {@link SessionEventType} and {@link CountDownLatch}.
     *
     * @see CTSWorkerProcessGuiceModule#install(Module)
     */
    interface StateChangeResultHandlerFactory {

        /**
         * Create a new {@link StateChangeResultHandler} with additional constructor dependencies provided by Guice.
         *
         * @param eventType The type of timeout to be processed.
         * @param latch The {@link CountDownLatch} to decrement as each result or error is processed by the handler.
         * @return A new {@link StateChangeResultHandler}.
         */
        StateChangeResultHandler create(SessionEventType eventType, CountDownLatch latch);
    }

}
