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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts;

import static org.forgerock.openam.cts.api.CoreTokenConstants.CLEANUP_PERIOD;
import static org.forgerock.openam.cts.api.CoreTokenConstants.HEALTH_CHECK_PERIOD;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Represents any configuration required for the Core Token Service.
 */
public class CoreTokenConfig {

    private final CopyOnWriteArraySet<CoreTokenConfigListener> listeners = new CopyOnWriteArraySet<>();

    private volatile boolean caseSensitiveUserId;
    private volatile int sessionExpiryGracePeriod;
    private volatile int expiredSessionsSearchLimit;

    private volatile int runPeriod;
    private volatile int cleanupPageSize;
    private volatile int sleepInterval;

    // Token Blob strategy flags
    private volatile boolean tokensEncrypted;
    private volatile boolean tokensCompressed;
    private volatile boolean attributeNamesCompressed;

    /**
     * Create a new default instance of the CoreTokenConfig.
     * <p>
     * This factory method should be used to ensure that config is updated as underlying
     * system properties are changed.
     */
    static CoreTokenConfig newCoreTokenConfig() {
        final CoreTokenConfig coreTokenConfig = new CoreTokenConfig();
        String[] observedSystemProperties = new String[]{
                com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID,
                CoreTokenConstants.SYS_PROPERTY_EXPIRED_SEARCH_LIMIT,
                Constants.SESSION_REPOSITORY_ENCRYPTION,
                Constants.SESSION_REPOSITORY_COMPRESSION,
                Constants.SESSION_REPOSITORY_ATTRIBUTE_NAME_COMPRESSION,
                Constants.SESSION_REPOSITORY_ATTRIBUTE_NAME_COMPRESSION,
                CLEANUP_PERIOD,
                HEALTH_CHECK_PERIOD
        };
        ConfigurationListener listener = new ConfigurationListener() {
            @Override
            public void notifyChanges() {
                coreTokenConfig.loadSystemProperties();
                coreTokenConfig.notifyListeners();
            }
        };
        SystemProperties.observe(listener, observedSystemProperties);
        return coreTokenConfig;
    }

    /**
     * Create a new default instance of the CoreTokenConfig which will establish the various configuration
     * it requires from System Properties.
     */
    @VisibleForTesting
    CoreTokenConfig() {
        loadSystemProperties();
    }

    private void loadSystemProperties() {
        caseSensitiveUserId = SystemProperties.getAsBoolean(com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID);
        // 5 minutes
        sessionExpiryGracePeriod = 5 * 60;
        // Derive the expired Session Search Limit from system properties
        expiredSessionsSearchLimit = getSystemManagerPropertyAsInt(CoreTokenConstants.SYS_PROPERTY_EXPIRED_SEARCH_LIMIT, 250);

        int cleanupPeriod = getSystemManagerPropertyAsInt(CoreTokenConstants.CLEANUP_PERIOD, 5 * 60 * 1000);
        int healthCheckPeriod = getSystemManagerPropertyAsInt(CoreTokenConstants.HEALTH_CHECK_PERIOD, 1 * 60 * 1000);
        runPeriod = Math.min(cleanupPeriod, healthCheckPeriod);

        // Sleep interval between cycles in Core Token Service thread.
        sleepInterval = 60 * 1000;

        // Indicate if all Tokens stored in the Core Token Service should be encrypted.
        tokensEncrypted = SystemProperties.getAsBoolean(Constants.SESSION_REPOSITORY_ENCRYPTION);

        // Control Token Compression.
        tokensCompressed = SystemProperties.getAsBoolean(Constants.SESSION_REPOSITORY_COMPRESSION);

        // Control Attribute Name Compression.
        attributeNamesCompressed = SystemProperties.getAsBoolean(Constants.SESSION_REPOSITORY_ATTRIBUTE_NAME_COMPRESSION);

        // Controls the size of pages requested for CTS Reaper
        cleanupPageSize = 1000;
    }

    /**
     * Utility function to correctly derive the value of a SystemPropertiesManager value.
     *
     * @param prop Property to extract from SystemPropertiesManager.
     * @param defaultValue Default value to use in the event of an error of any kind.
     * @return An integer, either the value from the system properties or the default.
     */
    private static int getSystemManagerPropertyAsInt(String prop, int defaultValue) {
        if (prop == null) {
            return defaultValue;
        }

        String value = SystemPropertiesManager.get(prop);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * The Expired Session Search Limit.
     * @return A value greater than zero.
     */
    public int getExpiredSessionsSearchLimit() {
        return expiredSessionsSearchLimit;
    }

    /**
     * @return True if the User Id is case sensitive.
     */
    public boolean isCaseSensitiveUserId() {
        return caseSensitiveUserId;
    }

    /**
     * @return The time period in the given time units before a Session will timeout.
     */
    public long getSessionExpiryGracePeriod(TimeUnit timeUnit) {
        return timeUnit.convert(sessionExpiryGracePeriod, TimeUnit.SECONDS);
    }

    /**
     * @return The period in milliseconds for how often the clean up thread of the Core Token Service should
     * run to check for expired Tokens.
     */
    public int getRunPeriod() {
        return runPeriod;
    }

    /**
     * @return The interval in milliseconds for the
     */
    public int getSleepInterval() {
        return sleepInterval;
    }

    /**
     * Extract the UserId from the InternalSession.
     *
     * Account for some foibles around the user id. In particular
     *
     * @param session Non null.
     * @return Non null user id.
     */
    public String getUserId(InternalSession session) {
        // If the sessions Users ID has not been initialised, calling set will initialise it.
        String userId = session.getUUID();

        // Now process the case sensitivity for users id.
        if (isCaseSensitiveUserId()) {
            return userId;
        }
        return userId.toLowerCase();
    }

    /**
     * @return True if the Binary object stored for each Token should be encrypted by the Core Token Service.
     */
    public boolean isTokenEncrypted() {
        return tokensEncrypted;
    }

    /**
     * @return True if the tokens within the Core Token Service can be compressed. False is the default.
     */
    public boolean isTokenCompressed() {
        return tokensCompressed;
    }

    /**
     * @return True if The Token Attribute Names should be compressed as well. False by default.
     */
    public boolean isAttributeNamesCompressed() {
        return attributeNamesCompressed;
    }

    /**
     * @return The LDAP Query Page size in Tokens that will be deleted by the CTS Reaper.
     */
    public int getCleanupPageSize() {
        return cleanupPageSize;
    }

    /**
     * Register a listener to be notified when {@link CoreTokenConfig} changes.
     *
     * @param listener the event listener to call when {@link CoreTokenConfig} changes.
     */
    public void addListener(CoreTokenConfigListener listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners() {
        for (final CoreTokenConfigListener listener : listeners) {
            listener.configChanged();
        }
    }

}