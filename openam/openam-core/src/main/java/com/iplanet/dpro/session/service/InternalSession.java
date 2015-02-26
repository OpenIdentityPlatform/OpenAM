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
 * $Id: InternalSession.java,v 1.21 2009/03/20 21:05:25 weisun2 Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.HeadTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * The <code>InternalSession</code> class represents a Webtop internal
 * session. A session has four states: invalid, valid, inactive, and destroy.
 * The initial state of a session is invalid. The following is the state diagram
 * for a session:
 * <pre>
 *  | | | V -------------<invalid> | | | | creation (authentication OK) | |
 * |max login time | max idle time |destroy V ---------------> | <valid>
 * <inactive> -- | | <-------------- | | | reactivate | | | | | | logout |
 * destroy | | destroy | max session time | | max session time | | V |
 * ------------> <destroy> <--------------------------
 * </pre>
 * 
 */

public class InternalSession implements TaskRunnable, Serializable {

    // Debug should not be serialised.
    private transient Debug DEBUG;

    // Session Service should not be serialised.
    private transient SessionService ss;

    /* user universal unique ID*/
    private String uuid;

    /* Session Id (sid) */
    private SessionID sessionID;

    /*Session type user or application */
    private int sessionType;

    /* Client ID for the internal session */
    private String clientID;

    /* Client Domain for the internal session */
    private String clientDomain;

    /* maximum internal session time in minutes */
    private long maxSessionTime;

    /* maximum internal session idle time */
    private long maxIdleTime; // in minutes

    /*Maximum internal session cache time */
    private long maxCachingTime; // in minutes

    /* Internal session state INACTIVE, VALID,INVALID,DESTROYED*/
    private int sessionState;

    /** session properties LoginURL,Timeout, Host */
    private Properties sessionProperties;

    /** Flag indicates for session to expire at max timeout */
    private boolean willExpireFlag;

    /** Flag to indicate upgrading the session */
    transient private boolean isSessionUpgrade = false;

    /*
     * The following object map is meant to be used to store the transient
     * objects such as Auth related user properties (e.g. AuthContext and
     * LoginState) within the InternalSession object. There are a few
     * characteristics for this type of objects:
     *  - These objects and the corresponding interfaces are for internal use
     * only. - These objects are "transient" objects which don't require
     * persisency. In other words, they won't be saved into the session
     * repository in the SFO case. - These object are not session properties
     * since they are not meant to exposed to any client.
     */
    transient private Map internalObjects = new HashMap();

    /** holds session creation time */
    private long creationTime; // in seconds

    /** holds latest accesstime*/
    private long latestAccessTime;// in seconds

    transient private HttpSession httpSession;

    private boolean isISStored = false;

    Boolean cookieMode = null;

    private String cookieStr;

    private Map restrictedTokensBySid = Collections
            .synchronizedMap(new HashMap());

    private Map restrictedTokensByRestriction = Collections
            .synchronizedMap(new HashMap());

    private static String superUserDN;

    private static boolean isEnableHostLookUp = Boolean.valueOf(
            SystemProperties.get(Constants.ENABLE_HOST_LOOKUP)).booleanValue();

    /*
     * This is the Time(seconds) when the session timedout. Value zero means the
     * session has not timed out.
     */
    private volatile long timedOutAt = 0;

    /**
     * Logical version timestamp used to implement optimistic concurrency
     * control in shared session repository
     */
    private long version = 0;

    /*
     * This is the maximum extra time for which the timed out sessions lives in
     * the session server
     */
    private static long purgeDelay;

    /*
     * session handle is used to prevent administrator from impersonating other
     * users
     */
    private String sessionHandle = null;

    /** session property LoginURL */
    private static final String LOGIN_URL = "loginURL";

    /* Internal session time out property */
    private static final String SESSION_TIMED_OUT = "SessionTimedOut";

    /* Internal session host property */
    private static final String HOST = "Host";

    private static final String HOST_NAME = "HostName";

    /* AM Internal session maximum idle time */
    private static final String AM_MAX_IDLE_TIME = "AMMaxIdleTime";

    /* session property: SAML2IDPSessionIndex */
    private static final String SAML2_IDP_SESSION_INDEX = 
        "SAML2IDPSessionIndex";

    /** AM User Universal Identifier Property*/
    protected static final String UNIVERSAL_IDENTIFIER = 
        "sun.am.UniversalIdentifier";

    private static final String LOG_MSG_SESSION_MAX_LIMIT_REACHED = 
        "SESSION_MAX_LIMIT_REACHED";

    /* Time interval, if the current time minus lastest access time is 
       greater than this time interval. In SFO mode, the session record 
       will get refreshed in data repository.*/
    private static int interval = Integer.parseInt(
        SystemProperties.get(
        "com.sun.identity.session.interval", "10")); 
    private transient volatile TaskRunnable nextTask = null;
    private transient volatile TaskRunnable previousTask = null;
    private transient volatile HeadTaskRunnable headTask = null;
    private transient TimerPool timerPool = null;
    private volatile boolean reschedulePossible;
    
