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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2016 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2023 Open Identity Platform Community.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.service;

import static com.iplanet.dpro.session.service.SessionConstants.*;
import static com.sun.identity.shared.Constants.*;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.openam.sso.providers.stateless.JwtSessionMapper;
import org.forgerock.openam.sso.providers.stateless.JwtSessionMapperConfig;
import org.forgerock.openam.utils.ConfigListener;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.cluster.ClusterStateService;
import com.iplanet.services.naming.ServiceListeners;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Responsible for collating System Properties and amSession.xml configuration state relating to the Session Service.
 *
 * @since 13.0.0
 */
@Singleton
public class SessionServiceConfig {

    public static final String AM_SESSION_SERVICE_NAME = "iPlanetAMSessionService";

    private final Debug sessionDebug;

    /*
     * Constant Properties
     */

    private final String HTTP_SESSION_PROPERTY_NAME = "DSAMEInternalSession";
    private final String HTTP_SESSION_OWNER_LIST_PROPERTY_NAME = "DSAMEInternalSession.ownerList";
    private final String SECURITY_COOKIE_NAME = "DSAMESecurityCookie";

    /*
     * System Properties
     */

    private static final int DEFAULT_MAX_SESSION_CACHE_SIZE = 64000;
    
    private static final long DEFAULT_MAX_SESSION_CACHE_TIME = 60;

    private static final String LOGSTATUS_ACTIVE = "ACTIVE";
    private final boolean logStatus;

    private static final String DEFAULT_HTTP_SESSION_TRACKING_COOKIE_NAME = "JSESSIONID";
    private final String httpSessionTrackingCookieName;

    private static final boolean DEFAULT_COOKIE_ENCODING = false;
    private final boolean cookieEncoding;

    private static final int DEFAULT_NOTIFICATION_THEAD_POOL_SIZE = 10;
    private final int notificationThreadPoolSize;

    private static final int DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD = DEFAULT_NOTIFICATION_THEAD_POOL_SIZE * 10;
    private final int notificationThreadPoolThreshold;

    private static final long DEFAULT_APPLICATION_MAX_CACHING_TIME = Long.MAX_VALUE / 60;
    private final long applicationMaxCachingTime;

    private static final boolean DEFAULT_RETURN_APP_SESSION = false;
    private final boolean returnAppSession;

    private final int sessionFailoverClusterStateCheckTimeout;
    private final long sessionFailoverClusterStateCheckPeriod;

    /*
     * amSession.xml (SMS) Properties
     */

    private volatile HotSwappableSessionServiceConfig hotSwappableSessionServiceConfig;
    private final CopyOnWriteArraySet<ConfigListener> listeners = new CopyOnWriteArraySet<>();

    /**
     * Private value object for storing snapshot state of amSession.xml config settings.
     *
     * This allows immutable value objects to be published as an atomic operation.
     */
    private class HotSwappableSessionServiceConfig {

        private static final long DEFAULT_SESSION_RETRIEVAL_TIMEOUT = 5;
        private static final int DEFAULT_MAX_SESSION_LIST_SIZE = 200;
        private static final int DEFAULT_MAX_WAIT_TIME_FOR_CONSTRAINT = 6000;

        private final JwtSessionMapperConfig jwtSessionMapperConfig;
        private final Set<String> timeoutHandlers;
        private final boolean sessionConstraintEnabled;
        private final boolean denyLoginIfDBIsDown;
        private final String constraintHandler;
        private final boolean propertyNotificationEnabled;
        private final Set notificationProperties;
        private final long sessionRetrievalTimeout; //  in seconds
        private final int maxSessionListSize;
        private final int maxWaitTimeForConstraint; // in milli-seconds

        private final boolean sessionBlacklistEnabled;
        private final int sessionBlacklistCacheSize;
        private final long sessionBlacklistPollIntervalSeconds;
        private final long sessionBlacklistPurgeDelayMinutes;


