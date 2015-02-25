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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.utils.OpenAMSettingsImpl;

/**
 * An implementation of the UmaPolicyStore.
 *
 * @since 13.0.0
 */
@Singleton
public class UmaPolicyStoreImpl implements UmaPolicyStore {

    private static final long DEFAULT_CACHE_SIZE = 1000L;

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final ConcurrentHashMap<String, Cache<String, UmaPolicy>> cache = new ConcurrentHashMap<String, Cache<String, UmaPolicy>>();
    private final OpenAMSettingsImpl umaSettings;

    @Inject
    public UmaPolicyStoreImpl() {
        umaSettings = new OpenAMSettingsImpl(UmaConstants.SERVICE_NAME, UmaConstants.SERVICE_VERSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cache<String, UmaPolicy> getUserCache(String userId, String realm) {
        cache.putIfAbsent(userId, newCache(realm));
        return cache.get(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToUserCache(String userId, String realm, String uid, UmaPolicy policy)
            throws InternalServerErrorException {
        addToCache(getUserCache(userId, realm), uid, policy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        cache.clear();
    }

    private Cache<String, UmaPolicy> newCache(String realm) {
        return CacheBuilder.newBuilder().maximumSize(getCacheSize(realm)).build();
    }

    private void addToCache(Cache<String, UmaPolicy> userCache, String uid, final UmaPolicy policy)
            throws InternalServerErrorException {
        try {
            userCache.get(uid, new Callable<UmaPolicy>() {
                @Override
                public UmaPolicy call() throws Exception {
                    return policy;
                }
            });
        } catch (ExecutionException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private long getCacheSize(String realm) {
        try {
            return umaSettings.getLongSetting(realm, UmaConstants.BACKEND_POLICIES_CACHE_SIZE);
        } catch (SMSException e) {
            logger.error("Failed to get UMA backend policy cache size using default " + DEFAULT_CACHE_SIZE, e);
        } catch (SSOException e) {
            logger.error("Failed to get UMA backend policy cache size using default " + DEFAULT_CACHE_SIZE, e);
        }
        return DEFAULT_CACHE_SIZE;
    }
}
