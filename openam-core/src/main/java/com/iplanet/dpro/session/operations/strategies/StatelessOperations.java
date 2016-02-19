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
import static org.forgerock.openam.utils.Time.*;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import org.forgerock.openam.session.blacklist.SessionBlacklist;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionFactory;

import javax.inject.Inject;

/**
 * Handles client-side sessions.
 *
 * @since 13.0.0
 */
public class StatelessOperations implements SessionOperations {
    private final SessionOperations localOperations;
    private final SessionService sessionService;
    private final StatelessSessionFactory statelessSessionFactory;
    private final SessionBlacklist sessionBlacklist;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;

    @Inject
    public StatelessOperations(final LocalOperations localOperations,
                               final SessionService sessionService,
                               final StatelessSessionFactory statelessSessionFactory,
                               final SessionBlacklist sessionBlacklist,
                               final SessionLogging sessionLogging,
                               final SessionAuditor sessionAuditor) {
        this.localOperations = localOperations;
        this.sessionService = sessionService;
        this.statelessSessionFactory = statelessSessionFactory;
        this.sessionBlacklist = sessionBlacklist;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
    }

    @Override
    public SessionInfo refresh(final Session session, final boolean reset) throws SessionException {
        final SessionInfo sessionInfo = statelessSessionFactory.getSessionInfo(session.getID());
        if (sessionInfo.getExpiryTime() < currentTimeMillis()) {
            throw new SessionTimedOutException("Stateless session corresponding to client "
                    + sessionInfo.getClientID() + " timed out.");
        }
        return sessionInfo;
    }

    @Override
    public void logout(final Session session) throws SessionException {
        if (session instanceof StatelessSession) {
            SessionInfo sessionInfo = statelessSessionFactory.getSessionInfo(session.getID());
            sessionLogging.logEvent(sessionInfo, SessionEvent.LOGOUT);
            // Required since not possible to mock SessionAuditor in test case
            if (sessionAuditor != null) {
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_LOGGED_OUT);
            }
        }
        sessionBlacklist.blacklist(session);
    }

    @Override
    public void destroy(final Session requester, final Session session) throws SessionException {
        sessionService.checkPermissionToDestroySession(requester, session.getID());
        if (session instanceof StatelessSession) {
            SessionInfo sessionInfo = statelessSessionFactory.getSessionInfo(session.getID());
            sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
            // Required since not possible to mock SessionAuditor in test case
            if (sessionAuditor != null) {
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
            }
        }

        sessionBlacklist.blacklist(session);
    }

    @Override
    public void setProperty(final Session session, final String name, final String value) throws SessionException {
        localOperations.setProperty(session, name, value);
    }

}
