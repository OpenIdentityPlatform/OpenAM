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

package org.forgerock.openam.core.realms;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Implementation of {@link RealmLookup} which caches realm look up results from its delegate
 * {@code Realms} instance.
 *
 * <p>Caches are cleared on any global or organisation config change.</p>
 *
 * @since 14.0.0
 */
@Singleton
final class CachingRealmLookup implements RealmLookup {

    private static Debug debug = AMIdentityRepository.debug;

    private final Cache<String, Realm> orgIdentifierToOrgName=CacheBuilder.newBuilder()
    		.maximumSize(1024)
    		.expireAfterAccess(1, TimeUnit.HOURS)
    		.build();

    private final Cache<String, Boolean> unknownOrgLookupCache=CacheBuilder.newBuilder()
    		.maximumSize(1024)
    		.expireAfterAccess(1, TimeUnit.HOURS)
    		.build();
    
    private final Cache<Realm, Boolean> orgStatusCache=CacheBuilder.newBuilder()
    		.maximumSize(1024)
    		.expireAfterAccess(1, TimeUnit.HOURS)
    		.build();
    
    private final RealmLookup delegate;
    private final Provider<ServiceConfigManager> idRepoServiceProvider;
    private AtomicBoolean initialised = new AtomicBoolean();

    CachingRealmLookup(RealmLookup delegate, Provider<ServiceConfigManager> idRepoServiceProvider) {
        this.delegate = delegate;
        this.idRepoServiceProvider = idRepoServiceProvider;
    }

    private void init() {
        if (initialised.compareAndSet(false, true)) {
            if (idRepoServiceProvider.get().addListener(new RealmsCacheListener()) == null) {
                if (debug.warningEnabled()) {
                    debug.warning("RealmsCache: Failed to add ServiceListener. "
                            + "Caches will not be refreshed on config changes.");
                }
            }
        }
    }

    @Override
    public Realm lookup(String orgIdentifier) throws RealmLookupException {
        init();
        Realm realm = orgIdentifierToOrgName.getIfPresent(orgIdentifier);
        if (realm != null) {
            return realm;
        }

        // Don't go to the expense of an organisation search if we have failed to look this up already.
        if (unknownOrgLookupCache.getIfPresent(orgIdentifier)!=null) {
            debug.message("RealmsCache.lookup: orgIdentifier {} found in unknown org lookup cache.", orgIdentifier);
            throw new NoRealmFoundException(orgIdentifier);
        }

        try {
            realm = delegate.lookup(orgIdentifier);
        } catch (NoRealmFoundException e) {
            unknownOrgLookupCache.put(orgIdentifier,true);
            throw e;
        }

        orgIdentifierToOrgName.put(orgIdentifier, realm);

        return realm;
    }

    @Override
    public boolean isActive(Realm realm) throws RealmLookupException {
        init();
        Boolean res=orgStatusCache.getIfPresent(realm);
        if (res!=null) {
            return res;
        }
        Boolean active = delegate.isActive(realm);
        orgStatusCache.put(realm, active);
        return active;
    }

    private class RealmsCacheListener implements ServiceListener {

        @Override
        public void schemaChanged(String serviceName, String version) {
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
            clearCaches();
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            clearCaches();
        }

        private void clearCaches() {
            debug.message("RealmsCache: Clearing caches due to config change.");
            orgIdentifierToOrgName.invalidateAll();
            orgStatusCache.invalidateAll();
            unknownOrgLookupCache.invalidateAll();
        }
    }
}
