package org.forgerock.openam.utils.collections;

import static org.fest.assertions.Assertions.assertThat;

import java.util.UUID;

import org.testng.annotations.Test;

public class LeastRecentlyUsedTest{
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