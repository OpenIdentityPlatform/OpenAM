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
package org.forgerock.openam.cts.worker.process;

import static com.iplanet.dpro.session.SessionEvent.IDLE_TIMEOUT;
import static com.iplanet.dpro.session.SessionEvent.MAX_TIMEOUT;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_IDLE_TIMED_OUT;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_MAX_TIMED_OUT;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.inject.Provider;

import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.CountDownHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionState;

/**
 * Delegate for {@link SessionIdleTimeExpiredProcess} and {@link MaxSessionTimeExpiredProcess} which handles
 * the two stage process of changing the session CTS token state to DESTROYED and then performing logging and
 * notification if this succeeds.
 */
class SessionExpiryBatchHandler {

    private final TaskDispatcher queue;
    private final Provider<SessionAccessManager> sessionAccessManager;
    private final SessionAuditor sessionAuditor;
    private final int sessionEvent;
    private final EventName auditEvent;

    private SessionExpiryBatchHandler(
            final TaskDispatcher queue,
            final Provider<SessionAccessManager> sessionAccessManager,
            final SessionAuditor sessionAuditor,
            final int sessionEvent,
            final EventName auditEvent) {
        this.queue = queue;
        this.sessionAccessManager = sessionAccessManager;
        this.sessionAuditor = sessionAuditor;
        this.sessionEvent = sessionEvent;
        this.auditEvent = auditEvent;
    }

    /**
     * Creates delegate for {@link MaxSessionTimeExpiredProcess}.
     */
    static SessionExpiryBatchHandler forMaxSessionTimeExpired(
            final TaskDispatcher queue,
            final Provider<SessionAccessManager> accessManager,
            final SessionAuditor auditor) {
        return new SessionExpiryBatchHandler(queue, accessManager, auditor, MAX_TIMEOUT, AM_SESSION_MAX_TIMED_OUT);
    }

    /**
     * Creates delegate for {@link SessionIdleTimeExpiredProcess}.
     */
    static SessionExpiryBatchHandler forSessionIdleTimeExpired(
            final TaskDispatcher queue,
            final Provider<SessionAccessManager> accessManager,
            final SessionAuditor auditor) {
        return new SessionExpiryBatchHandler(queue, accessManager, auditor, IDLE_TIMEOUT, AM_SESSION_IDLE_TIMED_OUT);
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
        StateChangeResultHandler stateChangeResultHandler = new StateChangeResultHandler(sessionEvent, auditEvent, latch);

        for (PartialToken partialToken : tokenIds) {
            // Attempt to timeout session; if setting the session state succeeds, then
            // stateChangeResultHandler will log audit events and send notifications
            String tokenId = partialToken.getValue(CoreTokenField.TOKEN_ID);
            Token token = new Token(tokenId, TokenType.SESSION);
            token.setAttribute(CoreTokenField.ETAG, partialToken.getValue(CoreTokenField.ETAG));
            token.setAttribute(SessionTokenField.SESSION_STATE.getField(), SessionState.DESTROYED.toString());
            queue.update(token, stateChangeResultHandler);
        }
        return latch;
    }

    /**
     * {@link ResultHandler} which publishes session timeout to audit and notification systems and then
     * deletes the session from CTS.
     * <p>
     * Decrements a {@link CountDownLatch} so that a thread can await completion of a batch of results.
     */
    private class StateChangeResultHandler implements ResultHandler<Token, CoreTokenException> {

        private final int sessionEvent;
        private final EventName auditEvent;
        private final CountDownLatch latch;

        /**
         * Constructs a new {@link CountDownHandler}.
         *
         * @param sessionEvent Identifies the timeout that has occurred.
         * @param auditEvent Identifies the timeout that has occurred.
         * @param latch The {@link CountDownLatch} to update as results and errors are received.
         */
        private StateChangeResultHandler(
                final int sessionEvent,
                final EventName auditEvent,
                final CountDownLatch latch) {
            this.sessionEvent = sessionEvent;
            this.auditEvent = auditEvent;
            this.latch = latch;
        }

        @Override
        public Token getResults() throws CoreTokenException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void processResults(Token result) {
            try {
                String sessionId = result.getAttribute(SessionTokenField.SESSION_ID.getField());
                InternalSession session = sessionAccessManager.get().getInternalSession(new SessionID(sessionId));
                session.changeStateAndNotify(sessionEvent);
                sessionAuditor.auditActivity(session.toSessionInfo(), auditEvent);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void processError(Exception error) {
            latch.countDown();
        }

    }

}
