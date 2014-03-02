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

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

/**
 * Implements per-thread caching (using {@link PerThreadCache}) for SAXParser instances, delegating to an underlying
 * {@link SAXParserProvider} for the actual parser instances.
 *
 * @since 12.0.0
 */
class PerThreadSAXParserProvider implements SAXParserProvider {
    private static final int DEFAULT_CACHE_SIZE = 500;

    private final PerThreadSAXParserCache validatingParserCache;
    private final PerThreadSAXParserCache nonValidatingParserCache;

    /**
     * Initialises the parser cache with the given delegate parser provider and maximum cache size. Note that two
     * internal caches will be maintained - one for validating and one for non-validating parsers. Therefore the total
     * cache size will be equal to {@code maxSize * 2}.
     *
     * @param delegate the underlying parser provider to use to create new parser instances.
     * @param maxSize the maximum size of the LRU parser cache.
     * @throws NullPointerException if the delegate provider is null.
     * @throws IllegalArgumentException if maxSize &lt;= 0.
     */
    PerThreadSAXParserProvider(SAXParserProvider delegate, int maxSize) {
        if (delegate == null) {
            throw new NullPointerException("Delegate SAXParserProvider cannot be null");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        validatingParserCache = new PerThreadSAXParserCache(maxSize, true, delegate);
        nonValidatingParserCache = new PerThreadSAXParserCache(maxSize, false, delegate);
    }

    /**
     * Initialises the per-thread cache with the given delegate provider and the default (500) cache size.
     *
     * @param delegate the delegate parser provider to use for creating fresh parser instances.
     * @see #PerThreadSAXParserProvider(SAXParserProvider, int)
     */
    PerThreadSAXParserProvider(SAXParserProvider delegate) {
        this(delegate, DEFAULT_CACHE_SIZE);
    }

    /**
     * Returns the per-thread SAXParser instance for this thread, creating one if it does not already exist. If the
     * maximum size of the cache has been exceeded then the least recently used (LRU) parser in the cache will be
     * evicted to make room.
     *
     * @param validating Whether the returned document builder should perform XML validation or not.
     * @return a SAXParser instance for exclusive use of this thread.
     * @throws ParserConfigurationException if the parser cannot be configured.
     * @throws SAXException if the parser cannot be instantiated.
     */
    public SAXParser getSAXParser(boolean validating) throws ParserConfigurationException, SAXException {
        try {
            return validating ? validatingParserCache.getInstanceForCurrentThread()
                              : nonValidatingParserCache.getInstanceForCurrentThread();
        } catch (SAXException ex) {
            // Unwrap any parser configuration exceptions and rethrow as correct type
            if (ex.getCause() instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) ex.getCause();
            }
            throw ex;
        }
    }

    /**
     * Specialised version of {@link PerThreadCache} for SAXParsers.
     */
    private static class PerThreadSAXParserCache extends PerThreadCache<SAXParser, SAXException> {
        private final boolean validating;
        private final SAXParserProvider delegate;

        PerThreadSAXParserCache(int maxSize, boolean validating, SAXParserProvider delegate) {
            super(maxSize);
            this.validating = validating;
            this.delegate = delegate;
        }

        @Override
        protected SAXParser initialValue() throws SAXException {
            // Provider interface currently only supports a single checked exception so rethrow ParserConfigurationExceptions
            // as SAXExceptions.
            try {
                return delegate.getSAXParser(validating);
            } catch (ParserConfigurationException ex) {
                throw new SAXException(ex);
            }
        }
    }
}
