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

import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.json.resource.InternalServerErrorException;

@Singleton
public class UmaPolicyStoreImpl implements UmaPolicyStore {

    private final ConcurrentHashMap<String, Cache<String, UmaPolicy>> cache = new ConcurrentHashMap<String, Cache<String, UmaPolicy>>();

    @Override
    public Cache<String, UmaPolicy> getUserCache(String userId) {
        cache.putIfAbsent(userId, newCache());
        return cache.get(userId);
    }

    @Override
    public void addToUserCache(String userId, String uid, UmaPolicy policy) throws InternalServerErrorException {
        addToCache(getUserCache(userId), uid, policy);
    }

    private Cache<String, UmaPolicy> newCache() {
        return CacheBuilder.newBuilder().maximumSize(1000).build(); //TODO needs to be configurable
    }

    private void addToCache(Cache<String, UmaPolicy> userCache, String uid, final UmaPolicy policy) throws InternalServerErrorException {
        try {
            userCache.get(uid, new Callable<UmaPolicy>() {
                @Override
                public UmaPolicy call() throws Exception {
                    return policy;
                }
            });
        } catch (ExecutionException e) {
            throw new InternalServerErrorException(); //TODO
        }
    }
}
