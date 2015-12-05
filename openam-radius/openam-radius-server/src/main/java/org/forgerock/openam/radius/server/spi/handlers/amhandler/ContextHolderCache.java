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
/**
 *
 */
package org.forgerock.openam.radius.server.spi.handlers.amhandler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.utils.collections.LeastRecentlyUsed;

import com.sun.identity.shared.debug.Debug;
/**
 * A thread safe cache for ContextHolder objects.
 */
@SuppressWarnings("deprecation")
// Deprecated due to move to org.apache.commons.collections.map.LRUMap in v4
@Singleton
public class ContextHolderCache {

    private static Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * This class is really a synchronization wrapper around a LeastRecentlyUsed cache. It also prevents inappropriate
     * access to the cache methods (hence the use of encapsulation rather than inheritance).
     */
    private LeastRecentlyUsed<String, ContextHolder> cache;

    /**
     * The determination of cache size is also encapsulated so that the cache behaviour can be more easily tested, but
     * also so the strategy for deciding cache size can be simply modified.
     */
    private final ContextCacheSize contextCacheSize;

    /**
     * Constructs a thread safe ContextHolderCache.
     *
     * @param contextCacheSize
     *            - provides the ContextHolderCache with it's desired max size.
     */
    @Inject
    public ContextHolderCache(ContextCacheSize contextCacheSize) {
        cache = new LeastRecentlyUsed<String, ContextHolder>(contextCacheSize.getDesiredCacheSize());
        this.contextCacheSize = contextCacheSize;
    }

    /**
     * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
     *
     * @param key
     *            - the key whose associated value is to be returned.
     * @return the cached value associated with the specified key, or null if no cache entry is held with the key.
     */
    public synchronized ContextHolder get(String key) {
        return cache.get(key);
    }

    /**
     * Remove an entry from the cache.
     *
     * @param key
     *            - the key whose mapping is to be removed from the cache.
     * @return The item removed from the cache, or null if there was no mapping for the key.
     */
    public synchronized ContextHolder remove(String key) {
        return cache.remove(key);
    }

    /**
     * Creates a new cache key, creates and caches a new <code>ContextHolder</code> using the key and returns the newly
     * cached object.
     *
     * @return the newly cached <code>ContextHolder</code> object.
     */
    public synchronized ContextHolder createCachedContextHolder() {
        updateCacheSize();
        while (true) {
            final String key = UUID.randomUUID().toString();

            if (!cache.containsKey(key)) {
                final ContextHolder holder = new ContextHolder(key);
                cache.put(key, holder);
                return holder;
            }
        }
    }

    /**
     * Associates the specified value with the specified key in this cache. If the cache previously contained a mapping
     * for the key, the old value is replaced.
     *
     * @param key
     *            - key with which the specified value is to be associated
     * @param contextHolder
     *            - the <code>ContextHolder</code> associated with the specified key
     * @return the previous value associated with key, or null if there was no entry for key. (A null return can also
     *         indicate that the cache previously associated null with key.)
     */
    public synchronized ContextHolder put(String key, ContextHolder contextHolder) {
        updateCacheSize();
        return cache.put(key, contextHolder);
    }

    /**
     * Returns the number of key-ContextHolder entries in the cache.
     *
     * @return the number of key-ContextHolder entries in the cache.
     */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * Get the bounded size of the cache. This is the maximum number of entries that may be held. When the number of
     * entries in the cache equals this max size, then adding more entries will result in the least recently used number
     * of entries being removed to make space for the newly added entries.
     *
     * @return the bounded size of the cache.
     */
    public synchronized int getMaxSize() {
        return cache.getMaxSize();
    }

    /**
     * Updates the maximum cache size. *** Warning - the least recently used cache entries may be lost if the size of
     * the existing cache is greater than new cache size. ***
     */
    private void updateCacheSize() {
        final int desiredMaxSize = contextCacheSize.getDesiredCacheSize();
        final int actualMaxSize = cache.getMaxSize();
        if (desiredMaxSize != actualMaxSize) {
            final LeastRecentlyUsed<String, ContextHolder> newCache = new LeastRecentlyUsed<String, ContextHolder>(
                    desiredMaxSize);

            if (desiredMaxSize > cache.size()) {
                for (final Entry<String, ContextHolder> entry : cache.entrySet()) {
                    newCache.put(entry.getKey(), entry.getValue());
                }
            } else {
                logger.warning("Shrinking ContextHolderCache in response to change of system setting that determines "
                        + "the maximum number of allowable concurrent sessions. Some cache entries will.");
                // Bit of a faff this, but as there are too many entries in the existing cache to fit into the new
                // cache we need to reverse the entries, copy the desiredSize number off the back (the most recently
                // used), Then insert the entries we copied into the new cache, making sure to enter the least recently
                // used first.
                final LinkedList<Entry<String, ContextHolder>> entriesAsList =
                        new LinkedList<Entry<String, ContextHolder>>(cache.entrySet());
                final Iterator<Entry<String, ContextHolder>> itr = entriesAsList.descendingIterator();
                final LinkedList<Entry<String, ContextHolder>> entriesToCopy =
                        new LinkedList<Entry<String, ContextHolder>>();

                int numberCopied = 0;
                while (itr.hasNext() && numberCopied++ < desiredMaxSize) {
                    final Entry<String, ContextHolder> entry = itr.next();
                    entriesToCopy.add(entry);
                }

                final Iterator<Entry<String, ContextHolder>> toCopyRevItr = entriesToCopy.descendingIterator();
                while (toCopyRevItr.hasNext()) {
                    final Entry<String, ContextHolder> toCopy = toCopyRevItr.next();
                    newCache.put(toCopy.getKey(), toCopy.getValue());
                }
            }
            cache = newCache;
        }
    }
}
