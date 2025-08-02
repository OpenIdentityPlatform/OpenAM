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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package com.iplanet.dpro.session.operations.strategies;

import static com.iplanet.dpro.session.service.SessionState.INVALID;
import static org.forgerock.openam.session.SessionEventType.IDLE_TIMEOUT;
import static org.forgerock.openam.session.SessionEventType.MAX_TIMEOUT;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.text.MessageFormat;
import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.session.service.access.SessionQueryManager;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionEvent;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.InternalSessionListener;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SearchResults;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;

/**
 * Handles sessions that are stored in CTS.
 */
public class LocalOperations implements SessionOperations {

    private final Debug debug;
    private final SessionAccessManager sessionAccessManager;
    private final SessionQueryManager sessionQueryManager;
    private final SessionInfoFactory sessionInfoFactory;
    private final SessionServerConfig serverConfig;
    private final InternalSessionListener sessionEventBroker;
    private final SessionChangeAuthorizer sessionChangeAuthorizer;

    /**
     * Guice initialised constructor.
     * @param debug Non null.
     * @param sessionAccessManager access manager for returning the session information from the storage mechanism
     * @param sessionInfoFactory the factory for creating session information objects from a session
     * @param serverConfig session server config
     * @param internalSessionEventBroker observer of session events
     * @param sessionChangeAuthorizer class for verifying permissions and authorisation for the current user to
     *                                perform tasks on the session.  Used during deleting a session and getting access
     *
     */
    @Inject
    LocalOperations(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                    final SessionAccessManager sessionAccessManager,
                    final SessionQueryManager sessionQueryManager,
                    final SessionInfoFactory sessionInfoFactory,
                    final SessionServerConfig serverConfig,
                    final InternalSessionEventBroker internalSessionEventBroker,
                    final SessionChangeAuthorizer sessionChangeAuthorizer) {
        this.debug = debug;
        this.sessionAccessManager = sessionAccessManager;
        this.sessionQueryManager = sessionQueryManager;
        this.sessionInfoFactory = sessionInfoFactory;
        this.serverConfig = serverConfig;
        this.sessionEventBroker = internalSessionEventBroker;
        this.sessionChangeAuthorizer = sessionChangeAuthorizer;
    }

    /**
     * Fetches the SessionInfo from the SessionService.
     *
     * @param session The Session to update.
     * @param reset If true, then update the last modified timestamp of the Session.
     * @return Null if there was an error locating the Session, otherwise non null.
     */
    public SessionInfo refresh(Session session, boolean reset) throws SessionException {
        SessionID sessionID = session.getID();
        if (debug.messageEnabled()) {
            debug.message("Local fetch SessionInfo for {}\nReset: {}", sessionID.toString(), reset);
        }
        return getSessionInfo(sessionID, reset);
    }

