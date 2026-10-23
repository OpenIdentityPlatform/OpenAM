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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.core.realms;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the DNS aliases of all realms, so that the host name of every request can be checked against them
 * without searching the configuration store.
 *
 * <p>The aliases are read again after any change to the realm configuration.</p>
 */
@Singleton
public class RealmDnsAliases {

    private static Logger logger = LoggerFactory.getLogger("amIdm");

    private final Provider<SSOToken> adminTokenProvider;
    private final OrganizationConfigManagerFactory organizationConfigManagerFactory;
    private final Provider<ServiceConfigManager> idRepoServiceProvider;
    private final AtomicBoolean initialised = new AtomicBoolean();
    private final AtomicLong configVersion = new AtomicLong();
    private volatile Snapshot snapshot;

    @Inject
    RealmDnsAliases(@Named("AdminToken") Provider<SSOToken> adminTokenProvider,
            OrganizationConfigManagerFactory organizationConfigManagerFactory,
            @Named(IdConstants.REPO_SERVICE) Provider<ServiceConfigManager> idRepoServiceProvider) {
        this.adminTokenProvider = adminTokenProvider;
        this.organizationConfigManagerFactory = organizationConfigManagerFactory;
        this.idRepoServiceProvider = idRepoServiceProvider;
    }

    /**
     * Determines whether a host name is configured as a DNS alias of any realm.
     *
     * @param hostname The host name, in any case.
     * @return {@code true} if a realm has the host name among its DNS aliases, {@code false} otherwise or if the
     * realms cannot be read.
     */
    public boolean contains(String hostname) {
        if (hostname == null) {
            return false;
        }
        init();
        return getAliases().contains(hostname.toLowerCase());
    }

    private void init() {
        if (initialised.compareAndSet(false, true)) {
            if (idRepoServiceProvider.get().addListener(new RealmsChangeListener()) == null) {
                logger.warn("RealmDnsAliases: Failed to add ServiceListener. "
                        + "Realm DNS aliases will not be refreshed on config changes.");
            }
        }
    }

    private Set<String> getAliases() {
        Snapshot current = snapshot;
        if (current != null && current.configVersion == configVersion.get()) {
            return current.aliases;
        }
        return reload();
    }

    private synchronized Set<String> reload() {
        // Taken before reading, so that a change notified while reading makes the next call read again
        long version = configVersion.get();
        if (snapshot != null && snapshot.configVersion == version) {
            return snapshot.aliases;
        }
        try {
            Set<String> aliases = readAliases();
            snapshot = new Snapshot(version, aliases);
            return aliases;
        } catch (SMSException e) {
            logger.warn("RealmDnsAliases: Unable to read the DNS aliases of the realms", e);
            return Collections.emptySet();
        }
    }

    private Set<String> readAliases() throws SMSException {
        SSOToken adminToken = adminTokenProvider.get();
        Set<String> aliases = new HashSet<>();
        addAliases(aliases, getRealmAliases(adminToken, "/"));
        for (String subRealm : getSubRealms(adminToken)) {
            addAliases(aliases, getRealmAliases(adminToken, "/" + subRealm));
        }
        return Collections.unmodifiableSet(aliases);
    }

    private void addAliases(Set<String> aliases, Set<String> realmAliases) {
        if (realmAliases != null) {
            for (String alias : realmAliases) {
                aliases.add(alias.toLowerCase());
            }
        }
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    Set<String> getSubRealms(SSOToken token) throws SMSException {
        return organizationConfigManagerFactory.create(token, "/").getSubOrganizationNames("*", true);
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    Set<String> getRealmAliases(SSOToken token, String realm) throws SMSException {
        OrganizationConfigManager realmManager = organizationConfigManagerFactory.create(token, realm);
        return (Set<String>) realmManager.getAttributes(IdConstants.REPO_SERVICE)
                .get(IdConstants.ORGANIZATION_ALIAS_ATTR);
    }

    private static final class Snapshot {

        private final long configVersion;
        private final Set<String> aliases;

        private Snapshot(long configVersion, Set<String> aliases) {
            this.configVersion = configVersion;
            this.aliases = aliases;
        }
    }

    private class RealmsChangeListener implements ServiceListener {

        @Override
        public void schemaChanged(String serviceName, String version) {
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
            configVersion.incrementAndGet();
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            configVersion.incrementAndGet();
        }
    }
}
