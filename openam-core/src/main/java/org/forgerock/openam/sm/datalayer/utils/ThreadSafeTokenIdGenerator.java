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

package org.forgerock.openam.sm.datalayer.utils;

import java.util.UUID;

import javax.inject.Singleton;

import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.utils.PerThreadCache;

/**
 * An implementation of TokenIdGenerator that uses a single cached UUID and appends a rolling number to the string
 * representation of that UUID.
 */
@Singleton
public class ThreadSafeTokenIdGenerator implements TokenIdGenerator {

    private static final PerThreadCache<IdCache, RuntimeException> ID_CACHE =
            new PerThreadCache<IdCache, RuntimeException>(Integer.MAX_VALUE) {
                @Override
                protected IdCache initialValue() {
                    return new IdCache();
                }
            };

    private static final class IdCache {
        private final String baseId = UUID.randomUUID().toString();
        private long count = 0;
        public String get() {
            return baseId + count++;
        }
    }

    @Override
    public String generateTokenId(String existingId) {
        return existingId != null ? existingId : ID_CACHE.getInstanceForCurrentThread().get();
    }

}
