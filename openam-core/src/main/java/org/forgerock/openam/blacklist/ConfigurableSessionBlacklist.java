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
 */

package org.forgerock.openam.blacklist;

import java.util.concurrent.TimeUnit;

import org.forgerock.openam.utils.ConfigListener;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.service.SessionServiceConfig;

/**
 * Blacklist which determines the appropriate strategy to use for blacklisting based on current configuration.
 * Strategies may include caching types, or disabling blacklisting entirely. Will be updated automatically when the
 * session service configuration changes.
 */
public class ConfigurableSessionBlacklist implements Blacklist<Session> {

    private CTSBlacklist<Session> ctsBlacklist;
    private final SessionServiceConfig sessionServiceConfig;
    private Blacklist<Session> delegate;

    /**
     * Create a configurable session blacklist based on the session service config.
     * @param ctsBlacklist The underlying CTS blacklist.
     * @param sessionServiceConfig The configuration of the session service.
     * @return The created ConfigurableSessionBlacklist.
     */
    public static ConfigurableSessionBlacklist createConfigurableSessionBlacklist(CTSBlacklist<Session> ctsBlacklist,
                                                                                  SessionServiceConfig sessionServiceConfig) {
        final ConfigurableSessionBlacklist configurableSessionBlacklist =
                new ConfigurableSessionBlacklist(ctsBlacklist, sessionServiceConfig);
        sessionServiceConfig.addListener(new ConfigListener() {
            @Override
            public void configChanged() {
                configurableSessionBlacklist.reloadDelegate();
            }
        });
        return configurableSessionBlacklist;
    }

    private ConfigurableSessionBlacklist(CTSBlacklist<Session> ctsBlacklist, SessionServiceConfig sessionServiceConfig) {
        this.ctsBlacklist = ctsBlacklist;
        this.sessionServiceConfig = sessionServiceConfig;
        reloadDelegate();
    }

    @Override
    public void blacklist(Session entry) throws BlacklistException {
        delegate.blacklist(entry);
    }

    @Override
    public boolean isBlacklisted(Session entry) throws BlacklistException {
        return delegate.isBlacklisted(entry);
    }

    @Override
    public void subscribe(Listener listener) {
        delegate.subscribe(listener);
    }

    private void reloadDelegate() {
        if (!sessionServiceConfig.isSessionBlacklistingEnabled()) {
            this.delegate = new NoOpBlacklist<>();
            return;
        }

        final long purgeDelayMs = sessionServiceConfig.getSessionBlacklistPurgeDelay(TimeUnit.MILLISECONDS);
        final int cacheSize = sessionServiceConfig.getSessionBlacklistCacheSize();
        final long pollIntervalMs = sessionServiceConfig.getSessionBlacklistPollInterval(TimeUnit.MILLISECONDS);

        Blacklist<Session> blacklist = ctsBlacklist;
        if (cacheSize > 0) {
            blacklist = new CachingBlacklist<>(blacklist, cacheSize, purgeDelayMs);
        }

        if (pollIntervalMs > 0) {
            blacklist = new BloomFilterBlacklist<>(blacklist, purgeDelayMs);
        }

        this.delegate = blacklist;
    }
}
