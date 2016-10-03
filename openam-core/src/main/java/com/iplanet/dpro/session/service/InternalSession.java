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
 * $Id: InternalSession.java,v 1.21 2009/03/20 21:05:25 weisun2 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.iplanet.dpro.session.service;

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.openam.utils.Time.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.authentication.service.LoginContext;
import org.forgerock.openam.session.AMSession;
import org.forgerock.openam.session.SessionMeta;
import org.forgerock.openam.session.service.persistence.SessionPersistenceManager;
import org.forgerock.openam.session.service.persistence.SessionPersistenceObservable;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>InternalSession</code> class represents a Webtop internal session.
 * <p>
 * A session has four states: invalid, valid, inactive, and destroyed. The initial state of a session is invalid.
 * 
 * @see SessionState
 *
 */
public class InternalSession implements Serializable, AMSession, SessionPersistenceObservable {

    /*
     * Logging message
     */
    private static final String LOG_MSG_SESSION_MAX_LIMIT_REACHED = "SESSION_MAX_LIMIT_REACHED";

    /*
     * Session property names
     */
    private static final String LOGIN_URL = "loginURL";
    private static final String SESSION_TIMED_OUT = "SessionTimedOut";
    private static final String HOST = "Host";
    private static final String HOST_NAME = "HostName";
    private static final String AM_MAX_IDLE_TIME = "AMMaxIdleTime";
    private static final String AM_MAX_SESSION_TIME = "AMMaxSessionTime";
    private static final String SAML2_IDP_SESSION_INDEX = "SAML2IDPSessionIndex";
    private static final String UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";
    private static final Set<String> protectedProperties = initialiseProtectedProperties();

    /*
     * Support objects (do not serialize)
     */
    private transient Debug debug;
    private transient SessionService sessionService;
    private transient SessionServiceConfig serviceConfig;
    private transient SessionLogging sessionLogging;
    private transient SessionAuditor sessionAuditor;

    /*
     * System properties
     */
    private static boolean isEnableHostLookUp = SystemProperties.getAsBoolean(Constants.ENABLE_HOST_LOOKUP);

    /* This is the maximum extra time for which the timed out sessions lives in the session server */
    @JsonProperty("purgeDelay")
    private static long purgeDelayInSeconds = SessionMeta.getPurgeDelay(SECONDS);

    /* Maximum frequency with which the access time in the repository will be updated. */
    private static int interval = SystemProperties.getAsInt("com.sun.identity.session.interval", 10);

    /* default idle time for invalid sessions */
    @JsonProperty("maxDefaultIdleTime")
    private static final long maxDefaultIdleTimeInMinutes =
            SystemProperties.getAsLong("com.iplanet.am.session.invalidsessionmaxtime", 3);

    /*
     * State
     */
    private SessionID sessionID;
    private int sessionType; // Either user or application (TODO: Replace these constants with an enum)
    private SessionState sessionState = SessionState.INVALID;
    private String clientID;
    private String clientDomain;
    private Properties sessionProperties; // e.g. LoginURL, Timeout, Host, etc
    private boolean willExpireFlag;
    private boolean isSessionUpgrade = false;
    private Boolean cookieMode = null;
    private String cookieStr;
    private transient AuthContextLocal authContext;
    private transient LoginContext loginContext;
    private transient SessionPersistenceManager persistenceManager;

    @JsonProperty("creationTime")
    private long creationTimeInSeconds;

    @JsonProperty("latestAccessTime")
    private long latestAccessTimeInSeconds;

    @JsonProperty("maxSessionTime")
    private long maxSessionTimeInMinutes;

    @JsonProperty("maxIdleTime")
    private long maxIdleTimeInMinutes;

    @JsonProperty("maxCachingTime")
    private long maxCachingTimeInMinutes;

    @JsonProperty("timedOutAt")
    private volatile long timedOutTimeInSeconds = 0; // Value zero means the session has not timed out.

    private final ConcurrentMap<SessionID, TokenRestriction> restrictedTokensBySid = new ConcurrentHashMap<>();

    private transient final ConcurrentMap<TokenRestriction, SessionID> restrictedTokensByRestriction =
            new ConcurrentHashMap<>();

    /*
     * The URL map for session events of THIS session only : SESSION_CREATION, IDLE_TIMEOUT, MAX_TIMEOUT, LOGOUT,
     * REACTIVATION, DESTROY. Each URL in the map is associated with a set of token ids (master and potentially all of
     * the restricted token ids associated with the master) that will be used in notification
     */
    private final ConcurrentMap<String, Set<SessionID>> sessionEventURLs = new ConcurrentHashMap<>();

    @JsonIgnore private boolean isISStored = false;
    private volatile boolean reschedulePossible;
    private transient InternalSessionTaskRunnable taskRunnable = new InternalSessionTaskRunnable();

    /* Session handle is used to prevent administrator from impersonating other users. */
    @JsonIgnore private String sessionHandle = null;

    /**
     * Creates an instance of the Internal Session with its key dependencies exposed.
     *
     * Note: This InternalSession will be in an invalid state.
     *
     * @param sid Non null Session ID.
     * @param service Non null SessionService.
     * @param debug Debugging instance to use for all logging.
     */
    @VisibleForTesting
    InternalSession(SessionID sid,
                           SessionService service,
                           SessionServiceConfig serviceConfig,
                           SessionLogging sessionLogging,
                           SessionAuditor sessionAuditor,
                           Debug debug) {
        sessionID = sid;
        setSessionServiceDependencies(service, serviceConfig, sessionLogging, sessionAuditor, debug);

        maxIdleTimeInMinutes = maxDefaultIdleTimeInMinutes;
        maxSessionTimeInMinutes = maxDefaultIdleTimeInMinutes;
        reschedulePossible = maxDefaultIdleTimeInMinutes > maxIdleTimeInMinutes;
        sessionState = SessionState.INVALID;
        sessionProperties = new Properties();
        willExpireFlag = true;
        setCreationTime();
    }

