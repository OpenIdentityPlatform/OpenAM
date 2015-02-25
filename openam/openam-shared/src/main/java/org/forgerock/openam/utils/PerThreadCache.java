/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.utils;

import com.sun.identity.shared.debug.Debug;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides caching of some expensive objects in a per-thread LRU cache for performance. Note: this cache does not use
 * real ThreadLocal storage because of issues with cleanup and garbage collection. Instead we use an internal map keyed
 * by thread id to lookup a per-thread instance of the underlying resource, creating one if not present for this thread.
 * The map also acts as a LRU cache, ensuring a maximum size limit is obeyed by evicting the least recently used (LRU)
 * entry whenever it grows beyond capacity.
 *
 * @since 12.0.0
 */
public abstract class PerThreadCache<T, E extends Exception> {
    private static final Debug DEBUG = Debug.getInstance("amUtil");
    private static final int INITIAL_CACHE_SIZE = 16;
    private static final float CACHE_LOAD_FACTOR = 0.75f;

    private final int maxSize;

    /**
     * Configure a {@link LinkedHashMap} as a simple LRU cache, indexed by thread id.
     * <p/>
     * <strong>Thread-safety:</strong> clients of this map within this class should synchronize on it around use.
     */
    private final Map<Long, T> cache = new LinkedHashMap<Long, T>(INITIAL_CACHE_SIZE, CACHE_LOAD_FACTOR, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, T> eldestEntry) {
            boolean remove = size() > maxSize;
            if (remove && DEBUG.warningEnabled()) {
                T value = eldestEntry.getValue();
                String name = (value == null ? "null" : value.getClass().getName());
                DEBUG.warning("Cache size limit [" + maxSize + "] exceeded: evicting eldest entry: " + name);
            }
            return remove;
        }
    };

    /**
     * Initialises the thread-local cache with the given maximum size (i.e., number of threads).
     *
     * @param maxSize the maximum number of instances to cache.
     */
    public PerThreadCache(final int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Fetches an instance of the resource from the cache for the current thread. If no instance has been cached for
     * this thread, then calls {@link #initialValue()} to create one and then adds it to the cache.
     * If this causes the cache to exceed the configured maximum size then the least recently used (LRU)
     * entry will be deleted from the cache to preserve the capacity restriction.
     *
     * @return a configured instance from the cache. May be null depending on the initialValue() implementation.
     */
    public final T getInstanceForCurrentThread() throws E {
        final long threadId = Thread.currentThread().getId();
        T result;

        // Only synchronize around looking up the value and updating it. We do not synchronise while initialising a new
        // value (cache miss) as this could be expensive and so could cause the lock to be held for a long time,
        // increasing contention. There is no race condition here as we are keying by unique thread-id.
        synchronized (cache) {
            result = cache.get(threadId);
        }

        if (result == null) {
            result = initialValue();
            synchronized (cache) {
                cache.put(threadId, result);
            }
        }

        return result;
    }

    /**
     * Method will be called to initialise a fresh instance of the underlying resource when one is not found in the
     * cache. Concrete sub-classes should implement this method as per requirements.
     *
     * @return a fresh instance of the underlying resource.
     * @throws E if the resource instance cannot be created.
     */
    protected abstract T initialValue() throws E;
}
