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
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.operations.strategies;

import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_DESTROYED;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_LOGGED_OUT;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.blacklist.BlacklistException;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;

/**
 * Handles client-side sessions.
 *
 * @since 13.0.0
 */
public class StatelessOperations implements SessionOperations {
    private final SessionService sessionService;
    private final StatelessSessionManager statelessSessionManager;
    private final Blacklist<Session> sessionBlacklist;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;

    @Inject
    public StatelessOperations(final SessionService sessionService,
                               final StatelessSessionManager statelessSessionManager,
                               final Blacklist<Session> sessionBlacklist,
                               final SessionLogging sessionLogging,
                               final SessionAuditor sessionAuditor) {
        this.sessionService = sessionService;
        this.statelessSessionManager = statelessSessionManager;
        this.sessionBlacklist = sessionBlacklist;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
    }

    @Override
    public SessionInfo refresh(final Session session, final boolean reset) throws SessionException {
        final SessionInfo sessionInfo = statelessSessionManager.getSessionInfo(session.getID());
        if (sessionInfo.getExpiryTime(TimeUnit.MILLISECONDS) < currentTimeMillis()) {
            throw new SessionTimedOutException("Stateless session corresponding to client "
                    + sessionInfo.getClientID() + " timed out.");
        }
        return sessionInfo;
    }

    @Override
    public void logout(final Session session) throws SessionException {
        if (session instanceof StatelessSession) {
            SessionInfo sessionInfo = statelessSessionManager.getSessionInfo(session.getID());
            sessionLogging.logEvent(sessionInfo, SessionEvent.LOGOUT);
            // Required since not possible to mock SessionAuditor in test case
            if (sessionAuditor != null) {
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_LOGGED_OUT);
            }
        }
        try {
            sessionBlacklist.blacklist(session);
        } catch (BlacklistException e) {
            throw new SessionException(e);
        }
    }

    @Override
    public void destroy(final Session requester, final Session session) throws SessionException {
        sessionService.checkPermissionToDestroySession(requester, session.getID());
        if (session instanceof StatelessSession) {
            SessionInfo sessionInfo = statelessSessionManager.getSessionInfo(session.getID());
            sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
            // Required since not possible to mock SessionAuditor in test case
            if (sessionAuditor != null) {
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
            }
        }

        try {
            sessionBlacklist.blacklist(session);
        } catch (BlacklistException e) {
            throw new SessionException(e);
        }
    }

    @Override
    public void setProperty(final Session session, final String name, final String value) throws SessionException {
        // Nothing to do
    }

}
