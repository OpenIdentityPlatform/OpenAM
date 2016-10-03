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

import static com.iplanet.dpro.session.service.SessionState.*;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.utils.Time.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
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
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.SearchResults;
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
    private final SessionServerConfig serverConfig;
    private final SessionNotificationSender sessionNotificationSender;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;
    private final SessionChangeAuthorizer sessionChangeAuthorizer;
    private final SessionServiceConfig serviceConfig;


    /**
     * Guice initialised constructor.
     * @param debug Non null.
     * @param sessionAccessManager access manager for returning the session information from the storage mechanism
     * @param sessionInfoFactory the factory for creating session information objects from a session
     * @param serverConfig session server config
     * @param sessionNotificationSender notification sender for session removal notification
     * @param sessionLogging special logging service for session removal logging
     * @param sessionAuditor audit logger
     * @param sessionChangeAuthorizer class for verifying permissions and authorisation for the current user to
*                                perform tasks on the session.  Used during deleting a session and getting access
     * @param serviceConfig contains configuration relating to the session service.
     *
     */
    @Inject
    LocalOperations(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                    final SessionAccessManager sessionAccessManager,
                    final SessionInfoFactory sessionInfoFactory,
                    final SessionServerConfig serverConfig,
                    final SessionNotificationSender sessionNotificationSender,
                    final SessionLogging sessionLogging,
                    final SessionAuditor sessionAuditor,
                    final SessionChangeAuthorizer sessionChangeAuthorizer,
                    final SessionServiceConfig serviceConfig) {
        this.debug = debug;
        this.sessionAccessManager = sessionAccessManager;
        this.sessionInfoFactory = sessionInfoFactory;
        this.serverConfig = serverConfig;
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
        this.sessionChangeAuthorizer = sessionChangeAuthorizer;
        this.serviceConfig = serviceConfig;
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
            sessionChangeAuthorizer.checkPermissionToDestroySession(requester, internalSessionToDestroy.getSessionID());
            destroyInternalSession(internalSessionToDestroy.getSessionID());
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
    public boolean checkSessionLocal(SessionID sessionId) throws SessionException {
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
                sessionAccessManager.reloadSessionHandleAndRestrictedIds(session);
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

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    @Override
    public SearchResults<SessionInfo> getValidSessions(Session s, String pattern) throws SessionException {
        if (s.getState(false) != VALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + s.getID().toString());
        }

        try {

            SearchResults<InternalSession> sessions = getValidInternalSessions(pattern);
            Set<SessionID> sessionIdList = new HashSet<>();
            for (InternalSession session : sessions.getSearchResults()) {
                sessionIdList.add(session.getSessionID());
            }

            Collection<InternalSession> sessionsWithPermission = sessionChangeAuthorizer.filterPermissionToAccess(
                    s.getSessionID(), sessions.getSearchResults());


            Set<SessionInfo> infos = new HashSet<>(sessionsWithPermission.size());

            for (InternalSession session : sessionsWithPermission) {
                SessionInfo info = session.toSessionInfo();
                // replace session id with session handle to prevent impersonation
                info.setSessionID(session.getSessionHandle());
                infos.add(info);
            }

            return new SearchResults<>(sessions.getTotalResultCount(), infos, sessions.getErrorCode());
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Get all valid Internal Sessions matched with pattern.
     */
    private SearchResults<InternalSession> getValidInternalSessions(String pattern)
            throws SessionException {
        Set<InternalSession> sessions = new HashSet<>();
        int errorCode = SearchResults.SUCCESS;

        if (pattern == null) {
            pattern = "*";
        }

        try {
            long startTime = currentTimeMillis();

            pattern = pattern.toLowerCase();
            List<InternalSession> allValidSessions = getValidInternalSessions();
            boolean matchAll = pattern.equals("*");

            for (InternalSession sess : allValidSessions) {
                if (!matchAll) {
                    // For application sessions, the client ID
                    // will not be in the DN format but just uid.
                    String clientID = (!sess.isAppSession()) ?
                            DNUtils.DNtoName(sess.getClientID()) :
                            sess.getClientID();

                    if (clientID == null) {
                        continue;
                    } else {
                        clientID = clientID.toLowerCase();
                    }

                    if (!matchFilter(clientID, pattern)) {
                        continue;
                    }
                }

                if (sessions.size() == serviceConfig.getMaxSessionListSize()) {
                    errorCode = SearchResults.SIZE_LIMIT_EXCEEDED;
                    break;
                }
                sessions.add(sess);

                if ((currentTimeMillis() - startTime) >=
                        serviceConfig.getSessionRetrievalTimeout()) {
                    errorCode = SearchResults.TIME_LIMIT_EXCEEDED;
                    break;
                }
            }
        } catch (Exception e) {
            debug.error("SessionService : "
                    + "Unable to get Session Information ", e);
            throw new SessionException(e);
        }
        return new SearchResults<>(sessions.size(), sessions, errorCode);
    }

    /**
     * Get all valid Internal Sessions.
     */
    private List<InternalSession> getValidInternalSessions() {

        synchronized (sessionAccessManager) {
            List<InternalSession> sessions = new ArrayList<>();
            for (InternalSession session : sessionAccessManager.getAllInternalSessions()) {
                if (session.getState() == VALID
                        && (!session.isAppSession() || serviceConfig.isReturnAppSessionEnabled())) {
                    sessions.add(session);
                }
            }
            return sessions;
        }
    }

    /**
     * Returns true if the given pattern is contained in the string.
     *
     * @param string  to examine
     * @param pattern to match
     * @return true if string matches <code>filter</code>
     */
    private boolean matchFilter(String string, String pattern) {
        if (pattern.equals("*") || pattern.equals(string)) {
            return true;
        }

        int length = pattern.length();
        int wildCardIndex = pattern.indexOf("*");

        if (wildCardIndex >= 0) {
            String patternSubStr = pattern.substring(0, wildCardIndex);

            if (!string.startsWith(patternSubStr, 0)) {
                return false;
            }

            int beginIndex = patternSubStr.length() + 1;
            int stringIndex = 0;

            if (wildCardIndex > 0) {
                stringIndex = beginIndex;
            }

            String sub = pattern.substring(beginIndex, length);

            while ((wildCardIndex = pattern.indexOf("*", beginIndex)) != -1) {
                patternSubStr = pattern.substring(beginIndex, wildCardIndex);

                if (string.indexOf(patternSubStr, stringIndex) == -1) {
                    return false;
                }

                beginIndex = wildCardIndex + 1;
                stringIndex = stringIndex + patternSubStr.length() + 1;
                sub = pattern.substring(beginIndex, length);
            }

            if (string.endsWith(sub)) {
                return true;
            }
        }
        return false;
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
        session.setState(SessionState.DESTROYED);
        sessionNotificationSender.sendEvent(session, event);
    }
}
