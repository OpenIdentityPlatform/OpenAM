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

package org.forgerock.openam.session.blacklist;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.bloomfilter.BloomFilter;
import org.forgerock.bloomfilter.BloomFilters;
import org.forgerock.bloomfilter.ConcurrencyStrategy;
import org.forgerock.bloomfilter.ExpiryStrategy;
import org.forgerock.guava.common.hash.Funnel;
import org.forgerock.guava.common.hash.Funnels;
import org.forgerock.guava.common.hash.PrimitiveSink;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * A session blacklist decorator implementation that uses a bloom filter to reduce the number of checks that need to
 * be performed against the underlying blacklist implementation. The advantage of a bloom filter is that it can store
 * very large blacklists (millions of entries) in memory, but with some possibility of false positives.
 */
public final class BloomFilterSessionBlacklist implements SessionBlacklist {
    private static final double FALSE_POSITIVE_PROBABILITY = 0.001d; // 0.1%
    private static final int NUM_EXPECTED_BLACKLISTED_SESSIONS = 10000;
    private static final int CAPACITY_GROWTH_FACTOR = 2;
    private static final double FALSE_POSITIVE_PROBABILITY_SCALE_FACTOR = 0.6d;

    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final SessionBlacklist delegate;
    private final BloomFilter<SessionBlacklistEntry> bloomFilter;
    private final SessionServiceConfig serviceConfig;

    @VisibleForTesting
    BloomFilterSessionBlacklist(final SessionBlacklist delegate,
                                final SessionServiceConfig serviceConfig,
                                final BloomFilter<SessionBlacklistEntry> bloomFilter) {
        Reject.ifNull(delegate, serviceConfig, bloomFilter);

        this.delegate = delegate;
        this.bloomFilter = bloomFilter;
        this.serviceConfig = serviceConfig;

        delegate.subscribe(new Listener() {
            @Override
            public void onBlacklisted(final String id, final long expiryTime) {
                DEBUG.message("BloomFilterSessionBlacklist: Blacklisting session from event: {}", id);
                bloomFilter.add(new SessionBlacklistEntry(id, expiryTime));
            }
        });

    }

    /**
     * Creates the bloom filter session blacklist using the given delegate blacklist to confirm membership, and the
     * given service configuration. If the bloom filter does not contain a given session, then we know for definite
     * that it is not blacklisted. Otherwise, we delegate to the given session blacklist to check if it actually is
     * blacklisted or not, to eliminate false positives.
     * <p/>
     * In order to ensure that the bloom filter is kept in-sync with the definitive blacklist (to avoid false
     * negatives), this implementation will subscribe to blacklist notifications from the delegate.
     *
     * @param delegate the definitive blacklist.
     * @param serviceConfig the session service configuration to get blacklist settings from.
     */
    public BloomFilterSessionBlacklist(final SessionBlacklist delegate,
                                       final SessionServiceConfig serviceConfig) {
        this(delegate, serviceConfig, BloomFilters.<SessionBlacklistEntry>create(SessionFunnel.INSTANCE)
                        .withFalsePositiveProbability(FALSE_POSITIVE_PROBABILITY)
                        .withInitialCapacity(NUM_EXPECTED_BLACKLISTED_SESSIONS)
                        .withExpiryStrategy(SessionExpirationStrategy.INSTANCE)
                        .withCapacityGrowthFactor(CAPACITY_GROWTH_FACTOR)
                        .withFalsePositiveProbabilityScaleFactor(FALSE_POSITIVE_PROBABILITY_SCALE_FACTOR)
                        .withConcurrencyStrategy(ConcurrencyStrategy.ATOMIC)
                        .build());
    }

    @Override
    public void blacklist(final Session session) throws SessionException {
        // Just delegate - the event listener on the delegate will add the session to the bloom filter
        delegate.blacklist(session);
    }

    @Override
    public boolean isBlacklisted(final Session session) throws SessionException {
        DEBUG.message("BloomFilterSessionBlacklist: checking blacklist");
        boolean blacklisted = false;
        if (bloomFilter.mightContain(SessionBlacklistEntry.from(session,
                serviceConfig.getSessionBlacklistPurgeDelay(TimeUnit.MILLISECONDS)))) {
            blacklisted = delegate.isBlacklisted(session);
        }
        return blacklisted;
    }

    @Override
    public void subscribe(final Listener listener) {
        delegate.subscribe(listener);
    }

    /**
     * Adapter to allow session objects to be stored in Guava bloom filters. Uses the UTF-8 encoded bytes of the
     * stable id of the session as the key.
     */
    private enum SessionFunnel implements Funnel<SessionBlacklistEntry> {
        INSTANCE;

        private static final Funnel<CharSequence> UTF8FUNNEL = Funnels.stringFunnel(Charset.forName("UTF-8"));

        @Override
        public void funnel(final @Nonnull SessionBlacklistEntry session, final @Nonnull PrimitiveSink primitiveSink) {
            UTF8FUNNEL.funnel(session.stableId, primitiveSink);
        }
    }

    /**
     * Strategy to determine when a session has expired and no longer needs to be stored in the bloom filter.
     */
    private enum SessionExpirationStrategy implements ExpiryStrategy<SessionBlacklistEntry> {
        INSTANCE;

        @Override
        public long expiryTime(final SessionBlacklistEntry session) {
            return session.expiryTime;
        }
    }

    /**
     * Minimal information about a session required for blacklisting in the bloom filter.
     */
    @VisibleForTesting
    static final class SessionBlacklistEntry {
        private final String stableId;
        private final long expiryTime;

        SessionBlacklistEntry(final String stableId, final long expiryTime) {
            this.stableId = stableId;
            this.expiryTime = expiryTime;
        }

        static SessionBlacklistEntry from(Session session, long purgeDelayMs) throws SessionException {
            return new SessionBlacklistEntry(session.getStableStorageID(), session.getBlacklistExpiryTime(purgeDelayMs));
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SessionBlacklistEntry that = (SessionBlacklistEntry) o;
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
