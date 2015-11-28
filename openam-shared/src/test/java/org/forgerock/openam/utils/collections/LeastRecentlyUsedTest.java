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

package org.forgerock.openam.utils.collections;

import static org.fest.assertions.Assertions.assertThat;

import java.util.UUID;

import org.testng.annotations.Test;

public class LeastRecentlyUsedTest {
    @Test
    public void shouldLimitCacheSize() {
        final LeastRecentlyUsed<Integer, Integer> cache = new LeastRecentlyUsed<Integer, Integer>(1);
        cache.put(1, 2);
        cache.put(3, 4);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    public void shouldLimitCacheSizeWithLargerTest() {
        final int max = 10000;
        final LeastRecentlyUsed<String, String> cache = new LeastRecentlyUsed<String, String>(max);
        for (int ii = 0; ii < max * 5; ii++) {
            cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        assertThat(cache.size()).isEqualTo(max);
    }

    @Test
    public void shouldKeepMostRecentEntry() {
        final LeastRecentlyUsed<Integer, Integer> cache = new LeastRecentlyUsed<Integer, Integer>(1);
        cache.put(1, 2);
        cache.put(3, 4);
        assertThat(cache.get(3)).isEqualTo(4);
        assertThat(cache.get(1)).isNull();
    }

    @Test
    public void getMaxCacheSize() {
        final int max = 100;
        final LeastRecentlyUsed<String, Integer> cache = new LeastRecentlyUsed<String, Integer>(max);
        final int actualMax = cache.getMaxSize();
        assertThat(actualMax).isEqualTo(max);
    }
}