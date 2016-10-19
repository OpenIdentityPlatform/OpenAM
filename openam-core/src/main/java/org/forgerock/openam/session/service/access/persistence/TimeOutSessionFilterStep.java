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

package org.forgerock.openam.session.service.access.persistence;

import static org.forgerock.openam.audit.AuditConstants.EventName.*;

import javax.inject.Inject;

import org.forgerock.openam.session.SessionEventType;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionState;

/**
 * Checks that sessions which are being read are not timed out. Silently consumes them if they are not.
 * This should be run after any caching, so that invalid sessions are not reread every time.
 */
public class TimeOutSessionFilterStep implements InternalSessionStoreStep {

    private final SessionNotificationSender sessionNotificationSender;
    private final SessionAuditor sessionAuditor;

    @Inject
    public TimeOutSessionFilterStep(SessionNotificationSender sessionNotificationSender, SessionAuditor sessionAuditor) {
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionAuditor = sessionAuditor;
    }

    @Override
    public InternalSession getBySessionID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getBySessionID(sessionID), next);
    }

    @Override
    public InternalSession getByHandle(String sessionHandle, InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getByHandle(sessionHandle), next);
    }

    @Override
    public InternalSession getByRestrictedID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getByRestrictedID(sessionID), next);
    }

    private InternalSession processInternalSession(InternalSession internalSession, InternalSessionStore next) throws SessionPersistenceException {
        if (internalSession == null || destroySessionIfNecessary(internalSession, next)) {
            return null; // If the session was destroyed, do not return it.
        }
        return internalSession;
    }

    /**
     * Checks whether the session should be destroyed or not, and if so performs the operation.
     */
    private boolean destroySessionIfNecessary(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        switch (session.checkSessionUpdate()) {
            case NO_CHANGE:
                return false;
            case DESTROY:
                next.remove(session.getSessionID());
                session.changeStateWithoutNotify(SessionState.DESTROYED);
                sessionNotificationSender.sendEvent(session, SessionEventType.DESTROY);
                return true;
            case MAX_TIMEOUT:
                session.changeStateAndNotify(SessionEventType.MAX_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                return false;
            case IDLE_TIMEOUT:
                session.changeStateAndNotify(SessionEventType.IDLE_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
                return false;
            default:
                return false;
        }
    }

    @Override
    public void store(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        next.store(session);
    }

    @Override
    public void remove(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        next.remove(sessionID);
    }
}
