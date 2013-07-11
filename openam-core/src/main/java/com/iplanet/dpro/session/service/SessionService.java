/**
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
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.ThreadPool;
import com.iplanet.am.util.ThreadPoolException;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionNotificationHandler;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionNotification;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerSessSvcImpl;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.adapters.SessionAdapter;
import com.sun.identity.sm.ldap.adapters.TokenAdapter;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.KeyConversion;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.session.service.SessionTimeoutHandler;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class represents a Session Service
 */
public class SessionService {

    private static String LOG_PROVIDER = "Session";

    /**
     * Session Service Thread Pool for Session
     * Handler Tasks.
     */
    static private ThreadPool threadPool = null;

    /**
     * Our Session Service Singleton Service Implementation Instance.
     */
    private static volatile SessionService sessionService = null;
    /**
     * AM Session Repository for Session Persistence.
     */
    private static volatile CTSPersistentStore coreTokenService = null;

    /**
     * SSO Token Manager Instance Reference.
     */
    static SSOTokenManager ssoManager = null;

    /**
     * Globals
     */
    public static Debug sessionDebug = null;

    public static int maxSessions = 10000;

    public static Stats stats;

    private static int numberOfActiveSessions = 0;

    private static String dsameAdminDN = null;

    private static String dsameAdminPassword = null;

    private static SessionMaxStats maxSessionStats;

    private static boolean logStatus = false;

    private static Logger logger = null;
    private static Logger errorLogger = null;
    private static final String amSSOErrorLogFile = "amSSO.error";
    private static final String amSSOLogFile = "amSSO.access";

    private static LogMessageProvider logProvider = null;

    public static final String SHANDLE_SCHEME_PREFIX = "shandle:";

    private static final String amSessionService = "iPlanetAMSessionService";

    private static final String httpSessionTrackingCookieName =
            SystemProperties.get(
                    Constants.AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME,
                    "JSESSIONID");

    private static boolean cookieEncoding =
            (SystemProperties.get(Constants.AM_COOKIE_ENCODE) != null) &&
                    (SystemProperties.get(Constants.AM_COOKIE_ENCODE)
                            .equalsIgnoreCase("true"));

    private static final String sunAppServerLBRoutingCookieName =
            SystemProperties.get(
                    "com.iplanet.am.session.failover.sunAppServerLBRoutingCookieName",
                    "JROUTE");

    private static final String httpSessionPropertyName =
            "DSAMEInternalSession";

    private static final String httpSessionOwnerListPropertyName =
            "DSAMEInternalSession.ownerList";

    private static final int DEFAULT_POOL_SIZE = 10;

    private static final int DEFAULT_THRESHOLD = DEFAULT_POOL_SIZE * 10;

    protected static final String securityCookieName = "DSAMESecurityCookie";

    protected static final String defaultApplicationMaxCachingTime = String
            .valueOf(Long.MAX_VALUE / 60);

    protected static final long applicationMaxCachingTime = Long.valueOf(
            SystemProperties.get(
                    Constants.APPLICATION_SESSION_MAX_CACHING_TIME,
                    defaultApplicationMaxCachingTime)).longValue();

    // Session Constraints specific properties
    private static final String SESSION_CONSTRAINT =
            "iplanet-am-session-enable-session-constraint";

    private static final String DENY_LOGIN_IF_DB_IS_DOWN =
            "iplanet-am-session-deny-login-if-db-is-down";

    private static final String MAX_WAIT_TIME_FOR_CONSTARINT =
            "iplanet-am-session-constraint-max-wait-time";

    private static final String CONSTRAINT_HANDLER =
            "iplanet-am-session-constraint-handler";

    private static final String SESSION_REPOSITORY_TYPE =
            "iplanet-am-session-sfo-store-type";

    // constants for permissions
    private static final String PERMISSION_READ = "READ";
    private static final String PERMISSION_MODIFY = "MODIFY";
    private static final String PERMISSION_DELEGATE = "DELEGATE";

    private static String sessionStoreUserName = null;

    private static String sessionStorePassword = null;

    private static HashMap clusterMemberMap = new HashMap();

    private static int connectionMaxWaitTime = 5000; // in milli-seconds

    private static String sessionExternalRepositoryURL = null;

    private static int maxWaitTimeForConstraint = 6000; // in milli-seconds

    private static boolean isPropertyNotificationEnabled = false;

    protected static Set notificationProperties;
    protected static volatile Set<String> timeoutHandlers;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    /*
     * This token is used to satisfy the admin interfaces
     */
    private static SSOToken adminToken = null;

    protected static boolean returnAppSession = Boolean
            .valueOf(
                    SystemProperties.get(Constants.SESSION_RETURN_APP_SESSION,
                            "false")).booleanValue();

    public static final String SESSION_SERVICE = "session";

    private static SecureRandom secureRandom = null;

    private static Hashtable sessionTable = null;

    private static Set remoteSessionSet = null;

    private static Hashtable sessionHandleTable = new Hashtable();

    private static Map restrictedTokenMap = Collections.synchronizedMap(new HashMap());

    private static String sessionServer;

    private static String sessionServerPort;

    private static String sessionServerProtocol;

    private static String sessionServerURI;

    private static String sessionServerID;

    private static Set secondaryServerIDs;

    public static String deploymentURI = SystemProperties
            .get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

    // used for session trimming    
    private static boolean isSessionTrimmingEnabled = false;

    /* the following group of members are for session constraints */
    private static boolean isSessionConstraintEnabled = false;

    private static boolean denyLoginIfDBIsDown = false;

    private static String constraintHandler =
            SessionConstraint.DESTROY_OLDEST_SESSION_CLASS;

    private static String thisSessionServer;

    private static String thisSessionServerPortAsString;

    private static int thisSessionServerPort;

    private static String thisSessionURI;

    private static String thisSessionServerProtocol;

    private static String thisSessionServerID;

    private static String thisSessionServerURL;

    private static URL thisSessionServiceURL;

    // Must be True to permit Session Failover HA to be available.
    private static boolean useRemoteSaveMethod = Boolean.valueOf(
            SystemProperties
                    .get(Constants.AM_SESSION_FAILOVER_USE_REMOTE_SAVE_METHOD,
                            "true")).booleanValue();

    // Must be True to permit Session Failover HA to be available.
    private static boolean useInternalRequestRouting = Boolean.valueOf(
            SystemProperties.get(
                    Constants.AM_SESSION_FAILOVER_USE_INTERNAL_REQUEST_ROUTING,
                    "true")).booleanValue();

    // Must be True to permit Session Failover HA to be available,
    // but we default this to Disabled or Off for Now.
    private static boolean isSessionFailoverEnabled = Boolean.valueOf(
            SystemProperties.get(
                    CoreTokenConstants.IS_SFO_ENABLED,
                    "false")).booleanValue();

    // Must be True to permit Session Failover HA to be available.
    private static boolean isSiteEnabled = false;  // If this is set to True and no Site is found, issues will arise
    // Trying to resolve the serverID and will hang install and subsequent login attempts.

    /**
     * The following InternalSession is for the Authentication Service to use
     * Profile API to fetch user profile.
     */
    private static InternalSession authSession = null;

    /**
     * The URL Vector for ALL session events : SESSION_CREATION, IDLE_TIMEOUT,
     * MAX_TIMEOUT, LOGOUT, REACTIVATION, DESTROY.
     */
    private static Vector sessionEventURLs = new Vector();

    private static URL sessionServiceID = null;

    private static ClusterStateService clusterStateService = null;

    private TokenIdFactory tokenIdFactory;
    private CoreTokenConfig coreTokenConfig;
    private TokenAdapter<InternalSession> tokenAdapter;