    /**
     * Destroy the Session using the SessionService.
     *
     * @param requester {@inheritDoc}
     * @param session {@inheritDoc}
     * @throws SessionException {@inheritDoc}
     */
    @Override
    public void destroy(Session requester, Session session) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message("Local destroy for {}", session.getID().toString());
        }
        if (session == null) {
            return;
        }

        InternalSession internalSessionToDestroy = sessionAccessManager.getInternalSession(session.getSessionID());

        if (internalSessionToDestroy == null) {
            // let us check if the argument is a session handle
            internalSessionToDestroy = sessionAccessManager.getInternalSessionByHandle(
                    session.getSessionID().toString());
        }

        if (internalSessionToDestroy != null) {
            sessionChangeAuthorizer.checkPermissionToDestroySession(requester, internalSessionToDestroy.getSessionID());
            destroyInternalSession(internalSessionToDestroy);
        }
    }

    /**
     * Timeout the Internal Session.
     * <p>
     * Calling this method leads to the Internal Session's state being updated and both timeout
     * and destroy events being emitted.
     * <p>
     * However, as CTS Worker tasks are responsible for token deletion, this method will not
     * attempt to delete the session's CTS token from persistent storage.
     *
     * @param session The InternalSession to time out.
     * @param eventType The type of time out event (must be either {@link SessionEventType#MAX_TIMEOUT}
     *                  or {@link SessionEventType#IDLE_TIMEOUT}.
     */
    public void timeout(InternalSession session, SessionEventType eventType) {
        Reject.rejectStateIfTrue(session.isStored(),
                "Session should be deleted from persistent storage before being timed out.");
        Reject.ifFalse(eventType == IDLE_TIMEOUT || eventType == MAX_TIMEOUT,
                "Only time out events permitted.");
        long timedOutTimeInMillis = currentTimeMillis();
        session.setTimedOutTime(timedOutTimeInMillis);
        sessionEventBroker.onEvent(new InternalSessionEvent(session, eventType, timedOutTimeInMillis));
        signalRemove(session, SessionEventType.DESTROY, timedOutTimeInMillis);
    }

    /**
     * Destroy the Internal Session.
     *
     * @param session The InternalSession to destroy.
     */
    private void destroyInternalSession(InternalSession session) {
        sessionAccessManager.removeInternalSession(session);
        if (session != null && session.getState() != INVALID) {
            signalRemove(session, SessionEventType.DESTROY);
        }
        sessionAccessManager.removeSessionId(session.getSessionID());
    }

    /**
     * Sets the property using the SessionService.
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message("Local setProperty for {} {}={}", session.getID().toString(), name, value);
        }
        resolveToken(session.getID()).putProperty(name, value);
    }

    @Override
    public SessionInfo getSessionInfo(SessionID sessionID, boolean reset) throws SessionException {
        InternalSession session = resolveToken(sessionID);
        if (reset) {
            session.setLatestAccessTime();
        }
        return sessionInfoFactory.getSessionInfo(session, sessionID);
    }

    @Override
    public void addSessionListener(Session session, String url) throws SessionException {
        SessionID sessionId = session.getSessionID();
        InternalSession internalSession = resolveToken(sessionId);
        if (internalSession.getState() == INVALID) {
            throw new SessionException(SessionBundle.getString("invalidSessionState") + sessionId.toString());
        }
        if (!sessionId.equals(internalSession.getID()) && internalSession.getRestrictionForToken(sessionId) == null) {
            throw new IllegalArgumentException("Session id mismatch");
        }
        internalSession.addSessionEventURL(url, sessionId);
    }

    @Override
    public boolean checkSessionExists(SessionID sessionId) throws SessionException {
        // Attempt to load the session. If one is found, the InternalSesion is now local.
        return sessionAccessManager.getInternalSession(sessionId) != null;
    }

    /**
     * As opposed to locateSession() this one accepts normal or restricted token
     * This is expected to be only called once the session is detected as local
     *
     * @param token
     * @return
     */
    private InternalSession resolveToken(SessionID token) throws SessionException {
        InternalSession sess = sessionAccessManager.getInternalSession(token);
        if (sess == null) {
            sess = resolveRestrictedToken(token, true);
        }
        if (sess == null) {
            throw new InvalidSessionIdException(token);
        }
        return sess;
    }

    private InternalSession resolveRestrictedToken(SessionID token,
                                                   boolean checkRestriction) throws SessionException {
        InternalSession session = sessionAccessManager.getByRestrictedID(token);
        if (session == null) {
            return null;
        }
        if (checkRestriction) {
            try {
                TokenRestriction restriction = session.getRestrictionForToken(token);
                if (restriction != null && !restriction.isSatisfied(RestrictedTokenContext.getCurrent())) {
                    throw new SessionException(SessionBundle.rbName, "restrictionViolation", null);
                }
            } catch (SessionException se) {
                throw se;
            } catch (Exception e) {
                throw new SessionException(e);
            }
        }
        return session;
    }

    /**
     * Returns the restricted token
     *
     * @param masterSessionId   master session id
     * @param restriction TokenRestriction Object
     * @return restricted token id
     * @throws SessionException
     */
    public String getRestrictedTokenId(final SessionID masterSessionId,
                                       final TokenRestriction restriction) throws SessionException {
        // locate master session
        InternalSession session = sessionAccessManager.getInternalSession(masterSessionId);

        if (session == null) {
            throw new InvalidSessionIdException(masterSessionId);
        }
        sessionInfoFactory.validateSession(session, masterSessionId);
        // attempt to reuse the token if restriction is the same
        SessionID restrictedSessionID = session.getRestrictedTokenForRestriction(restriction);
        if (restrictedSessionID == null) {
            SessionID generatedRestrictedSessionID = session.getID().generateRelatedSessionID(serverConfig);
            restrictedSessionID = session.addRestrictedToken(generatedRestrictedSessionID, restriction);
        }
        return restrictedSessionID.toString();
    }

    @Override
    public String deferenceRestrictedID(Session session, SessionID restrictedID) throws SessionException {
        return resolveRestrictedToken(restrictedID, false).getID().toString();
    }

    @Override
    public void setExternalProperty(SSOToken clientToken, SessionID sessionId, String name, String value) throws SessionException {
        resolveToken(sessionId).putExternalProperty(clientToken, name, value);
    }

    @Override
    public void logout(Session session) throws SessionException {
        SessionID sessionId = session.getSessionID();
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format("Local logout for {0}", sessionId.toString()));
        }
        if (sessionId == null || sessionId.isSessionHandle()) {
            throw new InvalidSessionIdException(sessionId);
        }
        //if the provided session ID was a restricted token, resolveToken will always validate the restriction, so there is no
        //need to check restrictions here.
        InternalSession internalSession = resolveToken(sessionId);
        logoutInternalSession(internalSession);
    }

    @Override
    public Session resolveSession(SessionID sessionID) throws SessionException {
        return sessionAccessManager.getSession(sessionID);
    }

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    @Override
    public SearchResults<SessionInfo> getValidSessions(Session s, String pattern) throws SessionException {
        return sessionQueryManager.getValidSessions(s, pattern);
    }

    @Override
    public Collection<PartialSession> getMatchingSessions(CrestQuery crestQuery) throws SessionException {
        return sessionQueryManager.getMatchingValidSessions(crestQuery);
    }

    private void logoutInternalSession(final InternalSession session) {
        sessionAccessManager.removeInternalSession(session);
        if (session != null && session.getState() != INVALID) {
            signalRemove(session, SessionEventType.LOGOUT);
        }
    }

    /**
     * Simplifies the signalling that a Session has been removed.
     * @param session Non null InternalSession.
     * @param eventType An integrate from the SessionEvent class.
     */
    private void signalRemove(InternalSession session, SessionEventType eventType) {
        signalRemove(session, eventType, Time.currentTimeMillis());
    }

    private void signalRemove(InternalSession session, SessionEventType eventType, long eventTime) {
        session.setState(SessionState.DESTROYED);
        sessionEventBroker.onEvent(new InternalSessionEvent(session, eventType, eventTime));
    }
}
