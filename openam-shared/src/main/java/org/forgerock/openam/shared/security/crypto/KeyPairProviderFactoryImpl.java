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

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory caches key pair providers for a given realm.
 *
 * @since 13.0.0
 */
@Singleton
public final class KeyPairProviderFactoryImpl implements KeyPairProviderFactory {

    private final ConcurrentMap<String, KeyPairProvider> providerCache;

    /**
     * Creates a new key pair provider factory.
     */
    public KeyPairProviderFactoryImpl() {
        providerCache = new ConcurrentHashMap<>();
    }

    @Override
    public KeyPairProvider getProvider(String realm) {
        KeyPairProvider provider = providerCache.get(realm);

        if (provider == null) {
            provider = new KeyPairProviderImpl();
            KeyPairProvider existingProvider = providerCache.putIfAbsent(realm, provider);

            if (existingProvider != null) {
                provider = existingProvider;
            }
        }

        return provider;
    }

}