        private HotSwappableSessionServiceConfig(ServiceSchemaManager ssm) throws SMSException {
            ServiceSchema schema = ssm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();

            jwtSessionMapperConfig = new JwtSessionMapperConfig(attrs);
            sessionRetrievalTimeout = loadSessionRetrievalTimeoutSchemaSetting(attrs);
            maxSessionListSize = loadMaxSessionListSizeSchemaSetting(attrs);
            propertyNotificationEnabled = loadPropertyNotificationEnabledSchemaSetting(attrs);
            notificationProperties = loadPropertyNotificationPropertiesSchemaSetting(attrs);
            timeoutHandlers = loadTimeoutHandlersServiceSchemaSetting(attrs);
            sessionConstraintEnabled = loadSessionConstraintServiceSchemaSetting(attrs);
            denyLoginIfDBIsDown = loadDenyLoginIfDBIsDownServiceSchemaSetting(attrs);
            constraintHandler = loadConstraintHandlerServiceSchemaSetting(attrs);
            maxWaitTimeForConstraint = loadMaxWaitTimeForConstraintHandlerServiceSchemaSetting(attrs);

            sessionBlacklistEnabled = CollectionHelper.getBooleanMapAttr(attrs, SESSION_BLACKLIST_ENABLED_ATTR, false);
            sessionBlacklistCacheSize = CollectionHelper.getIntMapAttr(attrs, SESSION_BLACKLIST_CACHE_SIZE_ATTR, 0,
                    sessionDebug);
            sessionBlacklistPollIntervalSeconds = CollectionHelper.getLongMapAttr(attrs,
                    SESSION_BLACKLIST_POLL_INTERVAL_ATTR, 60, sessionDebug);
            sessionBlacklistPurgeDelayMinutes = CollectionHelper.getLongMapAttr(attrs,
                    SESSION_BLACKLIST_PURGE_DELAY_ATTR, 1, sessionDebug);
        }

        public boolean isSendPropertyNotification(String key) {
            return propertyNotificationEnabled && notificationProperties.contains(key);
        }

        private long loadSessionRetrievalTimeoutSchemaSetting(Map attrs) {
            long value = TimeUnit.SECONDS.toMillis(CollectionHelper.getLongMapAttr(
                    attrs, AM_SESSION_SESSION_LIST_RETRIEVAL_TIMEOUT, DEFAULT_SESSION_RETRIEVAL_TIMEOUT, sessionDebug));
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("sessionRetrievalTimeout=" + value);
            }
            return value;
        }

