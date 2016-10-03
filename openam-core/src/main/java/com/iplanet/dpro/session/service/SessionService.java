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
 */
package com.iplanet.dpro.session.service;

import static org.forgerock.openam.audit.AuditConstants.EventName.*;

import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.session.service.SessionTimeoutHandler;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.monitoring.ForeignSessionHandler;
import com.iplanet.dpro.session.operations.SessionOperationStrategy;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.SearchResults;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;

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
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;
    private final SSOTokenManager ssoTokenManager;
    private final DsameAdminTokenProvider dsameAdminTokenProvider;
    private final MonitoringOperations monitoringOperations;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;
    private final InternalSessionFactory internalSessionFactory;
    private final SessionNotificationSender sessionNotificationSender;
    private final SessionMaxStats maxSessionStats; // TODO: Inject from Guice
    private final ExecutorService executorService = Executors.newCachedThreadPool(); // TODO: Inject from Guice

    private final SessionOperationStrategy sessionOperationStrategy;

    private final SessionAccessManager sessionAccessManager;
    private final ForeignSessionHandler foreignSessionHandler;

    private final ServicesClusterMonitorHandler servicesClusterMonitorHandler;

    /**
     * Private Singleton Session Service.
     */
    @Inject
    private SessionService(
            final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            final @Named(SessionConstants.STATS_MASTER_TABLE) Stats stats,
            final SSOTokenManager ssoTokenManager,
            final DsameAdminTokenProvider dsameAdminTokenProvider,
            final SessionServerConfig serverConfig,
            final SessionServiceConfig serviceConfig,
            final MonitoringOperations monitoringOperations,
            final SessionLogging sessionLogging,
            final SessionAuditor sessionAuditor,
            final InternalSessionFactory internalSessionFactory,
            final SessionNotificationSender sessionNotificationSender,
            final SessionAccessManager sessionAccessManager,
            final SessionOperationStrategy sessionOperationStrategy,
            final ServicesClusterMonitorHandler servicesClusterMonitorHandler,
            final ForeignSessionHandler foreignSessionHandler) {

        this.sessionDebug = sessionDebug;
        this.ssoTokenManager = ssoTokenManager;
        this.dsameAdminTokenProvider = dsameAdminTokenProvider;
        this.serverConfig = serverConfig;
        this.serviceConfig = serviceConfig;
        this.monitoringOperations = monitoringOperations;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
        this.internalSessionFactory = internalSessionFactory;
        this.sessionOperationStrategy = sessionOperationStrategy;
        this.servicesClusterMonitorHandler = servicesClusterMonitorHandler;
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionAccessManager = sessionAccessManager;
        this.foreignSessionHandler = foreignSessionHandler;

        try {

            if (stats.isEnabled()) {
                maxSessionStats = new SessionMaxStats(
                        sessionAccessManager, monitoringOperations, sessionNotificationSender, stats);
                stats.addStatsListener(maxSessionStats);
            } else {
                maxSessionStats = null;
            }

        } catch (Exception ex) {

            sessionDebug.error("SessionService initialization failed.", ex);
            throw new IllegalStateException("SessionService initialization failed.", ex);

        }
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
        return internalSessionFactory.newInternalSession(domain, stateless);
    }

    /**
     * Removes the Internal Session from the Internal Session table.
     *
     * @param sessionId Session ID
     */
    InternalSession removeCachedInternalSession(final SessionID sessionId) {
        if (null == sessionId) {
            return null;
        }

        return sessionAccessManager.removeInternalSession(sessionId);
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
     * @param sessionID The id of the session to destroy.
     */
    void destroyInternalSession(SessionID sessionID) {
        InternalSession internalSession = removeCachedInternalSession(sessionID);
        if (internalSession != null && internalSession.getState() != SessionState.INVALID) {
            signalRemove(internalSession, SessionEvent.DESTROY);
            sessionAuditor.auditActivity(internalSession.toSessionInfo(), AM_SESSION_DESTROYED);
        }
        sessionAccessManager.removeSessionId(sessionID);
    }

    /**
     * Destroy a Internal Session, whose session id has been specified.
     *
     * @param sessionID
     */
    public void destroyAuthenticationSession(final SessionID sessionID) {

        InternalSession authenticationSession = InjectorHolder.getInstance(AuthenticationSessionStore.class).removeSession(sessionID);
        if (authenticationSession == null) {
            authenticationSession = removeCachedInternalSession(sessionID);
        }
        if (authenticationSession != null && authenticationSession.getState() != SessionState.INVALID) {
            signalRemove(authenticationSession, SessionEvent.DESTROY);
            sessionAuditor.auditActivity(authenticationSession.toSessionInfo(), AM_SESSION_DESTROYED);
        }
        sessionAccessManager.removeSessionId(sessionID);
    }

    /**
     * Checks whether current session should be considered local (so that local
     * invocations of SessionService methods are to be used) and if local and
     * Session Failover is enabled will recover the Session if the Session is
     * not found locally.
     *
     * @return a boolean
     */
    public boolean checkSessionLocal(SessionID sessionId) throws SessionException {
        return sessionOperationStrategy.getOperation(sessionId).checkSessionLocal(sessionId);
    }

    /**
     * Returns the Internal Session corresponding to a Session ID.
     *
     * @param sessionId Session Id
     */
    public InternalSession getInternalSession(SessionID sessionId) { // TODO Used to recover authentication session by AuthD

        if (sessionId == null) {
            return null;
        }
        // check if sid is actually a handle return null (in order to prevent from assuming recovery case)
        if (sessionId.isSessionHandle()) {
            return null;
        }
        return sessionAccessManager.getInternalSession(sessionId);
    }

    /**
     * Simplifies the signalling that a Session has been removed.
     * @param session Non null InternalSession.
     * @param event An integrate from the SessionEvent class.
     */
    private void signalRemove(InternalSession session, int event) {
        sessionLogging.logEvent(session.toSessionInfo(), event);
        session.setState(SessionState.DESTROYED);
        sendEvent(session, event);
    }

    /**
     * Decrements number of active sessions
     */
    public void decrementActiveSessions() {
        monitoringOperations.decrementActiveSessions();
    }

    /**
     * Increments number of active sessions
     */
    public void incrementActiveSessions() {
        monitoringOperations.incrementActiveSessions();
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
     * Returns current Notification queue size.
     */
    public int getNotificationQueueSize() {
        return sessionNotificationSender.getNotificationQueueSize();
    }

    /**
     * Sends the Internal Session event to the SessionNotificationSender.
     *
     * @param internalSession Internal Session.
     * @param eventType Event Type.
     */
    public void sendEvent(InternalSession internalSession, int eventType) {
        sessionNotificationSender.sendEvent(internalSession, eventType);
    }

    /**
     * Given a restricted token, returns the SSOTokenID of the master token
     * can only be used if the requester is an app token
     *
     * @param session Must be an app token
     * @param restrictedID The SSOTokenID of the restricted token
     * @return The SSOTokenID string of the master token
     * @throws SSOException If the master token cannot be dereferenced
     */
    public String deferenceRestrictedID(Session session, String restrictedID) throws SessionException {
        SessionID sessionId = new SessionID(restrictedID);
        return sessionOperationStrategy.getOperation(session.getSessionID()).deferenceRestrictedID(session, sessionId);
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
    public void setExternalProperty(SSOToken clientToken, SessionID sessionId,
                                    String name, String value)
            throws SessionException {
        sessionOperationStrategy.getOperation(sessionId).setExternalProperty(clientToken, sessionId, name, value);
    }

    /**
     * This is a key method for "internal request routing" mode It determines
     * the server id which is currently hosting session identified by sid. In
     * "internal request routing" mode, this method also has a side effect of
     * releasing a session which no longer "belongs locally" (e.g., due to
     * primary server instance restart)
     *
     * @param sessionId session id
     * @return server id for the server instance determined to be the current
     *         host
     * @throws SessionException
     */
    public String getCurrentHostServer(SessionID sessionId) throws SessionException {
        return foreignSessionHandler.getCurrentHostServer(sessionId);
    }

    /**
     * Actively check if server identified by serverID is up
     *
     * @param serverID server id
     * @return true if server is up, false otherwise
     */
    public boolean checkServerUp(String serverID) {
        return servicesClusterMonitorHandler.checkServerUp(serverID);
    }

    /**
     * Indicates that the Site is up.
     *
     * @param siteId A possibly null Site Id.
     * @return True if the Site is up, False if it failed to respond to a query.
     */
    public boolean isSiteUp(String siteId) {
        return servicesClusterMonitorHandler.isSiteUp(siteId);
    }

    /**
     * This method will execute all the globally set session timeout handlers
     * with the corresponding timeout event simultaneously.
     *
     * @param sessionId  The timed out sessions ID
     * @param changeType Type of the timeout event: IDLE_TIMEOUT (1) or MAX_TIMEOUT (2)
     */
    void execSessionTimeoutHandlers(final SessionID sessionId, final int changeType) {
        // Take snapshot of reference to ensure atomicity.
        final Set<String> handlers = serviceConfig.getTimeoutHandlers();

        if (!handlers.isEmpty()) {
            try {
                final SSOToken token = ssoTokenManager.createSSOToken(sessionId.toString());
                final List<Future<?>> futures = new ArrayList<>();
                final CountDownLatch latch = new CountDownLatch(handlers.size());

                for (final String clazz : handlers) {
                    Runnable timeoutTask = new Runnable() {

                        public void run() {
                            try {
                                SessionTimeoutHandler handler =
                                        Class.forName(clazz).asSubclass(
                                                SessionTimeoutHandler.class).newInstance();
                                switch (changeType) {
                                    case SessionEvent.IDLE_TIMEOUT:
                                        handler.onIdleTimeout(token);
                                        break;
                                    case SessionEvent.MAX_TIMEOUT:
                                        handler.onMaxTimeout(token);
                                        break;
                                }
                            } catch (Exception ex) {
                                if (Thread.interrupted()
                                        || ex instanceof InterruptedException
                                        || ex instanceof InterruptedIOException) {
                                    sessionDebug.warning("Timeout Handler was interrupted");
                                } else {
                                    sessionDebug.error("Error while executing the following session timeout handler: " + clazz, ex);
                                }
                            } finally {
                                latch.countDown();
                            }
                        }
                    };
                    futures.add(executorService.submit(timeoutTask)); // This should not throw any exceptions.
                }

                // Wait 1000ms for all handlers to complete.
                try {
                    latch.await(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    // This should never happen: we can't handle it here, so propagate it.
                    Thread.currentThread().interrupt();
                }

                for (Future<?> future : futures) {
                    if (!future.isDone()) {
                        // It doesn't matter really if the future completes between isDone and cancel.
                        future.cancel(true); // Interrupt.
                    }
                }
            } catch (SSOException ssoe) {
                sessionDebug.warning("Unable to construct SSOToken for executing timeout handlers", ssoe);
            }
        }
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
            sessionDebug.error("SessionService.isSuperUser: Cannot get the admin token for this operation.");

        } catch (IdRepoException idme) {
            sessionDebug.error("SessionService.isSuperUser: Cannot get the user identity.");
        }

        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("SessionService.isSuperUser: " + isSuperUser);
        }

        return isSuperUser;
    }

    /**
     * This method is the "server side" of the getRestrictedTokenIdRemotely()
     *
     * @param masterSid   SessionID
     * @param restriction restriction
     */
    String handleGetRestrictedTokenIdRemotely(SessionID masterSid,
                                              TokenRestriction restriction) {
        try {
            return sessionOperationStrategy.getOperation(masterSid).getRestrictedTokenId(masterSid, restriction);
        } catch (Exception ex) {
            sessionDebug.error("Failed to create restricted token remotely", ex);
        }
        return null;
    }

    /**
     * Returns true if the URL is the URL of the local session service.
     *
     * @param svcurl the url to check
     * @return true if the url represents the local session service.
     */
    public boolean isLocalSessionService(URL svcurl) {
        return serverConfig.isLocalSessionService(svcurl);
    }

    /**
     * Determines if the Maximum  umber of active sessions has been reached.
     * @return true if the maximum number of sessions has ben reached.
     */
    public boolean hasExceededMaxSessions() {
        return monitoringOperations.getActiveSessions() >= serviceConfig.getMaxSessions();
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