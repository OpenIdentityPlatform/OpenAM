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
package org.forgerock.openam.session.stateless.cache;

import com.iplanet.dpro.session.share.SessionInfo;
import org.forgerock.openam.session.stateless.StatelessConfig;
import org.forgerock.openam.utils.collections.LeastRecentlyUsed;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;

/**
 * Responsible for providing a caching layer for JWT/SessionInfo conversion.
 *
 * This cache acts as a performance enhancement which will reduce the number of times JWT
 * tokens need to be decrypted and decoded.
 *
 * This cache is intentionally unable to perform the reverse lookup of SessionInfo to
 * JWT, as we expect the JWT to change each time the SessionInfo changes.
 *
 * Assumption: There is only one representation of a JWT to the SessionInfo it contains.
 *
 * Thread Safety: This class uses a synchronized data structure and so is thread safe.
 */
@Singleton
public class StatelessJWTCache {
    private final Map<String, SessionInfo> sessionInfoCache;

    @Inject
    public StatelessJWTCache(StatelessConfig config) {
        sessionInfoCache = Collections.synchronizedMap(
                new LeastRecentlyUsed<String, SessionInfo>(config.getJWTCacheSize()));
    }

    /**
     * Stores the relationship between a SessionInfo, and its encrypted JWT.
     *
     * @param jwtToken Non null JWT Token to store.
     * @param info Non null SessionInfo to store against the JWT.
     */
    public void cache(SessionInfo info, String jwtToken) {
        Reject.ifNull(info, jwtToken, "Arguments cannot be null.");
        sessionInfoCache.put(jwtToken, info);
    }

    /**
     * @param jwt Possibly null JWT token.
     * @return Possibly null. Cached SessionInfo that corresponds to the given JWT token.
     */
    public SessionInfo getSessionInfo(String jwt) {
        return sessionInfoCache.get(jwt);
    }

    /**
     * @param info Non null SessionInfo to test.
     * @return True if there is a JWT representation for this SessionInfo.
     */
    public boolean contains(SessionInfo info) {
        return sessionInfoCache.containsValue(info);
    }

    /**
     * @param jwtToken Possibly null JWT token.
     * @return True if this JWT has been stored in the cache previously.
     */
    public boolean contains(String jwtToken) {
        return sessionInfoCache.containsKey(jwtToken);
    }
}