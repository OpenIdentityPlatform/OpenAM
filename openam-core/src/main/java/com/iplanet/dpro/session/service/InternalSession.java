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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.HeadTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.openam.utils.Time.*;

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
    private transient Debug debug;

    // Session Service and its collaborators should not be serialised.
    private transient SessionService sessionService;
    private transient SessionServiceConfig serviceConfig;
    private transient SessionLogging sessionLogging;
    private transient SessionAuditor sessionAuditor;

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
    private transient boolean isSessionUpgrade = false;

    static private SessionCookies sessionCookies;

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

    private final ConcurrentMap<SessionID, TokenRestriction> restrictedTokensBySid =
            new ConcurrentHashMap<SessionID, TokenRestriction>();

    private transient final ConcurrentMap<TokenRestriction, SessionID> restrictedTokensByRestriction =
            new ConcurrentHashMap<TokenRestriction, SessionID>();

    private static String superUserDN;

    private static boolean isEnableHostLookUp = Boolean.valueOf(
            SystemProperties.get(Constants.ENABLE_HOST_LOOKUP)).booleanValue();

    /*
     * This is the Time(seconds) when the session timedout. Value zero means the
     * session has not timed out.
     */
    private volatile long timedOutAt = 0;

    /*
     * This is the maximum extra time for which the timed out sessions lives in
     * the session server
     */
    private static long purgeDelay;

    /**
     * Session handle is used to prevent administrator from impersonating other users.
     */
    @JsonIgnore
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

    /* AM Internal session maximum session time */
    private static final String AM_MAX_SESSION_TIME = "AMMaxSessionTime";

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
    protected static final Set<String> protectedProperties;

    static {
        protectedProperties = new HashSet<String>();
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

    }

    /*
     * The URL map for session events of THIS session only : SESSION_CREATION,
     * IDLE_TIMEOUT, MAX_TIMEOUT, LOGOUT, REACTIVATION, DESTROY. Each URL in the
     * map is associated with a set of token ids (master and potentially all of
     * the restricted token ids associated with the master) that will be used in
     * notification
     */
    private final ConcurrentMap<String, Set<SessionID>> sessionEventURLs =
            new ConcurrentHashMap<String, Set<SessionID>>();

    /**
     * Creates an instance of the Internal Session with its key dependences exposed.
     *
     * Note: This InternalSession will be in an invalid state.
     *
     * @param sid Non null Session ID.
     * @param service Non null SessionService.
     * @param debug Debugging instance to use for all logging.
     */
    public InternalSession(SessionID sid,
                           SessionService service,
                           SessionServiceConfig serviceConfig,
                           SessionLogging sessionLogging,
                           SessionAuditor sessionAuditor,
                           SessionCookies sessionCookies,
                           Debug debug) {
        sessionID = sid;
        setSessionServiceDependencies(service, serviceConfig, sessionLogging, sessionAuditor, sessionCookies, debug);

        maxIdleTime = maxDefaultIdleTime;
        maxSessionTime = maxDefaultIdleTime;
        reschedulePossible = maxDefaultIdleTime > maxIdleTime;
        sessionState = INVALID;
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
        this(sid,
                InjectorHolder.getInstance(SessionService.class),
                InjectorHolder.getInstance(SessionServiceConfig.class),
                InjectorHolder.getInstance(SessionLogging.class),
                InjectorHolder.getInstance(SessionAuditor.class),
                InjectorHolder.getInstance(SessionCookies.class),
                InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG))));
    }

    /**
     * Default constructor required for deserialisation.
     *
     * This constructor is intentionally blank. When deserialised the code responsible for
     * instantiating it will have no means of resolving dependencies.
     *
     * Instead this is deferred to
     * {@link com.iplanet.dpro.session.service.InternalSession#setSessionServiceDependencies(
     * SessionService, SessionServiceConfig, SessionLogging, SessionAuditor,
     * org.forgerock.openam.session.SessionCookies, com.sun.identity.shared.debug.Debug)}
     */
    public InternalSession() {}

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
                                              SessionCookies sessionCookies, Debug debug) {

        this.sessionService = service;
        this.serviceConfig = serviceConfig;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
        this.sessionCookies = sessionCookies;
        this.debug = debug;
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
            if (isInvalid()) {
                removeSession();
            } else {
                long timeLeft = getTimeLeft();
                if (timeLeft == 0) {
                    changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                    sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                    if (timerPool != null) {
                        if (purgeDelay > 0) {
                            timerPool.schedule(this, new Date((timedOutAt +
                                (purgeDelay * 60)) * 1000));
                        }
                    }
                } else {
                    long idleTimeLeft = (maxIdleTime * 60) - getIdleTime();
                    if (idleTimeLeft <= 0 && sessionState != INACTIVE) {
                        changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                        sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
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
                                    currentTimeMillis() / 1000) +
                                timeToWait) * 1000));
                        }
                    }
                }
            }
        } else {
            removeSession();
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
                case INVALID:
                    timeoutTime = (creationTime +
                        (maxDefaultIdleTime * 60)) * 1000;
                    break;
                case VALID:
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
        long now = currentTimeMillis() / 1000;
        return now - latestAccessTime;
    }

    /**
     * Returns the total time left(in seconds) for the Internal Session.
     * @return Time left for the internal session to be invalid
     */
    public long getTimeLeft() {
        long now = currentTimeMillis() / 1000;
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
        long now = currentTimeMillis() / 1000;
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
     * Sets the time at which this session timed out due to idle/max timeout. The time is in seconds since the same
     * epoch as {@link System#currentTimeMillis()}. A value of 0 indicates that the session has not timed out.
     *
     * @param timedOutAt the time in seconds at which the session timed out.
     */
    @VisibleForTesting
    void setTimedOutAt(long timedOutAt) {
        this.timedOutAt = timedOutAt;
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

        if (sessionState == VALID && serviceConfig.isSendPropertyNotification(key)) {
            sessionService.sendEvent(this, SessionEvent.PROPERTY_CHANGED);
            SessionInfo sessionInfo = toSessionInfo();
            sessionLogging.logEvent(sessionInfo, SessionEvent.PROPERTY_CHANGED);
            sessionAuditor.auditActivity(sessionInfo, AM_SESSION_PROPERTY_CHANGED);
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
     * (1) the login user is the super user (not including users assigned the top level admin role), or
     * (2) the token is an application token (e.g. Agent)
     */
    private boolean shouldIgnoreSessionQuotaChecking(String userDN) {
        boolean ignore = false;
        if (sessionService.isSuperUser(getUUID()) || (isAppSession())) {
            ignore = true;
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
        // throws SessionConstraintException {

        if (userDN == null) {
            return false;
        }
        // Exceeded max active sessions, but allow if the user is super-admin
        if ((sessionService.hasExceededMaxSessions()) && (!userDN.equalsIgnoreCase(superUserDN))) {
            sessionLogging.logSystemMessage(LOG_MSG_SESSION_MAX_LIMIT_REACHED, java.util.logging.Level.INFO);
            return false;
        }

        SessionInfo sessionInfo = toSessionInfo();

        // checking Session Quota Constraints
        if ((serviceConfig.isSessionConstraintEnabled()) && !shouldIgnoreSessionQuotaChecking(userDN)) {

            if (SessionConstraint.checkQuotaAndPerformAction(this)) {
                if (debug.messageEnabled()) {
                    debug.message("Session Quota exhausted!");
                }
                sessionLogging.logEvent(sessionInfo, SessionEvent.QUOTA_EXHAUSTED);
                return false;
            }
        }
        setLatestAccessTime();
        setState(VALID);
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
        cancel();
        setCreationTime();
        setLatestAccessTime();
        setState(VALID);
        reschedule();
        SessionInfo sessionInfo = toSessionInfo();
        sessionLogging.logEvent(sessionInfo, SessionEvent.REACTIVATION);
        sessionAuditor.auditActivity(sessionInfo, AM_SESSION_REACTIVATED);
        sessionService.sendEvent(this, SessionEvent.REACTIVATION);
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
            maxCachingTime = serviceConfig.getApplicationMaxCachingTime();
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
        long now = currentTimeMillis() / 1000;
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
        SessionInfo sessionInfo = toSessionInfo();

        if (!isTimedOut()) {
            if (isInvalid()) {
                if (checkInvalidSessionDefaultIdleTime()) {
                    setState(DESTROYED);
                    sessionService.sendEvent(this, SessionEvent.DESTROY);
                    return true;
                } else {
                    return false;
                }
            }

            if (getTimeLeft() == 0) {
                changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_MAX_TIMED_OUT);
                return false;
            }

            if (getIdleTime() >= maxIdleTime * 60
                    && sessionState != INACTIVE) {
                changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_IDLE_TIMED_OUT);
                return false;
            }
            return false;
        } else {
            // do something special for the timed out sessions
            if (getTimeLeftBeforePurge() <= 0) {
                // destroy the session
                sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
                setState(DESTROYED);
                sessionService.sendEvent(this, SessionEvent.DESTROY);
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
        sessionLogging.logEvent(toSessionInfo(), eventType);
        setTimedOutAt(TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
        putProperty("SessionTimedOut", String.valueOf(timedOutAt));
        sessionService.execSessionTimeoutHandlers(sessionID, eventType);
        if(purgeDelay == 0) {
            sessionService.destroyInternalSession(sessionID);
            return;
        }
        if (!isAppSession() || serviceConfig.isReturnAppSessionEnabled()) {
            sessionService.decrementActiveSessions();
        }
        SessionCount.decrementSessionCount(this);
        setState(INVALID);
        if (serviceConfig.isSessionTrimmingEnabled()){
            trimSession();
        }
        sessionService.sendEvent(this, eventType);
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
        if (willExpireFlag == true) {
            info.setTimeIdle(getIdleTime());
            info.setTimeLeft(getTimeLeft());
        } else {
            // Sessions such as authentication session will never be destroyed
            info.setNeverExpiring(true);
        }

        if (isInvalid()) {
            info.setState("invalid");
        } else if (sessionState == VALID) {
            info.setState("valid");
        } else if (sessionState == INACTIVE) {
            info.setState("inactive");
        } else if (sessionState == DESTROYED) {
            info.setState("destroyed");
        }

        info.setProperties((Hashtable<String, String>) sessionProperties.clone());
        if (withIds) {
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
        long oldLatestAccessTime = latestAccessTime;
        latestAccessTime = currentTimeMillis() / 1000;
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
     * restricted token ids.
     * @return Map of session event URLs and their associated SessionIDs.
     */
    Map<String, Set<SessionID>> getSessionEventURLs(int eventType, SessionBroadcastMode logoutDestroyBroadcast) {
        Map<String, Set<SessionID>> urls = new HashMap<String, Set<SessionID>>();

        if ((eventType == SessionEvent.DESTROY || eventType == SessionEvent.LOGOUT) &&
                logoutDestroyBroadcast != SessionBroadcastMode.OFF) {
            try {
                String localServer = WebtopNaming.getLocalServer();
                Collection<String> servers;
                if (logoutDestroyBroadcast == SessionBroadcastMode.ALL_SITES) {
                    servers = WebtopNaming.getPlatformServerList();
                } else {
                    servers = new ArrayList<String>();
                    for (String serverID : WebtopNaming.getSiteNodes(WebtopNaming.getAMServerID())) {
                        servers.add(WebtopNaming.getServerFromID(serverID));
                    }
                }
                for (String url : servers) {
                    if (!localServer.equals(url)) {
                        urls.put(url + "/notificationservice", new HashSet<SessionID>(Arrays.asList(this.getID())));
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
    void addSessionEventURL(String url, SessionID sid) {

        Set<SessionID> sids = sessionEventURLs.get(url);
        if (sids == null) {
            sids = Collections.newSetFromMap(new ConcurrentHashMap<SessionID, Boolean>());
            Set<SessionID> previousValue = sessionEventURLs.putIfAbsent(url, sids);
            if (previousValue != null) {
                sids = previousValue;
            }
        }

        if (sids.add(sid))  {
            updateForFailover();
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
    boolean willExpire() {
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
     * Sets the creation time of the Internal Ssession, as the number of seconds
     * since midnight January 1, 1970 GMT.
     */
    public void setCreationTime() {
        if (httpSession != null) {
            creationTime = httpSession.getCreationTime() / 1000;
        } else {
            creationTime = currentTimeMillis() / 1000;
        }
    }

    /**
     * Add new restricted token pointing at the same session to the list.
     *
     * @param sid The session ID.
     * @param restriction The token restriction.
     * @return The existing session ID instance if this TokenRestriction was already mapped to a session ID,
     * <code>null</code> otherwise.
     */
    SessionID addRestrictedToken(SessionID sid, TokenRestriction restriction) {
        SessionID previousValue = restrictedTokensByRestriction.putIfAbsent(restriction, sid);
        if (previousValue == null) {
            restrictedTokensBySid.put(sid, restriction);
            updateForFailover();
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

    SessionID getRestrictedTokenForRestriction(TokenRestriction restriction) {
        return restrictedTokensByRestriction.get(restriction);
    }

    Set<SessionID> getRestrictedTokens() {
        return new HashSet<SessionID>(restrictedTokensBySid.keySet());
    }

    /**
     * Encodes the url by adding the cookiename=sid to it. if cookie support is
     * true returns without encoding
     * @param res HttpServletResponse
     * @param url url to be encoded
     * @return encoded URL
     */
    public String encodeURL(HttpServletResponse res, String url) {
        return encodeURL(res, url, sessionCookies.getCookieName());
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
     * This is the default and OpenAM always encodes in this format
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
        return encodeURL(url, encodingScheme, escape, sessionCookies.getCookieName());
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
     * This is the default and OpenAM always encodes in this format
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

        if (debug.messageEnabled()) {
            debug.message("Session: url: " + url);
        }
        String encodedURL = url;

        if (((url != null) && (url.length() > 0)) && !getCookieSupport()) {
            if ((cookieStr != null && cookieStr.length() != 0) && (sessionCookies.containsCookie(cookieStr, cookieName))) {
                encodedURL = SessionEncodeURL.buildCookieString(url, cookieStr, encodingScheme, escape);
            } else { // cookie str not set so call encodeURL
                if (sessionID != null) {
                    cookieStr = SessionEncodeURL.createCookieString(cookieName, sessionID.toString());
                    encodedURL = SessionEncodeURL.encodeURL(cookieStr, url, encodingScheme, escape);
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("Returning encoded Session: url: " + encodedURL);
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
            debug.error(
                    "Error getting cookieSupport value: ", ex);
            cookieSupport = true;
        }
        if (debug.messageEnabled()) {
            debug.message("InternalSession: getCookieSupport: " + cookieSupport);
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
     */
    private void updateForFailover() {
        if (serviceConfig.isSessionFailoverEnabled() && isISStored) {
            if (sessionState != VALID) {
                sessionService.deleteFromRepository(sessionID);
                isISStored = false;
            } else if (!isTimedOut() || purgeDelay > 0) {
                // Only save for failover if we are not about to delete the session anyway.
                sessionService.saveForFailover(this);
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
      * Time value is in seconds and uses the same epoch start as {@link System#currentTimeMillis()}
      * @return session expiration time in seconds.
      * @see #getExpirationTime(TimeUnit)
      */
    public long getExpirationTime() {
        return getExpirationTime(TimeUnit.SECONDS);
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
        long timeLeftSeconds = Math.max(0L, TimeUnit.MINUTES.toSeconds(getMaxIdleTime()) - getIdleTime());

        if (timeLeftSeconds == 0) {
            timeLeftSeconds = getTimeLeftBeforePurge();
        }

        return timeUnit.convert(currentTimeMillis(), MILLISECONDS)
                + Math.min(timeUnit.convert(getTimeLeft(), TimeUnit.SECONDS),
                           timeUnit.convert(timeLeftSeconds, TimeUnit.SECONDS));
    }

    /**
     * Correctly read and reschedule this session when it is read.
     */
    public void scheduleExpiry() {
        if (willExpireFlag) {
            timerPool = SystemTimerPool.getTimerPool();
            if (!isTimedOut()) {
                if (isInvalid()) {
                    long expectedTime = creationTime +
                        (maxDefaultIdleTime * 60);
                    if (expectedTime > (currentTimeMillis() / 1000)) {
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date(expectedTime *
                                1000));
                        }
                    } else {
                        removeSession();
                    }
                } else {
                    long timeLeft = getTimeLeft();
                    if (timeLeft == 0) {
                        changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                        sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                        if (timerPool != null) {
                            timerPool.schedule(this, new Date((timedOutAt +
                                (purgeDelay * 60)) * 1000));
                        }
                    } else {
                        long idleTimeLeft = (maxIdleTime * 60) - getIdleTime();
                        if (idleTimeLeft <= 0 &&
                            sessionState != INACTIVE) {
                            changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                            sessionAuditor.auditActivity(toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
                            if (timerPool != null) {
                                timerPool.schedule(this, new Date((timedOutAt +
                                    (purgeDelay * 60)) * 1000));
                            }
                        } else {
                            long timeToWait = Math.min(timeLeft, idleTimeLeft);
                            if (timerPool != null) {
                                timerPool.schedule(this, new Date(((
                                        currentTimeMillis() / 1000) +
                                    timeToWait) * 1000));
                            }
                        }
                    }
                }
            } else {
                long expectedTime = timedOutAt + purgeDelay * 60;
                if (expectedTime > (currentTimeMillis() / 1000)) {
                    if (timerPool != null) {
                        timerPool.schedule(this, new Date(expectedTime *
                            1000));
                    }
                } else {
                    removeSession();
                }
            }
        }
    }

    /**
     * @return True if the Session has reached an invalid state.
     */
    public boolean isInvalid() {
        return sessionState == INVALID;
    }

    /**
     * Signals the Session for removal.
     */
    private void removeSession() {
        SessionInfo sessionInfo = toSessionInfo();
        sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
        sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
        setState(DESTROYED);
        sessionService.removeInternalSession(sessionID);
        sessionService.sendEvent(this, SessionEvent.DESTROY);
    }

    @VisibleForTesting
    static void setPurgeDelay(long newPurgeDelay) {
        purgeDelay = newPurgeDelay;
    }

}
