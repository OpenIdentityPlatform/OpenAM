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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static com.sun.identity.shared.Constants.*;
import static org.forgerock.openam.cts.api.CoreTokenConstants.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.cluster.ClusterStateService;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for collating System Properties and amSession.xml configuration state relating to the Session Service.
 *
 * @since 13.0.0
 */
@Singleton
public class SessionServiceConfig {

    private static final String AM_SESSION_SERVICE_NAME = "iPlanetAMSessionService";

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

    /**
     * Property string for max number of sessions
     */
    static final int DEFAULT_MAX_SESSIONS = 10000;
    private final int maxSessions;

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

    // Must be True to permit Session Failover HA to be available.
    private static final boolean DEFAULT_USE_REMOTE_SAVE_METHOD = true;
    private boolean useRemoteSaveMethod;

    // Must be True to permit Session Failover HA to be available.
    private static final boolean DEFAULT_USE_INTERNAL_REQUEST_ROUTING = true;
    private boolean useInternalRequestRouting;

    // Must be True to permit Session Failover HA to be available, but we default this to Disabled or Off for Now.
    private static final boolean DEFAULT_SESSION_FAILOVER_ENABLED = false;
    private boolean sessionFailoverEnabled;

    private final int sessionFailoverClusterStateCheckTimeout;
    private final long sessionFailoverClusterStateCheckPeriod;

    /*
     * amSession.xml (SMS) Properties
     */

    private volatile HotSwappableSessionServiceConfig hotSwappableSessionServiceConfig;

    /**
     * Indicates whether to use crosstalk or session persistence to resolve remote sessions.
     * Always true when session persistence/SFO is disabled.
     */
    private static final boolean DEFAULT_REDUCED_CROSSTALK_ENABLED = true;
    private volatile boolean reducedCrosstalkEnabled = DEFAULT_REDUCED_CROSSTALK_ENABLED;

    /**
     * The number of minutes to retain {@link com.iplanet.dpro.session.Session} objects in DESTROYED state
     * while waiting for delete replication to occur if reduced cross-talk is enabled.
     */
    private static final long DEFAULT_REDUCED_CROSSTALK_PURGE_DELAY = 5;
    private volatile long reducedCrosstalkPurgeDelay = DEFAULT_REDUCED_CROSSTALK_PURGE_DELAY;

    /**
     * Indicates what broadcast to undertake on session logout/destroy
     */
    private static final SessionBroadcastMode DEFAULT_LOGOUT_DESTROY_BROADCAST = SessionBroadcastMode.OFF;
    private volatile SessionBroadcastMode logoutDestroyBroadcast = DEFAULT_LOGOUT_DESTROY_BROADCAST;

    /**
     * Private value object for storing snapshot state of amSession.xml config settings.
     *
     * This allows immutable value objects to be published as an atomic operation.
     */
    private class HotSwappableSessionServiceConfig {

        private static final long DEFAULT_SESSION_RETRIEVAL_TIMEOUT = 5;
        private static final int DEFAULT_MAX_SESSION_LIST_SIZE = 200;
        private static final int DEFAULT_MAX_WAIT_TIME_FOR_CONSTRAINT = 6000;

        private final Set<String> timeoutHandlers;
        private final boolean sessionTrimmingEnabled;
        private final boolean sessionConstraintEnabled;
        private final boolean denyLoginIfDBIsDown;
        private final String constraintHandler;
        private final boolean propertyNotificationEnabled;
        private final Set notificationProperties;
        private final long sessionRetrievalTimeout; //  in seconds
        private final int maxSessionListSize;
        private final int maxWaitTimeForConstraint; // in milli-seconds

        private HotSwappableSessionServiceConfig(ServiceSchemaManager ssm) throws SMSException {
            ServiceSchema schema = ssm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();

            sessionRetrievalTimeout = loadSessionRetrievalTimeoutSchemaSetting(attrs);
            maxSessionListSize = loadMaxSessionListSizeSchemaSetting(attrs);
            propertyNotificationEnabled = loadPropertyNotificationEnabledSchemaSetting(attrs);
            notificationProperties = loadPropertyNotificationPropertiesSchemaSetting(attrs);
            timeoutHandlers = loadTimeoutHandlersServiceSchemaSetting(attrs);
            sessionTrimmingEnabled = loadSessionTrimmingServiceSchemaSetting(attrs);
            sessionConstraintEnabled = loadSessionConstraintServiceSchemaSetting(attrs);
            denyLoginIfDBIsDown = loadDenyLoginIfDBIsDownServiceSchemaSetting(attrs);
            constraintHandler = loadConstraintHandlerServiceSchemaSetting(attrs);
            maxWaitTimeForConstraint = loadMaxWaitTimeForConstraintHandlerServiceSchemaSetting(attrs);
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
            Set<String> value = (Set<String>) attrs.get(Constants.TIMEOUT_HANDLER_LIST);
            if (value == null) {
                value = Collections.emptySet();
            } else {
                value = new HashSet<String>(value);
            }
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("timeoutHandlers=" + value);
            }
            return value;
        }

