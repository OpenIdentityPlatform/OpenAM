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

package org.forgerock.openam.shared.security.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides key pairs from the cache if the key properties match, else creates and caches a new key pair.
 *
 * @since 13.0.0
 */
final class KeyPairProviderImpl implements KeyPairProvider {

    private final Map<CacheKey, KeyPair> keyPairCache;

    KeyPairProviderImpl() {
        keyPairCache = new HashMap<>();
    }

    @Override
    public KeyPair getKeyPair(String algorithm, int keySize) {
        CacheKey cacheKey = new CacheKey(algorithm, keySize);
        KeyPair keyPair = keyPairCache.get(cacheKey);

        if (keyPair == null) {
            synchronized (keyPairCache) {
                keyPair = keyPairCache.get(cacheKey);

                if (keyPair == null) {
                    keyPair = newKeyPair(algorithm, keySize);
                    keyPairCache.put(cacheKey, keyPair);
                }
            }
        }

        return keyPair;
    }

    private KeyPair newKeyPair(String algorithm, int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(keySize);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException nsaE) {
            throw new IllegalArgumentException("Unsupported key algorithm " + algorithm, nsaE);
        }
    }

    private static final class CacheKey {

        private final String algorithm;
        private final int keySize;

        CacheKey(String algorithm, int keySize) {
            this.algorithm = algorithm;
            this.keySize = keySize;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey)) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) other;
            return Objects.equals(keySize, cacheKey.keySize)
                    && Objects.equals(algorithm, cacheKey.algorithm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(algorithm, keySize);
        }

    }

}