        private int loadMaxSessionListSizeSchemaSetting(Map attrs) {
            int value = CollectionHelper.getIntMapAttr(
                    attrs, AM_SESSION_MAX_SESSION_LIST_SIZE, DEFAULT_MAX_SESSION_LIST_SIZE, sessionDebug);
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("maxSessionListSize=" + value);
            }
            return value;
        }

        private boolean loadPropertyNotificationEnabledSchemaSetting(Map attrs) {
            boolean value = "ON".equalsIgnoreCase(
                    CollectionHelper.getMapAttr(attrs, Constants.PROPERTY_CHANGE_NOTIFICATION));
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("value=" + value);
            }
            return value;
        }

        private Set loadPropertyNotificationPropertiesSchemaSetting(Map attrs) {
            Set value = null;
            if (propertyNotificationEnabled) {
                value = (Set) attrs.get(Constants.NOTIFICATION_PROPERTY_LIST);
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("notificationProperties=" + value);
            }
            return value;
        }

        private Set<String> loadTimeoutHandlersServiceSchemaSetting(Map attrs) {
            Set<String> values = (Set<String>) attrs.get(Constants.TIMEOUT_HANDLER_LIST);
            if (values == null) {
                values = Collections.emptySet();
            } else {
                // Only copy non-empty String values to avoid triggering a ClassNotFoundException on empty values when
                // SessionService iterates over the list to call the handlers.
                Set<String> valuesCopy = new HashSet<String>(values.size());
                for (String value : values) {
                    if (!value.isEmpty()) {
                        valuesCopy.add(value);
                    }
                }
                values = valuesCopy;
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("timeoutHandlers=" + values);
            }
            return values;
        }

        private boolean loadSessionConstraintServiceSchemaSetting(Map attrs) {
            boolean value = "ON".equalsIgnoreCase(
                    CollectionHelper.getMapAttr(attrs, AM_SESSION_ENABLE_SESSION_CONSTRAINT));
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("sessionConstraintEnabled=" + value);
            }
            return value;
        }

        private boolean loadDenyLoginIfDBIsDownServiceSchemaSetting(Map attrs) {
            boolean value = "YES".equalsIgnoreCase(
                    CollectionHelper.getMapAttr(attrs, AM_SESSION_DENY_LOGIN_IF_DB_IS_DOWN));
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("denyLoginIfDBIsDown=" + value);
            }
            return value;
        }

        private String loadConstraintHandlerServiceSchemaSetting(Map attrs) {
            String value = CollectionHelper.getMapAttr(
                    attrs, AM_SESSION_CONSTRAINT_HANDLER, SessionConstraint.DESTROY_OLDEST_SESSION_CLASS);
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("Resulting behavior if session quota exhausted:" + value);
            }
            return value;
        }

        private int loadMaxWaitTimeForConstraintHandlerServiceSchemaSetting(Map attrs) {
            int value = CollectionHelper.getIntMapAttr(
                    attrs, AM_SESSION_CONSTRAINT_MAX_WAIT_TIME, DEFAULT_MAX_WAIT_TIME_FOR_CONSTRAINT, sessionDebug);
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("maxWaitTimeForConstraint=" + maxWaitTimeForConstraint);
            }
            return value;
        }
    }

    @Inject
    SessionServiceConfig(
            @Named(SessionConstants.SESSION_DEBUG) final Debug sessionDebug,
            PrivilegedAction<SSOToken> adminTokenProvider,
            final ServiceListeners serviceListeners) {

        this.sessionDebug = sessionDebug;

        // Initialize values set from System properties
        logStatus =
                LOGSTATUS_ACTIVE.equalsIgnoreCase(SystemProperties.get(AM_LOGSTATUS));
        httpSessionTrackingCookieName =
                SystemProperties.get(AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME, DEFAULT_HTTP_SESSION_TRACKING_COOKIE_NAME);
        cookieEncoding =
                SystemProperties.getAsBoolean(AM_COOKIE_ENCODE, DEFAULT_COOKIE_ENCODING);
        notificationThreadPoolSize =
                loadNotificationThreadPoolSizeSystemProperty();
        notificationThreadPoolThreshold =
                loadNotificationThreadPoolThresholdSystemProperty();
        applicationMaxCachingTime =
                SystemProperties.getAsLong(APPLICATION_SESSION_MAX_CACHING_TIME, DEFAULT_APPLICATION_MAX_CACHING_TIME);
        returnAppSession =
                SystemProperties.getAsBoolean(SESSION_RETURN_APP_SESSION, DEFAULT_RETURN_APP_SESSION);
        sessionFailoverClusterStateCheckTimeout =
                loadSessionFailoverClusterStateCheckTimeout();
        sessionFailoverClusterStateCheckPeriod =
                loadSessionFailoverClusterStateCheckPeriod();

        try {

            // Initialize settings from SMS

            final ServiceSchemaManager serviceSchemaManager =
                    new ServiceSchemaManager(AM_SESSION_SERVICE_NAME, AccessController.doPrivileged(adminTokenProvider));
            hotSwappableSessionServiceConfig = new HotSwappableSessionServiceConfig(serviceSchemaManager);

            ServiceListeners.Action action = new ServiceListeners.Action() {
                @Override
                public void performUpdate() {
                    try {
                        hotSwappableSessionServiceConfig = new HotSwappableSessionServiceConfig(serviceSchemaManager);
                        notifyListeners();
                    } catch (SMSException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            serviceListeners.config(AM_SESSION_SERVICE_NAME)
                    .global(action)
                    .schema(action).listen();
        } catch (Exception ex) {
            sessionDebug.error("SessionService: Initialization Failed", ex);
            // Rethrow exception rather than hobbling on with invalid configuration state
            throw new IllegalStateException("Failed to load SessionService configuration", ex);
        }
    }

    private int loadNotificationThreadPoolSizeSystemProperty() {
        try {
            return SystemProperties.getAsInt(NOTIFICATION_THREADPOOL_SIZE, DEFAULT_NOTIFICATION_THEAD_POOL_SIZE);
        } catch (NumberFormatException e) {
            sessionDebug.warning(
                    "Invalid value for " + NOTIFICATION_THREADPOOL_SIZE +
                            " defaulting to " + DEFAULT_NOTIFICATION_THEAD_POOL_SIZE);
            return DEFAULT_NOTIFICATION_THEAD_POOL_SIZE;
        }
    }

    private int loadNotificationThreadPoolThresholdSystemProperty() {
        try {
            return SystemProperties.getAsInt(NOTIFICATION_THREADPOOL_THRESHOLD,
                    DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD);
        } catch (NumberFormatException e) {
            sessionDebug.warning(
                    "Invalid value for " + NOTIFICATION_THREADPOOL_THRESHOLD +
                            " defaulting to " + DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD);
            return DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD;
        }
    }

    private int loadSessionFailoverClusterStateCheckTimeout() {
        try {
            return SystemProperties.getAsInt(AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT,
                    ClusterStateService.DEFAULT_TIMEOUT);
        } catch (Exception e) {
            sessionDebug.warning(
                    "Invalid value for " + Constants.AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT +
                            " defaulting to " + ClusterStateService.DEFAULT_TIMEOUT);
            return ClusterStateService.DEFAULT_TIMEOUT;
        }
    }

    private long loadSessionFailoverClusterStateCheckPeriod() {
        try {
            return SystemProperties.getAsLong(AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD,
                    ClusterStateService.DEFAULT_PERIOD);
        } catch (Exception e) {
            sessionDebug.warning(
                    "Invalid value for " + Constants.AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD +
                            " defaulting to " + ClusterStateService.DEFAULT_PERIOD);
            return ClusterStateService.DEFAULT_PERIOD;
        }
    }

    /**
     * Returns amSession.xml property "iplanet-am-session-constraint-handler".
     *
     * This should be the fully qualified name of a class implementing
     * {@link com.iplanet.dpro.session.service.QuotaExhaustionAction}.
     *
     * Defaults to {@link org.forgerock.openam.session.service.DestroyOldestAction}.
     *
     * @see com.iplanet.dpro.session.service.QuotaExhaustionAction
     * @see org.forgerock.openam.session.service.DestroyOldestAction
     */
    public String getConstraintHandler() {
        return hotSwappableSessionServiceConfig.constraintHandler;
    }

    /**
     * Returns true if amSession.xml property "iplanet-am-session-deny-login-if-db-is-down" is "YES" (case insensitive).
     *
     * Defaults to false.
     */
    public boolean isDenyLoginIfDBIsDown() {
        return hotSwappableSessionServiceConfig.denyLoginIfDBIsDown;
    }

    /**
     * Returns true if amSession.xml property "iplanet-am-session-enable-session-constraint" is "ON" (case insensitive).
     *
     * Defaults to false.
     */
    public boolean isSessionConstraintEnabled() {
        return hotSwappableSessionServiceConfig.sessionConstraintEnabled;
    }

    public String getHttpSessionPropertyName() {
        return HTTP_SESSION_PROPERTY_NAME;
    }

    public String getHttpSessionOwnerListPropertyName() {
        return HTTP_SESSION_OWNER_LIST_PROPERTY_NAME;
    }

    public String getSecurityCookieName() {
        return SECURITY_COOKIE_NAME;
    }

    /**
     * The maximum number of sessions to cache in the internal session cache.
     *
     * @return SystemProperty "org.forgerock.openam.session.service.access.persistence.caching.maxsize". Default 64000.
     */
    public int getMaxSessionCacheSize() {
        return SystemProperties.getAsInt(AM_SESSION_MAX_CACHE_SIZE, DEFAULT_MAX_SESSION_CACHE_SIZE);
    }
    
    /**
     * The maximum time of cache for internal session in the internal session cache.
     *
     * @return SystemProperty "org.openidentityplatform.openam.session.service.access.persistence.caching.maxtime". Default 60.
     */
    public long getMaxSessionCacheTime() {
        return SystemProperties.getAsLong(AM_SESSION_MAX_CACHE_TIME, DEFAULT_MAX_SESSION_CACHE_TIME);
    }

    /**
     * Returns true if SystemProperty "com.iplanet.am.logstatus" is "ACTIVE" (case insensitive).
     *
     * Defaults to false.
     */
    public boolean isLoggingEnabled() {
        return logStatus;
    }

    /**
     * Returns the name of the cookie/URL parameter used by J2EE container for
     * session tracking (currently hardcoded to "JSESSIONID")
     */
    public String getHttpSessionTrackingCookieName() {
        return httpSessionTrackingCookieName;
    }

    /**
     * Returns true if SystemProperty "com.iplanet.am.cookie.encode" is true.
     *
     * Defaults to false.
     */
    public boolean isCookieEncodingEnabled() {
        return cookieEncoding;
    }

    /**
     * Returns value of SystemProperty "com.iplanet.am.notification.threadpool.size".
     *
     * Defaults to 10 if not specified.
     */
    public int getNotificationThreadPoolSize() {
        return notificationThreadPoolSize;
    }

    /**
     * Returns value of SystemProperty "com.iplanet.am.notification.threadpool.threshold".
     *
     * Defaults to 100 if not specified.
     */
    public int getNotificationThreadPoolThreshold() {
        return notificationThreadPoolThreshold;
    }

    /**
     * Returns value of SystemProperty "com.sun.identity.session.returnAppSession".
     *
     * Defaults to false.
     */
    public boolean isReturnAppSessionEnabled() {
        return returnAppSession;
    }

    /**
     * Returns values of amSession.xml property "openam-session-timeout-handler-list".
     *
     * Each value should be the fully qualified name of a class implementing
     * {@link org.forgerock.openam.session.service.SessionTimeoutHandler}.
     *
     * @see org.forgerock.openam.session.service.SessionTimeoutHandler
     */
    public Set<String> getTimeoutHandlers() {
        return hotSwappableSessionServiceConfig.timeoutHandlers;
    }

    /**
     * Returns value of SystemProperty "com.sun.identity.session.application.maxCacheTime" (minutes).
     *
     * Defaults to Long.MAX_VALUE / 60 (i.e. essentially forever).
     */
    public long getApplicationMaxCachingTime() {
        return applicationMaxCachingTime;
    }

    /**
     * Returns value of amSession.xml property "iplanet-am-session-session-list-retrieval-timeout" (seconds).
     *
     * Defaults to 5.
     */
    public long getSessionRetrievalTimeout() {
        return hotSwappableSessionServiceConfig.sessionRetrievalTimeout;
    }

    /**
     * Returns value of amSession.xml property "iplanet-am-session-max-session-list-size".
     *
     * Defaults to 200.
     */
    public int getMaxSessionListSize() {
        return hotSwappableSessionServiceConfig.maxSessionListSize;
    }

    /**
     * Returns true if property change notifications are enabled for the specified property.
     *
     * Property change notifications are activated by setting the amSession.xml property
     * "iplanet-am-session-property-change-notification" to "ON" (case-insensitive); defaults to false.
     *
     * Properties for which notifications should be sent are then specified vis the amSession.xml property
     * "iplanet-am-session-notification-property-list"; no properties are selected by default.
     *
     * @param key Name of the property to check
     */
    public boolean isSendPropertyNotification(String key) {
        return hotSwappableSessionServiceConfig.isSendPropertyNotification(key);
    }

    /**
     * @return JwtSessionMapper configured according to hot-swappable SMS settings.
     */
    public JwtSessionMapper getJwtSessionMapper() {
        return hotSwappableSessionServiceConfig.jwtSessionMapperConfig.getJwtSessionMapper();
    }

    /**
     * Returns value of amSession.xml property "com.iplanet.am.session.failover.cluster.stateCheck.timeout" (milliseconds).
     *
     * Defaults to 1000.
     */
    public int getSessionFailoverClusterStateCheckTimeout() {
        return sessionFailoverClusterStateCheckTimeout;
    }

    /**
     * Returns value of amSession.xml property "com.iplanet.am.session.failover.cluster.stateCheck.period" (milliseconds).
     *
     * Defaults to 1000.
     */
    public long getSessionFailoverClusterStateCheckPeriod() {
        return sessionFailoverClusterStateCheckPeriod;
    }

    /**
     * Whether session blacklisting is enabled for stateless session logout.
     *
     * Defaults to false.
     */
    public boolean isSessionBlacklistingEnabled() {
        return hotSwappableSessionServiceConfig.sessionBlacklistEnabled;
    }

    /**
     * Maximum number of blacklisted sessions to cache in memory on each server. Beyond this number, sessions will be
     * evicted from memory (but kept in the CTS) in a least-recently used (LRU) strategy.
     *
     * Defaults to 10000.
     */
    public int getSessionBlacklistCacheSize() {
        return hotSwappableSessionServiceConfig.sessionBlacklistCacheSize;
    }

    /**
     * The interval at which to poll for changes to the session blacklist. May be 0 to indicate polling is disabled.
     *
     * @param unit the desired time unit for the poll interval.
     */
    public long getSessionBlacklistPollInterval(TimeUnit unit) {
        return unit.convert(hotSwappableSessionServiceConfig.sessionBlacklistPollIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Amount of time to keep sessions in the blacklist beyond their expiry time to account for clock skew.
     *
     * @param unit the desired time unit for the purge delay.
     */
    public long getSessionBlacklistPurgeDelay(TimeUnit unit) {
        return unit.convert(hotSwappableSessionServiceConfig.sessionBlacklistPurgeDelayMinutes, TimeUnit.MINUTES);
    }

    /**
     * Register a listener to be notified when {@link SessionServiceConfig} changes.
     *
     * @param listener the event listener to call when {@link SessionServiceConfig} changes.
     */
    public void addListener(ConfigListener listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners() {
        for (final ConfigListener listener : listeners) {
            listener.configChanged();
        }
    }
}
