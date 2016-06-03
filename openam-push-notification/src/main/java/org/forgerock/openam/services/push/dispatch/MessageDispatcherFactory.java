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
package org.forgerock.openam.services.push.dispatch;

import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;

/**
 * Generates message dispatchers.
 */
@Singleton
public class MessageDispatcherFactory {

    /**
     * Generate a new MessageDispatcher configured with the appropriate settings.
     *
     * @param maxSize Maximum size of the cache.
     * @param concurrency Level fo concurrency the cache supports.
     * @param expireAfter Entries should expire after this time from the cache.
     * @param debug A debug writer for errors.
     * @return A newly constructed MessageDispatcher.
     */
    public MessageDispatcher build(long maxSize, int concurrency, long expireAfter, Debug debug) {

        Cache<String, MessagePromise> cache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrency)
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfter, TimeUnit.SECONDS)
                .build();

        return new MessageDispatcher(cache, debug);
    }

}