    /**
     * Static initialisation section will be called the first time the SessionService is initailised.
     *
     * Note: This function depends on the singleton pattern that the SessionService follows.
     */
    private static void initialiseStatic() {
        sessionDebug = Debug.getInstance("amSession");
        stats = Stats.getInstance("amMasterSessionTableStats");

        int poolSize = DEFAULT_POOL_SIZE;
        int threshold = DEFAULT_THRESHOLD;

        // Notification Thread Pool Size
        String size = SystemProperties.get(
                Constants.NOTIFICATION_THREADPOOL_SIZE);
        if (size != null) {
            try {
                poolSize = Integer.parseInt(size);
            } catch (NumberFormatException e) {
                sessionDebug.error(
                        "SessionService.<init>: incorrect thread pool size" + size +
                                "defaulting to " + DEFAULT_POOL_SIZE);
            }
        }

        // Notification Thread Pool Threshold
        String thres = SystemProperties.get(
                Constants.NOTIFICATION_THREADPOOL_THRESHOLD);
        if (thres != null) {
            try {
                threshold = Integer.parseInt(thres);
            } catch (Exception e) {
                sessionDebug.error(
                        "SessionService.<init>: incorrect thread threshold" + thres
                                + "defaulting to " + DEFAULT_THRESHOLD);
            }
        }

        // Establish Shutdown Manager.
        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                threadPool = new ThreadPool("amSession", poolSize, threshold, true,
                        sessionDebug);
                shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                                threadPool.shutdown();
                            }
                        }
                );
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
        if (threadPool != null) {
            try {
                maxSessions = Integer.parseInt(SystemProperties
                        .get(Constants.AM_SESSION_MAX_SESSIONS));
            } catch (Exception ex) {
                maxSessions = 10000;
            }
        }

        String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        if (status == null) {
            status = "INACTIVE";
        }
        logStatus = status.equalsIgnoreCase("ACTIVE");
    }

    /**
     * Returns Session Service. If a Session Service already exists then it
     * returns the existing one. Else it creates a new one and returns.
     */
    public static SessionService getSessionService() {
        if (sessionService == null) {
            initialiseStatic();

            if (SystemProperties.isServerMode()) {
                synchronized (SessionService.class) {
                    if (sessionService == null) {
                        sessionService = new SessionService();
                    }
                } // End of synchronized Block.
            }
        }
        return sessionService;
    }

    /**
     * Returns the name of the cookie/URL parameter used by J2EE container for
     * session tracking (currently hardcoded to "JSESSIONID")
     */
    public static String getHttpSessionTrackingCookieName() {
        return httpSessionTrackingCookieName;
    }

    /**
     * Returns the Internal Session used by the Auth Services.
     *
     * @param domain      Authentication Domain
     * @param httpSession HttpSession
     */
    public Session getAuthenticationSession(String domain,
                                            HttpSession httpSession) {
        try {
            if (authSession == null) {
                // Create a special InternalSession for Authentication Service
                // ....
                authSession = getServiceSession(domain, httpSession);
            }
            return authSession != null ? Session
                    .getSession(authSession.getID()) : null;
        } catch (Exception e) {
            sessionDebug.error("Error creating service session", e);
            return null;
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

    public String getRestrictedTokenId(String masterSid,
                                       TokenRestriction restriction) throws SessionException {
        SessionID sid = new SessionID(masterSid);

        // we need to accommodate session failover situation
        if (SessionService.getUseInternalRequestRouting()) {
            // first try
            String hostServerID = getCurrentHostServer(sid);

            if (!isLocalServer(hostServerID)) {
                if (!sessionService.checkServerUp(hostServerID)) {
                    hostServerID = getCurrentHostServer(sid);
                }
                if (!isLocalServer(hostServerID)) {
                    String token = getRestrictedTokenIdRemotely(Session
                            .getSessionServiceURL(hostServerID), sid,
                            restriction);
                    if (token == null) {
                        // TODO consider one retry attempt
                        throw new SessionException(SessionBundle
                                .getString("invalidSessionID")
                                + masterSid);
                    } else {
                        return token;
                    }
                }
            }
        }
        return doGetRestrictedTokenId(sid, restriction);
    }

    /**
     * This method is expected to only be called for local sessions
     */

    String doGetRestrictedTokenId(SessionID masterSid,
                                  TokenRestriction restriction) throws SessionException {

        // locate master session
        InternalSession session = (InternalSession) sessionTable.get(masterSid);
        if (session == null) {
            session = sessionService.recoverSession(masterSid);

            if (session == null) {
                throw new SessionException(SessionBundle
                        .getString("invalidSessionID")
                        + masterSid);
            }
        }
        checkSession(session, masterSid);
        // attempt to reuse the token if restriction is the same
        SessionID restrictedSid = session
                .getRestrictedTokenForRestriction(restriction);
        if (restrictedSid == null) {
            restrictedSid = new SessionID(SessionID.makeRelatedSessionID(
                    generateEncryptedID(), session.getID()));
            session.addRestrictedToken(restrictedSid, restriction);
            restrictedTokenMap.put(restrictedSid, session.getID());
        }
        return restrictedSid.toString();
    }

    /**
     * Returns the Internal Session which can be used by services
     *
     * @param domain      Authentication Domain
     * @param httpSession HttpSession
     */
    private InternalSession getServiceSession(String domain,
                                              HttpSession httpSession) {
        try {
            InternalSession session = null;
            // Create a special InternalSession which can be used as
            // service token
            // note that this session does not need failover protection
            // as its scope is only this same instance
            // more over creating an HTTP session by making a self-request
            // results in dead-lock if called from within synchronized
            // section in getSessionService()
            session = newInternalSession(domain, httpSession, false);
            session.setType(Session.APPLICATION_SESSION);
            String adminDN = (String) AccessController
                    .doPrivileged(new AdminDNAction());
            session.setClientID(adminDN);
            session.setClientDomain(domain);
            session.setExpire(false);
            session.setState(Session.VALID);
            incrementActiveSessions();
            return session;
        } catch (Exception e) {
            sessionDebug.error("Error creating service session", e);
            return null;
        }
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain      Authentication Domain
     * @param httpSession Http Session
     */
    public InternalSession newInternalSession(String domain,
                                              HttpSession httpSession) {
        try {
            return newInternalSession(domain, httpSession, true);
        } catch (SessionException e) {
            sessionDebug.error("Error creating new session", e);
            return null;
        }
    }

    /**
     * Creates a new Internal Session
     *
     * @param domain                   Authentication Domain
     * @param httpSession              Http Session
     * @param forceHttpSessionCreation in session failover mode if this parameter is true and
     *                                 httpSession is null, it will cause SessionService to create a
     *                                 new Http session for failover purposes
     */
    private InternalSession newInternalSession(String domain,
                                               HttpSession httpSession, boolean forceHttpSessionCreation)
            throws SessionException {

        if (isSessionFailoverEnabled && !getUseInternalRequestRouting()
                && httpSession == null && forceHttpSessionCreation) {
            return createSession(domain);
        }

        InternalSession session = null;
        SessionID sid = null;

        // generate primary id
        sid = generateSessionId(domain, httpSession);

        // generate session handle which looks like normal session id
        // except it is not a valid session id
        // and can not be used for anything other than destroySession
        // TODO consider unifying RestrictedTokens and session handle

        String sessionHandle = SHANDLE_SCHEME_PREFIX
                + SessionID.makeRelatedSessionID(generateEncryptedID(), sid);

        session = new InternalSession(sid);
        session.setSessionHandle(sessionHandle);
        session.setHttpSession(httpSession);
        sessionTable.put(sid, session);
        if (SystemProperties.isServerMode()) {
            if (MonitoringUtil.isRunning()) {
                SsoServerSessSvcImpl sessImpl =
                        Agent.getSessSvcMBean();
                sessImpl.incCreatedSessionCount();
            }
        }
        sessionHandleTable.put(sessionHandle, session);

        session.setCreationTime();
        session.setLatestAccessTime();
        String amCtxId = Long.toHexString(secureRandom.nextLong())
                + (isSiteEnabled ? thisSessionServerID : sessionServerID);
        session.putProperty(Constants.AM_CTX_ID, amCtxId);
        session.putProperty(Session.lbCookieName,
                WebtopNaming.getLBCookieValue(getLocalServerID()));
        session.reschedule();
        return session;
    }

    /**
     * Generates new encrypted ID string to be used as part of session id
     *
     * @return emcrypted ID string
     */
    private String generateEncryptedID() {
        String r = Long.toHexString(secureRandom.nextLong());
        // TODO note that this encryptedID string is kept only
        // to make this compatible with older Java SDK clients
        // which knew too much about the structure of the session id
        // newer clients will mostly treat session id as opaque
        // 
        return (String) AccessController.doPrivileged(new EncodeAction(r + "@"
                + sessionServerID, Crypt.getHardcodedKeyEncryptor()));
    }

    /**
     * Generates new SessionID
     *
     * @param domain      session domain
     * @param httpSession http session for failover purposes
     * @return newly generated session id
     * @throws SessionException
     */
    private SessionID generateSessionId(String domain, HttpSession httpSession)
            throws SessionException {
        SessionID sid;
        String httpSessionId = null;

        do {
            String encryptedID = generateEncryptedID();

            Map ext = new HashMap();
            ext.put(SessionID.SITE_ID, sessionServerID);

            // AME-129 Required for Automatic Session Failover Persistence
            if ((isSiteEnabled) && (thisSessionServerID != null) && (!thisSessionServerID.isEmpty())) {
                ext.put(SessionID.PRIMARY_ID, thisSessionServerID);
            }

            // AME-129, always set a Storage Key regardless of persisting or not.
            ext.put(SessionID.STORAGE_KEY, String.valueOf(secureRandom.nextLong()));

            String sessionID = SessionID.makeSessionID(encryptedID, ext,
                    httpSessionId);

            sid = new ExtendedSessionID(sessionID,
                    isSiteEnabled ? thisSessionServerID : sessionServerID,
                    domain);

        } while (sessionTable.get(sid) != null
                || sessionHandleTable.get(sid) != null);
        return sid;
    }

    /**
     * extract http session id useable as http session cookie
     *
     * @param httpSession http session
     * @return http session id
     */
    private static String extractHttpSessionId(HttpSession httpSession) {
        return httpSession.getId();
    }

    /**
     * Removes the Internal Session from the Internal Session table.
     *
     * @param sid Session ID
     */
    InternalSession removeInternalSession(SessionID sid) {
        boolean isSessionStored = true;
        if (sid == null)
            return null;
        InternalSession session = (InternalSession) sessionTable.remove(sid);

        if (session != null) {
            remoteSessionSet.remove(sid);
            session.cancel();
            removeSessionHandle(session);
            removeRestrictedTokens(session);
            isSessionStored = session.getIsISstored();
            // Session Constraint
            if (session.getState() == Session.VALID) {
                decrementActiveSessions();
                SessionCount.decrementSessionCount(session);
            }
        }

        if (isSessionFailoverEnabled && isSessionStored) {
            if (getUseInternalRequestRouting()) {
                try {
                    String tokenId = tokenIdFactory.toSessionTokenId(session);
                    getRepository().delete(tokenId);
                } catch (Exception e) {
                    sessionDebug.error(
                            "SessionService : failed deleting session ", e);
                }
            } else {
                invalidateHttpSession(sid);
            }
        }

        return session;
    }

    void deleteFromRepository(SessionID sid) {
        if (isSessionFailoverEnabled) {
            try {
                String tokenId = tokenIdFactory.toSessionTokenId(sid);
                getRepository().delete(tokenId);
            } catch (Exception e) {
                sessionDebug.error("SessionService : failed deleting session ",
                        e);
            }
        }
    }

    private void removeRestrictedTokens(InternalSession session) {
        if (session == null)
            return;
        Object[] tokens = session.getRestrictedTokens();
        for (int i = 0; i < tokens.length; ++i) {
            restrictedTokenMap.remove(tokens[i]);
        }
    }

    private void removeSessionHandle(InternalSession session) {
        if (session == null)
            return;

        // remove from sessionHandleTable (if present)
        String sessionHandle = session.getSessionHandle();
        if (sessionHandle != null) {
            sessionHandleTable.remove(sessionHandle);
        }
    }

    /**
     * Returns true if session failover is enabled
     */
    public boolean isSessionFailoverEnabled() {
        return isSessionFailoverEnabled;
    }

    /**
     * This method checks if Internal session is already present locally
     *
     * @param sid
     * @return a boolean
     */
    public boolean isSessionPresent(SessionID sid) {
        boolean isPresent = sessionTable.get(sid) != null
                || restrictedTokenMap.get(sid) != null
                || sessionHandleTable.get(sid) != null;

        return isPresent;
    }

    /**
     * Checks whether current session should be considered local (so that local
     * invocations of SessionService methods are to be used) and if local and
     * Session Failover is enabled will recover the Session if the Session is
     * not found locally.
     *
     * @return a boolean
     */
    public boolean checkSessionLocal(SessionID sid) throws SessionException {
        if (isSessionPresent(sid)) {
            return true;
        } else {
            if (isSessionFailoverEnabled) {
                String hostServerID = getCurrentHostServer(sid);
                if (isLocalServer(hostServerID)) {
                    if (recoverSession(sid) == null) {
                        throw new SessionException(SessionBundle
                                .getString("sessionNotObtained"));
                    }
                    return true;
                }

            } else {
                return isLocalSessionService(Session.getSessionServiceURL(sid));
            }
        }
        return false;
    }

    /**
     * Returns true if URL is a URL of the local service local to this instance
     *
     * @param url
     */
    public boolean isLocalSessionService(URL url) {
        URL localURL = isSiteEnabled ? thisSessionServiceURL : sessionServiceID;

        return localURL != null
                && localURL.getProtocol().equalsIgnoreCase(url.getProtocol())
                && localURL.getHost().equalsIgnoreCase(url.getHost())
                && localURL.getPort() == url.getPort()
                && url.getPath().startsWith(localURL.getPath());
    }

    /**
     * Checks if server instance identified by serverID is the same as local
     * instance
     *
     * @param serverID server id
     * @return true if serverID is the same as local instance, false otherwise
     */
    public boolean isLocalServer(String serverID) {

        // TODO it appears that in non-failover mode
        // thisSessionServerID == sessionServerID
        // so we could do away without the if()
        if (isSiteEnabled) {
            return thisSessionServerID.equals(serverID);
        } else {
            return sessionServerID.equals(serverID);
        }
    }

    /**
     * Checks if server instance identified by serverID is the same as local
     * instance
     *
     * @param sid server id
     * @return true if serverID is the same as local instance, false otherwise
     */
    public boolean isLocalSite(SessionID sid) {
        String siteID = sid.getSessionServerID();

        if (sessionServerID.equals(siteID) || secondaryServerIDs.contains(siteID)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the local server ID
     *
     * @return The local server ID
     */
    public String getLocalServerID() {
        if (isSiteEnabled) {
            return thisSessionServerID;
        } else {
            return sessionServerID;
        }
    }

    /**
     * Returns the Internal Session corresponding to a Session ID.
     *
     * @param sid Session Id
     */
    public InternalSession getInternalSession(SessionID sid) {

        if (sid == null)
            return null;
        // check if sid is actually a handle return null
        // (in order to prevent from assuming recovery case)
        if (sid.toString().startsWith(SHANDLE_SCHEME_PREFIX)) {
            return null;
        }

        InternalSession is = (InternalSession) sessionTable.get(sid);
        return is;
    }

    /**
     * Returns the Internal Session corresponding to a session handle.
     *
     * @param shandle Session Id
     * @return Internal Session corresponding to a session handle
     */
    public InternalSession getInternalSessionByHandle(String shandle) {
        return (InternalSession) sessionHandleTable.get(shandle);
    }

    /**
     * As opposed to locateSession() this one accepts normal or restricted token
     * This is expected to be only called once the session is detected as local
     *
     * @param token
     * @return
     */
    private InternalSession resolveToken(SessionID token)
            throws SessionException {
        InternalSession sess = (InternalSession) sessionTable.get(token);
        if (sess == null) {
            sess = resolveRestrictedToken(token, true);
        }

        if (sess == null) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionID")
                    + token.toString());
        }
        return sess;
    }

    private InternalSession resolveRestrictedToken(SessionID token,
                                                   boolean checkRestriction) throws SessionException {
        SessionID sid = (SessionID) restrictedTokenMap.get(token);
        if (sid == null)
            return null;
        InternalSession session = (InternalSession) sessionTable.get(sid);
        if (session == null) {
            // orphaned restricted token
            restrictedTokenMap.remove(token);
            return null;
        }
        if (checkRestriction) {
            try {
                TokenRestriction restriction = session
                        .getRestrictionForToken(token);
                if (restriction != null
                        && !restriction.isSatisfied(RestrictedTokenContext
                        .getCurrent())) {
                    throw new SessionException(SessionBundle.rbName,
                            "restrictionViolation", null);
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
     * Get all valid Internal Sessions.
     */
    private List<InternalSession> getValidInternalSessions() {
        List<InternalSession> sessions = new ArrayList<InternalSession>();
        synchronized (sessionTable) {
            Enumeration enumerator = sessionTable.elements();
            while (enumerator.hasMoreElements()) {
                InternalSession sess = (InternalSession) enumerator
                        .nextElement();
                if (sess.getState() == Session.VALID) {
                    if (!sess.isAppSession() || returnAppSession) {
                        sessions.add(sess);
                    }
                }
            }
        }
        return sessions;
    }

    /**
     * Get all valid Internal Sessions matched with pattern.
     */
    private List<InternalSession> getValidInternalSessions(String pattern, int[] status)
            throws SessionException {
        List<InternalSession> sessions = new ArrayList<InternalSession>();

        if (pattern == null) {
            pattern = "*";
        }

        try {
            long startTime = System.currentTimeMillis();

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

                if (sessions.size() == SessionConfigListener.getMaxsize()) {
                    status[0] = IdSearchResults.SIZE_LIMIT_EXCEEDED;
                    break;
                }
                sessions.add(sess);

                if ((System.currentTimeMillis() - startTime) >=
                        SessionConfigListener.getTimeout()) {
                    status[0] = IdSearchResults.TIME_LIMIT_EXCEEDED;
                    break;
                }
            }
        } catch (Exception e) {
            sessionDebug.error("SessionService : "
                    + "Unable to get Session Information ", e);
            throw new SessionException(e);
        }
        return sessions;
    }

    /**
     * Destroy a Internal Session, whose session id has been specified.
     *
     * @param sid
     */
    public void destroyInternalSession(SessionID sid) {
        InternalSession sess = removeInternalSession(sid);
        if (sess != null) {
            sess.setIsISStored(false);
        }
        if (sess != null && sess.getState() != Session.INVALID) {
            logEvent(sess, SessionEvent.DESTROY);
            sess.setState(Session.DESTROYED);
            sendEvent(sess, SessionEvent.DESTROY);
        }
        Session.removeSID(sid);
    }

    /**
     * Logout a Internal Session, whose session id has been specified.
     *
     * @param sid
     */
    public void logoutInternalSession(SessionID sid) {
        InternalSession sess = removeInternalSession(sid);
        if (sess != null) {
            sess.setIsISStored(false);
        }
        if (sess != null && sess.getState() != Session.INVALID) {
            logEvent(sess, SessionEvent.LOGOUT);
            sess.setState(Session.DESTROYED);
            sendEvent(sess, SessionEvent.LOGOUT);
        }
    }

    /**
     * Decrements number of active sessions
     */
    public static synchronized void decrementActiveSessions() {
        // Fix for OPENAM-486: this is a sanity-check for sessioncount, so it
        // can't go below zero any more in case of erroneous behavior..
        if (numberOfActiveSessions > 0) {
            numberOfActiveSessions--;
            if (SystemProperties.isServerMode() && MonitoringUtil.isRunning()) {
                SsoServerSessSvcImpl sessImpl =
                        Agent.getSessSvcMBean();
                sessImpl.decSessionActiveCount();
            }
        }
    }

    /**
     * Increments number of active sessions
     */
    public static synchronized void incrementActiveSessions() {
        numberOfActiveSessions++;
        if (SystemProperties.isServerMode()) {
            if (MonitoringUtil.isRunning()) {
                SsoServerSessSvcImpl sessImpl =
                        Agent.getSessSvcMBean();
                sessImpl.incSessionActiveCount();
            }
        }
    }

    /**
     * Returns number of active sessions
     */
    public static synchronized int getActiveSessions() {
        return numberOfActiveSessions;
    }

    /*
     * Returns current Notification queue size.
     */
    public static int getNotificationQueueSize() {
        return threadPool.getCurrentSize();
    }


    /**
     * Add a listener to a Internal Session.
     *
     * @param session - InternalSession Object
     * @param url
     * @param sid     sid to be used with notification (master or restricted)
     */
    private void addInternalSessionListener(InternalSession session, String url,
                                            SessionID sid) {
        if (session != null) {
            if (!sid.equals(session.getID())
                    && session.getRestrictionForToken(sid) == null) {
                throw new IllegalArgumentException("Session id mismatch");
            }

            Map<String, Set<SessionID>> urls = session.getSessionEventURLs();
            Set<SessionID> sids = urls.get(url);

            if (sids == null) {
                sids = new HashSet<SessionID>();
            }

            sids.add(sid);
            urls.put(url, sids);
            session.updateForFailover();
        }
    }

    /**
     * Add a listener to all Internal Sessions.
     *
     * @param url
     */
    private void addListenerOnAllInternalSessions(String url) {
        if (!sessionEventURLs.contains(url)) {
            sessionEventURLs.addElement(url);
        }
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
    public SessionInfo getSessionInfo(SessionID sid, boolean reset)
            throws SessionException {

        InternalSession sess = resolveToken(sid);
        checkSession(sess, sid);
        SessionInfo info = makeSessionInfo(sess, sid);
        if (reset) {
            sess.setLatestAccessTime();
        }
        return info;
    }

    private SessionInfo makeSessionInfo(InternalSession sess, SessionID sid)
            throws SessionException {
        SessionInfo info = sess.toSessionInfo();
        TokenRestriction restriction = sess.getRestrictionForToken(sid);
        if (restriction != null) {
            try {
                info.properties.put(Session.TOKEN_RESTRICTION_PROP,
                        TokenRestrictionFactory.marshal(restriction));
            } catch (Exception e) {
                throw new SessionException(e);
            }
        } else if (!sid.equals(sess.getID())) {
            throw new IllegalArgumentException("Session id mismatch");
        }
        // replace master sid with the sid from the request (either master or
        // restricted) in order not to leak the master sid
        info.sid = sid.toString();
        return info;
    }

    private void checkSession(InternalSession sess, SessionID sid)
            throws SessionException {
        if (!sid.equals(sess.getID())
                && sess.getRestrictionForToken(sid) == null) {
            throw new IllegalArgumentException("Session id mismatch");
        }

        if (sess.getState() != Session.VALID) {
            if (sess.getTimeLeftBeforePurge() > 0) {
                throw new SessionTimedOutException(SessionBundle
                        .getString("sessionTimedOut")
                        + " " + sid);
            } else {
                throw new SessionException(SessionBundle
                        .getString("invalidSessionState")
                        + " " + sid);
            }
        }
    }

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    public List<SessionInfo> getValidSessions(Session s)
            throws SessionException {
        int status[] = {0};
        return getValidSessions(s, null, status);
    }

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    public List<SessionInfo> getValidSessions(Session s, String pattern, int[] status)
            throws SessionException {
        if (s.getState(false) != Session.VALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + s.getID().toString());
        }

        try {
            AMIdentity user = getUser(s);
            Set orgList = user.getAttribute(
                    "iplanet-am-session-get-valid-sessions");
            if (orgList == null) {
                orgList = Collections.EMPTY_SET;
            }

            List<InternalSession> sessions = sessionService.getValidInternalSessions(pattern, status);
            List<SessionInfo> infos = new ArrayList<SessionInfo>(sessions.size());

            // top level admin gets all sessions
            boolean isTopLevelAdmin = hasTopLevelAdminRole(s);

            for (InternalSession sess : sessions) {
                if (isTopLevelAdmin || orgList.contains(sess.getClientDomain())) {
                    SessionInfo info = sess.toSessionInfo();
                    // replace session id with session handle to prevent from
                    // impersonation
                    info.sid = sess.getSessionHandle();
                    infos.add(info);
                }
            }
            return infos;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Destroy a Internal Session, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @param sid
     * @throws SessionException
     */
    public void destroySession(Session s, SessionID sid)
            throws SessionException {
        if (sid == null)
            return;
        if (s.getState(false) != Session.VALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + sid.toString());
        }
        InternalSession sess = sessionService.getInternalSession(sid);

        if (sess == null) {
            // let us check if the argument is a session handle
            sess = sessionService.getInternalSessionByHandle(sid.toString());
        }

        if (sess != null) {
            sid = sess.getID();
            try {
                // a session can destroy itself or super admin can destroy
                // anyone including another super admin
                if ((s.getID().equals(sid)) || (hasTopLevelAdminRole(s))) {
                    sessionService.destroyInternalSession(sid);
                    return;
                }
                AMIdentity user = getUser(s);
                Set orgList = user
                        .getAttribute("iplanet-am-session-destroy-sessions");
                if (!orgList.contains(s.getClientDomain())) {
                    throw new SessionException(SessionBundle.rbName,
                            "noPrivilege", null);
                }
                sessionService.destroyInternalSession(sid);
            } catch (Exception e) {
                throw new SessionException(e);
            }

        }
    }

    /**
     * Logout the user.
     *
     * @param sid
     * @throws SessionException
     */
    public void logout(SessionID sid) throws SessionException {
        locateSession(sid);
        logoutInternalSession(sid);
    }

    /**
     * Adds listener to a Internal Sessions.
     *
     * @param sid Session ID
     * @param url
     * @throws SessionException Session is null OR the Session is invalid
     */
    public void addSessionListener(SessionID sid, String url)
            throws SessionException {
        InternalSession sess = resolveToken(sid);
        if (sess.getState() == Session.INVALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + sid.toString());
        }
        addInternalSessionListener(sess, url, sid);
    }

    /**
     * Add a listener to all Internal Sessions.
     *
     * @param s
     * @param url
     * @throws SessionException
     */
    public void addSessionListenerOnAllSessions(Session s, String url)
            throws SessionException {
        if (s.getState(false) != Session.VALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + s.getID().toString());
        }
        if (s.getClientID().equals(dsameAdminDN)) {
            addListenerOnAllInternalSessions(url);
            return;
        }
        try {
            AMIdentity user = getUser(s);
            Set values = user.getAttribute(
                    "iplanet-am-session-add-session-listener-on-all-sessions");
            String value = ((values != null) && !values.isEmpty()) ?
                    (String) values.iterator().next() : null;

            if ((value == null) || value.equals("false")) {
                throw new SessionException(SessionBundle.rbName, "noPrivilege",
                        null);
            }
            addListenerOnAllInternalSessions(url);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Sets internal property to the Internal Session.
     *
     * @param sid
     * @param name
     * @param value
     * @throws SessionException
     */
    public void setProperty(SessionID sid, String name, String value)
            throws SessionException {
        resolveToken(sid).putProperty(name, value);
    }

    /**
     * Given a restricted token, returns the SSOTokenID of the master token
     * can only be used if the requester is an app token
     *
     * @param s            Must be an app token
     * @param restrictedID The SSOTokenID of the restricted token
     * @return The SSOTokenID string of the master token
     * @throws SSOException If the master token cannot be dereferenced
     */
    public String deferenceRestrictedID(Session s, String restrictedID)
            throws SessionException {
        SessionID rid = new SessionID(restrictedID);

        // we need to accomodate session failover situation
        if (SessionService.getUseInternalRequestRouting()) {
            //first try
            String hostServerID = getCurrentHostServer(rid);

            if (!isLocalServer(hostServerID)) {
                if (!sessionService.checkServerUp(hostServerID)) {
                    hostServerID = getCurrentHostServer(rid);
                }

                if (!isLocalServer(hostServerID)) {
                    String masterID = deferenceRestrictedIDRemotely(
                            s, Session.getSessionServiceURL(hostServerID), rid);

                    if (masterID == null) {
                        //TODO consider one retry attempt
                        throw new SessionException("unable to get master id remotely " + rid);
                    } else {
                        return masterID;
                    }
                }
            }
        }

        return resolveRestrictedToken(rid, false).getID().toString();
    }

    // sjf bug 6797573
    public String deferenceRestrictedIDRemotely(Session s, URL hostServerID, SessionID sessionID) {
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            String query =
                    "?" + GetHttpSession.OP + "=" + GetHttpSession.DEREFERENCE_RESTRICTED_TOKEN_ID;

            URL url = new URL(hostServerID.getProtocol(),
                    hostServerID.getHost(),
                    hostServerID.getPort(),
                    deploymentURI + "/GetHttpSession" + query);

            HttpURLConnection conn = invokeRemote(url, s.getID(), null);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bs);

            ds.writeUTF(sessionID.toString());
            ds.flush();
            ds.close();

            byte[] getRemotePropertyString = bs.toByteArray();

            conn.setRequestProperty("Content-Length",
                    Integer.toString(getRemotePropertyString.length));

            out = new DataOutputStream(conn.getOutputStream());

            out.write(getRemotePropertyString);
            out.close();
            out = null;

            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            return in.readUTF();

        } catch (Exception ex) {
            sessionDebug.error("Failed to dereference the master token remotely", ex);
        } finally {
            closeStream(in);
            closeStream(out);
        }

        return null;
    }

    /**
     * Locate InternalSession by session id
     *
     * @param sid
     * @throws SessionException
     */
    protected InternalSession locateSession(SessionID sid)
            throws SessionException {
        InternalSession sess = getInternalSession(sid);
        if (sess == null) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionID")
                    + sid.toString());
        }
        return sess;
    }

    /**
     * Sets external property in the Internal Session as long as it is not
     * protected
     *
     * @param clientToken - Token of the client setting external property.
     * @param sid
     * @param name
     * @param value
     * @throws SessionException
     */
    public void setExternalProperty(SSOToken clientToken, SessionID sid,
                                    String name, String value)
            throws SessionException {
        resolveToken(sid).putExternalProperty(clientToken, name, value);
    }

    /**
     * Sends the Internal Session event to the SessionNotificationSender.
     *
     * @param sess    Internal Session.
     * @param evttype Event Type.
     */
    public void sendEvent(InternalSession sess, int evttype) {
        sessionDebug.message("Running sendEvent, type = " + evttype);
        try {
            SessionNotificationSender sns =
                    new SessionNotificationSender(this, sess, evttype);
            // First send local notification. sendToLocal will return
            // true if remote URL's exists than add the notification 
            // to the thread pool to process remote notifications.
            if (sns.sendToLocal()) {
                threadPool.run(sns);
            }

        } catch (ThreadPoolException e) {
            sessionDebug.error("Sending Notification Error: ", e);
        }
    }

    /**
     * Logs the Internal Session Events.
     *
     * @param sess      Internal Session
     * @param eventType event type.
     */
    public void logEvent(InternalSession sess, int eventType) {
        switch (eventType) {
            case 0:
                logIt(sess, "SESSION_CREATED");
                break;
            case 1:
                logIt(sess, "SESSION_IDLE_TIMED_OUT");
                break;
            case 2:
                logIt(sess, "SESSION_MAX_TIMEOUT");
                break;
            case 3:
                logIt(sess, "SESSION_LOGOUT");
                break;
            case 4:
                logIt(sess, "SESSION_REACTIVATION");
                break;
            case 5:
                logIt(sess, "SESSION_DESTROYED");
                break;
            case 6:
                logIt(sess, "SESSION_PROPERTY_CHANGED");
                break;
            case 7:
                logIt(sess, "SESSION_QUOTA_EXHAUSTED");
                break;
            default:
                logIt(sess, "SESSION_UNKNOWN_EVENT");
                break;
        }
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = (Logger) Logger.getLogger(amSSOLogFile);
        }
        return logger;
    }

    private LogMessageProvider getLogMessageProvider()
            throws Exception {

        if (logProvider == null) {
            logProvider =
                    MessageProviderFactory.getProvider(LOG_PROVIDER);
        }
        return logProvider;
    }

    public void logIt(InternalSession sess, String id) {
        if (!logStatus) {
            return;
        }
        try {
            String sidString = sess.getID().toString();
            String clientID = sess.getClientID();
            String uidData = null;
            if ((clientID == null) || (clientID.length() < 1)) {
                uidData = "N/A";
            } else {
                StringTokenizer st = new StringTokenizer(clientID, ",");
                uidData = (st.hasMoreTokens()) ? st.nextToken() : clientID;
            }
            String[] data = {uidData};
            LogRecord lr =
                    getLogMessageProvider().createLogRecord(id, data, null);

            lr.addLogInfo(LogConstants.LOGIN_ID_SID, sidString);

            String amCtxID = sess.getProperty(Constants.AM_CTX_ID);
            String clientDomain = sess.getClientDomain();
            String ipAddress = sess.getProperty("Host");
            String hostName = sess.getProperty("HostName");

            lr.addLogInfo(LogConstants.CONTEXT_ID, amCtxID);
            lr.addLogInfo(LogConstants.LOGIN_ID, clientID);
            lr.addLogInfo(LogConstants.LOG_LEVEL, lr.getLevel().toString());
            lr.addLogInfo(LogConstants.DOMAIN, clientDomain);
            lr.addLogInfo(LogConstants.IP_ADDR, ipAddress);
            lr.addLogInfo(LogConstants.HOST_NAME, hostName);
            getLogger().log(lr, getSessionServiceToken());
        } catch (Exception ex) {
            sessionDebug.error("SessionService.logIt(): " +
                    "Cannot write to the session log file: ", ex);
        }
    }

    public void logSystemMessage(String msgID, Level level) {

        if (!logStatus) {
            return;
        }
        if (errorLogger == null) {
            errorLogger =
                    (Logger) Logger.getLogger(amSSOErrorLogFile);
        }
        try {
            String[] data = {msgID};
            LogRecord lr =
                    getLogMessageProvider().createLogRecord(msgID,
                            data,
                            null);
            SSOToken serviceToken = getSessionServiceToken();
            lr.addLogInfo(LogConstants.LOGIN_ID_SID,
                    serviceToken.getTokenID().toString());
            lr.addLogInfo(LogConstants.LOGIN_ID,
                    serviceToken.getPrincipal().getName());
            errorLogger.log(lr, serviceToken);
        } catch (Exception ex) {
            sessionDebug.error("SessionService.logSystemMessage(): " +
                    "Cannot write to the session error " +
                    "log file: ", ex);
        }
    }

    private SSOTokenManager getSSOTokenManager() throws SSOException {
        if (ssoManager == null) {
            ssoManager = SSOTokenManager.getInstance();
        }
        return ssoManager;
    }

    SSOToken getSessionServiceToken() throws Exception {
        return ((SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance()));
    }

    private SSOToken getAdminToken() throws SSOException {
        if (adminToken == null) {

            adminToken = getSSOTokenManager().createSSOToken(
                    new AuthPrincipal(dsameAdminDN), dsameAdminPassword);
            return adminToken;
        }

        return adminToken;
    }

    /**
     * Private Singleton Session Service.
     */
    private SessionService() {
        // Initialise all CTS fields.
        KeyConversion keyConversion = new KeyConversion();
        tokenIdFactory = new TokenIdFactory(keyConversion);
        coreTokenConfig = new CoreTokenConfig();
        tokenAdapter = new SessionAdapter(
                tokenIdFactory,
                coreTokenConfig,
                new JSONSerialisation(),
                new LDAPDataConversion());

        try {
            dsameAdminDN = (String) AccessController
                    .doPrivileged(new AdminDNAction());
            dsameAdminPassword = (String) AccessController
                    .doPrivileged(new AdminPasswordAction());

            sessionServerProtocol = SystemProperties
                    .get(Constants.AM_SERVER_PROTOCOL);
            sessionServer = SystemProperties.get(Constants.AM_SERVER_HOST);
            sessionServerPort = SystemProperties.get(Constants.AM_SERVER_PORT);
            sessionServerURI = SystemProperties.get(
                    Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

            /*
             * We need to get the session server unique id from the platform
             * server list. Naming table has all the platform servers and the
             * unique key mappings for each server. We will append this server
             * id as part of the session id so that naming service will use this
             * id to find the respective session while decrypting the session id
             */
            sessionServerID = WebtopNaming.getServerID(sessionServerProtocol,
                    sessionServer, sessionServerPort, sessionServerURI);
            isSiteEnabled = WebtopNaming.isSiteEnabled(sessionServerProtocol,
                    sessionServer, sessionServerPort, sessionServerURI);

            if (isSiteEnabled) {
                sessionServerID = WebtopNaming.getSiteID(sessionServerProtocol,
                        sessionServer, sessionServerPort, sessionServerURI);
                String secondaryIDs =
                        WebtopNaming.getSecondarySites(sessionServerProtocol,
                                sessionServer, sessionServerPort, sessionServerURI);
                secondaryServerIDs = new HashSet();

                if (secondaryIDs != null) {
                    if (secondaryIDs.contains("|")) {
                        StringTokenizer st = new StringTokenizer(secondaryIDs, "|");

                        while (st.hasMoreTokens()) {
                            secondaryServerIDs.add(st.nextToken());
                        }
                    } else {
                        secondaryServerIDs.add(secondaryIDs);
                    }
                }

                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                        sessionServerID));
                sessionServerProtocol = sessionServiceID.getProtocol();
                sessionServer = sessionServiceID.getHost();
                sessionServerPort = Integer.toString(
                        sessionServiceID.getPort());
            } else {
                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                        sessionServerID));
            }

            // Obtain the secureRandom instance
            try {
                secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
            } catch (NoSuchProviderException e) {
                secureRandom = SecureRandom.getInstance("SHA1PRNG");
            }

            sessionTable = new Hashtable();
            remoteSessionSet = Collections.synchronizedSet(new HashSet());
            if (stats.isEnabled()) {
                maxSessionStats = new SessionMaxStats(sessionTable);
                stats.addStatsListener(maxSessionStats);
            }

            /*
             * In session failover mode we need to distinguish between server
             * instance own address and the cluster address We will use new set
             * of properties
             *
             * com.iplanet.am.localserver.{protocol,host,port}
             *
             * to point to this instance while existing properties
             *
             * com.iplanet.am.server.{protocol,host,port}
             *
             * will point to the load balancer address
             */

            /**
             * Provide "this" Instance Variables available for either
             * single instance or a site enabled instance.
             *
             * ** Special note the "this" instance or self Globals,
             * must be initialized prior to the PostInit() phase,
             * otherwise Null Pointer Exceptions will occur when
             * referencing these variables during postInit.
             *
             */
                thisSessionServerProtocol = SystemProperties
                        .get(Constants.AM_SERVER_PROTOCOL);
                thisSessionServer = SystemProperties
                        .get(Constants.AM_SERVER_HOST);
                thisSessionServerPortAsString = SystemProperties
                        .get(Constants.AM_SERVER_PORT);
                thisSessionURI = SystemProperties
                        .get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

                if ((thisSessionServerProtocol == null) ||
                        (thisSessionServerPortAsString == null) ||
                        (thisSessionServer == null) ||
                        (thisSessionURI == null)
                        ) {
                    throw new SessionException(SessionBundle.rbName,
                            "propertyMustBeSet", null);
                }
                thisSessionServerPort = Integer
                        .parseInt(thisSessionServerPortAsString);

                thisSessionServerID = WebtopNaming.getServerID(
                        thisSessionServerProtocol, thisSessionServer,
                        thisSessionServerPortAsString, thisSessionURI);

                thisSessionServerURL = thisSessionServerProtocol + "://" +
                        thisSessionServer + ":" + thisSessionServerPortAsString +
                        thisSessionURI;

                thisSessionServiceURL = Session.getSessionServiceURL(
                        thisSessionServerProtocol, thisSessionServer,
                        thisSessionServerPortAsString, thisSessionURI);

            /*
            * Initialize global session parameters. do not move this method to
            * any other place.
            */
            postInit();


        } catch (Exception ex) {
            sessionDebug.error(
                    "SessionService.SessionService(): Initialization Failed",
                    ex);
        }
    } // End of private single constructor.

    /**
     * Initialization Helper Class.
     *
     * @throws Exception
     */
    private synchronized void initializationClusterService() throws Exception {
        int timeout = ClusterStateService.DEFAULT_TIMEOUT;
        try {
            timeout = Integer.parseInt(SystemProperties.get(
                    Constants.
                            AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT,
                    String.valueOf(
                            ClusterStateService.DEFAULT_TIMEOUT)));
        } catch (Exception e) {
            sessionDebug.error("Invalid value for " +
                    Constants.
                            AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT
                    + ", using default");
        }

        long period = ClusterStateService.DEFAULT_PERIOD;
        try {
            period = Integer.parseInt(SystemProperties.get(
                    Constants.
                            AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD,
                    String.valueOf(
                            ClusterStateService.DEFAULT_PERIOD)));
        } catch (Exception e) {
            sessionDebug.error("Invalid value for "
                    + Constants.
                    AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD
                    + ", using default");
        }
        // Initialize Our Cluster State Service
        // Ensure we place our Server in Member Map.
        clusterMemberMap.put(thisSessionServerID, thisSessionServiceURL.toExternalForm());
        // Instantiate the State Service.
        clusterStateService = new ClusterStateService(this, thisSessionServerID, timeout, period, clusterMemberMap);
        // Show our State Server Info Map
        if (sessionDebug.messageEnabled())
            { sessionDebug.message("SessionService's ClusterStateService Initialized Successfully, "+
                    clusterStateService.toString());
            }
    }

    /**
     * This is a key method for "internal request routing" mode It determines
     * the server id which is currently hosting session identified by sid. In
     * "internal request routing" mode, this method also has a side effect of
     * releasing a session which no longer "belongs locally" (e.g., due to
     * primary server instance restart)
     *
     * @param sid session id
     * @return server id for the server instance determined to be the current
     *         host
     * @throws SessionException
     */
    public String getCurrentHostServer(SessionID sid) throws SessionException {
        if (!isSessionFailoverEnabled) {
            return sid.getSessionServerID();
        } else {
            if (getUseInternalRequestRouting()) {
                String serverID = locateCurrentHostServer(sid);
                if (serverID == null) {
                    return sid.getSessionServerID();
                }
                // if we happen to have local session replica
                // get rid of it, as hosting server instance
                // is not supposed to be local
                if (!isLocalServer(serverID)) {
                    // actively clean up duplicates
                    handleReleaseSession(sid);
                }
                return serverID;
            } else {
                return sid.getSessionServerID();
            }
        }
    }

    /**
     * Determines current hosting server instance for internal request routing
     * mode.
     *
     * @param sid session id
     * @return server id for the server instance determined to be the current
     *         host
     * @throws SessionException
     */
    private String locateCurrentHostServer(SessionID sid)
            throws SessionException {
        String primaryID = sid.getExtension(SessionID.PRIMARY_ID);
        String serverID = sid.getSessionServerID();
        // if this is our local Server
        if (serverID.equalsIgnoreCase(this.getLocalServerID())) {
            return serverID;
        }
        // if session is from remote site
        if (!serverID.equals(sessionServerID)) {
            return serverID;
        }

        // Ensure we have a Cluster State Service Available.
        synchronized (this) {
            if (clusterStateService == null) {
                try {
                    initializationClusterService();
                } catch (Exception e) {
                    sessionDebug.error("Unable to Initialize the Cluster Service, please review Configuration settings.", e);
                    throw new SessionException(e);
                }
            }
        }

        // Check for Service Available.
        if (clusterStateService.isUp(primaryID)) {
            return primaryID;
        } else {
            int selectionListSize = clusterStateService
                    .getServerSelectionListSize();
            PermutationGenerator perm = new PermutationGenerator(sid
                    .getExtension(SessionID.STORAGE_KEY).hashCode(),
                    selectionListSize);

            String selectedServerId = null;
            for (int i = 0; i < selectionListSize; ++i) {
                selectedServerId = clusterStateService.getServerSelection(perm
                        .itemAt(i));
                if (selectedServerId == null) {
                    continue;
                }
                if (clusterStateService.isUp(selectedServerId)) {
                    break;
                }
            }

            // since current server is also part of the selection list
            // selection process is guaranteed to succeed
            return selectedServerId;
        }
    }

    /**
     * Indicates whether server is running in "internal request routing" mode
     *
     * @return true if internal request routing is enabled, false otherwise
     */
    static public boolean getUseInternalRequestRouting() {
        if (isSessionFailoverEnabled) {
            return useInternalRequestRouting;
        }
        return false;
    }

    static public void setSessionTrimmingEnabled(boolean value) {
        isSessionTrimmingEnabled = value;
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("SessionService.setSessionTrimmingEnabled()="
                    + isSessionTrimmingEnabled);
        }
    }

    static public boolean isSessionTrimmingEnabled() {
        return isSessionTrimmingEnabled;
    }

    static public void setSessionConstraintEnabled(boolean value) {
        isSessionConstraintEnabled = value;
    }

    static public boolean isSessionConstraintEnabled() {
        return isSessionConstraintEnabled;
    }

    static public void setDenyLoginIfDBIsDown(boolean value) {
        denyLoginIfDBIsDown = value;
    }

    static public boolean denyLoginIfDBIsDown() {
        return denyLoginIfDBIsDown;
    }

    public static String getConstraintHandler() {
        return constraintHandler;
    }

    public static void setConstraintHandler(String val) {
        constraintHandler = val;
    }

    /**
     * Utility helper method to obtain session repository reference
     *
     * @return reference to session repository
     */
    protected static CTSPersistentStore getRepository() {
        if (!getUseInternalRequestRouting()) {
            sessionDebug.warning("Not Using Internal Request Routing, unable to provide Session Storage!");
            return null;
        }
        if (coreTokenService == null) {
            coreTokenService = InjectorHolder.getInstance(CTSPersistentStore.class);
        }
        return coreTokenService;
    }

    /**
     * Actively check if server identified by serverID is up
     *
     * @param serverID server id
     * @return true if server is up, false otherwise
     */
    public boolean checkServerUp(String serverID) {
        return ((serverID == null) || (serverID.isEmpty())) ? false : clusterStateService.checkServerUp(serverID);
    }

    /**
     * Post Initialization
     */
    private void postInit() {
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                    amSessionService, getAdminToken());

            ServiceSchema schema = ssm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();

            String notificationStr = CollectionHelper.getMapAttr(
                    attrs, Constants.PROPERTY_CHANGE_NOTIFICATION, "OFF");
            if (notificationStr.equalsIgnoreCase("ON")) {
                isPropertyNotificationEnabled = true;
                notificationProperties = (Set) attrs
                        .get(Constants.NOTIFICATION_PROPERTY_LIST);
            }

            timeoutHandlers = (Set<String>) attrs.get(Constants.TIMEOUT_HANDLER_LIST);

            String trimSessionStr = CollectionHelper.getMapAttr(
                    attrs, Constants.ENABLE_TRIM_SESSION, "NO");
            if (trimSessionStr.equalsIgnoreCase("YES")) {
                isSessionTrimmingEnabled = true;
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("SessionService.postInit():" +
                        " isSessionTrimmingEnabled=" + isSessionTrimmingEnabled);
            }

            String constraintStr = CollectionHelper.getMapAttr(
                    attrs, SESSION_CONSTRAINT, "OFF");
            if (constraintStr.equalsIgnoreCase("ON")) {
                isSessionConstraintEnabled = true;
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("isSessionConstraintEnabled="
                        + isSessionConstraintEnabled);
            }

            String denyLoginStr = CollectionHelper.getMapAttr(attrs,
                    DENY_LOGIN_IF_DB_IS_DOWN, "NO");
            if (denyLoginStr.equalsIgnoreCase("YES")) {
                denyLoginIfDBIsDown = true;
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("SessionService.postInit: " +
                        "denyLoginIfDBIsDown=" + denyLoginIfDBIsDown);
            }

            constraintHandler = CollectionHelper.getMapAttr(attrs,
                    CONSTRAINT_HANDLER,
                    SessionConstraint.DESTROY_OLDEST_SESSION_CLASS);

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("Resulting behavior if session "
                        + "quota exhausted:" + constraintHandler);
            }

            maxWaitTimeForConstraint = Integer.parseInt(
                    CollectionHelper.getMapAttr(attrs,
                            MAX_WAIT_TIME_FOR_CONSTARINT, "6000"));

            ServiceConfigManager scm = new ServiceConfigManager(
                    amSessionService, getAdminToken());
            ServiceConfig serviceConfig = scm.getGlobalConfig(null);

            /* in OpenSSO 8.0, we have switched to create sub
            * configuration with
            * site name. hence we need to lookup the site name based on the URL
            */
            String subCfgName = (ServerConfiguration.isLegacy(adminToken)) ?
                    sessionServiceID.toString() :
                    SiteConfiguration.getSiteIdByURL(adminToken,
                            sessionServiceID.toString());
            ServiceConfig subConfig = serviceConfig.getSubConfig(subCfgName);

            if ((subConfig != null) && subConfig.exists()) {

                Map sessionAttrs = subConfig.getAttributes();
                boolean sfoEnabled = Boolean.valueOf(
                        CollectionHelper.getMapAttr(
                                sessionAttrs, CoreTokenConstants.IS_SFO_ENABLED, "false")
                ).booleanValue();
                // Currently, we are not allowing to default to Session Failover HA,
                // even with a single server to enable session persistence.
                // But can easily be turned on in the Session SubConfig.
                if (sfoEnabled) {

                    isSessionFailoverEnabled = true;

                    useRemoteSaveMethod = true;

                    useInternalRequestRouting = true;

                    // Obtain Site Ids
                    Set<String> serverIDs = WebtopNaming.getSiteNodes(sessionServerID);
                    if ((serverIDs == null) || (serverIDs.isEmpty())) {
                        serverIDs = new HashSet<String>();
                        serverIDs.add(this.getLocalServerID());
                    }
                    // Next, must be in this Order!
                    // Initialize our Cluster Member Map, first!
                    initClusterMemberMap(serverIDs);
                    // Initialize the Cluster State Service, second!
                    // (As Cluster Service uses Cluster Member Map).
                    initializationClusterService();

                    // ************************************************************************
                    // Now Bootstrap CoreTokenService Implementation, if one was specified.
                    if (coreTokenService == null) {
                        // Instantiate our Session Repository Implementation.
                        // Allows Static Elements to Initialize.
                        coreTokenService = getRepository();
                        sessionDebug.message("amTokenRepository Implementation: " +
                                ((coreTokenService == null) ? "None" : coreTokenService.getClass().getSimpleName()));
                    }
                } // End of sfoEnabled check.
            } // End of Sub-Configuration Existence check.

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("Session Failover Enabled = "
                        + isSessionFailoverEnabled);
            }
            SessionConfigListener utils = new SessionConfigListener(ssm);
            ssm.addListener(utils);
            utils.schemaChanged(amSessionService, null);
        } catch (Exception ex) {
            sessionDebug.error("SessionService.postInit(): "
                    + "Unable to get Session Schema Information", ex);
        }
    }

    /**
     * This method will execute all the globally set session timeout handlers
     * with the corresponding timeout event simultaniously.
     *
     * @param sessionId  The timed out sessions ID
     * @param changeType Type of the timeout event: IDLE_TIMEOUT (1) or MAX_TIMEOUT (2)
     */
    static void execSessionTimeoutHandlers(final SessionID sessionId, final int changeType) {
        // Take snapshot of reference to ensure atomicity.
        final Set<String> handlers = timeoutHandlers;

        if (!handlers.isEmpty()) {
            try {
                final SSOToken token = ssoManager.createSSOToken(sessionId.toString());
                final List<Future<?>> futures = new ArrayList<Future<?>>();
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
     * Initialize the cluster server map given the server IDs in Set (AM70).
     */
    private void initClusterMemberMap(Set serverIDs) throws Exception {
        for (Iterator m = serverIDs.iterator(); m.hasNext(); ) {
            String serverID = (String) m.next();
            String serverURL = WebtopNaming.getServerFromID(serverID);
            if ((serverID == null) || (serverURL == null)) {
                continue;
            }
            // ************************************************************
            // This if Clause is very important, please do not think it is
            // not.  If we pollute the cluster map with duplicate URLs
            // There is a very good chance Login processing will
            // automatically fail, since it can not determine
            // which serverId is which.
            // Only Associate one Server URL to a Single ServerID.
            // @since 10.1
            //
            // Further investigate, as we should not get duplicates here...
            //
            if (!clusterMemberMap.containsValue(serverURL))
                { clusterMemberMap.put(serverID, serverURL); }
        }
    }

    /**
     * Initialize the cluster server map given the server IDs in Set.
     * Invoked by NamingService whenever any global configuration changes occur.
     */
    public void ReInitClusterMemberMap() throws Exception {
        Set serverIDs = null;

        if (isSessionFailoverEnabled()) {
            serverIDs = WebtopNaming.getSiteNodes(this.getLocalServerID());
            if ((serverIDs == null) || (serverIDs.isEmpty())) {
                serverIDs = WebtopNaming.getSiteNodes(thisSessionServerID);
            }
            initClusterMemberMap(serverIDs);
        }
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("Re-Initialized ClusterServerList=" + getClusterServerList());
        }
    }

    /**
     * A convenience method to get the cluster server list in delimiter
     * separated String format. This is currently used in message debug log.
     */
    private String getClusterServerList() {

        StringBuilder clusterServerList = new StringBuilder();
        Set serverIDs = clusterMemberMap.keySet();
        for (Iterator m = serverIDs.iterator(); m.hasNext(); ) {
            String serverID = (String) m.next();
            clusterServerList.append(serverID).append(" ");
        }
        return clusterServerList.toString();
    }

    /**
     * Inner Session Notification Publisher Class Thread.
     */
    class SessionNotificationSender implements Runnable {

        private SessionService sessionService;

        private InternalSession session;

        private int eventType;

        SessionNotificationSender(SessionService ss, InternalSession sess,
                                  int evttype) {
            sessionService = ss;
            session = sess;
            eventType = evttype;
        }

        /**
         * returns true if remote URL exists else returns false.
         */
        boolean sendToLocal() {
            boolean remoteURLExists = false;
            Map<String, Set<SessionID>> urls = session.getSessionEventURLs();   
            // CHECK THE GLOBAL URLS FIRST
            if (!sessionService.sessionEventURLs.isEmpty()) {
                Enumeration aenum = sessionService.sessionEventURLs.elements();

                SessionNotification snGlobal = new SessionNotification(session
                        .toSessionInfo(), eventType, System.currentTimeMillis());

                while (aenum.hasMoreElements()) {
                    String url = (String) aenum.nextElement();
                    try {
                        URL parsedUrl = new URL(url);
                        if (sessionService.isLocalSessionService(parsedUrl)) {
                            SessionNotificationHandler.handler
                                    .processNotification(snGlobal);
                            // remove this URL from the individual url list
                            urls.remove(url);
                        } else {
                            remoteURLExists = true;
                        }
                        // If the Global notification is processed successfully
                        // than no need to send individual notification.

                    } catch (Exception e) {
                        sessionService.sessionDebug.error(
                                "Local Global notification to " + url, e);
                    }
                }
            }

            // CHECK THE INDVIDUAL URL LIST
            if (!urls.isEmpty()) {
                synchronized (urls) {
                    for (Map.Entry<String, Set<SessionID>> entry : urls.entrySet()) {
                        // ONLY SEND ONCE TO ONE LOCATION
                        String url = entry.getKey();

                        try {
                            URL parsedUrl = new URL(url);

                            if (sessionService.isLocalSessionService(parsedUrl)) {
                                for (SessionID sid : entry.getValue()) {
                                    SessionInfo info = makeSessionInfo(session, sid);
                                    SessionNotification sn = new SessionNotification(
                                            info, eventType, System.currentTimeMillis());
                                    SessionNotificationHandler.handler.processNotification(sn);
                                }
                            } else {
                                remoteURLExists = true;
                            }
                        } catch (Exception e) {
                            sessionService.sessionDebug.error(
                                "Local Individual notification to " + url, e);
                        }
                    }
                }
            }
            return remoteURLExists;
        }

        /**
         * Thread which sends the Session Notification.
         */
        public void run() {
            Map<String, Set<SessionID>> urls = session.getSessionEventURLs();
            if (!sessionService.sessionEventURLs.isEmpty()) {

                SessionNotification snGlobal = new SessionNotification(session
                        .toSessionInfo(), eventType, System.currentTimeMillis());
                Notification notGlobal = new Notification(snGlobal
                        .toXMLString());
                NotificationSet setGlobal = new NotificationSet(SESSION_SERVICE);
                setGlobal.addNotification(notGlobal);

                // CHECK THE GLOBAL URLS FIRST
                Enumeration aenum = sessionService.sessionEventURLs.elements();

                while (aenum.hasMoreElements()) {
                    String url = (String) aenum.nextElement();
                    try {
                        URL parsedUrl = new URL(url);
                        // ONLY SEND TO REMOTE URL
                        if (!sessionService.isLocalSessionService(parsedUrl)) {
                            PLLServer.send(parsedUrl, setGlobal);
                            //remove this url from the indvidual url list.
                            urls.remove(url);
                        }
                    } catch (Exception e) {
                        sessionService.sessionDebug.error(
                                "Remote Global notification to " + url, e);
                    }
                }
            }
            // CHECK THE INDIVIDUAL URLS LIST
            if (!urls.isEmpty()) {
                synchronized (urls) {
                    for (Map.Entry<String, Set<SessionID>> entry: urls.entrySet()) {
                        String url = (String) entry.getKey();
                        // ONLY SEND ONCE TO ONE LOCATION

                        try {
                            URL parsedUrl = new URL(url);

                            if (!sessionService.isLocalSessionService(parsedUrl)) {
                                for (SessionID sid : entry.getValue()) {
                                    SessionInfo info = makeSessionInfo(session, sid);
                                    SessionNotification sn = new SessionNotification(
                                        info, eventType, System.currentTimeMillis());
                                    Notification not = new Notification(sn.toXMLString());
                                    NotificationSet set = new NotificationSet(SESSION_SERVICE);
                                    set.addNotification(not);
                                    PLLServer.send(parsedUrl, set);
                                }
                            }
                        } catch (Exception e) {
                            sessionService.sessionDebug.error(
                                "Remote Individual notification to " + url, e);
                        }
                    }
                }
            }
        }
    } // End of SessionNotificationSender Inner Class.

    /**
     * Returns the User of the Session
     *
     * @param s Session
     * @throws SessionException
     * @throws SSOException
     */
    private AMIdentity getUser(Session s)
            throws SessionException, SSOException {
        SSOToken ssoSession = getSSOTokenManager().createSSOToken(
                s.getID().toString());
        AMIdentity user = null;
        try {
            user = IdUtils.getIdentity(ssoSession);
        } catch (IdRepoException e) {
            sessionDebug.error(
                    "SessionService: failed to get the user's identity object", e);
        }
        return user;
    }

    /**
     * Returns true if the user has top level admin role
     *
     * @param s Session.
     * @throws SessionException
     * @throws SSOException
     */
    private boolean hasTopLevelAdminRole(Session s)
            throws SessionException, SSOException {
        SSOToken ssoSession = getSSOTokenManager().createSSOToken(
                s.getID().toString());
        return hasTopLevelAdminRole(ssoSession, s.getClientID());
    }

    /**
     * Returns true if the user has top level admin role
     *
     * @param tokenUsedForSearch Single Sign on token used to do the search.
     * @param clientID           Client ID of the login user.
     * @throws SessionException
     * @throws SSOException
     */
    private boolean hasTopLevelAdminRole(
            SSOToken tokenUsedForSearch,
            String clientID
    ) throws SessionException, SSOException {
        boolean topLevelAdmin = false;
        Set actions = new HashSet();
        actions.add(PERMISSION_READ);
        actions.add(PERMISSION_MODIFY);
        actions.add(PERMISSION_DELEGATE);
        try {
            DelegationPermission perm = new DelegationPermission(
                    "/", "*", "*", "*", "*", actions, Collections.EMPTY_MAP);
            DelegationEvaluator evaluator = new DelegationEvaluator();
            topLevelAdmin = evaluator.isAllowed(
                    tokenUsedForSearch, perm, Collections.EMPTY_MAP);
        } catch (DelegationException de) {
            sessionDebug.error("SessionService.hasTopLevelAdminRole: " +
                    "failed to check the delegation permission.", de);
        }
        return topLevelAdmin;
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
            String adminUser = SystemProperties.get(
                    Constants.AUTHENTICATION_SUPER_USER);
            if (adminUser != null) {
                adminUserId = new AMIdentity(getAdminToken(),
                        adminUser, IdType.USER, "/", null);
            }

            //Get the AMIdentity Object for login user
            AMIdentity user =
                    IdUtils.getIdentity(getAdminToken(), uuid);

            //Check for the equality
            isSuperUser = user.equals(adminUserId);

        } catch (SSOException ssoe) {
            sessionDebug.error("SessionService.isSuperUser:" +
                    "Cannot get the admin token for this operation.");
        } catch (IdRepoException idme) {
            sessionDebug.error("SessionService.isSuperUser:" +
                    "Cannot get the user identity.");
        }

        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("SessionService.isSuperUser: "
                    + isSuperUser);
        }

        return isSuperUser;
    }

    /**
     * Creates InternalSession which is always coupled with Http session This is
     * only used in session failover mode to ensure that every internal session
     * is associated with Http session used as fail-over store
     *
     * @param domain authentication domain passed to newInternalSession
     */

    private InternalSession createSession(String domain) {

        DataInputStream in = null;

        try {
            String query = "?" + GetHttpSession.OP + "="
                    + GetHttpSession.CREATE_OP;

            if (domain != null) {
                query += "&" + GetHttpSession.DOMAIN + "="
                        + URLEncDec.encode(domain);
            }

            String routingCookie = null;

            URL url = new URL(thisSessionServerProtocol, thisSessionServer,
                    thisSessionServerPort, deploymentURI
                    + "/GetHttpSession" + query);
            HttpURLConnection conn = invokeRemote(url, null, routingCookie);
            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            SessionID sid = new SessionID(in.readUTF());
            return (InternalSession) sessionTable.get(sid);

        } catch (Exception ex) {
            sessionDebug.error("Failed to retrieve new session", ex);
        } finally {
            closeStream(in);
        }

        return null;
    }

    /**
     * This functions invalidates the http session associated with identity
     * session specified by sid
     *
     * @param sid
     * @return
     */

    private boolean invalidateHttpSession(SessionID sid) {

        if (!isSessionFailoverEnabled || sid.getTail() == null) {
            return true;
        }

        DataInputStream in = null;
        URL url = null;

        try {
            String query = "?" + GetHttpSession.OP + "="
                    + GetHttpSession.INVALIDATE_OP;

            url = new URL(thisSessionServerProtocol, thisSessionServer,
                    thisSessionServerPort, deploymentURI + "/GetHttpSession"
                    + query);

            HttpURLConnection conn = invokeRemote(url, sid, null);
            in = new DataInputStream(conn.getInputStream());

            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (ConnectException ex) {
            if (sessionDebug.messageEnabled()) {
                sessionDebug
                        .message("invalidateHttpSesion: failed to connect to  "
                                + url);
            }
            return true;
        } catch (Exception ex) {
            sessionDebug.error("Failed to invalidate session", ex);
        } finally {
            closeStream(in);
        }

        return false;
    }

    /**
     * Helper function to handle stream closure and associated exceptions
     */

    static void closeStream(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                sessionDebug.error("Unable to close input", e);
            }
        }
    }

    /**
     * Helper function to handle stream closure and associated exceptions
     */

    static void closeStream(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                sessionDebug.error("Unable to close output", e);
            }
        }
    }

    /**
     * Removes InternalSession from the session table so that another server
     * instance can be an owner This is used to help work around persistent
     * association of OpenSSO session id and Http session id which
     * some loadbalancers (like Weblogic) use to make routing decisions. This
     * helps to deal with the case a session is migrated while the current owner
     * is still alive to avoid having redundant copies of the session. This is
     * the client side of distributed invocation
     *
     * @param owner url of the server instance who previously owned the session
     * @param sid   session id of the session migrated
     */
    private boolean releaseSession(URL owner, SessionID sid) {
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("Attempting to release InternalSession " + sid
                    + " from server instance: " + owner);
        }

        DataInputStream in = null;
        URL url = null;

        try {
            String query = "?" + GetHttpSession.OP + "="
                    + GetHttpSession.RELEASE_OP;

            url = new URL(owner.getProtocol(), owner.getHost(),
                    owner.getPort(), deploymentURI + "/GetHttpSession" + query);

            HttpURLConnection conn = invokeRemote(url, sid, null);
            in = new DataInputStream(conn.getInputStream());

            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (ConnectException ex) {
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("releaseSession: failed to connect to  "
                        + url);
            }
            return true;
        } catch (Exception ex) {
            sessionDebug.error("Failed to release session", ex);
        } finally {
            closeStream(in);
        }

        return false;
    }

    /**
     * Removes InternalSession from the session table so that another server
     * instance can be an owner This is the server side of distributed
     * invocation initiated by calling releaseSession()
     *
     * @param sid session id of the session migrated
     */
    int handleReleaseSession(SessionID sid) {
        if (!isSessionFailoverEnabled) {
            return HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        }

        // switch to non-local mode for cached cient side session
        // image
        Session.markNonLocal(sid);
        InternalSession is = (InternalSession) sessionTable.remove(sid);
        if (is != null) {
            is.cancel();
            removeSessionHandle(is);
            removeRestrictedTokens(is);
        } else {
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("releaseSession: session not found  "
                        + sid);
            }
        }
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * If InternalSession is not present, we attempt to recover its state from
     * associated HttpSession. We have to set the session tracking cookie to
     * HttpID which is present in the SessionID object. This will work in the
     * fail over cases. We first get the HttpSession by invoking the
     * GetHttpSession Servlet on the SAME server instance this code is invoked.
     * This should trigger the Web container to perform recovery of the
     * associated Http session
     * <p/>
     * We also pass the SessionID to the servlet to double check the match
     * between the session id and Http session
     * <p/>
     * This is the "client side" of the remote invocation. The servlet will call
     * retrieveSession() to complete the work
     *
     * @param sid Session ID
     */
    InternalSession recoverSession(SessionID sid) {
        if (!isSessionFailoverEnabled) {
            return null;
        }

        if (getUseInternalRequestRouting()) {
            InternalSession sess = null;
            try {
                String tokenId = tokenIdFactory.toSessionTokenId(sid);
                Token token = getRepository().read(tokenId);

                if (token == null) {
                    return sess;
                }

                sess = tokenAdapter.fromToken(token);
                updateSessionMaps(sess);
            } catch (CoreTokenException e) {
                sessionDebug.error("Failed to retrieve new session", e);
            }
            return sess;

        } else {
            if (sessionDebug.messageEnabled()) {
                sessionDebug
                        .message("Recovering InternalSession from HttpSession: "
                                + sid);
            }

            DataInputStream in = null;

            InternalSession sess = null;

            try {
                String query = "?" + GetHttpSession.OP + "="
                        + GetHttpSession.RECOVER_OP;

                URL url = new URL(thisSessionServerProtocol, thisSessionServer,
                        thisSessionServerPort, deploymentURI
                        + "/GetHttpSession" + query);

                HttpURLConnection conn = invokeRemote(url, sid, null);
                in = new DataInputStream(conn.getInputStream());

                sess = (InternalSession) sessionTable.get(sid);
                if (sess == null) {
                    sess = resolveRestrictedToken(sid, false);
                }

            } catch (Exception ex) {
                sessionDebug.error("Failed to retrieve new session", ex);
            } finally {
                closeStream(in);
            }

            return sess;
        }
    }

    /**
     * This is the "server side" of the remote invocation for recoverSession()
     * It is being called by GetHttpSession servlet to complete the work
     * <p/>
     * If recovery is possible we need to first notify existing server instance
     * "owning" the session (if any) to release the session instance otherwise
     * we end up with duplicates
     *
     * @param sid Session ID
     */

    InternalSession retrieveSession(SessionID sid, HttpSession httpSession) {
        if (isSessionFailoverEnabled && httpSession != null) {
            String sessionState = (String) httpSession
                    .getAttribute(httpSessionPropertyName);
            if (sessionState == null) {
                sessionDebug
                        .message("GISFHS-No InternalSession in HttpSession");
                return null;
            } else {
                InternalSession sess = decrypt(sessionState);

                if (sess == null
                        || (sess.getRestrictionForToken(sid) == null && !sess
                        .getID().equals(sid))) {
                    return null;
                }

                // tell all previously registered owners of this session
                // who might still potentially have a copy to release it
                // (we do not expect a session to migrate more than one
                // time so typically this list will have a size of 2).

                Set ownerList = (Set) httpSession
                        .getAttribute(httpSessionOwnerListPropertyName);
                if (ownerList == null) {
                    ownerList = new HashSet();
                    httpSession.setAttribute(httpSessionOwnerListPropertyName,
                            ownerList);
                }
                for (Iterator iter = ownerList.iterator(); iter.hasNext(); ) {
                    URL formerOwner = (URL) iter.next();
                    if (isLocalSessionService(formerOwner))
                        continue;

                    if (!releaseSession(formerOwner, sess.getID())) {
                        return null;
                    }
                }

                ownerList.add(thisSessionServiceURL);
                // add current server to the list of former owners
                httpSession.setAttribute(httpSessionOwnerListPropertyName,
                        ownerList);

                sess.setHttpSession(httpSession);
                updateSessionMaps(sess);
                return sess;
            }
        }
        return null;
    }

    /**
     * Utility used to updated various cross-reference mapping data structures
     * associated with sessions up-to-date when sessions are being recovered
     * after server instance failure
     *
     * @param sess session object
     */
    private void updateSessionMaps(InternalSession sess) {
        if (sess == null)
            return;

        if (checkIfShouldDestroy(sess))
            return;

        sess.putProperty(Session.lbCookieName,
                WebtopNaming.getLBCookieValue(getLocalServerID()));
        if (getUseInternalRequestRouting()) {
            SessionID sid = sess.getID();
            String primaryID = sid.getExtension(SessionID.PRIMARY_ID);
            if (!isLocalServer(primaryID)) {
                remoteSessionSet.add(sid);
            }
        }
        sessionTable.put(sess.getID(), sess);

        String sessionHandle = sess.getSessionHandle();
        if (sessionHandle != null) {
            sessionHandleTable.put(sessionHandle, sess);
        }
        Object[] tokens = sess.getRestrictedTokens();
        for (int i = 0; i < tokens.length; ++i) {
            restrictedTokenMap.put(tokens[i], sess.getID());
        }
    }

    /**
     * function to remove remote sessions when primary server is up
     */
    public void cleanUpRemoteSessions() {
        if (getUseInternalRequestRouting()) {
            synchronized (remoteSessionSet) {
                for (Iterator iter = remoteSessionSet.iterator();
                     iter.hasNext(); ) {
                    SessionID sid = (SessionID) iter.next();
                    // getCurrentHostServer automatically releases local
                    // session replica if it does not belong locally
                    String hostServer = null;
                    try {
                        hostServer = getCurrentHostServer(sid);
                    } catch (Exception ex) {
                    }
                    // if session does not belong locally remove it
                    if (!isLocalServer(hostServer)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * Utility method to check if session has to be destroyed and to remove it
     * if so Note that contrary to the name sess.shouldDestroy() has non-trivial
     * side effects of changing session state and sending notification messages!
     *
     * @param sess session object
     * @return true if session should (and has !) been destroyed
     */
    boolean checkIfShouldDestroy(InternalSession sess) {
        boolean shouldDestroy = false;
        try {
            shouldDestroy = sess.shouldDestroy();
        } catch (Exception ex) {
            sessionDebug.error("Exception in session shouldDestroy() : ", ex);
            shouldDestroy = true;
        }

        if (shouldDestroy) {
            try {
                sessionService.removeInternalSession(sess.getID());
            } catch (Exception ex) {
                sessionDebug.error("Exception while removing session : ", ex);
            }
        }
        return shouldDestroy;
    }

    /**
     * This is used to save session state using remote method rather than
     * directly using a reference to HttpSession saved in the InternalSession
     * which is not guaranteed to survive across http requests. We set the
     * session tracking cookie to HttpID which is present in the SessionID
     * object. We first get the HttpSession by invoking the GetHttpSession
     * Servlet on the SAME server instance this code is invoked. This should
     * trigger the Web container to provide a valid instance of the associated
     * Http session and then use it to save the session
     * <p/>
     * This is the "client side" of the remote invocation. The servlet will call
     * handleSaveSession() to complete the work
     *
     * @param sid Session ID
     */
    boolean saveSession(SessionID sid) {
        if (!isSessionFailoverEnabled) {
            return false;
        }
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("Saving internal session using remote method "
                    + sid);
        }

        InputStream in = null;

        try {
            String query = "?" + GetHttpSession.OP + "="
                    + GetHttpSession.SAVE_OP;

            URL url = new URL(thisSessionServerProtocol, thisSessionServer,
                    thisSessionServerPort, deploymentURI + "/GetHttpSession"
                    + query);

            HttpURLConnection conn = invokeRemote(url, sid, null);
            in = conn.getInputStream();

            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;

        } catch (Exception ex) {
            sessionDebug.error("Failed to save session", ex);
        } finally {
            closeStream(in);
        }
        return false;
    }

    /**
     * This is the "server side" of the remote invocation for saveSession() It
     * is being called by GetHttpSession servlet to complete the work
     *
     * @param sid         master Session ID
     * @param httpSession http session
     */

    int handleSaveSession(SessionID sid, HttpSession httpSession) {
        if (!isSessionFailoverEnabled) {
            return HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        }
        InternalSession is = (InternalSession) sessionTable.get(sid);

        if (is == null) {
            sessionDebug.error("handleSaveSession: session not found " + sid);
            return HttpURLConnection.HTTP_NOT_FOUND;
        }
        if (!is.getID().getTail().equals(extractHttpSessionId(httpSession))) {
            sessionDebug.error(
                    "handleSaveSession: http session id does not match sid "
                            + sid);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        doSaveSession(is, httpSession);

        return HttpURLConnection.HTTP_OK;
    }

    /**
     * This method is used to create restricted token
     *
     * @param owner       server instance URL
     * @param masterSid   SessionID
     * @param restriction restriction
     */

    private String getRestrictedTokenIdRemotely(URL owner, SessionID masterSid,
                                                TokenRestriction restriction) {

        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            String query = "?" + GetHttpSession.OP + "="
                    + GetHttpSession.GET_RESTRICTED_TOKEN_OP;

            URL url = new URL(owner.getProtocol(), owner.getHost(), owner
                    .getPort(), deploymentURI + "/GetHttpSession" + query);

            HttpURLConnection conn = invokeRemote(url, masterSid, null);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bs);

            ds.writeUTF(TokenRestrictionFactory.marshal(restriction));
            ds.flush();
            ds.close();

            byte[] marshalledRestriction = bs.toByteArray();

            conn.setRequestProperty("Content-Length", Integer
                    .toString(marshalledRestriction.length));

            out = new DataOutputStream(conn.getOutputStream());

            out.write(marshalledRestriction);
            out.close();
            out = null;

            in = new DataInputStream(conn.getInputStream());

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            return in.readUTF();

        } catch (Exception ex) {
            sessionDebug
                    .error("Failed to create restricted token remotely", ex);
        } finally {
            closeStream(in);
            closeStream(out);
        }

        return null;
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
            return doGetRestrictedTokenId(masterSid, restriction);
        } catch (Exception ex) {
            sessionDebug
                    .error("Failed to create restricted token remotely", ex);
        }
        return null;
    }

    /**
     * This method is used to update the HttpSession when InternalSession
     * property changes.
     *
     * @param session Session Object
     */
    void saveForFailover(InternalSession session) {

        if (!isSessionFailoverEnabled) {
            return;
        }
        if (getUseInternalRequestRouting()) {
            // do not save sessions which never expire
            if (!session.willExpire()) {
                return;
            }
            try {
                getRepository().update(tokenAdapter.toToken(session));
            } catch (Exception e) {
                sessionDebug.error("SessionService.saveForFailover: " +
                        "exception encountered", e);
                // handleReleaseSession(session.getID());
            }
        } else {
            if (useRemoteSaveMethod) {
                saveSession(session.getID());
            } else {
                HttpSession httpSession = session.getHttpSession();
                if (httpSession != null) {
                    doSaveSession(session, httpSession);
                }
            }
        }
    }

    /**
     * Save identity session state in associated http session
     *
     * @param session
     * @param httpSession
     */
    private void doSaveSession(InternalSession session, HttpSession httpSession) {
        try {
            httpSession.setAttribute(httpSessionPropertyName, SessionService
                    .encrypt(session));
        } catch (Exception e) {
            sessionDebug.error(
                    "SessionService.doSaveSession: exception encountered", e);
        }
    }

    /**
     * Helper function used for remote invocation over HTTP It constructs
     * HttpURLConnection using url and adding cookies based on sid and returns
     * it to the caller. In order to complete the invocation caller is supposed
     * to open input stream
     *
     * @param url url
     * @param sid SessionID
     */
    private HttpURLConnection invokeRemote(URL url, SessionID sid,
                                           String extraCookies) throws Exception {
        if (!isSessionFailoverEnabled) {
            return null;
        }

        HttpURLConnection connection = null;

        try {
            connection = HttpURLConnectionManager.getConnection(url);

            StringBuilder securityCookieValue = new StringBuilder();
            securityCookieValue.append(thisSessionServerURL);
            securityCookieValue.append(Constants.AT);
            securityCookieValue.append(System.currentTimeMillis());

            String securityCookie = (String) AccessController
                    .doPrivileged(new EncodeAction(securityCookieValue.toString()));

            StringBuilder cookie = new StringBuilder();
            cookie.append(securityCookieName);
            cookie.append(Constants.EQUALS);
            cookie.append(cookieEncoding ? URLEncDec.encode(securityCookie) : securityCookie);

            if (extraCookies != null) {
                cookie.append(Constants.SEMI_COLON);
                cookie.append(extraCookies);
            }

            if (sid != null) {
                cookie.append(Constants.SEMI_COLON).append(Session.getCookieName());
                cookie.append(Constants.EQUALS);
                cookie.append(cookieEncoding ? URLEncDec.encode(sid.toString()) : sid.toString());

                String httpId = sid.getTail();

                if (httpId != null) {
                    cookie.append(Constants.SEMI_COLON);
                    cookie.append(getHttpSessionTrackingCookieName());
                    cookie.append(Constants.EQUALS);
                    cookie.append(cookieEncoding ? URLEncDec.encode(httpId) : httpId);
                }

            }

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("created cookie value: " + cookie.toString());
            }

            connection.setRequestProperty("Cookie", cookie.toString());
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

        } catch (Exception ex) {
            sessionDebug.message("Failed contacting " + url, ex);
            throw ex;
        }
        return connection;
    }

    /**
     * This method is used to encrypt the InternalSession object before storing
     * into HttpSession.
     *
     * @param obj Object to be encrypted
     */
    public static String encrypt(Object obj) {
        String strUnEncrypted, strEncrypted;
        ByteArrayOutputStream byteOut;
        ObjectOutputStream objOutStream;
        try {
            byteOut = new ByteArrayOutputStream();
            objOutStream = new ObjectOutputStream(byteOut);

            // convert object to byte using streams
            objOutStream.writeObject(obj);

            // convert byte to string
            strUnEncrypted = Base64.encode(byteOut.toByteArray());
            // encrypt string
            strEncrypted = (String) AccessController
                    .doPrivileged(new EncodeAction(strUnEncrypted, Crypt
                            .getHardcodedKeyEncryptor()));

        } catch (Exception e) {
            sessionDebug
                    .message("Error in encrypting the Internal Session object");
            return null;
        }
        return strEncrypted;
    }

    /**
     * This method is used to decrypt the InternalSession object, after
     * obtaining from HttpSession.
     *
     * @param strEncrypted Object to be decrypted
     */
    public static InternalSession decrypt(String strEncrypted) {

        if (strEncrypted == null)
            return null;
        String strDecrypted;
        byte byteDecrypted[] = null;
        ByteArrayInputStream byteIn;
        ObjectInputStream objInStream;
        Object tempObject = null;
        try {
            // decrypt string
            strDecrypted = (String) AccessController
                    .doPrivileged(new DecodeAction(strEncrypted, Crypt
                            .getHardcodedKeyEncryptor()));
            // convert string to byte
            byteDecrypted = Base64.decode(strDecrypted);
            // convert byte to object using streams
            byteIn = new ByteArrayInputStream(byteDecrypted);
            objInStream = new ObjectInputStream(byteIn);
            tempObject = objInStream.readObject();
        } catch (Exception e) {
            sessionDebug
                    .message("Error in decrypting the Internal Session object"
                            + e.getMessage());
            return null;
        }
        if (tempObject == null) {
            return null;
        }
        return (InternalSession) tempObject;
    }

    /**
     * This method is used to encode the SessionID.
     *
     * @param decodedStr Decoded String
     */
    public static String encodeID(String decodedStr) {

        if (decodedStr == null)
            return null;
        char encoded[], decoded[];
        int numberOfChar = 0, finalLength;

        decoded = decodedStr.toCharArray();
        for (int i = 0; i < decoded.length; i++) {
            if (decoded[i] == '%')
                numberOfChar++;
        }
        finalLength = decoded.length + (numberOfChar * 2);
        encoded = new char[finalLength];
        for (int i = 0, j = 0; i < decoded.length; i++) {
            switch (decoded[i]) {
                case '%':
                    encoded[j] = '%';
                    encoded[j + 1] = '2';
                    encoded[j + 2] = '5';
                    j = j + 3;
                    break;
                default:
                    encoded[j] = decoded[i];
                    j++;
                    break;
            }
        }
        return (String.valueOf(encoded));
    }

    /**
     * This is uia very useful method for debuggin the InternalSession values.
     * It can be deleted later.
     */
    protected static void debugIS(String calledFrom, Object obj) {
        InternalSession is = null;
        if (obj == null) {
            SessionService.sessionDebug
                    .message("InternalSession Attribute is NULL in -->"
                            + calledFrom);
            return;
        }
        is = (InternalSession) obj;
        if (is != null) {
            try {
                SessionService.sessionDebug.message(calledFrom
                        + " --Value of sessionID-->" + is.getID());
                SessionService.sessionDebug.message(calledFrom
                        + " --Value of clientDomain-->" + is.getClientDomain());
                SessionService.sessionDebug.message(calledFrom
                        + " --Value of maxSessionTime-->"
                        + is.getMaxSessionTime());
                SessionService.sessionDebug.message(calledFrom
                        + " --Value of sessionState-->" + is.getState());
                SessionService.sessionDebug.message(calledFrom
                        + " --Value of idleTime-->" + is.getIdleTime());
                if (is.getProperty("Name") != null) {
                    SessionService.sessionDebug.message(calledFrom
                            + " --Value of property Name is -->"
                            + is.getProperty("Name"));
                } else {
                    SessionService.sessionDebug.message(calledFrom
                            + "  --Value of property Name is NULL");
                }
            } catch (Exception e) {
                SessionService.sessionDebug.message("ERROR in debugIS"
                        + e.getMessage());
            }
        } else {
            SessionService.sessionDebug
                    .message("InternalSession is NULL in -->" + calledFrom);
        }
    }

    /**
     * Returns true if the given pattern is contained in the string.
     *
     * @param string  to examine
     * @param pattern to match
     * @return true if string matches <code>filter</code>
     */
    public static boolean matchFilter(String string, String pattern) {
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

    /**
     * @return Returns the connectionMaxWaitTime.
     */
    public static int getConnectionMaxWaitTime() {
        return connectionMaxWaitTime;
    }

    public static void setMaxWaitTimeForConstraint(int value) {
        maxWaitTimeForConstraint = value;
    }

    /**
     * @return Returns the maxWaitTimeForConstraint.
     */
    public static int getMaxWaitTimeForConstraint() {
        return maxWaitTimeForConstraint;
    }

    /**
     * @return Returns the sessionExternalRepositoryURL.
     */
    public static String getSessionExternalRepositoryURL() {
        return sessionExternalRepositoryURL;
    }

    /**
     * @return Returns the sessionStorePassword.
     */
    public static String getSessionStoreUserName() {
        return sessionStoreUserName;
    }

    /**
     * @return Returns the sessionStorePassword.
     */
    public static String getSessionStorePassword() {
        return sessionStorePassword;
    }

    public boolean isSiteEnabled() {
        return isSiteEnabled;
    }

    /**
     * @return Returns true if Property change notifications are enabled.
     */
    public static boolean isPropertyNotificationEnabled() {
        return isPropertyNotificationEnabled;
    }

    protected static void setPropertyNotificationEnabled(boolean value) {
        isPropertyNotificationEnabled = value;
    }

    public static boolean isSendPropertyNotification(String key) {
        if (!isPropertyNotificationEnabled) {
            return false;
        }
        return notificationProperties.contains(key);
    }

    protected static Set getNotificationProperties() {
        return notificationProperties;
    }

    protected static void setNotificationProperties(Set prop) {
        notificationProperties = prop;
    }

    protected static void setTimeoutHandlers(Set<String> handlers) {
        if (handlers == null) {
            timeoutHandlers = Collections.EMPTY_SET;
        } else {
            timeoutHandlers = new HashSet<String>(handlers);
        }
    }
    // TODO check if restructuring of interface between Auth and Session
    // can eliminate the need for this utility class

    static public class ExtendedSessionID extends SessionID {
        ExtendedSessionID(String sid, String serverID, String domain) {
            super(sid);
            setServerID(serverID);
            sessionDomain = domain;
        }

    }



}
