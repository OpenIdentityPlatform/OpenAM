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

import javax.crypto.Cipher;

/**
 * A {@link CipherProvider} that caches ciphers in per-thread storage. To avoid using an actual thread-local (and polluting
 * any thread pools that may touch this), we instead maintain our own map from thread id to cipher instances. As this
 * is just a normal (concurrent) map, then it can be garbage collected as normal so there is no memory leak on
 * hot-redeploy.
 *
 * @since 12.0.0
 */
public class PerThreadCipherProvider implements CipherProvider {
    private static final int DEFAULT_MAX_SIZE = 500;

    /**
     * Cache of constructed cipher instances indexed by thread id. Ensures that each thread receives its own cipher
     * instance. Uses a LinkedHashMap initialised as a LRU cache to limit the amount of memory used for cached
     * ciphers. Access to this map should synchronize on it for thread safety.
     */
    private final PerThreadCache<Cipher, RuntimeException> cipherCache;

    /**
     * Initialises the thread-local cipher cache, delegating to the supplied cipher provider for actual ciphers.
     *
     * @param delegate the non-null provider to get ciphers from.
     * @param maxSize the maximum size of the thread-local cache.
     * @throws NullPointerException if the delegate is null
     * @throws IllegalArgumentException if maxSize is not a positive integer (&gt; 0).
     */
    public PerThreadCipherProvider(final CipherProvider delegate, final int maxSize) {
        if (delegate == null) {
            throw new NullPointerException("Must specify a valid delegate CipherProvider");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.cipherCache = new PerThreadCache<Cipher, RuntimeException>(maxSize) {
            @Override
            protected Cipher initialValue() {
                return delegate.getCipher();
            }
        };
    }

    public PerThreadCipherProvider(CipherProvider delegate) {
        this(delegate, DEFAULT_MAX_SIZE);
    }

    /**
     * Gets a cipher from the thread-local storage, initialising to the delegate cipher provider if not already
     * initialised for this thread.
     *
     * {@inheritDoc}
     *
     * @return a cipher instance for this thread. May be null.
     */
    public Cipher getCipher() {
        return cipherCache.getInstanceForCurrentThread();
    }
}