        private boolean loadSessionTrimmingServiceSchemaSetting(Map attrs) {
            boolean value = "YES".equalsIgnoreCase(
                    CollectionHelper.getMapAttr(attrs, Constants.ENABLE_TRIM_SESSION));
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("sessionTrimmingEnabled=" + value);
            }
            return value;
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
            @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            @Named(SessionConstants.PRIMARY_SERVER_URL) String primaryServerURL,
            DsameAdminTokenProvider dsameAdminTokenProvider) {

        this.sessionDebug = sessionDebug;

        // Initialize values set from System properties
        maxSessions =
                SystemProperties.getAsInt(AM_SESSION_MAX_SESSIONS, DEFAULT_MAX_SESSIONS);
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
        useRemoteSaveMethod =
                SystemProperties.getAsBoolean(AM_SESSION_FAILOVER_USE_REMOTE_SAVE_METHOD, DEFAULT_USE_REMOTE_SAVE_METHOD);
        useInternalRequestRouting =
                SystemProperties.getAsBoolean(AM_SESSION_FAILOVER_USE_INTERNAL_REQUEST_ROUTING, DEFAULT_USE_INTERNAL_REQUEST_ROUTING);
        sessionFailoverEnabled =
                SystemProperties.getAsBoolean(IS_SFO_ENABLED, DEFAULT_SESSION_FAILOVER_ENABLED);
        sessionFailoverClusterStateCheckTimeout =
                loadSessionFailoverClusterStateCheckTimeout();
        sessionFailoverClusterStateCheckPeriod =
                loadSessionFailoverClusterStateCheckPeriod();

        try {

            // Initialize settings from SMS

            ServiceSchemaManager serviceSchemaManager =
                    new ServiceSchemaManager(AM_SESSION_SERVICE_NAME, dsameAdminTokenProvider.getAdminToken());
            hotSwappableSessionServiceConfig = new HotSwappableSessionServiceConfig(serviceSchemaManager);

            ServiceConfigManager scm =
                    new ServiceConfigManager(AM_SESSION_SERVICE_NAME, dsameAdminTokenProvider.getAdminToken());
            ServiceConfig serviceConfig = scm.getGlobalConfig(null);

            /*
             * In OpenSSO 8.0, we have switched to create sub configuration with
             * site name. hence we need to lookup the site name based on the URL
             */
            String subCfgName =
                    ServerConfiguration.isLegacy(dsameAdminTokenProvider.getAdminToken()) ?
                    primaryServerURL :
                    SiteConfiguration.getSiteIdByURL(dsameAdminTokenProvider.getAdminToken(), primaryServerURL);
            ServiceConfig subConfig =
                    subCfgName != null ?
                    serviceConfig.getSubConfig(subCfgName) :
                    null;

            if ((subConfig != null) && subConfig.exists()) {

                Map sessionAttrs = subConfig.getAttributes();
                boolean sfoEnabled = CollectionHelper.getBooleanMapAttr(sessionAttrs, IS_SFO_ENABLED, false);

                // Currently, we are not allowing to default to Session Failover HA,
                // even with a single server to enable session persistence.
                // But can easily be turned on in the Session SubConfig.
                if (sfoEnabled) {

                    sessionFailoverEnabled = true;
                    useRemoteSaveMethod = true;
                    useInternalRequestRouting = true;

                    // Determine whether crosstalk is enabled or disabled.
                    reducedCrosstalkEnabled =
                            CollectionHelper.getBooleanMapAttr(sessionAttrs, IS_REDUCED_CROSSTALK_ENABLED, true);

                    if (reducedCrosstalkEnabled) {
                        logoutDestroyBroadcast = SessionBroadcastMode.valueOf(
                                CollectionHelper.getMapAttr(
                                        sessionAttrs, LOGOUT_DESTROY_BROADCAST, SessionBroadcastMode.OFF.name()));
                    }

                    reducedCrosstalkPurgeDelay =
                            CollectionHelper.getLongMapAttr(sessionAttrs,
                                    REDUCED_CROSSTALK_PURGE_DELAY, 1, sessionDebug);

                }
            }

            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("Session Failover Enabled = " + sessionFailoverEnabled);
            }
            SessionConfigListener sessionConfigListener = new SessionConfigListener(serviceSchemaManager);
            serviceSchemaManager.addListener(sessionConfigListener);

        } catch (Exception ex) {
            sessionDebug.error("SessionService: Initialization Failed", ex);
            // Rethrow exception rather than hobbling on with invalid configuration state
            throw new IllegalStateException("Failed to load SessionService configuration", ex);
        }
    }

    private int loadNotificationThreadPoolSizeSystemProperty() {
        String size = SystemProperties.get(NOTIFICATION_THREADPOOL_SIZE);
        try {
            return Integer.parseInt(size);
        } catch (NumberFormatException e) {
            sessionDebug.error(
                    "Invalid value for " + NOTIFICATION_THREADPOOL_SIZE +
                            " defaulting to " + DEFAULT_NOTIFICATION_THEAD_POOL_SIZE);
            return DEFAULT_NOTIFICATION_THEAD_POOL_SIZE;
        }
    }

    private int loadNotificationThreadPoolThresholdSystemProperty() {
        String threshold = SystemProperties.get(NOTIFICATION_THREADPOOL_THRESHOLD);
        try {
            return Integer.parseInt(threshold);
        } catch (NumberFormatException e) {
            sessionDebug.error(
                    "Invalid value for " + NOTIFICATION_THREADPOOL_THRESHOLD +
                            " defaulting to " + DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD);
            return DEFAULT_NOTIFICATION_THEAD_POOL_THRESHOLD;
        }
    }

    private int loadSessionFailoverClusterStateCheckTimeout() {
        String timeout = SystemProperties.get(AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT);
        try {
            return Integer.parseInt(timeout);
        } catch (Exception e) {
            sessionDebug.error(
                    "Invalid value for " + Constants.AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT +
                            " defaulting to " + ClusterStateService.DEFAULT_TIMEOUT);
            return ClusterStateService.DEFAULT_TIMEOUT;
        }
    }

    private long loadSessionFailoverClusterStateCheckPeriod() {
        String period = SystemProperties.get(AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD);
        try {
            return Long.parseLong(period);
        } catch (Exception e) {
            sessionDebug.error(
                    "Invalid value for " + Constants.AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD +
                            " defaulting to " + ClusterStateService.DEFAULT_PERIOD);
            return ClusterStateService.DEFAULT_PERIOD;
        }
    }

    /**
     * Returns true if SystemProperty "com.iplanet.am.session.failover.useRemoteSaveMethod" is true or
     * Session Failover is enabled.
     *
     * @see #isSessionFailoverEnabled()
     */
    public boolean isUseRemoteSaveMethod() {
        return useRemoteSaveMethod;
    }

    /**
     * Returns amSession.xml property "iplanet-am-session-logout-destroy-broadcast" choice.
     *
     * Defaults to {@link SessionBroadcastMode.OFF }.
     */
    public SessionBroadcastMode getLogoutDestroyBroadcast() {
        return logoutDestroyBroadcast;
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

    /**
     * Returns true if amSession.xml property "iplanet-am-session-enable-session-trimming" is "YES" (case insensitive).
     *
     * Defaults to false.
     */
    public boolean isSessionTrimmingEnabled() {
        return hotSwappableSessionServiceConfig.sessionTrimmingEnabled;
    }

    /**
     * The number of minutes to retain {@link com.iplanet.dpro.session.Session} objects in DESTROYED state while waiting
     * for delete replication to occur if reduced cross-talk is enabled.
     */
    public long getReducedCrosstalkPurgeDelay() {
        return reducedCrosstalkPurgeDelay;
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
     * Returns SystemProperty "com.iplanet.am.session.maxSessions".
     *
     * Defaults to 10,000 if not specified.
     */
    public int getMaxSessions() {
        return maxSessions;
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
     * Returns true if SystemProperty or amSession.xml property "iplanet-am-session-sfo-enabled" is true.
     *
     * Defaults to false.
     */
    public boolean isSessionFailoverEnabled() {
        return sessionFailoverEnabled;
    }

    /**
     * Returns true if amSession.xml property "iplanet-am-session-reduced-crosstalk-enabled" is true
     * (and session failover is enabled).
     *
     * @see #isSessionFailoverEnabled()
     */
    public boolean isReducedCrossTalkEnabled() {
        return sessionFailoverEnabled && reducedCrosstalkEnabled;
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
     * Returns true if SystemProperty "com.iplanet.am.session.failover.useInternalRequestRouting" is enabled or
     * session failover is enabled.
     *
     * Defaults to true.
     *
     * @see #isSessionFailoverEnabled()
     */
    public boolean isUseInternalRequestRoutingEnabled() {
        return sessionFailoverEnabled && useInternalRequestRouting;
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
     * A single instance of this class is created to listen for changes to the amSession.xml configuration state
     * and ensure that {@link SessionServiceConfig#hotSwappableSessionServiceConfig} state is kept in sync.
     */
    class SessionConfigListener implements ServiceListener {

        private final ServiceSchemaManager serviceSchemaManager;

        /**
         * Creates a new SessionConfigListener
         * @param serviceSchemaManager ServiceSchemaManager
         */
        public SessionConfigListener(ServiceSchemaManager serviceSchemaManager) {
            this.serviceSchemaManager = serviceSchemaManager;
        }

        /**
         * {@inheritDoc}
         */
        public void schemaChanged(String serviceName, String version) {

            if ((serviceName != null) && !serviceName.equalsIgnoreCase(AM_SESSION_SERVICE_NAME)) {
                return;
            }

            try {
                hotSwappableSessionServiceConfig = new HotSwappableSessionServiceConfig(serviceSchemaManager);
            } catch (Exception e) {
                sessionDebug.error("SessionConfigListener : Unable to get Session Service attributes", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void globalConfigChanged(String serviceName, String version,
                                        String groupName, String serviceComponent, int type) {
            // No op.
        }

        /**
         * {@inheritDoc}
         */
        public void organizationConfigChanged(String serviceName, String version,
                                              String orgName, String groupName, String serviceComponent, int type) {
            // No op.
        }

    }
}
