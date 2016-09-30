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
 */
package com.iplanet.dpro.session.operations.strategies;

import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.session.SessionConstants.*;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.forgerock.openam.session.service.SessionAccessManager;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for applying Session operations on the local Server instance.
 *
 * This will be based on invoking the {@link SessionService} directly. This implementation
 * has been refactored out from {@link Session}.
 */
public class LocalOperations implements SessionOperations {

    private final Debug debug;
    private final SessionAccessManager sessionAccessManager;
    private final SessionInfoFactory sessionInfoFactory;
    private final ServicesClusterMonitorHandler servicesClusterMonitorHandler;
    private final SessionServerConfig serverConfig;
    private final TokenIdFactory tokenIdFactory;
    private final CTSPersistentStore coreTokenService;
    private final SessionAdapter tokenAdapter;
    private final SessionNotificationSender sessionNotificationSender;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;
    private final SessionChangeAuthorizer sessionChangeAuthorizer;

    /**
     * Guice initialised constructor.
     * @param debug Non null.
     * @param sessionAccessManager access manager for returning the session information from the storage mechanism
     * @param sessionInfoFactory the factory for creating session information objects from a session
     * @param servicesClusterMonitorHandler handler for dealing with server cluster monitoring related operations
     * @param serverConfig session server config
     * @param tokenIdFactory token id factory
     * @param coreTokenService core token service
     * @param tokenAdapter session adapter
     * @param sessionNotificationSender notification sender for session removal notification
     * @param sessionLogging special logging service for session removal logging
     * @param sessionAuditor audit logger
     * @param sessionChangeAuthorizer class for verifying permissions and authorisation for the current user to
     *                                perform tasks on the session.  Used during deleting a session and getting access
     *                                to session info.
     *
     */
    @Inject
    LocalOperations(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                    final SessionAccessManager sessionAccessManager,
                    final SessionInfoFactory sessionInfoFactory,
                    final ServicesClusterMonitorHandler servicesClusterMonitorHandler,
                    final SessionServerConfig serverConfig,
                    final TokenIdFactory tokenIdFactory,
                    final CTSPersistentStore coreTokenService,
                    final SessionAdapter tokenAdapter,
                    final SessionNotificationSender sessionNotificationSender,
                    final SessionLogging sessionLogging,
                    final SessionAuditor sessionAuditor,
                    final SessionChangeAuthorizer sessionChangeAuthorizer) {
        this.debug = debug;
        this.sessionAccessManager = sessionAccessManager;
        this.sessionInfoFactory = sessionInfoFactory;
        this.servicesClusterMonitorHandler = servicesClusterMonitorHandler;
        this.serverConfig = serverConfig;
        this.tokenIdFactory = tokenIdFactory;
        this.coreTokenService = coreTokenService;
        this.tokenAdapter = tokenAdapter;
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
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
            debug.message(MessageFormat.format(
                    "Local fetch SessionInfo for {0}\n" +
                            "Reset: {1}",
                    sessionID.toString(),
                    reset));
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
            debug.message(MessageFormat.format("Local destroy for {0}", session.getID().toString()));
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
            sessionChangeAuthorizer.checkPermissionToDestroySession(requester, session.getSessionID());
            destroyInternalSession(session.getSessionID());
        }
    }

    /**
     * Destroy a Internal Session, whose session id has been specified.
     *
     * @param sessionID
     */
    private void destroyInternalSession(SessionID sessionID) {
        InternalSession sess = sessionAccessManager.removeInternalSession(sessionID);
        if (sess != null && sess.getState() != INVALID) {
            signalRemove(sess, SessionEvent.DESTROY);
            sessionAuditor.auditActivity(sess.toSessionInfo(), AM_SESSION_DESTROYED);
        }
        sessionAccessManager.removeSessionId(sessionID);
    }

    /**
     * Sets the property using the SessionService.
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Local setProperty for {0} {1}={2}",
                    session.getID().toString(),
                    name,
                    value));
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
    public void addSessionListener(SessionID sessionId, String url) throws SessionException {
        InternalSession session = resolveToken(sessionId);
        if (session.getState() == INVALID) {
            throw new SessionException(SessionBundle.getString("invalidSessionState") + sessionId.toString());
        }
        if (!sessionId.equals(session.getID()) && session.getRestrictionForToken(sessionId) == null) {
            throw new IllegalArgumentException("Session id mismatch");
        }
        session.addSessionEventURL(url, sessionId);
    }

    @Override
    public boolean checkSessionLocal(SessionID sessionId) throws SessionException {
        // Attempt to load the session. If one is found, the InternalSesion is now local.
        return sessionAccessManager.getInternalSession(sessionId) != null;
    }

    /**
     * This method checks if Internal session is already present locally
     *
     * @param sessionId
     * @return a boolean
     */
    private boolean isSessionPresent(SessionID sessionId) {
        return sessionAccessManager.isInternalSessionPresentInCache(sessionId);
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
            throw new SessionException(SessionBundle.getString("invalidSessionID") + token.toString());
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
            throw new SessionException(SessionBundle.getString("invalidSessionID") + masterSessionId);
        }
        sessionInfoFactory.validateSession(session, masterSessionId);
        // attempt to reuse the token if restriction is the same
        SessionID restrictedSessionID = session.getRestrictedTokenForRestriction(restriction);
        if (restrictedSessionID == null) {
            restrictedSessionID = session.getID().generateRelatedSessionID(serverConfig);
            SessionID previousValue = session.addRestrictedToken(restrictedSessionID, restriction);
            if (previousValue == null) {
                sessionAccessManager.putInternalSessionIntoInternalSessionCache(session);
            } else {
                restrictedSessionID = previousValue;
            }
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
            throw new SessionException(SessionBundle.getString("invalidSessionID") + sessionId);
        }
        //if the provided session ID was a restricted token, resolveToken will always validate the restriction, so there is no
        //need to check restrictions here.
        InternalSession internalSession = resolveToken(sessionId);
        logoutInternalSession(internalSession.getID());
    }

    @Override
    public Session resolveSession(SessionID sessionID) throws SessionException {
        return sessionAccessManager.getSession(sessionID);
    }

    private void logoutInternalSession(final SessionID sessionId) {
        InternalSession session = sessionAccessManager.removeInternalSession(sessionId);
        if (session != null && session.getState() != INVALID) {
            signalRemove(session, SessionEvent.LOGOUT);
            sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_LOGGED_OUT);
        }
    }

    /**
     * Simplifies the signalling that a Session has been removed.
     * @param session Non null InternalSession.
     * @param event An integrate from the SessionEvent class.
     */
    private void signalRemove(InternalSession session, int event) {
        sessionLogging.logEvent(session.toSessionInfo(), event);
        session.setState(DESTROYED);
        sessionNotificationSender.sendEvent(session, event);
    }
}
