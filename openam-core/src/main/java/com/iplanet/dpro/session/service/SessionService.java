/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2023 3A Systems LLC
 */
package com.iplanet.dpro.session.service;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.operations.SessionOperationStrategy;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SearchResults;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This class represents a Session Service.
 */
@Singleton
public class SessionService {

    /**
     * Service name for NotificationSets.
     */
    public static final String SESSION_SERVICE = "session";

    private final Debug sessionDebug;
    private final DsameAdminTokenProvider dsameAdminTokenProvider;
    private final InternalSessionListener sessionEventBroker;
    private final InternalSessionFactory internalSessionFactory;
    private final SessionOperationStrategy sessionOperationStrategy;
    private final SessionAccessManager sessionAccessManager;

    /**
     * Private Singleton Session Service.
     */
    @Inject
    private SessionService(
            final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            final DsameAdminTokenProvider dsameAdminTokenProvider,
            final InternalSessionEventBroker internalSessionEventBroker,
            final InternalSessionFactory internalSessionFactory,
            final SessionAccessManager sessionAccessManager,
            final SessionOperationStrategy sessionOperationStrategy) {

        this.sessionDebug = sessionDebug;
        this.dsameAdminTokenProvider = dsameAdminTokenProvider;
        this.sessionEventBroker = internalSessionEventBroker;
        this.internalSessionFactory = internalSessionFactory;
        this.sessionOperationStrategy = sessionOperationStrategy;
        this.sessionAccessManager = sessionAccessManager;
    }

    /**
     * Returns the restricted token
     *
     * @param masterSid   master session id
     * @param restriction TokenRestriction Object
     * @return restricted token id
     * @throws SessionException
     */
    public String getRestrictedTokenId(String masterSid, TokenRestriction restriction) throws SessionException {
        SessionID sessionID = new SessionID(masterSid);
        return sessionOperationStrategy.getOperation(sessionID).getRestrictedTokenId(sessionID, restriction);
    }

    public InternalSession newInternalSession(String domain, boolean stateless) {
        return newInternalSession(domain, stateless, true);
    }
    public InternalSession newInternalSession(String domain, boolean stateless, boolean checkCts) {
        return internalSessionFactory.newInternalSession(domain, stateless, checkCts);
    }

    /**
     * Understands how to resolve a Token based on its SessionID.
     *
     * Stateless Sessions by their very nature do not need to be stored in memory, and so
     * can be resolved in a different way to Stateful Sessions.
     *
     * @param sessionID Non null Session ID.
     *
     * @return Null if no matching Session could be found, otherwise a non null
     * Session instance.
     *
     * @throws SessionException If there was an error resolving the Session.
     */
    private Session resolveSession(SessionID sessionID) throws SessionException {
        return sessionOperationStrategy.getOperation(sessionID).resolveSession(sessionID);
    }

    /**
     * Destroy a Internal Session, depending on the value of the user's permissions.
     * Performs no action if the sessionID cannot be matched.
     *
     * @param requester The requesting Session.
     * @param sessionToDestroy The session to destroy.
     * @throws SessionException If the user has insufficient permissions.
     */
    public void destroySession(Session requester, SessionID sessionToDestroy) throws SessionException {
        if (sessionToDestroy == null) {
            return;
        }
        sessionOperationStrategy.getOperation(sessionToDestroy).destroy(requester, resolveSession(sessionToDestroy));
    }

    /**
     * Destroy a Internal Session, whose session id has been specified.
     *
     * @param sessionID
     */
    public void destroyAuthenticationSession(final SessionID sessionID) {
        InternalSession authenticationSession = InjectorHolder.getInstance(AuthenticationSessionStore.class).removeSession(sessionID);
        if (authenticationSession == null) {
            authenticationSession = sessionAccessManager.getInternalSession(sessionID);
            sessionAccessManager.removeInternalSession(authenticationSession);
        }
        if (authenticationSession != null && authenticationSession.getState() != SessionState.INVALID) {
            fireSessionEvent(authenticationSession, SessionEventType.DESTROY);
            authenticationSession.setState(SessionState.DESTROYED);
        }
        sessionAccessManager.removeSessionId(sessionID);
    }

    private void fireSessionEvent(InternalSession session, SessionEventType sessionEventType) {
        sessionEventBroker.onEvent(new InternalSessionEvent(session, sessionEventType, Time.currentTimeMillis()));
    }

