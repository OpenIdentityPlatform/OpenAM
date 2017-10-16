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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.blacklist;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.bloomfilter.BloomFilter;
import org.forgerock.bloomfilter.BloomFilters;
import org.forgerock.bloomfilter.ConcurrencyStrategy;
import org.forgerock.bloomfilter.ExpiryStrategy;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * A entry blacklist decorator implementation that uses a bloom filter to reduce the number of checks that need to
 * be performed against the underlying blacklist implementation. The advantage of a bloom filter is that it can store
 * very large blacklists (millions of entries) in memory, but with some possibility of false positives.
 *
 * @param <T> The blacklist type.
 */
public final class BloomFilterBlacklist<T extends Blacklistable> implements Blacklist<T> {
    private static final double FALSE_POSITIVE_PROBABILITY = 0.001d; // 0.1%
    private static final int NUM_EXPECTED_BLACKLISTED_ENTRIES = 10000;
    private static final int CAPACITY_GROWTH_FACTOR = 2;
    private static final double FALSE_POSITIVE_PROBABILITY_SCALE_FACTOR = 0.6d;

    private static final Debug DEBUG = Debug.getInstance("blacklist");

    private final Blacklist<T> delegate;
    private final long purgeDelayMs;
    private final BloomFilter<BlacklistEntry> bloomFilter;

    @VisibleForTesting
    BloomFilterBlacklist(Blacklist<T> delegate, long purgeDelayMs, final BloomFilter<BlacklistEntry> bloomFilter) {
        Reject.ifNull(delegate, bloomFilter);

        this.delegate = delegate;
        this.purgeDelayMs = purgeDelayMs;
        this.bloomFilter = bloomFilter;

        delegate.subscribe(new Listener() {
            @Override
            public void onBlacklisted(String id, long expiryTime) {
                DEBUG.message("BloomFilterBlacklist: Blacklisting entry from event: {}", id);
                bloomFilter.add(new BlacklistEntry(id, expiryTime));
            }
        });

    }

    /**
     * Creates the bloom filter entry blacklist using the given delegate blacklist to confirm membership, and the
     * given service configuration. If the bloom filter does not contain a given entry, then we know for definite
     * that it is not blacklisted. Otherwise, we delegate to the given entry blacklist to check if it actually is
     * blacklisted or not, to eliminate false positives.
     * <p/>
     * In order to ensure that the bloom filter is kept in-sync with the definitive blacklist (to avoid false
     * negatives), this implementation will subscribe to blacklist notifications from the delegate.
     *
     * @param delegate the definitive blacklist.
     * @param purgeDelayMs The purge delay in milli seconds.
     */
    public BloomFilterBlacklist(Blacklist<T> delegate, long purgeDelayMs) {
        this(delegate, purgeDelayMs, BloomFilters.create(EntryFunnel.INSTANCE)
                        .withFalsePositiveProbability(FALSE_POSITIVE_PROBABILITY)
                        .withInitialCapacity(NUM_EXPECTED_BLACKLISTED_ENTRIES)
                        .withExpiryStrategy(EntryExpirationStrategy.INSTANCE)
                        .withCapacityGrowthFactor(CAPACITY_GROWTH_FACTOR)
                        .withFalsePositiveProbabilityScaleFactor(FALSE_POSITIVE_PROBABILITY_SCALE_FACTOR)
                        .withConcurrencyStrategy(ConcurrencyStrategy.ATOMIC)
                        .build());
    }

    @Override
    public void blacklist(T entry) throws BlacklistException {
        // Just delegate - the event listener on the delegate will add the entry to the bloom filter
        delegate.blacklist(entry);
    }

    @Override
    public boolean isBlacklisted(T entry) throws BlacklistException {
        DEBUG.message("BloomFilterBlacklist: checking blacklist");
        boolean blacklisted = false;
        if (bloomFilter.mightContain(BlacklistEntry.from(entry, purgeDelayMs))) {
            blacklisted = delegate.isBlacklisted(entry);
        }
        return blacklisted;
    }

    @Override
    public void subscribe(Listener listener) {
        delegate.subscribe(listener);
    }

    /**
     * Adapter to allow entries to be stored in Guava bloom filters. Uses the UTF-8 encoded bytes of the
     * stable id of the entry as the key.
     */
    private enum EntryFunnel implements Funnel<BlacklistEntry> {
        INSTANCE;

        private static final Funnel<CharSequence> UTF8FUNNEL = Funnels.stringFunnel(Charset.forName("UTF-8"));

        @Override
        public void funnel(@Nonnull BlacklistEntry entry, @Nonnull PrimitiveSink primitiveSink) {
            UTF8FUNNEL.funnel(entry.stableId, primitiveSink);
        }
    }

    /**
     * Strategy to determine when a entry has expired and no longer needs to be stored in the bloom filter.
     */
    private enum EntryExpirationStrategy implements ExpiryStrategy<BlacklistEntry> {
        INSTANCE;

        @Override
        public long expiryTime(BlacklistEntry entry) {
            return entry.expiryTime;
        }
    }

    /**
     * Minimal information about an entry required for blacklisting in the bloom filter.
     */
    @VisibleForTesting
    public static final class BlacklistEntry {
        private final String stableId;
        private final long expiryTime;

        BlacklistEntry(final String stableId, final long expiryTime) {
            this.stableId = stableId;
            this.expiryTime = expiryTime;
        }

        static BlacklistEntry from(Blacklistable entry, long purgeDelayMs) throws BlacklistException {
            return new BlacklistEntry(entry.getStableStorageID(), entry.getBlacklistExpiryTime() + purgeDelayMs);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final BlacklistEntry that = (BlacklistEntry) o;
            return expiryTime == that.expiryTime && StringUtils.isEqualTo(stableId, that.stableId);
        }

        @Override
        public int hashCode() {
            int result = stableId != null ? stableId.hashCode() : 0;
            result = 31 * result + (int) (expiryTime ^ (expiryTime >>> 32));
            return result;
        }
    }
}
