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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.iplanet.services.naming.ServiceListeners;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Methods of this class return Global settings of the OAuth2 Provider.
 */
public class OAuth2GlobalSettings {

    private final Debug logger;
    private volatile HotSwappableConfiguration configuration;

    @Inject
    public OAuth2GlobalSettings(@Named("OAuth2Provider") Debug logger,
            PrivilegedAction<SSOToken> adminTokenProvider, ServiceListeners serviceListeners) {
        this.logger = logger;

        try {
            final ServiceSchemaManager serviceSchemaManager = new ServiceSchemaManager("OAuth2Provider", AccessController.doPrivileged(adminTokenProvider));
            configuration = new HotSwappableConfiguration(serviceSchemaManager);

            ServiceListeners.Action action = new ServiceListeners.Action() {
                @Override
                public void performUpdate() {
                    try {
                        configuration = new HotSwappableConfiguration(serviceSchemaManager);
                    } catch (SMSException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            serviceListeners.config("OAuth2Provider")
                    .global(action)
                    .schema(action).listen();
        } catch (Exception e) {
            logger.error("OAuth2GlobalSettings: Initialization Failed", e);
            // Rethrow exception rather than hobbling on with invalid configuration state
            throw new IllegalStateException("Failed to load global OAuth2 configuration", e);
        }
    }

    /**
     * Whether token blacklisting is enabled for stateless token deletion.
     *
     * Defaults to false.
     */
    public boolean isSessionBlacklistingEnabled() {
        return configuration.blacklistingEnabled;
    }

    /**
     * Maximum number of blacklisted tokens to cache in memory on each server. Beyond this number, tokens will be
     * evicted from memory (but kept in the CTS) in a least-recently used (LRU) strategy.
     *
     * Defaults to 10000.
     */
    public int getBlacklistCacheSize() {
        return configuration.blacklistCacheSize;
    }

    /**
     * The interval at which to poll for changes to the token blacklist. May be 0 to indicate polling is disabled.
     *
     * @param unit the desired time unit for the poll interval.
     */
    public long getBlacklistPollInterval(TimeUnit unit) {
        return unit.convert(configuration.blacklistPollInterval, TimeUnit.SECONDS);
    }

    /**
     * Amount of time to keep tokens in the blacklist beyond their expiry time to account for clock skew.
     *
     * @param unit the desired time unit for the purge delay.
     */
    public long getBlacklistPurgeDelay(TimeUnit unit) {
        return unit.convert(configuration.blacklistPurgeDelay, TimeUnit.MINUTES);
    }

    /**
     * Private value object for storing snapshot state of amSession.xml config settings.
     *
     * This allows immutable value objects to be published as an atomic operation.
     */
    private class HotSwappableConfiguration {

        private final boolean blacklistingEnabled;
        private final int blacklistCacheSize;
        private final long blacklistPollInterval;
        private final long blacklistPurgeDelay;

        private HotSwappableConfiguration(ServiceSchemaManager ssm) throws SMSException {
            ServiceSchema schema = ssm.getGlobalSchema();
            Map<String, Set<String>> attrs = schema.getAttributeDefaults();

            blacklistingEnabled = CollectionHelper.getBooleanMapAttr(attrs, "blacklistingEnabled", false);
            blacklistCacheSize = CollectionHelper.getIntMapAttr(attrs, "blacklistCacheSize", 0, logger);
            blacklistPollInterval = CollectionHelper.getLongMapAttr(attrs, "blacklistPollInterval", 60, logger);
            blacklistPurgeDelay = CollectionHelper.getLongMapAttr(attrs, "blacklistPurgeDelay", 1, logger);
        }
    }
}