    /*
     * default idle time for invalid sessions
     */
    private static long maxDefaultIdleTime;
    static {
        maxDefaultIdleTime = getPropValue(
                "com.iplanet.am.session.invalidsessionmaxtime", 3);
        purgeDelay = getPropValue("com.iplanet.am.session.purgedelay", 120);
        superUserDN = SystemProperties
                .get("com.sun.identity.authentication.super.user");
    }

    /**
     * Get the default property values set for invalid session , purge delay
     * super user name etc 
     */
    private static long getPropValue(String propName, long defaultValue) {
        String defaultPropValue = SystemProperties.get(propName, String
                .valueOf(defaultValue));
        long propValue = 0;
        try {
            propValue = Long.parseLong(defaultPropValue);
        } catch (Exception le) {
            propValue = defaultValue;
        }
        return propValue;
    }

    /*
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
     */
    protected static Set protectedProperties;

    static {
        protectedProperties = new HashSet();
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
        protectedProperties.add(Session.SESSION_HANDLE_PROP);
        protectedProperties.add(Session.TOKEN_RESTRICTION_PROP);
        protectedProperties.add(AM_MAX_IDLE_TIME);
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
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug
                            .message("Added protected property [" + prop + "]");
                }

            }
        }

    }

    /*
     * The URL map for session events of THIS session only : SESSION_CREATION,
     * IDLE_TIMEOUT, MAX_TIMEOUT, LOGOUT, REACTIVATION, DESTROY. Each URL in the
     * map is associated with a set of token ids (master and potentially all of
     * the restricted token ids associated with the master) that will be used in 
     * notification
     */
    private Map<String, Set<SessionID>> sessionEventURLs =
        Collections.synchronizedMap(new HashMap<String, Set<SessionID>>());

    /**
     * Creates an instance of the Internal Session with its key dependences exposed.
     *
     * Note: This InternalSession will be in an invalid state.
     *
     * @param sid Non null Session ID.
     * @param service Non null SessionService.
     * @param debug Debugging instance to use for all logging.
     */
    public InternalSession(SessionID sid, SessionService service, Debug debug) {
        sessionID = sid;
        ss = service;
        DEBUG = debug;

        maxIdleTime = maxDefaultIdleTime;
        maxSessionTime = maxDefaultIdleTime;
        reschedulePossible = maxDefaultIdleTime > maxIdleTime;
        sessionState = Session.INVALID;
        timerPool = SystemTimerPool.getTimerPool();
        sessionProperties = new Properties();
        willExpireFlag = true;
    }

    /**
     * Creates a new InternalSession with the given Session ID.
     *
     * Note: This InternalSession will be in an invalid state.
     *
     * @param sid SessionID Non null Session ID.
     */
    public InternalSession(SessionID sid) {
        this(sid, SessionService.getSessionService(), SessionService.sessionDebug);
    }

    /**
     * Default constructor required for deserialisation.
     */
    public InternalSession() {
        this(null, SessionService.getSessionService(), SessionService.sessionDebug);
    }

    /**
     * Implements for TaskRunnable.
     *
     * @param headTask The HeadTask indicate the time to run this task.
     */
    public void setHeadTask(HeadTaskRunnable headTask) {
        this.headTask = headTask;
    }

    /**
     * Implements for TaskRunnable.
     *
     * @return The long value indicates the time this task is scheduled.
     */
    public long scheduledExecutionTime() {
        synchronized (this) {
            if (headTask != null) {
                return headTask.scheduledExecutionTime();
            }
        }
        return -1;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return The HeadTask of this task.
     */
    public HeadTaskRunnable getHeadTask() {
        // no need to synchronize for single operation
        return headTask;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return The task previous to this one.
     */
    public TaskRunnable previous() {
        return previousTask;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return The task next to this one.
     */
    public TaskRunnable next() {
        return nextTask;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @param task The task previous to this one.
     */
    public void setPrevious(TaskRunnable task) {
        previousTask = task;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @param task The task next to this one.
     */
    public void setNext(TaskRunnable task) {
        nextTask = task;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean addElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean removeElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for TaskRunnable.
     *
     * @return true since this class will not be used as container.
     */
    public boolean isEmpty() {
        return true;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return -1 since it is not a periodic task.
     */
    public long getRunPeriod() {
        return -1;
    }
    
    /**
     * The function to run when timeout.
     */
    public void run() {
        if (!isTimedOut()) {
            if (sessionState == Session.INVALID) {
                setState(Session.DESTROYED);
                ss.removeInternalSession(sessionID);
                ss.sendEvent(this, SessionEvent.DESTROY);
            } else {
                long timeLeft = getTimeLeft();
                if (timeLeft == 0) {
                    changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                    if (timerPool != null) {
                        if (purgeDelay > 0) {
                            timerPool.schedule(this, new Date((timedOutAt +
                                (purgeDelay * 60)) * 1000));
                        }
                    }
                } else {
                    long idleTimeLeft = (maxIdleTime * 60) - getIdleTime();
                    if (idleTimeLeft <= 0 && sessionState != Session.INACTIVE) {
                        changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                        if (timerPool != null) {
                            if (purgeDelay > 0) {
                                timerPool.schedule(this, new Date((timedOutAt +
                                    (purgeDelay * 60)) * 1000));
                            }
                        }
                    } else {
                        long timeToWait = Math.min(timeLeft, idleTimeLeft);
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date(((
                                System.currentTimeMillis() / 1000) +
                                timeToWait) * 1000));
                        }
                    }
                }        
            }
        } else {
            ss.logEvent(this, SessionEvent.DESTROY);
            setState(Session.DESTROYED);
            ss.removeInternalSession(sessionID);
            ss.sendEvent(this, SessionEvent.DESTROY);
        }
    }
    
    /**
     * Cancel the scheduled run of this task from TimerPool.
     */
    public void cancel() {
        HeadTaskRunnable oldHeadTask = null;
        do {
            oldHeadTask = headTask;
            if (oldHeadTask != null) {
                if (oldHeadTask.acquireValidLock()) {
                    try {
                        if (oldHeadTask == headTask) {
                            if (!oldHeadTask.isTimedOut()) {
                                previousTask.setNext(nextTask);
                                if (nextTask != null) {
                                    nextTask.setPrevious(previousTask);
                                    nextTask = null;
                                } else {
                                    oldHeadTask.setTail(previousTask);
                                }
                            }
                            break;
                        }
                    } finally {
                        oldHeadTask.releaseLockAndNotify();
                    }
                }
            }
        } while (oldHeadTask != headTask);
        headTask = null;
    }
    
    /**
     * Schedule this task to TimerPool according to the current state.
     */
    protected void reschedule() {
        if (timerPool != null) {
            long timeoutTime = Long.MAX_VALUE;
            switch (sessionState) {
                case Session.INVALID:
                    timeoutTime = (creationTime +
                        (maxDefaultIdleTime * 60)) * 1000;
                    break;
                case Session.VALID:
                    timeoutTime = Math.min((latestAccessTime +
                        (maxIdleTime * 60)) * 1000,  (creationTime +
                        (maxSessionTime * 60)) * 1000);
                    break;
            }
            if (timeoutTime < scheduledExecutionTime()) {
                cancel();
            }
            if (scheduledExecutionTime() == -1) {
                Date time = new Date(timeoutTime);
                timerPool.schedule(this, time);
            }
        }
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
        updateForFailover();
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
        updateForFailover();
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
        updateForFailover();
    }

    /**
     * Returns maximum time allowed for the Internal Session.
     * @return the number of maximum minutes for the session
     */
    public long getMaxSessionTime() {
        return maxSessionTime;
    }

    /**
     * Sets the maximum time(in minutes) allowed for the Internal Session
     * 
     * @param t
     *            Maximum Session Time
     */
    public void setMaxSessionTime(long t) {
        boolean mayReschedule = false;
        if (t < maxSessionTime) {
            mayReschedule = true;
        }
        maxSessionTime = t;
        if ((scheduledExecutionTime() != -1) && mayReschedule) {
            reschedule();
        }
        updateForFailover();
    }

    /**
     * Returns the maximum idle time(in minutes) for the Internal Session.
     * @return the number maximum idle minutes
     */
    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Sets the maximum idle time(in minutes) for the Internal Session.
     * 
     * @param t
     */
    public void setMaxIdleTime(long t) {
        boolean mayReschedule = false;
        if (t < maxIdleTime) {
            mayReschedule = true;
        }
        maxIdleTime = t;
        reschedulePossible = (maxDefaultIdleTime > maxIdleTime) || (maxDefaultIdleTime > maxSessionTime);
        if (httpSession != null) {
            int httpIdleTime = httpSession.getMaxInactiveInterval();
            if (maxIdleTime > httpIdleTime) {
                httpSession.setMaxInactiveInterval(((int) maxIdleTime) * 60);
            }
        }
        if ((scheduledExecutionTime() != -1) && (mayReschedule ||
            reschedulePossible)) {
            reschedule();
        }
        updateForFailover();
    }

    /**
     * Returns the maximum caching time(in minutes) allowed for the Internal
     * Session.
     * @return Maximum Cache Time
     */
    public long getMaxCachingTime() {
        return maxCachingTime;
    }

    /**
     * Sets the maximum caching time(in minutes) for the Internal Session.
     * 
     * @param t
     *        Maximum Caching Time
     */
    public void setMaxCachingTime(long t) {
        maxCachingTime = t;
        updateForFailover();
    }

    /**
     * Returns the time(in seconds) for which the Internal Session has not been
     * accessed.
     * @return session idle time
     */
    public long getIdleTime() {
        long now = System.currentTimeMillis() / 1000;
        return now - latestAccessTime;
    }

    /**
     * Returns the total time left(in seconds) for the Internal Session.
     * @return Time left for the internal session to be invalid
     */
    public long getTimeLeft() {
        long now = System.currentTimeMillis() / 1000;
        long left = creationTime + maxSessionTime * 60 - now;
        if (left >= 0) {
            return left;
        } else {
            return 0;
        }
    }

    /**
     * Returns the extra time left(in seconds) for the Internal Session after
     * the session timed out.
     * @return time remaining before purge. <code> -1 </code> if the session
     * has not yet timed out due idle/max timeout period.
     */
    public long getTimeLeftBeforePurge() {
        /**
         * Return -1 if the session has not timed out due to idle/max timeout
         * period.
         */
        if (!isTimedOut()) {
            return -1;
        }
        /**
         * Return the extra time left, if the session has timed out due to
         * idle/max time out period
         */
        long now = System.currentTimeMillis() / 1000;
        long left = (timedOutAt + purgeDelay * 60 - now);
        return (left > 0) ? left : 0;
    }

    /**
     * Returns true if the session has timed out due to idle/max timeout period.
     * @return <code>true</code> if the Internal session has timedout ,
     * <code>false</code> otherwise
     */
    public boolean isTimedOut() {
        return timedOutAt != 0;
    }

    /**
     * Returns the state of the Internal Session
     * @return the session state can be VALID,INVALID,INACTIVE,DESTORYED
     */
    public int getState() {
        return sessionState;
    }

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
        if (internalObjects == null) {
            internalObjects = new HashMap();
        }
        return internalObjects;
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
    void putExternalProperty(SSOToken clientToken, String key, String value)
        throws SessionException {
		try {
        	SessionUtils.checkPermissionToSetProperty(clientToken, key, value);
		} catch (SessionException se) {
			SessionService.getSessionService().logIt(
				this, "SESSION_PROTECTED_PROPERTY_ERROR");
			throw se;
		}
        internalPutProperty(key,value);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Updated protected property"
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
    protected void internalPutProperty(String key, String value) {
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
                    DEBUG.error(
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
        } else {
            sessionProperties.put(key, value);
        }

        if (sessionState == Session.VALID
                && SessionService.isSendPropertyNotification(key)) {
            SessionService.getSessionService().sendEvent(this,
                    SessionEvent.PROPERTY_CHANGED);
            SessionService.getSessionService().logEvent(this,
                    SessionEvent.PROPERTY_CHANGED);
        }
        updateForFailover();
    }

    /** 
    * Sets the status of the isSessionUpgrade falg to which determines if the 
    * <code>Session</code> is in the upgrade state or not.
    * 
    * @param value <code>true</code> if it is an upgrade 
    *        <code>false</code> otherwise
    */
    public void setIsSessionUpgrade(boolean value) {

        isSessionUpgrade = value;
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
     * Sets the isISStored field.
     * @param value <code>true</code> if the internal session is stored
     *        <code>false</code> otherwise
     */
    public void setIsISStored(boolean value) {
        boolean wasISStored = isISStored;
        isISStored = value;
        if (isISStored && !wasISStored) {
            updateForFailover();
        }
    }

    /**
     * Returns the isISStored field
     * return <code>true</code> if the internal session is stored
     *        <code>false</code> otherwise
     */
    public boolean getIsISstored() {
        return isISStored;
    }

    /*
     * The session quota checking will be bypassed if:
     * (1) the login user is the super user, or
     * (2) the token is an application token (e.g. Agent), or
     * (3) the login user has the top level admin role (this
     *  checking will be enabled only when XXX
     */
    private boolean shouldIgnoreSessionQuotaChecking(String userDN) {

        boolean ignore = false;
        // FIXME Is this initialization necessary?
        SessionService.getSessionService();
        
        if (SessionService.getSessionService().
                isSuperUser(getUUID()) || (isAppSession())) {
            ignore = true;
        } else {
            // Need to check if the user has the top-level admin role
            // (expensive operation) only when the session constraint
            // needs to be bypassed for the top-level admins.
            boolean checkTopLevelAdminRole = SessionService
                    .bypassConstraintForToplevelAdmin();

            if (checkTopLevelAdminRole) {
                if (SessionService.getSessionService().
                    hasTopLevelAdminRole(getUUID())) {
                    ignore = true;
                }
            }
        }
        return ignore;
    }

    /**
     * Changes the state of the session to ACTIVE after creation.
     * @param userDN 
     * @return <code> true </code> if the session is successfully activated 
     *         after creation , <code>false</code> otherwise
     */
    public boolean activate(String userDN) {
        // throws SessionConstraintException {

        if (userDN == null) {
            return false;
        }
        // Exceeded max active sessions, but allow if the user is super-admin
        if ((SessionService.getActiveSessions() >= SessionService.maxSessions)
                && (!userDN.equalsIgnoreCase(superUserDN))) {
            SessionService.getSessionService().logSystemMessage(
                    LOG_MSG_SESSION_MAX_LIMIT_REACHED,
                    java.util.logging.Level.INFO);
            return false;
        }

        // need to set the uuid first
        setUUID();

        // checking Session Quota Constraints
        if ((SessionService.isSessionConstraintEnabled())
                && !shouldIgnoreSessionQuotaChecking(userDN)) {

            if (SessionConstraint.checkQuotaAndPerformAction(this)) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Session Quota "
                            + "exhausted!");
                }
                SessionService.getSessionService().logEvent(this,
                        SessionEvent.QUOTA_EXHAUSTED);
                return false;
            }
        }
        setLatestAccessTime();
        setState(Session.VALID);
        if (reschedulePossible) {
            reschedule();
        }
        SessionService.getSessionService().logEvent(this,
                SessionEvent.SESSION_CREATION);
        SessionService.getSessionService().sendEvent(this,
                SessionEvent.SESSION_CREATION);
        
        if (!isAppSession() || SessionService.returnAppSession) {
            SessionService.incrementActiveSessions();
        }
        return true;
    }

   /**
    *  Sets the User Universal ID
    *
    */
   public void setUUID() {
        uuid = getProperty(UNIVERSAL_IDENTIFIER);
    }

   /** 
    * Gets the User Universal ID
    * @return  UUID
    */
   public String getUUID() {
        return uuid;
    }

    /**
     * Changes the state of the session to ACTIVE from IN-ACTIVE.
     */
    public void reactivate() {
        cancel();
        setCreationTime();
        setLatestAccessTime();
        setState(Session.VALID);
        reschedule();
        SessionService.getSessionService().logEvent(this,
                SessionEvent.REACTIVATION);
        SessionService.getSessionService().sendEvent(this,
                SessionEvent.REACTIVATION);
    }

    /**
     * Sets the willExpireFlag. This flag specify that whether the session will
     * ever expire or not.
     * 
     * @param expire <code>true</code> will the set internal session to expire
     */
    public void setExpire(boolean expire) {
        if (expire == false) {
            maxSessionTime = Long.MAX_VALUE / 60;
            maxIdleTime = Long.MAX_VALUE / 60;
            maxCachingTime = SessionService.applicationMaxCachingTime;
            cancel();
            timerPool = null;
        }
        willExpireFlag = expire;
    }

    /**
     * Checks the invalid session idle time. If this session is invalid and idle
     * for more than 3 minutes, we will need to remove it from the session table
     * 
     * @return <code>true</code> if the max default idle time expires
     */
    private boolean checkInvalidSessionDefaultIdleTime() {
        long now = System.currentTimeMillis() / 1000;
        long left = creationTime + maxDefaultIdleTime * 60 - now;
        if (left >= 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the sesion should be destroyed or not.
     */
    boolean shouldDestroy() {
        if (willExpireFlag == false) {
            return false;
        }

        if (!isTimedOut()) {
            if (sessionState == Session.INVALID) {
                if (checkInvalidSessionDefaultIdleTime()) {
                    setState(Session.DESTROYED);
                    ss.sendEvent(this, SessionEvent.DESTROY);
                    return true;
                } else {
                    return false;
                }
            }

            if (getTimeLeft() == 0) {
                changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                return false;
            }

            if (getIdleTime() >= maxIdleTime * 60
                    && sessionState != Session.INACTIVE) {
                changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                return false;
            }
            return false;
        } else {
            // do something special for the timed out sessions
            if (getTimeLeftBeforePurge() <= 0) {
                // destroy the session
                SessionService.getSessionService().logEvent(this,
                        SessionEvent.DESTROY);
                setState(Session.DESTROYED);
                SessionService.getSessionService().sendEvent(this,
                        SessionEvent.DESTROY);
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Changes the state of the session and sends Session Notification when
     * session times out.
     */
    private void changeStateAndNotify(int eventType) {
        SessionService.getSessionService().logEvent(this, eventType);
        timedOutAt = System.currentTimeMillis()/1000;
        putProperty("SessionTimedOut", String.valueOf(timedOutAt));
        SessionService.execSessionTimeoutHandlers(sessionID, eventType);
        if(purgeDelay == 0) {
            ss.destroyInternalSession(sessionID);
            return;
        }
        if (!isAppSession() || SessionService.returnAppSession) {
            SessionService.decrementActiveSessions();
        }
        SessionCount.decrementSessionCount(this);  
        setState(Session.INVALID);
        if(SessionService.getSessionService().isSessionTrimmingEnabled()){
            trimSession();
        }        
        SessionService.getSessionService().sendEvent(this, eventType);
    }

    /**
     * Transfers the info about the Internal Session to Session Info.
     * @return SessionInfo
     */
    public SessionInfo toSessionInfo() {
        SessionInfo info = new SessionInfo();
        info.sid = sessionID.toString();
        if (sessionType == Session.USER_SESSION) {
            info.stype = "user";
        } else if (sessionType == Session.APPLICATION_SESSION) {
            info.stype = "application";
        }
        info.cid = clientID;
        info.cdomain = clientDomain;
        info.maxtime = Long.toString(maxSessionTime);
        info.maxidle = Long.toString(maxIdleTime);
        info.maxcaching = Long.toString(maxCachingTime);
        if (willExpireFlag == true) {
            info.timeidle = Long.toString(getIdleTime());
            info.timeleft = Long.toString(getTimeLeft());
        } else {
            // Sessions such as authentication session will never be destroyed
            info.timeidle = Long.toString(0);
            info.timeleft = Long.toString(Long.MAX_VALUE / 60);
        }

        if (sessionState == Session.INVALID) {
            info.state = "invalid";
        } else if (sessionState == Session.VALID) {
            info.state = "valid";
        } else if (sessionState == Session.INACTIVE) {
            info.state = "inactive";
        } else if (sessionState == Session.DESTROYED) {
            info.state = "destroyed";
        }

        info.properties = (Properties) sessionProperties.clone();
        return info;
    }

    /**
     * Sets the last time the client sent a request associated with this
     * session, as the number of seconds since midnight January 1, 1970 GMT.
     */
    void setLatestAccessTime() {
        long oldLatestAccessTime = latestAccessTime; 
        latestAccessTime = System.currentTimeMillis() / 1000;
        if ((latestAccessTime - oldLatestAccessTime) > interval) {
            updateForFailover();
        }    
    }

    /**
     * Sets the state of the Internal Session.
     * 
     * @param state
     */
    void setState(int state) {
        sessionState = state;
        updateForFailover();
    }

    /**
     * Returns the URL of the Session events and the associated master and
     * restricted token ids
     * @return Map of session event URLs
     */
    Map<String, Set<SessionID>> getSessionEventURLs() {
        return sessionEventURLs;
    }

    /**
     * Returns the value of willExpireFlag.
     * 
     */
    boolean willExpire() {
        return willExpireFlag;
    }

    /** 
     * Determine whether it is an application session
     */
    boolean isAppSession() {
        return (sessionType == Session.APPLICATION_SESSION || !willExpireFlag);
    }

    /**
     * Sets the creation time of the Internal Ssession, as the number of seconds
     * since midnight January 1, 1970 GMT.
     */
    public void setCreationTime() {
        if (httpSession != null) {
            creationTime = httpSession.getCreationTime() / 1000;
        } else {
            creationTime = System.currentTimeMillis() / 1000;
        }
    }

    /**
     * add new restricted token pointing at the same session to the list
     */
    void addRestrictedToken(SessionID sid, TokenRestriction restriction) {
        restrictedTokensBySid.put(sid, restriction);
        restrictedTokensByRestriction.put(restriction, sid);
        updateForFailover();
    }

    TokenRestriction getRestrictionForToken(SessionID sid) {
        return (TokenRestriction) restrictedTokensBySid.get(sid);
    }

    SessionID getRestrictedTokenForRestriction(TokenRestriction restriction) {
        return (SessionID) restrictedTokensByRestriction.get(restriction);
    }

    Object[] getRestrictedTokens() {
        return restrictedTokensBySid.keySet().toArray();
    }

    /**
     * Encodes the url by adding the cookiename=sid to it. if cookie support is
     * true returns without encoding
     * @param res HttpServletResponse
     * @param url url to be encoded
     * @return encoded URL
     */
    public String encodeURL(HttpServletResponse res, String url) {
        return encodeURL(res, url, Session.getCookieName());
    }

    /**
     * Encodes the url by adding the cookiename=sid to it.
     * if cookie support is true returns without encoding
     * @param res HttpServletResponse 
     * @param url url to be encoded
     * @param cookieName
     * @return url
     */
    public String encodeURL(HttpServletResponse res, String url,
            String cookieName) {
        return encodeURL(url, SessionUtils.QUERY, true, cookieName);
    }

    /**
     * Encodes the url by adding the cookiename=sid to it. if cookie support is
     * true returns without encoding
     * 
     * <p>
     * The cookie Value is written in the URL based on the encodingScheme
     * specified. The Cookie Value could be written as path info separated by
     * either a "/" OR ";" or as a query string.
     * 
     * <p>
     * If the encoding scheme is SLASH then the cookie value would be written in
     * the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
     *   queryString
     *</pre>
     * <p>
     * Note that this format works only if the path is a servlet, if a a jsp
     * file is specified then webcontainers return with "File Not found" error.
     * To rewrite links which are JSP files with cookie value use the SEMICOLON
     * OR QUERY encoding scheme.
     * 
     * <p>
     * If the encoding scheme is SEMICOLON then the cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/path;&lt;cookieName=cookieValue>?queryString
     * </pre>
     * Note that this is not supported in the servlet specification and some web
     * containers do not support this.
     * 
     * <p>
     * If the encoding scheme is QUERY then the cookie value would be written in
     * the URL in the following format:
     * <pre>
     * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
     * protocol://server:port/path?queryString&&lt;cookieName>=&lt;cookieValue>
     * </pre>
     * <p>
     * This is the default and OpenSSO always encodes in this format
     * unless otherwise specified. If the URL passed in has query parameter then
     * entity escaping of ampersand will be done before appending the cookie if
     * the escape is true. Only the ampersand before appending cookie parameter
     * will be entity escaped.
     * <p>
     * 
     * @param url the URL to be encoded.
     * @param encodingScheme possible values are <code>QUERY</code>,
     *        <code>SLASH</code>, <code>SEMICOLON</code>.
     * @param escape entity escaping of ampersand when appending the SSOToken 
     *        ID to request query string.
     * @return encoded URL with cookie value (session ID) based on the encoding
     *         scheme or the url itself if there is an error.
     */
    public String encodeURL(String url, short encodingScheme, boolean escape) {
        return encodeURL(url, encodingScheme, escape, Session.getCookieName());
    }

    
    /**
     * Encodes the url by adding the cookiename=sid to it.
     * if cookie support is true returns without encoding
     *
     * <p>
     * The cookie Value is written in the URL based on the encodingScheme
     * specified. The Cookie Value could be written as path info separated
     * by either a "/" OR  ";" or as a query string.
     *
     * <p>
     * If the encoding scheme is SLASH then the  cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
     *       queryString     
     * </pre>
     * <p>
     * Note that this format works only if the path is a servlet, if a
     * a jsp file is specified then webcontainers return with
     * "File Not found" error. To rewrite links which are JSP files with
     * cookie value use the SEMICOLON OR QUERY encoding scheme.     
     *
     * <p>
     * If the encoding scheme is SEMICOLON then the cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/path;&lt;cookieName=cookieValue>?queryString
     * </pre>
     * Note that this is not supported in the servlet specification and
     * some web containers do not support this.
     *
     * <p>
     * If the encoding scheme is QUERY then the cookie value would be
     * written in the URL in the following format:
     * <pre>
     * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
     * protocol://server:port/path?queryString&&lt;cookieName>=&lt;cookieValue>
     * </pre>
     * <p>
     * This is the default and OpenSSO always encodes in this format 
     * unless otherwise specified. If the URL passed in has query parameter then
     * entity escaping of ampersand will be done before appending the cookie
     * if the escape is true.  Only the ampersand before appending 
     * cookie parameter
     * will be entity escaped.
     * <p>
     * @param url the url to be encoded
     * @param encodingScheme possible values are QUERY,SLASH,SEMICOLON
     * @param escape entity escaping of ampersand when appending the
     *        SSOToken ID to request query string.
     * @param cookieName 
     * @return encoded URL with cookie value (session id) based
     *         on the encoding scheme or the url itself if there is an error.
     */
    public String encodeURL(String url, short encodingScheme, boolean escape,
            String cookieName) {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Session: url: " + url);
        }
        String encodedURL = url;

        if (((url != null) && (url.length() > 0)) && !getCookieSupport()) {
            if ((cookieStr != null && cookieStr.length() != 0)
                    && (Session.foundCookieName(cookieStr, cookieName))) {
                encodedURL = SessionEncodeURL.buildCookieString(url, cookieStr,
                        encodingScheme, escape);
            } else { // cookie str not set so call encodeURL
                if (sessionID != null) {
                    cookieStr = SessionEncodeURL.createCookieString(cookieName,
                            sessionID.toString());
                    encodedURL = SessionEncodeURL.encodeURL(cookieStr, url,
                            encodingScheme, escape);
                }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Returning encoded "
                    + "Session: url: " + encodedURL);
        }

        return encodedURL;
    }

    /**
     * Returns true if cookies are supported.
     * 
     * @return true if cookie supported;
     */

    private boolean getCookieSupport() {
        boolean cookieSupport = false;
        try {
            if (sessionID.getCookieMode() != null) {
                cookieSupport = sessionID.getCookieMode().booleanValue();
            } else if (this.cookieMode != null) {
                cookieSupport = this.cookieMode.booleanValue();
            }
        } catch (Exception ex) {
            DEBUG.error(
                    "Error getting cookieSupport value: ", ex);
            cookieSupport = true;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("InternalSession: getCookieSupport: "
                            + cookieSupport);
        }
        return cookieSupport;
    }

    /**
     * Sets the HttpSession into the Internal Session.
     * 
     * @param hSession
     */
    void setHttpSession(HttpSession hSession) {
        httpSession = hSession;
    }

    /**
     * Returns the HttpSession stored in the Internal Session.
     */
    HttpSession getHttpSession() {
        return (httpSession);
    }

    /**
     * Update for the session failover
     *
     */
    protected void updateForFailover() {
        if (SessionService.getSessionService().isSessionFailoverEnabled()
                && isISStored) {
            if (sessionState != Session.VALID) {
                SessionService.getSessionService().deleteFromRepository(
                        sessionID);
                isISStored = false;
            } else {
                SessionService.getSessionService().saveForFailover(this);
            }
        }

    }

    /**
     * Incase of session timeout the session is trimmed to reduce the memory
     * overhead. Even if the session lives in the server for the extra time out
     * period, the memory is not abused. Instance variables preserved are, 1)
     * sessionID 2) timedOutAt 3) clientID 4) purgeDelay 5)
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
        DEBUG.message("CookieMode is:" + cookieMode);
        if (cookieMode != null) {
            this.cookieMode = cookieMode;
        }
    }

    void setSessionHandle(String sessionHandle) {
        this.sessionHandle = sessionHandle;
        putProperty(Session.SESSION_HANDLE_PROP, sessionHandle);
        updateForFailover();
    }

    String getSessionHandle() {
        return sessionHandle;
    }

    /**
     * Sets logical version timestamp value
     * 
     * @param version
     *            version value
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Returns logical version timestamp value
     * 
     * @return version value
     */
    public long getVersion() {
        return version;
    }

     /**
      * Computes session object expiration time as smallest of idle limit or 
      * session maximum lifetime limit adjusted by purgeDelay
      * Time value is in seconds and uses the same epoch start as 
      * System.currentTimeMillis()
      * @return session expiration time
      */
    public long getExpirationTime() {
        long timeLeft = Math.max(0L, getMaxIdleTime() * 60 - getIdleTime());

        if (timeLeft == 0) {
            timeLeft = getTimeLeftBeforePurge();
        }
        return System.currentTimeMillis() / 1000
                + Math.min(getTimeLeft(), timeLeft);
    }
    
    /**
     * Correctly read and reschedule this session when it is read.
     */
    private void readObject(ObjectInputStream oin) throws IOException,
        ClassNotFoundException {
        oin.defaultReadObject();
        if (willExpireFlag) {
            timerPool = SystemTimerPool.getTimerPool();   
            if (!isTimedOut()) {
                if (sessionState == Session.INVALID) {
                    long expectedTime = creationTime +
                        (maxDefaultIdleTime * 60);
                    if (expectedTime > (System.currentTimeMillis() / 1000)) {
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date(expectedTime *
                                1000));
                        }
                    } else {
                        setState(Session.DESTROYED);
                        ss.removeInternalSession(sessionID);
                        ss.sendEvent(this, SessionEvent.DESTROY);
                    }
                } else {
                    long timeLeft = getTimeLeft();
                    if (timeLeft == 0) {
                        changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date((timedOutAt +
                                (purgeDelay * 60)) * 1000));
                        }
                    } else {
                        long idleTimeLeft = (maxIdleTime * 60) - getIdleTime();
                        if (idleTimeLeft <= 0 && 
                            sessionState != Session.INACTIVE) {
                            changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                            if (timerPool != null) {
                                timerPool.schedule(this, new Date((timedOutAt +
                                    (purgeDelay * 60)) * 1000));
                            }
                        } else {
                            long timeToWait = Math.min(timeLeft, idleTimeLeft);
                            if (timerPool != null) {
                                timerPool.schedule(this, new Date(((
                                    System.currentTimeMillis() / 1000) +
                                    timeToWait) * 1000));
                            }
                        }
                    }        
                }
            } else {
                long expectedTime = timedOutAt + purgeDelay * 60;
                if (expectedTime > (System.currentTimeMillis() / 1000)) {
                    if (timerPool != null) {
                        timerPool.schedule(this, new Date(expectedTime *
                            1000));
                    }
                } else {
                    ss.logEvent(this, SessionEvent.DESTROY);
                    setState(Session.DESTROYED);
                    ss.removeInternalSession(sessionID);
                    ss.sendEvent(this, SessionEvent.DESTROY);
                }
            }
        }
    }
}