    /**
     * Creates a new InternalSession with the given Session ID.
     *
     * Note: This InternalSession will be in an invalid state.
     *
     * @param sid SessionID Non null Session ID.
     */
    public InternalSession(SessionID sid) {
        this(sid,
                InjectorHolder.getInstance(SessionService.class),
                InjectorHolder.getInstance(SessionServiceConfig.class),
                InjectorHolder.getInstance(SessionLogging.class),
                InjectorHolder.getInstance(SessionAuditor.class),
                InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG))));
    }

    /**
     * Default constructor required for deserialisation, and should not be used elsewhere.
     *
     * This constructor is intentionally blank, except for setting isISStored to true (if the InternalSession is being
     * deserialised, it is being loaded from storage).
     * When deserialised the code responsible for instantiating it will have no means of resolving dependencies.
     *
     * Instead this is deferred to
     * {@link com.iplanet.dpro.session.service.InternalSession#setSessionServiceDependencies(
     * SessionService, SessionServiceConfig, SessionLogging, SessionAuditor, com.sun.identity.shared.debug.Debug)}
     */
    public InternalSession() {
        isISStored = true;
    }

    /**
     * The debug instance is not restored during deserialisation.
     * @param debug Non null debug instance.
     */
    public void setDebug(Debug debug) {
        this.debug = debug;
    }

    /**
     * The SessionService is not restored during deserialisation.
     * @param service Non null SessionService.
     */
    public void setSessionServiceDependencies(SessionService service, SessionServiceConfig serviceConfig,
                                              SessionLogging sessionLogging, SessionAuditor sessionAuditor,
                                              Debug debug) {

        this.sessionService = service;
        this.serviceConfig = serviceConfig;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
        this.debug = debug;
    }

    /**
     * Schedule this task to TimerPool according to the current state.
     */
    protected void reschedule() {
        taskRunnable.reschedule();
    }

    public void cancel() {
        taskRunnable.cancel();
    }

    /**
     * Returns the SessionID of this Internal Session.
     * @return SessionID for the internal session object
     */
    public SessionID getID() {
        return sessionID;
    }

    /**
     * Returns the type of Internal Session.
     * @return  <code>0 </code> if it is a USER_SESSION
     *          <code>1 </code> if it s a APPLICATION_SESSION
     */
    public int getType() {
        return sessionType;
    }

    /**
     * Set the type of Internal Session. User OR Application.
     *
     * @param type <code>0</code> for <code>USER_SESSION</code>.
     *             <code>1</code> for <code>APPLICATION_SESSION</code>.
     */
    public void setType(int type) {
        sessionType = type;
        notifyPersistenceManager();
    }

    /**
     * Returns Client ID, accessing this Internal Session.
     *
     * @return Client ID.
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * Sets Client ID for this Internal Session.
     *
     * @param id
     */
    public void setClientID(String id) {
        clientID = id;
        notifyPersistenceManager();
    }

    /**
     * Returns the Domain of the Client
     *
     * @return Client Domain
     */
    public String getClientDomain() {
        return clientDomain;
    }

    /**
     * Sets the Clieant's Domain.
     *
     * @param domain
     *            Client Domain
     */
    public void setClientDomain(String domain) {
        clientDomain = domain;
        notifyPersistenceManager();
    }

    /**
     * Returns maximum time allowed for the Internal Session.
     * @return the number of maximum minutes for the session
     */
    public long getMaxSessionTime() {
        return maxSessionTimeInMinutes;
    }

    /**
     * Sets the maximum time(in minutes) allowed for the Internal Session
     *
     * @param t
     *            Maximum Session Time
     */
    public void setMaxSessionTime(long t) {
        boolean mayReschedule = false;
        if (t < maxSessionTimeInMinutes) {
            mayReschedule = true;
        }
        maxSessionTimeInMinutes = t;
        if (taskRunnable.isScheduled() && mayReschedule) {
            reschedule();
        }
        notifyPersistenceManager();
    }

    /**
     * Returns the maximum idle time(in minutes) for the Internal Session.
     * @return the number maximum idle minutes
     */
    public long getMaxIdleTime() {
        return maxIdleTimeInMinutes;
    }

    /**
     * Sets the maximum idle time(in minutes) for the Internal Session.
     *
     * @param t
     */
    public void setMaxIdleTime(long t) {
        boolean mayReschedule = false;
        if (t < maxIdleTimeInMinutes) {
            mayReschedule = true;
        }
        maxIdleTimeInMinutes = t;
        reschedulePossible = (maxDefaultIdleTimeInMinutes > maxIdleTimeInMinutes)
                || (maxDefaultIdleTimeInMinutes > maxSessionTimeInMinutes);
        if (taskRunnable.isScheduled() && (mayReschedule || reschedulePossible)) {
            reschedule();
        }
        notifyPersistenceManager();
    }

    /**
     * Returns the maximum caching time(in minutes) allowed for the Internal
     * Session.
     * @return Maximum Cache Time
     */
    public long getMaxCachingTime() {
        return maxCachingTimeInMinutes;
    }

    /**
     * Sets the maximum caching time(in minutes) for the Internal Session.
     *
     * @param t
     *        Maximum Caching Time
     */
    public void setMaxCachingTime(long t) {
        maxCachingTimeInMinutes = t;
        notifyPersistenceManager();
    }

    /**
     * Returns the time(in seconds) for which the Internal Session has not been
     * accessed.
     * @return session idle time
     */
    public long getIdleTime() {
        long currentTimeInSeconds = MILLISECONDS.toSeconds(currentTimeMillis());
        return currentTimeInSeconds - latestAccessTimeInSeconds;
    }

    /**
     * Returns the total time left(in seconds) for the Internal Session. Returns 0 if the time left is negative.
     * @return Time left for the internal session to be invalid
     */
    public long getTimeLeft() {
        long timeLeftInMillis = getMaxSessionExpirationTime(MILLISECONDS) - currentTimeMillis();
        return MILLISECONDS.toSeconds(Math.max(timeLeftInMillis, 0));
    }

    /**
     * Returns the extra time left(in seconds) for the Internal Session after
     * the session timed out.
     * @return time remaining before purge. <code> -1 </code> if the session
     * has not yet timed out due idle/max timeout period.
     */
    public long getTimeLeftBeforePurge() {
        /*
         * Return -1 if the session has not timed out due to idle/max timeout
         * period.
         */
        if (!isTimedOut()) {
            return -1;
        }
        /*
         * Return the extra time left (in seconds), if the session has timed out due to
         * idle/max time out period
         */
        long timeLeftInMillis = getPurgeDelayExpirationTime(MILLISECONDS) - currentTimeMillis();
        return MILLISECONDS.toSeconds(Math.max(timeLeftInMillis, 0));
    }

    /**
     * Returns true if the session has timed out due to idle/max timeout period.
     * @return <code>true</code> if the Internal session has timedout ,
     * <code>false</code> otherwise
     */
    public boolean isTimedOut() {
        return timedOutTimeInSeconds != 0;
    }

    /**
     * Cache the cookie string. No guarantees are made as to its continued persistence.
     * @param cookieString The cookie string to persist.
     */
    public void cacheCookieString(String cookieString) {
        this.cookieStr = cookieString;
    }
    /**
     * Returns the cached cookie string for this InternalSession. May be null.
     * @return The cached cookie string. May be null.
     */
    public String getCachedCookieString() {
        return cookieStr;
    }

    /**
     * Return the SessionID object which represents this InternalSession.
     * @return The session ID.
     */
    public SessionID getSessionID() {
        return sessionID;
    }

    /**
     * Returns the state of the Internal Session
     * @return the session state can be VALID, INVALID, INACTIVE or DESTROYED
     */
    public SessionState getState() {
        return sessionState;
    }

    /**
     * Get the authentication context associated with this session.
     *
     * @return the AuthContextLocal associated with this session
     */
    public AuthContextLocal getAuthContext() {
        return authContext;
    }

    /**
     * Gets whether this session has an associated authenticationContext.
     * @return true if this session has an authentication context.
     */
    public boolean hasAuthenticationContext() {
        return null != authContext;
    }

    /**
     * Sets the authentication context.
     *
     * @param authContext the authentication context
     */
    public void setAuthContext(AuthContextLocal authContext) {
        this.authContext = authContext;
    }

    /**
     * Clears the authentication context from this session.
     */
    public void clearAuthContext() {
        this.authContext = null;
    }

    /**
     * Gets the login context from this session.
     * @return the login context
     */
    public LoginContext getLoginContext() {
        return loginContext;
    }

    /**
     * Sets the login context associated with this session.
     * @param loginContext a LoginContext to associate with this session.
     */
    public void setLoginContext(LoginContext loginContext) {
        this.loginContext = loginContext;
    }

    /**
     * Returns the value of the specified key from the Internal Session property
     * table.
     *
     * @param key
     *            Property key
     * @return string value for the key from Internal Session table.
     */
    public String getProperty(String key) {
        return sessionProperties.getProperty(key);
    }

    /**
     * Returns the Enumeration of property names of the Internal Session
     * property table.
     * @return list of properties in the Internal session table.
     */
    public Enumeration getPropertyNames() {
        return sessionProperties.propertyNames();
    }

    /**
     * Helper method to check if a property is protected or not.
     *
     * We introduce a mechanism to protect certain "core" or "internal"
     * properties from updates via remote SetProperty method of the
     * SessionService. Allowing remote self-updates to session properties leads
     * to a security vulnerability which allows unconstrained user impersonation
     * or privilege elevation. See bug # 4814922 for more information
     *
     * protectedProperties contains a set of property names which can not be
     * remotely updated. It is initially populated using static initializer. We
     * also implemented an extra safety mechanism intended to protect from
     * accidental reopening of this security hole in the future if a property
     * name changes or new property is introduced without corresponding update
     * of the static hardcoded list of protected properties below. This
     * mechanism automatically adds any property to protectedProperties if it is
     * set via local invocation of putProperty.
     *
     * However, some properties (such as Locale and CharSet) must be settable
     * both locally and remotely. In order to make it configurable we use a
     * second table called remotelyUpdateableProperties. Note that
     * protectedProperties takes precedence over remotelyUpdateableProperties:
     * remotelyUpdateableProperties will be consulted only if a property is not
     * on the protectedProperties list already.
     *
     * The following tables defines the behavior of putProperty() and
     * putExternalProperty() depending on whether property name is present in
     * protectedProperties or remotelyUpdateableProperty list
     *
     * protectedProperties remotelyUpdateableProperties putProperty()
     * putExternalProperty()
     *
     * in n/a sets value logs, does nothing
     *
     * out in sets value sets value
     *
     * out out sets value and sets value adds to protectedProperty
     *
     * @param key
     *          property name.
     * @return true if property is protected else false.
     */
    public static boolean isProtectedProperty(String key) {
        if (protectedProperties.contains(key) ||
            key.toLowerCase().startsWith(
                Constants.AM_PROTECTED_PROPERTY_PREFIX)) {
            return true;
         }
         return false;
    }

    private static Set<String> initialiseProtectedProperties() {
        Set<String> protectedProperties = new HashSet<>();
        protectedProperties.add(HOST);
        protectedProperties.add(HOST_NAME);
        protectedProperties.add("AuthLevel");
        protectedProperties.add("AuthType");
        protectedProperties.add("Principal");
        protectedProperties.add("UserId");
        protectedProperties.add("UserToken");
        protectedProperties.add("Organization");
        protectedProperties.add("cookieSupport");
        protectedProperties.add("authInstant");
        protectedProperties.add("Principals");
        protectedProperties.add("loginURL");
        protectedProperties.add("FullLoginURL");
        protectedProperties.add("Role");
        protectedProperties.add("Service");
        protectedProperties.add("SessionTimedOut");
        protectedProperties.add(SESSION_HANDLE_PROP);
        protectedProperties.add(TOKEN_RESTRICTION_PROP);
        protectedProperties.add(AM_MAX_IDLE_TIME);
        protectedProperties.add(AM_MAX_SESSION_TIME);
        protectedProperties.add(Constants.AM_CTX_ID);
        protectedProperties.add(Constants.UNIVERSAL_IDENTIFIER);

        String protectedPropertiesConfig = SystemProperties.get(
                Constants.PROTECTED_PROPERTIES_LIST, "");

        if (protectedPropertiesConfig != null) {
            StringTokenizer st = new StringTokenizer(protectedPropertiesConfig,
                    ",");
            while (st.hasMoreTokens()) {
                String prop = st.nextToken().trim();
                protectedProperties.add(prop);
                Debug sessionDebug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("Added protected property [" + prop + "]");
                }
            }
        }
        return protectedProperties;
    }

    /**
     * Sets the key-value pair in the InternalSession property table if it is
     * not protected. If it is protected client should have permission to set
     * it. This method is to be used in conjuction with
     * SessionRequestHandler/SessionService invocation path If the property is
     * protected, an attempt to remotely set a protected property is logged and
     * the method throws an Exception. Otherwise invocation is delegated to
     * internalPutProperty()
     *
     * Note that package default access is being used
     *
     * @param clientToken
     *            Token of the client setting external property.
     * @param key
     *            Property key
     * @param value
     *            Property value for the key
     * @exception SessionException is thrown if the key is protected property.
     *
     */
    public void putExternalProperty(SSOToken clientToken, String key, String value)
        throws SessionException {
		try {
        	SessionUtils.checkPermissionToSetProperty(clientToken, key, value);
		} catch (SessionException se) {
            sessionLogging.logEvent(toSessionInfo(), SessionEvent.PROTECTED_PROPERTY);
			throw se;
		}
        internalPutProperty(key,value);
        if (debug.messageEnabled()) {
            debug.message("Updated protected property"
                + " after validating client identity and permissions");
        }
    }

    /**
     * Sets the key-value pair in the Internal Session property table. This
     * method should only be invoked locally by code running in the same server
     * VM. Remote invocations should use putExternalProperty(). This is a simple
     * wrapper around internalPutProperty(), which in addition calls to
     * registerProtectedProperty() to make sure that if a property key is not
     * already on the list of protected properties, it will be automatically
     * added there (unless it is also on remotelyUpdateableProperties list!)
     *
     * @param key
     *            Property key
     * @param value
     *            Property value for the key
     */
    public void putProperty(String key, String value) {
        internalPutProperty(key, value);
    }

    /**
     * Sets the key-value pair in the Internal Session property table.
     *
     * @param key
     *            Property key
     * @param value
     *            Property value for the key
     */
    private void internalPutProperty(String key, String value) {
        if (key.equals(HOST_NAME) || key.equals(HOST)) {
            if (value == null || value.length() == 0) {
                return;
            }

            if (isEnableHostLookUp) {
                try {
                    InetAddress address = java.net.InetAddress.getByName(value);
                    String hostName = address.getHostName();
                    sessionProperties.put(HOST_NAME, hostName);
                    sessionProperties.put(HOST, value);
                } catch (UnknownHostException uhe) {
                    debug.error(
                            "InternalSession.internalputProperty():"
                                    + "Unable to get HostName for:" + value
                                    + " SessionException: ", uhe);
                }
            } else {
                sessionProperties.put(HOST_NAME, value);
                sessionProperties.put(HOST, value);
            }

        } else if (key.equals(AM_MAX_IDLE_TIME)) {
            setMaxIdleTime(Long.parseLong(value));
        } else if (key.equals(AM_MAX_SESSION_TIME)) {
            setMaxSessionTime(Long.parseLong(value));
        } else {
            sessionProperties.put(key, value);
        }

        if (sessionState == SessionState.VALID && serviceConfig.isSendPropertyNotification(key)) {
            sessionService.sendEvent(this, SessionEvent.PROPERTY_CHANGED);
            SessionInfo sessionInfo = toSessionInfo();
            sessionLogging.logEvent(sessionInfo, SessionEvent.PROPERTY_CHANGED);
            sessionAuditor.auditActivity(sessionInfo, AM_SESSION_PROPERTY_CHANGED);
        }
        notifyPersistenceManager();
    }

    /**
    * Sets the status of the isSessionUpgrade flag to which determines if the
    * <code>Session</code> is in the upgrade state or not.
    *
    * @param value <code>true</code> if it is an upgrade
    *        <code>false</code> otherwise
    */
    public void setIsSessionUpgrade(boolean value) {
        isSessionUpgrade = value;
        notifyPersistenceManager();
    }

    /**
     * Gets the status of the <code>Session</code> if is an upgrade state
     *
     * @return <code>true</code> if the session is in upgrade state
     *         <code>false</code> otherwise
     */
    public boolean getIsSessionUpgrade() {
        return isSessionUpgrade;
    }

    /**
     * Set whether this InternalSession is persisted.
     * @param isStored True if the session is persisted, false otherwise.
     */
    public void setStored(boolean isStored) {
        isISStored = isStored;
    }

    /**
     * Returns whether the InternalSession represented has been stored. If this is true, changes to this object will
     * update the stored version.
     * return <code>true</code> if the internal session is stored
     *        <code>false</code> otherwise
     */
    public boolean isStored() {
        return isISStored;
    }

    /**
     * Changes the state of the session to ACTIVE after creation.
     * @param userDN
     * @return <code> true </code> if the session is successfully activated
     *         after creation , <code>false</code> otherwise
     */
    public boolean activate(String userDN) {
        return activate(userDN, false);
    }

    /**
     * Changes the state of the session to ACTIVE after creation.
     * @param userDN
     * @param stateless Indicates that the log in session is a stateless session.
     * @return <code> true </code> if the session is successfully activated
     *         after creation , <code>false</code> otherwise
     */
    public boolean activate(String userDN, boolean stateless) {
        if (userDN == null) {
            return false;
        }
        // Exceeded max active sessions, but allow if the user is super-admin
        if ((sessionService.hasExceededMaxSessions()) && !sessionService.isSuperUser(userDN)) {
            sessionLogging.logSystemMessage(LOG_MSG_SESSION_MAX_LIMIT_REACHED, java.util.logging.Level.INFO);
            return false;
        }

        SessionInfo sessionInfo = toSessionInfo();

        // checking Session Quota Constraints
        if ((serviceConfig.isSessionConstraintEnabled()) && !shouldIgnoreSessionQuotaChecking()) {

            if (SessionConstraint.checkQuotaAndPerformAction(this)) {
                if (debug.messageEnabled()) {
                    debug.message("Session Quota exhausted!");
                }
                sessionLogging.logEvent(sessionInfo, SessionEvent.QUOTA_EXHAUSTED);
                return false;
            }
        }
        setLatestAccessTime();
        setState(SessionState.VALID);
        if (reschedulePossible && !stateless) {
            reschedule();
        }
        sessionLogging.logEvent(sessionInfo, SessionEvent.SESSION_CREATION);
        sessionAuditor.auditActivity(sessionInfo, AM_SESSION_CREATED);
        sessionService.sendEvent(this, SessionEvent.SESSION_CREATION);

        if (!stateless && (!isAppSession() || serviceConfig.isReturnAppSessionEnabled())) {
            sessionService.incrementActiveSessions();
        }
        return true;
    }

    /*
     * The session quota checking will be bypassed if:
     * (1) the login user is the super user (not including users assigned the top level admin role), or
     * (2) the token is an application token (e.g. Agent)
     */
    private boolean shouldIgnoreSessionQuotaChecking() {
        boolean ignore = false;
        if (sessionService.isSuperUser(getUUID()) || (isAppSession())) {
            ignore = true;
        }
        return ignore;
    }

    /**
     * Gets the User Universal ID
     * @return  UUID
     */
    public String getUUID() {
        return getProperty(UNIVERSAL_IDENTIFIER);
    }

    /**
     * Changes the state of the session to ACTIVE from IN-ACTIVE.
     */
    public void reactivate() {
        taskRunnable.cancel();
        setCreationTime();
        setLatestAccessTime();
        setState(SessionState.VALID);
        reschedule();
        SessionInfo sessionInfo = toSessionInfo();
        sessionLogging.logEvent(sessionInfo, SessionEvent.REACTIVATION);
        sessionAuditor.auditActivity(sessionInfo, AM_SESSION_REACTIVATED);
        sessionService.sendEvent(this, SessionEvent.REACTIVATION);
    }

    /**
     * Sets the willExpireFlag. This flag specify that whether the session will
     * ever expire or not.
     */
    public void setNonExpiring() {
        maxSessionTimeInMinutes = SECONDS.toMinutes(Long.MAX_VALUE);
        maxIdleTimeInMinutes = SECONDS.toMinutes(Long.MAX_VALUE);
        maxCachingTimeInMinutes = serviceConfig.getApplicationMaxCachingTime();
        cancel();
        taskRunnable.setNonExpiring();
        willExpireFlag = false;
    }

    /**
     * Checks the invalid session idle time. If this session is invalid and idle
     * for more than 3 minutes, we will need to remove it from the session table
     *
     * @return <code>true</code> if the max default idle time expires
     */
    private boolean checkInvalidSessionDefaultIdleTime() {
        long currentTimeInSeconds = MILLISECONDS.toSeconds(currentTimeMillis());
        long timeLeftInSeconds = creationTimeInSeconds + MINUTES.toSeconds(maxDefaultIdleTimeInMinutes) - currentTimeInSeconds;
        return timeLeftInSeconds < 0;
    }

    /**
     * Checks whether the session should change state and returns the state that the session should be in.
     */
    public StateTransition checkSessionUpdate() {
        if (!willExpireFlag) {
            return StateTransition.NO_CHANGE;
        }
        if (!isTimedOut()) {
            if (isInvalid()) {
                if (checkInvalidSessionDefaultIdleTime()) {
                    return StateTransition.DESTROY;
                } else {
                    return StateTransition.NO_CHANGE;
                }
            }

            if (getTimeLeft() == 0) {
                return StateTransition.MAX_TIMEOUT;
            }

            if (getIdleTime() >= MINUTES.toSeconds(maxIdleTimeInMinutes) && sessionState != SessionState.INACTIVE) {
                return StateTransition.IDLE_TIMEOUT;
            }
            return StateTransition.NO_CHANGE;
        } else {
            // do something special for the timed out sessions
            if (getTimeLeftBeforePurge() <= 0) {
                return StateTransition.PURGE;
            } else {
                return StateTransition.NO_CHANGE;
            }
        }
    }

    /**
     * Changes the state of the session and sends Session Notification when
     * session times out.
     *
     * TODO: This logic should be moved into LocalOperations once InternalSessionTaskRunnable is removed.
     *
     */
    public void changeStateAndNotify(int eventType) {
        sessionLogging.logEvent(toSessionInfo(), eventType);
        timedOutTimeInSeconds = MILLISECONDS.toSeconds(currentTimeMillis());
        putProperty("SessionTimedOut", String.valueOf(timedOutTimeInSeconds));
        sessionService.execSessionTimeoutHandlers(sessionID, eventType);
        if (purgeDelayInSeconds == 0) {
            sessionService.destroyInternalSession(sessionID);
            return;
        }
        if (!isAppSession() || serviceConfig.isReturnAppSessionEnabled()) {
            sessionService.decrementActiveSessions();
        }
        setState(SessionState.INVALID);
        if (serviceConfig.isSessionTrimmingEnabled()){
            trimSession();
        }
        sessionService.sendEvent(this, eventType);
    }

    /**
     * Changes the state of the session. Does not notify SessionService, or anything else using Session Notification.
     */
    public void changeStateWithoutNotify(SessionState state) {
        this.sessionState = state;
    }

    public SessionInfo toSessionInfo() {
        return toSessionInfo(true);
    }

    /**
     * Transfers the info about the Internal Session to Session Info.
     * @return SessionInfo
     */
    public SessionInfo toSessionInfo(boolean withIds) {
        SessionInfo info = new SessionInfo();

        if (withIds) {
            info.setSessionID(sessionID.toString());
        } else {
            info.setSecret(java.util.UUID.randomUUID().toString());
        }

        if (sessionType == USER_SESSION) {
            info.setSessionType("user");
        } else if (sessionType == APPLICATION_SESSION) {
            info.setSessionType("application");
        }
        info.setClientID(clientID);
        info.setClientDomain(clientDomain);
        info.setMaxTime(getMaxSessionTime());
        info.setMaxIdle(getMaxIdleTime());
        info.setMaxCaching(getMaxCachingTime());
        if (willExpireFlag) {
            info.setTimeIdle(getIdleTime());
            info.setTimeLeft(getTimeLeft());
        } else {
            // Sessions such as authentication session will never be destroyed
            info.setNeverExpiring(true);
        }

        info.setState(sessionState.name().toLowerCase());
        info.setProperties((Hashtable<String, String>) sessionProperties.clone());
        if (withIds && sessionHandle != null) {
            //Adding the sessionHandle as a session property, so the sessionHandle is available in Session objects.
            info.getProperties().put(SESSION_HANDLE_PROP, sessionHandle);
        }
        return info;
    }

    /**
     * Sets the last time the client sent a request associated with this
     * session, as the number of seconds since midnight January 1, 1970 GMT.
     *
     * Once updated the Session will be persisted.
     */
    public void setLatestAccessTime() {
        long oldLatestAccessTime = latestAccessTimeInSeconds;
        latestAccessTimeInSeconds = currentTimeMillis() / 1000;
        if ((latestAccessTimeInSeconds - oldLatestAccessTime) > interval) {
            notifyPersistenceManager();
        }
    }

    /**
     * Sets the state of the Internal Session.
     *
     * @param state
     */
    public void setState(SessionState state) {
        sessionState = state;
        notifyPersistenceManager();
    }

    /**
     * Returns the URL of the Session events and the associated master and
     * restricted token ids.
     * @return Map of session event URLs and their associated SessionIDs.
     */
    Map<String, Set<SessionID>> getSessionEventURLs(int eventType, SessionBroadcastMode logoutDestroyBroadcast) {
        Map<String, Set<SessionID>> urls = new HashMap<>();

        if ((eventType == SessionEvent.DESTROY || eventType == SessionEvent.LOGOUT) &&
                logoutDestroyBroadcast != SessionBroadcastMode.OFF) {
            try {
                String localServer = WebtopNaming.getLocalServer();
                Collection<String> servers;
                if (logoutDestroyBroadcast == SessionBroadcastMode.ALL_SITES) {
                    servers = WebtopNaming.getPlatformServerList();
                } else {
                    servers = new ArrayList<>();
                    for (String serverID : WebtopNaming.getSiteNodes(WebtopNaming.getAMServerID())) {
                        servers.add(WebtopNaming.getServerFromID(serverID));
                    }
                }
                for (String url : servers) {
                    if (!localServer.equals(url)) {
                        urls.put(url + "/notificationservice", new HashSet<>(Arrays.asList(this.getID())));
                    }
                }
            } catch (Exception e) {
                debug.warning("Unable to get list of servers", e);
            }
        }

        for (Map.Entry<String,Set<SessionID>> entry : sessionEventURLs.entrySet()) {
            Set<SessionID> sessionIDs = urls.get(entry.getKey());
            if (sessionIDs != null) {
                sessionIDs.addAll(entry.getValue());
            } else {
                urls.put(entry.getKey(), entry.getValue());
            }
        }

        return urls;
    }

    /**
     * Adds a listener for the associated session ID.
     * @param url The listening URL.
     * @param sid The associated SessionID.
     */
    public void addSessionEventURL(String url, SessionID sid) {

        Set<SessionID> sids = sessionEventURLs.get(url);
        if (sids == null) {
            sids = Collections.newSetFromMap(new ConcurrentHashMap<SessionID, Boolean>());
            Set<SessionID> previousValue = sessionEventURLs.putIfAbsent(url, sids);
            if (previousValue != null) {
                sids = previousValue;
            }
        }

        if (sids.add(sid))  {
            notifyPersistenceManager();
        }
    }

    /**
     * This setter method is used by the JSON serialization mechanism and should not be used for other purposes.
     *
     * @param restrictedTokensBySid The deserialized map of sid&lt;->restricted tokens that should be stored in a
     * ConcurrentHashMap.
     */
    @JsonSetter
    private void setRestrictedTokensBySid(ConcurrentMap<SessionID, TokenRestriction> restrictedTokensBySid) {
        for (Map.Entry<SessionID, TokenRestriction> entry : restrictedTokensBySid.entrySet()) {
            SessionID sid = entry.getKey();
            TokenRestriction restriction = entry.getValue();
            this.restrictedTokensBySid.put(sid, restriction);
            this.restrictedTokensByRestriction.put(restriction, sid);
        }
    }

    /**
     * This setter method is used by the JSON serialization mechanism and should not be used for other purposes.
     *
     * @param sessionEventURLs The deserialized map of sessionEventURLs that should be stored in a ConcurrentHashMap.
     */
    @JsonSetter
    private void setSessionEventURLs(ConcurrentMap<String, Set<SessionID>> sessionEventURLs) {
        for (Map.Entry<String, Set<SessionID>> entry : sessionEventURLs.entrySet()) {
            Set<SessionID> values = Collections.newSetFromMap(new ConcurrentHashMap<SessionID, Boolean>());
            values.addAll(entry.getValue());
            this.sessionEventURLs.put(entry.getKey(), values);
        }
    }

    /**
     * Returns the value of willExpireFlag.
     *
     */
    public boolean willExpire() {
        return willExpireFlag;
    }

    /**
     * Determine whether it is an application session.
     *
     * @return <code>true</code> if this is an application session, <code>false</code> otherwise.
     */
    boolean isAppSession() {
        return sessionType == APPLICATION_SESSION;
    }

    /**
     * Sets the creation time of the Internal Session, as the number of seconds
     * since midnight January 1, 1970 GMT.
     */
    private void setCreationTime() {
        creationTimeInSeconds = currentTimeMillis() / 1000;
    }

    /**
     * Add new restricted token pointing at the same session to the list.
     *
     * @param sid The session ID.
     * @param restriction The token restriction.
     * @return The existing session ID instance if this TokenRestriction was already mapped to a session ID,
     * <code>null</code> otherwise.
     */
    public SessionID addRestrictedToken(SessionID sid, TokenRestriction restriction) {
        SessionID previousValue = restrictedTokensByRestriction.putIfAbsent(restriction, sid);
        if (previousValue == null) {
            restrictedTokensBySid.put(sid, restriction);
            notifyPersistenceManager();
            return null;
        }
        return previousValue;
    }

    /**
     * Returns the TokenRestriction for the given SessionID.
     *
     * @param sid Possibly null SessionID.
     * @return Null indicates there is no restriction on the Session.
     */
    public TokenRestriction getRestrictionForToken(SessionID sid) {
        return restrictedTokensBySid.get(sid);
    }

    public SessionID getRestrictedTokenForRestriction(TokenRestriction restriction) {
        return restrictedTokensByRestriction.get(restriction);
    }

    Set<SessionID> getRestrictedTokens() {
        return new HashSet<>(restrictedTokensBySid.keySet());
    }

    /**
     * Returns true if cookies are supported.
     *
     * @return true if cookie supported;
     */
    public boolean getCookieSupport() {
        boolean cookieSupport = false;
        if (sessionID.getCookieMode() != null) {
            cookieSupport = sessionID.getCookieMode();
        } else if (this.cookieMode != null) {
            cookieSupport = this.cookieMode;
        }
        if (debug.messageEnabled()) {
            debug.message("InternalSession: getCookieSupport: " + cookieSupport);
        }
        return cookieSupport;
    }

    /**
     * In case of session timeout the session is trimmed to reduce the memory
     * overhead. Even if the session lives in the server for the extra time out
     * period, the memory is not abused. Instance variables preserved are, 1)
     * sessionID 2) timedOutAt 3) clientID 4) purgeDelayInSeconds 5)
     * sessionProperties(loginURL/SessionTimedOut/AM_CTX_ID/SAML2IDPSessionIndex)
     * 6) sessionEventURLs 7) sessionState All other instance variables are
     * cleaned to save memory.
     */
    private void trimSession() {
        clientDomain = null;
        cookieStr = null;
        // Clean Session Properties
        Properties newProperties = new Properties();
        String loginURL = getProperty(LOGIN_URL);
        String sessionTimedOut = getProperty(SESSION_TIMED_OUT);
        String  idpSessionIndex = getProperty(SAML2_IDP_SESSION_INDEX);
        if (loginURL != null)
            newProperties.put(LOGIN_URL, loginURL);
        if (sessionTimedOut != null)
            newProperties.put(SESSION_TIMED_OUT, sessionTimedOut);
        String ctxID = getProperty(Constants.AM_CTX_ID);
        if (ctxID != null) {
            newProperties.put(Constants.AM_CTX_ID, ctxID);
        }
        if (idpSessionIndex != null) {
            newProperties.put(SAML2_IDP_SESSION_INDEX, idpSessionIndex);
        }
        sessionProperties = newProperties;
    }

    /**
     * set the cookieMode based on whether the request has cookies or not. This
     * method is called from createSSOToken(request) method in SSOTokenManager.
     *
     * @param cookieMode ,
     *            Boolean value whether request has cookies or not.
     */

    public void setCookieMode(Boolean cookieMode) {
        debug.message("CookieMode is:" + cookieMode);
        if (cookieMode != null) {
            this.cookieMode = cookieMode;
        }
    }

    /**
     * Used during session deserialization. This method SHALL NOT be invoked by custom code.
     *
     * @param sessionHandle The sessionHandle to set.
     */
    @JsonSetter
    public void setSessionHandle(String sessionHandle) {
        this.sessionHandle = sessionHandle;
        //No need to update the session for failover, as this method is invoked only upon session
        //creation/deserialization.
    }

    /**
     * Returns the session handle.
     *
     * @return The session handle.
     */
    public String getSessionHandle() {
        return sessionHandle;
    }

    /**
     * Computes session object expiration time as the smallest of the remaining idle time (or purge delay if the
     * session has already timed out) or the session lifetime limit.
     * <p>
     * Time value is returned in the requested unit (accurate to millisecond) and uses the
     * same epoch as {@link System#currentTimeMillis()}.
     *
     * @param timeUnit the time unit to return the result in.
     * @return the result in the given units.
     */
    public long getExpirationTime(final TimeUnit timeUnit) {
        long timeLeftInSeconds = Math.max(0L, MINUTES.toSeconds(getMaxIdleTime()) - getIdleTime());

        if (timeLeftInSeconds == 0) {
            timeLeftInSeconds = getTimeLeftBeforePurge();
        }

        return timeUnit.convert(currentTimeMillis(), MILLISECONDS)
                + Math.min(timeUnit.convert(getTimeLeft(), SECONDS),
                           timeUnit.convert(timeLeftInSeconds, SECONDS));
    }

    /**
     * Returns time at which session's lifetime expires.
     * <p>
     * Time value is returned in the requested unit (accurate to millisecond) and uses the
     * same epoch as {@link System#currentTimeMillis()}.
     *
     * @see #getMaxSessionTime()
     * @param timeUnit the time unit to return the result in.
     * @return the result in the given units.
     */
    public long getMaxSessionExpirationTime(final TimeUnit timeUnit) {
        return timeUnit.convert(creationTimeInSeconds + MINUTES.toSeconds(maxSessionTimeInMinutes), SECONDS);
    }

    /**
     * Returns time at which session's idle time expires.
     * <p>
     * Time value is returned in the requested unit (accurate to millisecond) and uses the
     * same epoch as {@link System#currentTimeMillis()}.
     *
     * @see #getMaxIdleTime()
     * @param timeUnit the time unit to return the result in.
     * @return the result in the given units.
     */
    public long getMaxIdleExpirationTime(final TimeUnit timeUnit) {
        return timeUnit.convert(latestAccessTimeInSeconds + MINUTES.toSeconds(maxIdleTimeInMinutes), SECONDS);
    }

    /**
     * If the session has timed out, returns time at which session's purge delay expires.
     * <p>
     * Time value is returned in the requested unit (accurate to millisecond) and uses the
     * same epoch as {@link System#currentTimeMillis()}.
     *
     * @param timeUnit the time unit to return the result in.
     * @return the result in the given units.
     */
    public long getPurgeDelayExpirationTime(final TimeUnit timeUnit) {
        if (!isTimedOut()) {
            return -1;
        }
        return timeUnit.convert(timedOutTimeInSeconds + purgeDelayInSeconds, SECONDS);
    }

    /**
     * Correctly read and reschedule this session when it is read.
     */
    public void scheduleExpiry() {
        if (willExpireFlag) {
            taskRunnable.scheduleExpiry();
        }
    }

    /**
     * @return True if the Session has reached an invalid state.
     */
    public boolean isInvalid() {
        return sessionState == SessionState.INVALID;
    }

    /**
     * Signals the Session for removal.
     */
    private void removeSession() {
        SessionInfo sessionInfo = toSessionInfo();
        sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
        sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
        setState(SessionState.DESTROYED);
        sessionService.removeCachedInternalSession(sessionID);
        sessionService.sendEvent(this, SessionEvent.DESTROY);
    }

    @VisibleForTesting
    static void setPurgeDelayInSeconds(long newPurgeDelay) {
        purgeDelayInSeconds = newPurgeDelay;
    }

    /**
     * Sets the time at which this session timed out due to idle/max timeout. The time is in seconds since the same
     * epoch as {@link System#currentTimeMillis()}. A value of 0 indicates that the session has not timed out.
     *
     * @param timedOutAt the time in seconds at which the session timed out.
     */
    @VisibleForTesting
    void setTimedOutAt(long timedOutAt) {
        this.timedOutTimeInSeconds = timedOutAt;
    }

    @Override
    public void setPersistenceManager(SessionPersistenceManager manager) {
        persistenceManager = manager;
    }

    private void notifyPersistenceManager() {
        if (persistenceManager != null) {
            persistenceManager.notifyUpdate(getSessionID());
        }
    }

    private class InternalSessionTaskRunnable extends GeneralTaskRunnable {
        private transient TimerPool timerPool = null;

        public InternalSessionTaskRunnable() {
            timerPool = SystemTimerPool.getTimerPool();
        }

        @Override
        public boolean addElement(Object obj) {
            return false;
        }

        @Override
        public boolean removeElement(Object obj) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        /**
         * @return -1 since it is not a periodic task.
         */
        @Override
        public long getRunPeriod() {
            return -1;
        }

        @Override
        public void run() {

            if (isTimedOut() || isInvalid()) {
                removeSession();
                return;
            }

            long timeLeftInSeconds = getTimeLeft();
            if (timeLeftInSeconds == 0) {
                timeoutSessionAndSchedulePurge(SessionEvent.MAX_TIMEOUT, AM_SESSION_MAX_TIMED_OUT);
            } else {
                long idleTimeLeftInSeconds = MINUTES.toSeconds(maxIdleTimeInMinutes) - getIdleTime();
                if (idleTimeLeftInSeconds <= 0 && sessionState != SessionState.INACTIVE) {
                    timeoutSessionAndSchedulePurge(SessionEvent.IDLE_TIMEOUT, AM_SESSION_IDLE_TIMED_OUT);
                } else {
                    if (timerPool != null) {
                        long timeToWaitInSeconds = Math.min(timeLeftInSeconds, idleTimeLeftInSeconds);
                        long timeoutInMillis = currentTimeMillis() + SECONDS.toMillis(timeToWaitInSeconds);
                        timerPool.schedule(this, new Date(timeoutInMillis));
                    }
                }
            }

        }

        private void timeoutSessionAndSchedulePurge(int event, AuditConstants.EventName eventName) {
            Reject.ifFalse(event == SessionEvent.MAX_TIMEOUT || event == SessionEvent.IDLE_TIMEOUT);
            changeStateAndNotify(event);
            sessionAuditor.auditActivity(toSessionInfo(), eventName);
            if (timerPool != null) {
                if (purgeDelayInSeconds > 0) {
                    long timeoutInMillis = SECONDS.toMillis(timedOutTimeInSeconds + purgeDelayInSeconds);
                    timerPool.schedule(this, new Date(timeoutInMillis));
                }
            }
        }

        /**
         * Schedule this task to TimerPool according to the current state.
         */
        private void reschedule() {
            if (timerPool != null) {
                long timeoutInMillis = Long.MAX_VALUE;
                switch (sessionState) {
                    case INVALID:
                        timeoutInMillis = SECONDS.toMillis(creationTimeInSeconds +
                                MINUTES.toSeconds(maxDefaultIdleTimeInMinutes));
                        break;
                    case VALID:
                        timeoutInMillis = Math.min(
                                SECONDS.toMillis(latestAccessTimeInSeconds + MINUTES.toSeconds(maxIdleTimeInMinutes)),
                                SECONDS.toMillis(creationTimeInSeconds + MINUTES.toSeconds(maxSessionTimeInMinutes)));
                        break;
                }
                if (timeoutInMillis < scheduledExecutionTime()) {
                    cancel();
                }
                if (scheduledExecutionTime() == -1) {
                    timerPool.schedule(this, new Date(timeoutInMillis));
                }
            }
        }

        /**
         * Correctly read and reschedule this session when it is read.
         */
        private void scheduleExpiry() {
            timerPool = SystemTimerPool.getTimerPool();
            if (!isTimedOut()) {
                if (isInvalid()) {
                    // creationTime + maxDefaultIdleTime looks like a suspicious combination
                    long expectedTimeInMillis = SECONDS.toMillis(creationTimeInSeconds +
                            MINUTES.toSeconds(maxDefaultIdleTimeInMinutes));
                    if (expectedTimeInMillis > MILLISECONDS.toSeconds(currentTimeMillis())) {
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date(expectedTimeInMillis));
                        }
                    } else {
                        removeSession();
                    }
                } else {
                    long timeLeftInSeconds = getTimeLeft();
                    if (timeLeftInSeconds == 0) {
                        changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                        sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                        if (timerPool != null) {
                            long timeoutInMillis = SECONDS.toMillis(timedOutTimeInSeconds + purgeDelayInSeconds);
                            timerPool.schedule(this, new Date(timeoutInMillis));
                        }
                    } else {
                        long idleTimeLeftInSeconds = MINUTES.toSeconds(maxIdleTimeInMinutes) - getIdleTime();
                        if (idleTimeLeftInSeconds <= 0 && sessionState != SessionState.INACTIVE) {
                            changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                            sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
                            if (timerPool != null) {
                                long timeoutInMillis = SECONDS.toMillis(timedOutTimeInSeconds + purgeDelayInSeconds);
                                timerPool.schedule(this, new Date(timeoutInMillis));
                            }
                        } else {
                            if (timerPool != null) {
                                long timeToWaitInSeconds = Math.min(timeLeftInSeconds, idleTimeLeftInSeconds);
                                long timeoutInMillis = currentTimeMillis() + SECONDS.toMillis(timeToWaitInSeconds);
                                timerPool.schedule(this, new Date(timeoutInMillis));
                            }
                        }
                    }
                }
            } else {
                long expectedTimeInMillis = SECONDS.toMillis(timedOutTimeInSeconds + purgeDelayInSeconds);
                if (expectedTimeInMillis > currentTimeMillis()) {
                    if (timerPool != null) {
                        timerPool.schedule(this, new Date(expectedTimeInMillis));
                    }
                } else {
                    removeSession();
                }
            }
        }

        private void setNonExpiring() {
            timerPool = null;
        }
    }

    /**
     * Simple enumeration to report how the session is changing in state
     */
    public enum StateTransition {
        DESTROY,
        MAX_TIMEOUT,
        IDLE_TIMEOUT,
        NO_CHANGE,
        PURGE
    }
}
