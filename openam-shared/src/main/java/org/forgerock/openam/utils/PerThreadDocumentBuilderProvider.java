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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Decorator {@link DocumentBuilderProvider} that caches instances in a per-thread LRU cache for performance. Each
 * instance is guaranteed only to be used by a single thread for the lifetime of that thread. The cache can be
 * configured with a maximum size, in which case the eldest entries will be evicted once the maximum capacity is
 * reached, and then recreated the next time an instance is required by that thread. The size of the cache should therefore
 * be tuned to match the size of the thread pool using it to avoid excessive eviction of configured instances (thrashing).
 *
 * @since 12.0.0
 */
class PerThreadDocumentBuilderProvider implements DocumentBuilderProvider {
    private static final int DEFAULT_CACHE_SIZE = 500;

    private final PerThreadCache<DocumentBuilder, ParserConfigurationException> validatingDBCache;
    private final PerThreadCache<DocumentBuilder, ParserConfigurationException> nonValidatingDBCache;

    /**
     * Initialises the per-thread document builder cache with the given maximum size and delegate provider instance.
     * Note that two caches will be created, one for validating and one for non-validating document builders. Each
     * cache will contain up to maxSize instances (so there will be 2x maxSize entries in total).
     *
     * @param delegate the underlying provider to create fresh instances.
     * @param maxSize the maximum number of document builders to cache in each cache (validating and non-validating).
     * @throws IllegalArgumentException if maxSize &lt;= 0.
     * @throws NullPointerException if the delegate provider is null.
     */
    PerThreadDocumentBuilderProvider(final DocumentBuilderProvider delegate, final int maxSize) {
        if (delegate == null) {
            throw new NullPointerException("Delegate DocumentBuilderProvider cannot be null");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("MaxSize must be positive");
        }
        this.validatingDBCache = new PerThreadDBCache(delegate, true, maxSize);
        this.nonValidatingDBCache = new PerThreadDBCache(delegate, false, maxSize);
    }

    /**
     * Initialises the thread-local document builder with the given delegate provider instance and the default maximum
     * cache size (500).
     * @param delegate the delegate to use to create fresh document builders.
     * @see #PerThreadDocumentBuilderProvider(DocumentBuilderProvider, int)
     */
    PerThreadDocumentBuilderProvider(final DocumentBuilderProvider delegate) {
        this(delegate, DEFAULT_CACHE_SIZE);
    }

    /**
     * Gets a document builder instance from the thread-local cache, initialising a fresh instance if one is not already
     * cached for this thread.
     *
     * @param validating Whether the returned document builder should perform XML validation or not.
     * @return an appropriate document builder instance for this thread.
     * @throws ParserConfigurationException if a fresh document builder could not be created.
     */
    public DocumentBuilder getDocumentBuilder(final boolean validating) throws ParserConfigurationException {
        return validating ? validatingDBCache.getInstanceForCurrentThread()
                          : nonValidatingDBCache.getInstanceForCurrentThread();
    }

    /**
     * Specialised thread-local cache for validating/non-validating document builder instances.
     */
    private static class PerThreadDBCache extends PerThreadCache<DocumentBuilder, ParserConfigurationException> {
        private final DocumentBuilderProvider delegate;
        private final boolean validating;
        private PerThreadDBCache(DocumentBuilderProvider delegate, boolean validating, int maxSize) {
            super(maxSize);
            this.validating = validating;
            this.delegate = delegate;
        }

        @Override
        protected DocumentBuilder initialValue() throws ParserConfigurationException {
            return delegate.getDocumentBuilder(validating);
        }
    }
}
