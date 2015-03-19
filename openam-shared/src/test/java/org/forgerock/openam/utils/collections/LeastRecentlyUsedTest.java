package org.forgerock.openam.utils.collections;

import org.testng.annotations.Test;

import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class LeastRecentlyUsedTest{
    @Test
    public void shouldLimitCacheSize() {
        LeastRecentlyUsed<Integer, Integer> cache = new LeastRecentlyUsed<Integer, Integer>(1);
        cache.put(1, 2);
        cache.put(3, 4);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    public void shouldLimitCacheSizeWithLargerTest() {
        int max = 10000;
        LeastRecentlyUsed<String, String> cache = new LeastRecentlyUsed<String, String>(max);
        for (int ii = 0; ii < max * 5; ii++) {
            cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        assertThat(cache.size()).isEqualTo(max);
    }

    @Test
    public void shouldKeepMostRecentEntry() {
        LeastRecentlyUsed<Integer, Integer> cache = new LeastRecentlyUsed<Integer, Integer>(1);
        cache.put(1, 2);
        cache.put(3, 4);
        assertThat(cache.get(3)).isEqualTo(4);
        assertThat(cache.get(1)).isNull();
    }
}