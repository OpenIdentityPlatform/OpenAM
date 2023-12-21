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
 * Portions Copyrighted 2023 3A Systems LLC
 */
package com.iplanet.dpro.session.service;

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
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
import org.forgerock.openam.session.AMSession;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.service.access.SessionPersistenceManager;
import org.forgerock.openam.session.service.access.SessionPersistenceObservable;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.session.util.SessionUtilsWrapper;
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
    @Override
	public String toString() {
		return String.format("%s sid=%s" , super.toString(),sessionID,this.getID());
	}

    /*
     * Session property names
     */
    private static final String HOST = "Host";
    private static final String HOST_NAME = "HostName";
    private static final String AM_MAX_IDLE_TIME = "AMMaxIdleTime";
    private static final String AM_MAX_SESSION_TIME = "AMMaxSessionTime";
    private static final String UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";
    private static final String SESSION_TIMED_OUT = "SessionTimedOut";
    private static final Set<String> protectedProperties = initialiseProtectedProperties();

    /*
     * Support objects (do not serialize)
     */
    private transient Debug debug;
    private transient SessionService sessionService;
    private transient SessionServiceConfig serviceConfig;
    private transient InternalSessionListener sessionEventBroker;
    private transient SessionUtilsWrapper sessionUtilsWrapper;
    private transient SessionConstraint sessionConstraint;
    private transient AuthContextLocal authContext;
    private transient SessionPersistenceManager persistenceManager;

    /*
     * System properties
     */
    private static boolean isEnableHostLookUp = SystemProperties.getAsBoolean(Constants.ENABLE_HOST_LOOKUP);

    /* Maximum frequency with which the access time in the repository will be updated. */
    private static int interval = SystemProperties.getAsInt("com.sun.identity.session.interval", 30);

    /* default idle time for invalid sessions */
    @JsonProperty("maxDefaultIdleTime")
    private static final long maxDefaultIdleTimeInMinutes =
            SystemProperties.getAsLong("com.iplanet.am.session.invalidsessionmaxtime", 3);

    /*
     * State
     */
    private SessionID sessionID;
    private SessionType sessionType = SessionType.USER;
    private SessionState sessionState = SessionState.INVALID;
    private String clientID;
    private String clientDomain;
    public Properties sessionProperties; // e.g. LoginURL, Timeout, Host, etc
    private boolean willExpireFlag;
    private boolean isSessionUpgrade = false;
    private Boolean cookieMode = null;
    private String cookieStr;

    @JsonProperty("creationTime")
    private long creationTimeInSeconds;

    @JsonProperty("latestAccessTime")
    private long latestAccessTimeInSeconds;
    
    @JsonIgnore
    private long latestSaveAccessTimeInSeconds=0;

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
    InternalSession(
            SessionID sid, SessionService service, SessionServiceConfig serviceConfig,
            InternalSessionEventBroker internalSessionEventBroker, SessionUtilsWrapper sessionUtilsWrapper,
            SessionConstraint sessionConstraint, Debug debug) {
        sessionID = sid;
        setSessionServiceDependencies(service, serviceConfig, internalSessionEventBroker,
                sessionUtilsWrapper, sessionConstraint, debug);
        maxIdleTimeInMinutes = maxDefaultIdleTimeInMinutes;
        maxSessionTimeInMinutes = maxDefaultIdleTimeInMinutes;
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
                InjectorHolder.getInstance(InternalSessionEventBroker.class),
                InjectorHolder.getInstance(SessionUtilsWrapper.class),
                InjectorHolder.getInstance(SessionConstraint.class),
                InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG))));
    }

    /**
     * Default constructor required for deserialisation, and should not be used elsewhere.
     *
     * When deserialised the code responsible for instantiating it will have no means of resolving dependencies.
     *
     * Instead this is deferred to
     * {@link InternalSession#setSessionServiceDependencies(SessionService, SessionServiceConfig,
     *                      InternalSessionEventBroker, SessionUtilsWrapper, SessionConstraint, Debug)}
     */
    public InternalSession() {
        // Intentionally left blank
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
    public void setSessionServiceDependencies(
            SessionService service, SessionServiceConfig serviceConfig, InternalSessionEventBroker internalSessionEventBroker,
            SessionUtilsWrapper sessionUtilsWrapper, SessionConstraint sessionConstraint, Debug debug) {

        this.sessionService = service;
        this.serviceConfig = serviceConfig;
        this.sessionEventBroker = internalSessionEventBroker;
        this.sessionUtilsWrapper = sessionUtilsWrapper;
        this.sessionConstraint = sessionConstraint;
        this.debug = debug;
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
     * @return <code>USER</code> or <code>APPLICATION</code>.
     */
    public SessionType getType() {
        return sessionType;
    }

    /**
     * Set the type of Internal Session. User OR Application.
     *
     * @param type <code>USER</code> or <code>APPLICATION</code>.
     */
    public void setType(SessionType type) {
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
     * Sets the maximum time (in minutes) allowed for the Internal Session
     *
     * @param maxSessionTimeInMinutes
     *            Maximum Session Time
     */
    public void setMaxSessionTime(long maxSessionTimeInMinutes) {
        if (this.maxSessionTimeInMinutes != maxSessionTimeInMinutes) {
        	if (willExpireFlag == false &&maxSessionTimeInMinutes<NON_EXPIRING_SESSION_LENGTH_MINUTES)
        		willExpireFlag = true;
            this.maxSessionTimeInMinutes = maxSessionTimeInMinutes;
            notifyPersistenceManager();
        }
    }

    /**
     * Returns the maximum idle time(in minutes) for the Internal Session.
     * @return the number maximum idle minutes
     */
    public long getMaxIdleTime() {
        return maxIdleTimeInMinutes;
    }

    /**
     * Sets the maximum idle time (in minutes) for the Internal Session.
     *
     * @param maxIdleTimeInMinutes
     */
    public void setMaxIdleTime(long maxIdleTimeInMinutes) {
    	if (this.maxIdleTimeInMinutes != maxIdleTimeInMinutes) {
	    	if (willExpireFlag == false && maxIdleTimeInMinutes<NON_EXPIRING_SESSION_LENGTH_MINUTES)
	    		willExpireFlag = true;
	            
	    	this.maxIdleTimeInMinutes = maxIdleTimeInMinutes;
	        notifyPersistenceManager();
    	}
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
    	if (this.maxCachingTimeInMinutes != t) {
	    	if (willExpireFlag == false && t<serviceConfig.getApplicationMaxCachingTime())
	    		willExpireFlag = true;
	        maxCachingTimeInMinutes = t;
	        notifyPersistenceManager();
    	}
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
        return protectedProperties.contains(key) || key.toLowerCase().startsWith(Constants.AM_PROTECTED_PROPERTY_PREFIX);
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
        protectedProperties.add(SESSION_TIMED_OUT);
        protectedProperties.add(SESSION_HANDLE_PROP);
        protectedProperties.add(TOKEN_RESTRICTION_PROP);
        protectedProperties.add(AM_MAX_IDLE_TIME);
        protectedProperties.add(AM_MAX_SESSION_TIME);
        protectedProperties.add(Constants.AM_CTX_ID);
        protectedProperties.add(Constants.UNIVERSAL_IDENTIFIER);

        String protectedPropertiesConfig = SystemProperties.get(Constants.PROTECTED_PROPERTIES_LIST, "");

        if (protectedPropertiesConfig != null) {
            StringTokenizer st = new StringTokenizer(protectedPropertiesConfig, ",");
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
    public void putExternalProperty(SSOToken clientToken, String key, String value) throws SessionException {
		try {
        	sessionUtilsWrapper.checkPermissionToSetProperty(clientToken, key, value);
		} catch (SessionException se) {
            fireSessionEvent(SessionEventType.PROTECTED_PROPERTY);
			throw se;
		}
        internalPutProperty(key,value);
        debug.message("Updated protected property after validating client identity and permissions");
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
            fireSessionEvent(SessionEventType.PROPERTY_CHANGED);
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
     * Returns whether the InternalSession represented has been stored. If this is true, changes to this object will
     * update the stored version.
     * return <code>true</code> if the internal session is stored
     *        <code>false</code> otherwise
     */
    public boolean isStored() {
        return persistenceManager != null;
    }

    /**
     * Changes the state of the session to ACTIVE after creation.
     * @param userDN
     * @return <code> true </code> if the session is successfully activated
     *         after creation , <code>false</code> otherwise
     */
    public boolean activate(String userDN) {
        // check userDN was provided
        if (userDN == null) {
            return false;
        }

        // check session quota constraints
        if ((serviceConfig.isSessionConstraintEnabled()) && !shouldIgnoreSessionQuotaChecking()) {
            if (sessionConstraint.checkQuotaAndPerformAction(this)) {
                debug.message("Session Quota exhausted!");
                fireSessionEvent(SessionEventType.QUOTA_EXHAUSTED);
                return false;
            }
        }

        // safe to proceed with session activation
        setLatestAccessTime();
        setState(SessionState.VALID);
        fireSessionEvent(SessionEventType.SESSION_CREATION);

        return true;
    }

    /*
     * The session quota checking will be bypassed if:
     * (1) the login user is the super user (not including users assigned the top level admin role), or
     * (2) the token is an application token (e.g. Agent)
     */
    private boolean shouldIgnoreSessionQuotaChecking() {
        return (getState().equals(SessionState.VALID) || sessionService.isSuperUser(getUUID()) || (isAppSession()));
    }

    /**
     * Gets the User Universal ID
     * @return  UUID
     */
    public String getUUID() {
        return getProperty(UNIVERSAL_IDENTIFIER);
    }

    /**
     * Sets the willExpireFlag. This flag specify that whether the session will
     * ever expire or not.
     */
    public void setNonExpiring() {
        maxSessionTimeInMinutes = NON_EXPIRING_SESSION_LENGTH_MINUTES;
        maxIdleTimeInMinutes = NON_EXPIRING_SESSION_LENGTH_MINUTES;
        maxCachingTimeInMinutes = serviceConfig.getApplicationMaxCachingTime();
        willExpireFlag = false;
    }

    /**
     * Sets session timeout time (in millis).
     *
     * @param timeoutTime The timeout time (in millis).
     */
    public void setTimedOutTime(long timeoutTime) {
   		if (!willExpire()) {
   			debug.error("!willExpire {}",toString());
   			throw new IllegalStateException("Cannot timeout non-expiring session.");
   		}
        Reject.rejectStateIfTrue(isTimedOut(), "Session already timed out.");
        timedOutTimeInSeconds = MILLISECONDS.toSeconds(timeoutTime);
        putProperty(SESSION_TIMED_OUT, String.valueOf(timedOutTimeInSeconds));
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

        if (sessionType == SessionType.USER) {
            info.setSessionType("user");
        } else if (sessionType == SessionType.APPLICATION) {
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
        info.setSessionEventUrls(new HashSet<>(sessionEventURLs.keySet()));
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
        latestAccessTimeInSeconds = currentTimeMillis() / 1000;
        if ((latestAccessTimeInSeconds - latestSaveAccessTimeInSeconds) > interval) {
        	latestSaveAccessTimeInSeconds=latestAccessTimeInSeconds;
            notifyPersistenceManager();
        }
    }

    /**
     * Sets the {@link SessionState} of the Internal Session.
     *
     * @param sessionState
     */
    public void setState(SessionState sessionState) {
        if (this.sessionState != sessionState) {
            this.sessionState = sessionState;
            notifyPersistenceManager();
        }
    }

    /**
     * Returns the URL of the Session events and the associated master and
     * restricted token ids.
     * @return Map of session event URLs and their associated SessionIDs.
     */
    public Map<String, Set<SessionID>> getSessionEventURLs() {
        Map<String, Set<SessionID>> urls = new HashMap<>();
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
            fireSessionEvent(SessionEventType.EVENT_URL_ADDED);
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
    public boolean isAppSession() {
        return sessionType == SessionType.APPLICATION;
    }

    /**
     * Determine whether it is a user session.
     *
     * @return <code>true</code> if this is a user session, <code>false</code> otherwise.
     */
    public boolean isUserSession() {
        return sessionType == SessionType.USER;
    }

    /**
     * Sets the creation time of the Internal Session, as the number of seconds
     * since midnight January 1, 1970 GMT.
     */
    public void setCreationTime() {
        creationTimeInSeconds = currentTimeMillis() / 1000;
    }

    /**
     * Add new restricted token pointing at the same session to the list.
     *
     * @param newRestrictedTokenId The session ID.
     * @param restriction The token restriction.
     * @return The restricted token id for this TokenRestriction.
     */
    public SessionID addRestrictedToken(SessionID newRestrictedTokenId, TokenRestriction restriction) {
        SessionID currentRestrictedTokenId = restrictedTokensByRestriction.putIfAbsent(restriction, newRestrictedTokenId);
        if (currentRestrictedTokenId == null) {
            restrictedTokensBySid.put(newRestrictedTokenId, restriction);
            notifyPersistenceManager();
            return newRestrictedTokenId;
        }
        return currentRestrictedTokenId;
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

    /**
     * Returns the SessionID of the restricted token for the provided restriction for this session.
     *
     * @param restriction restriction used to look up restricted token.
     * @return restricted token sessionID.
     */
    public SessionID getRestrictedTokenForRestriction(TokenRestriction restriction) {
        return restrictedTokensByRestriction.get(restriction);
    }

    /**
     * Returns the set (possibly empty) of restricted session IDs associated with this session. A restricted session
     * ID can only be used when the associated {@link TokenRestriction} is satisfied. Typically this ties a particular
     * user session to only be used via a particular agent or from a particular IP address.
     * <p>
     * The result is a copy of the current restricted token set: modifications to it will not change the set of
     * restricted tokens associated with the session.
     *
     * @return the set of restricted tokens associated with this session. Never null but can be empty.
     */
    public Set<SessionID> getRestrictedTokens() {
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
        debug.message("InternalSession: getCookieSupport: {}", cookieSupport);
        return cookieSupport;
    }

    /**
     * set the cookieMode based on whether the request has cookies or not. This
     * method is called from createSSOToken(request) method in SSOTokenManager.
     *
     * @param cookieMode ,
     *            Boolean value whether request has cookies or not.
     */
    // TODO: Remove unused method
    public void setCookieMode(Boolean cookieMode) {
        debug.message("CookieMode is: {}", cookieMode);
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
     * @return True if the Session has reached an invalid state.
     */
    public boolean isInvalid() {
        return sessionState == SessionState.INVALID;
    }

    @Override
    public void setPersistenceManager(SessionPersistenceManager manager) {
        persistenceManager = manager;
    }

    public void notifyPersistenceManager() {
        if (persistenceManager != null) {
            persistenceManager.notifyUpdate(this);
        }
    }

    private void fireSessionEvent(SessionEventType sessionEventType) {
        sessionEventBroker.onEvent(new InternalSessionEvent(this, sessionEventType, Time.currentTimeMillis()));
    }
    
    @JsonIgnore private final ConcurrentMap<String, Object> internalObjects = new ConcurrentHashMap<>();
    /**
     * Returns the value of the specified key from the internal object map.
     * 
     * @param key
     *            the key whose associated value is to be returned
     * @return internal object
     */
    public Object getObject(String key) {
        return getInternalObjectMap().get(key);
    }

    /**
     * Removes the mapping for this key from the internal object map if present.
     * 
     * @param key
     *            key whose mapping is to be removed from the map
     */
    public void removeObject(String key) {
        getInternalObjectMap().remove(key);
    }

    /**
     * Sets the key-value pair in the internal object map.
     * 
     * @param key with which the specified value is to be associated
     * @param value to be associated with the specified key
     */
    public void setObject(String key, Object value) {
        getInternalObjectMap().put(key, value);
    }

    private Map getInternalObjectMap() {
        return internalObjects;
    }

}
