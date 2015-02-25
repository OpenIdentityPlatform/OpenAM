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

import org.forgerock.guava.common.cache.Cache;
import org.forgerock.json.resource.InternalServerErrorException;

/**
 * UMA Policy store, which caches users UMA policies.
 *
 * @since 13.0.0
 */
public interface UmaPolicyStore {

    /**
     * Gets the cache of the given user.
     *
     * @param userId The id of the user.
     * @param realm The realm.
     * @return The cache.
     */
    Cache<String, UmaPolicy> getUserCache(String userId, String realm);

    /**
     * Adds a given policy to the users cache.
     *
     * @param userId The id of the user.
     * @param realm The realm.
     * @param uid The uid of the policy.
     * @param policy The policy.
     * @throws InternalServerErrorException If the cache entry could not be created.
     */
    void addToUserCache(String userId, String realm, String uid, UmaPolicy policy) throws InternalServerErrorException;

    /**
     * Clears the cache of backend policies.
     */
    void clearCache();
}