    /**
     * Check whether a session identified by {code sessionId} can be retrieved.
     *
     * @param sessionId the session ID to check.
     * @return returns true if the session is local
     * @throws SessionException if the session could not be accessed.
     */
    public boolean checkSessionExists(SessionID sessionId) throws SessionException {
        return sessionOperationStrategy.getOperation(sessionId).checkSessionExists(sessionId);
    }

    // The following methods are corresponding to the session requests
    // defined in the Session DTD. Those methods are being called
    // in SessionRequestHandler class

    /**
     * Returns the Session information.
     *
     * @param sid
     * @param reset
     * @throws SessionException
     */
    public SessionInfo getSessionInfo(SessionID sid, boolean reset) throws SessionException {
        return sessionOperationStrategy.getOperation(sid).getSessionInfo(sid, reset);
    }

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    public SearchResults<SessionInfo> getValidSessions(Session s, String pattern) throws SessionException {
        return sessionOperationStrategy.getOperation(s.getSessionID()).getValidSessions(s, pattern);
    }

    /**
     * Returns partial (stateful) sessions matching the provided CREST query. The resultset size is limited by the
     * "iplanet-am-session-max-session-list-size" attribute. The returned sessions are only "partial" sessions, meaning
     * that they do not represent the full session state.
     *
     * @param caller The session that initiated the query request. May not be null.
     * @param crestQuery The CREST query based on which we should look for matching sessions. May not be null.
     * @return The collection of matching partial sessions.
     * @throws SessionException If the request fails.
     * @see com.iplanet.dpro.session.operations.SessionOperations#getMatchingSessions(CrestQuery)
     */
    public Collection<PartialSession> getMatchingSessions(Session caller, CrestQuery crestQuery)
            throws SessionException {
        Reject.ifNull(caller, "Caller may not be null");
        Reject.ifNull(crestQuery, "CREST query may not be null");
        return sessionOperationStrategy.getOperation(caller.getSessionID()).getMatchingSessions(crestQuery);
    }

    /**
     * Logout the user.
     *
     * @param session
     * @throws SessionException
     */
    public void logout(final Session session) throws SessionException {
        sessionOperationStrategy.getOperation(session.getSessionID()).logout(session);
    }

    /**
     * Adds listener to a Internal Sessions.
     *
     * @param session Session
     * @param url
     * @throws SessionException Session is null OR the Session is invalid
     */
    public void addSessionListener(Session session, String url) throws SessionException {
        sessionOperationStrategy.getOperation(session.getSessionID()).addSessionListener(session, url);
    }

    /**
     * Sets external property in the Internal Session as long as it is not
     * protected
     *
     * @param clientToken - Token of the client setting external property.
     * @param sessionId
     * @param name
     * @param value
     * @throws SessionException
     */
    public void setExternalProperty(SSOToken clientToken, SessionID sessionId, String name, String value)
            throws SessionException {
        sessionOperationStrategy.getOperation(sessionId).setExternalProperty(clientToken, sessionId, name, value);
    }

    /**
     * Returns true if the user is super user
     *
     * @param uuid the uuid of the login user
     */
    public boolean isSuperUser(String uuid) {
        boolean isSuperUser = false;
        try {
            // Get the AMIdentity Object for super user 
            AMIdentity adminUserId = null;
            String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
            if (adminUser != null) {
                adminUserId = new AMIdentity(dsameAdminTokenProvider.getAdminToken(), adminUser, IdType.USER, "/", null);
            }
            //Get the AMIdentity Object for login user
            AMIdentity user = IdUtils.getIdentity(dsameAdminTokenProvider.getAdminToken(), uuid);
            //Check for the equality
            isSuperUser = adminUserId.equals(user);

        } catch (SSOException ssoe) {
            sessionDebug.error("SessionService.isSuperUser: Cannot get the admin token for this operation.", ssoe);

        } catch (IdRepoException idme) {
            sessionDebug.error("SessionService.isSuperUser: Cannot get the user identity '{}'.", uuid, idme);
        }

        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("SessionService.isSuperUser: " + isSuperUser);
        }

        return isSuperUser;
    }

    /**
     * Gets the AM Server ID.
     * @return the AM Server Id or null if WebtopNaming was unable to detmin the ID of this server.
     */
    public static String getAMServerID() {
        try {
            return WebtopNaming.getAMServerID();
        } catch (Exception le) {
            return null;
        }
    }
}
