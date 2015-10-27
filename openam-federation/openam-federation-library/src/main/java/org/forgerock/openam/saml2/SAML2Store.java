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
package org.forgerock.openam.saml2;

import java.util.concurrent.TimeUnit;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;

/**
 * An internal alternative to using the SAML2 Failover as a mechanism for data transfer.
 * This will be faster, but only usable when there's a single server in the cluster.
 * Stores all data in an in-memory cache which auto-deletes entries after an hour.
 */
public class SAML2Store {

    //dumb local store the holds anything for two hours locally before trashing
    private static Cache<String, Object> localCache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterWrite(2, TimeUnit.HOURS).build();

    /**
     * Statically stores Strings mapped to a key.
     * @param key Key under which to store the String.
     * @param value Value associated with the key.
     */
    public static void saveTokenWithKey(String key, Object value) {
        localCache.put(key, value);
    }

    /**
     * Statically retrieves an object form the store.
     * @param key Key indicating the value to retrieve from the map.
     */
    public static Object getTokenFromStore(String key) {
        return localCache.getIfPresent(key);
    }

}
