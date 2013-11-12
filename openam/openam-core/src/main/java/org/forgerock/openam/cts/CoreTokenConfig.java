/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openam.cts;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.openam.cts.api.CoreTokenConstants;

/**
 * Represents any configuration required for the Core Token Service.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenConfig {

    private final boolean caseSensitiveUserId;
    private final int sessionExpiryGracePeriod;
    private final int expiredSessionsSearchLimit;

    private final int cleanupPeriod;
    private final int healthCheckPeriod;
    private final int runPeriod;
    private final int cleanupPageSize;

    private final int sleepInterval;

    // Token Blob strategy flags
    private final boolean tokensEncrypted;
    private final boolean tokensCompressed;
    private final boolean attributeNamesCompressed;

    /**
     * Create a new default instance of the CoreTokenConfig which will establish the various configuration
     * it requires from System Properties.
     */
    public CoreTokenConfig() {
        caseSensitiveUserId = SystemProperties.getAsBoolean(com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID);
        // 5 minutes
        sessionExpiryGracePeriod = 5 * 60;
        // Derive the expired Session Search Limit from system properties
        expiredSessionsSearchLimit = getSystemManagerPropertyAsInt(CoreTokenConstants.SYS_PROPERTY_EXPIRED_SEARCH_LIMIT, 250);

        // Derive the run period for the Core Token Service, controls how often token cleanup occurs.
        cleanupPeriod = getSystemManagerPropertyAsInt(CoreTokenConstants.CLEANUP_PERIOD, 5 * 60 * 1000);
        healthCheckPeriod = getSystemManagerPropertyAsInt(CoreTokenConstants.HEALTH_CHECK_PERIOD, 1 * 60 * 1000);

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
     * @return The time period in seconds before a Session will timeout.
     */
    public int getSessionExpiryGracePeriod() {
        return sessionExpiryGracePeriod;
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
}