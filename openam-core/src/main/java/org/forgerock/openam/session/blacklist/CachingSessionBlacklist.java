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

package org.forgerock.openam.session.blacklist;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import org.forgerock.openam.utils.collections.LeastRecentlyUsed;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.time.TimeService;

import java.util.Collections;
import java.util.Map;

/**
 * Caches elements on a least-recently used (LRU) basis.
 */
public final class CachingSessionBlacklist implements SessionBlacklist {

    private final SessionBlacklist delegate;
    private final Map<String, Long> cache;
    private final long purgeDelayMs;

    @VisibleForTesting
    CachingSessionBlacklist(final SessionBlacklist delegate, final int maxSize, final long purgeDelayMs,
                            final TimeService clock) {
        Reject.ifNull(delegate);
        Reject.ifFalse(maxSize > 0, "maxSize must be > 0");
        this.delegate = delegate;
        this.purgeDelayMs = purgeDelayMs;
        this.cache = Collections.synchronizedMap(new LeastRecentlyUsed<String, Long>(maxSize) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, Long> eldestEntry) {
                return eldestEntry.getValue() < clock.now() || super.removeEldestEntry(eldestEntry);
            }
        });
    }

    /**
     * Constructs the caching session blacklist with the given delegate blacklist and maximum cache size.
     *
     * @param delegate the delegate to defer cache misses to.
     * @param maxSize the maximum size of the LRU cache to maintain.
     * @param purgeDelayMs the additional delay before purging elements from the cache.
     */
    public CachingSessionBlacklist(final SessionBlacklist delegate, final int maxSize, final long purgeDelayMs) {
        this(delegate, maxSize, purgeDelayMs, TimeService.SYSTEM);
    }

    @Override
    public void blacklist(final Session session) throws SessionException {
        if (cache.put(session.getStableStorageID(), session.getBlacklistExpiryTime(purgeDelayMs)) == null) {
            // Only blacklist sessions that are not already in the cache.
            delegate.blacklist(session);
        }
    }

    @Override
    public boolean isBlacklisted(final Session session) throws SessionException {
        final String key = session.getStableStorageID();

        if (cache.containsKey(key)) {
            // Cache hit
            return true;
        }

        // Cache miss
        boolean isBlacklisted = delegate.isBlacklisted(session);
        if (isBlacklisted) {
            // Only cache entries that have been blacklisted, as we should always re-check otherwise
            cache.put(key, session.getBlacklistExpiryTime(purgeDelayMs));
        }
        return isBlacklisted;
    }

    @Override
    public void subscribe(final Listener listener) {
        // Pass straight through to delegate
        delegate.subscribe(listener);
    }


}
