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
 * Portions Copyrighted 2022-2025 3A Systems, LLC.
 */

package org.forgerock.openam.blacklist;

import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.getCalendarInstance;
import static org.forgerock.util.query.QueryFilter.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.util.Reject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;

/**
 * Entry blacklist that stores blacklisted entries in the CTS until they expire. A stable ID is stored in the CTS
 * for each entry that has been blacklisted. Normal CTS reaper process will remove entries from the blacklist
 * after they have expired.
 * <p/>
 * The fields used by this class are:
 * <ul>
 *     <li>{@link CoreTokenField#TOKEN_TYPE} - the CTS token type.</li>
 *     <li>{@link CoreTokenField#TOKEN_ID} - the {@link Blacklistable#getStableStorageID()} (a random UUID that never
 *     changes for the lifetime of the entry).</li>
 *     <li>{@link CoreTokenField#DATE_ONE} - the timestamp (millisecond precision) at which the entry was first
 *     blacklisted. Used to discover newly blacklisted entries from other servers.</li>
 *     <li>{@link CoreTokenField#STRING_ONE} - the ID of the <em>server</em> on which the entry was first
 *     blacklisted. From {@link WebtopNamingQuery#getAMServerID()}.</li>
 * </ul>
 * <p/>
 * In addition to blacklisting entries and checking the blacklist, this class also periodically polls the CTS for
 * blacklist changes made on other servers since the last check. This is used to send local notifications to
 * subscribed blacklist {@link Listener}s for <em>all</em> blacklist entries, not just local ones. This feature is
 * essential for correct operation of the {@link BloomFilterBlacklist}, which would otherwise report false
 * negatives.
 *
 * @param <T> The blacklist type.
 * @since 13.0.0
 */
public final class CTSBlacklist<T extends Blacklistable> implements Blacklist<T> {
    private static final Debug DEBUG = Debug.getInstance("blacklist");

    private final AtomicLong lastPollTime = new AtomicLong(0);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    /**
     * CTS field to store the time at which each entry was blacklisted.
     */
    private static final CoreTokenField BLACKLIST_TIME_FIELD = CoreTokenField.DATE_ONE;
    private static final CoreTokenField SERVER_ID_FIELD = CoreTokenField.STRING_ONE;

    private final CTSPersistentStore cts;
    private final TokenType tokenType;
    private final PollTask pollTask;
    private final ScheduledExecutorService scheduledExecutorService;
    private final String localServerId;
    private final long purgeDelayMs;

    public CTSBlacklist(CTSPersistentStore cts, TokenType tokenType, ScheduledExecutorService scheduler,
            ThreadMonitor threadMonitor, WebtopNamingQuery serverConfig, long purgeDelayMs, long pollIntervalMs) {
        Reject.ifNull(cts, tokenType, scheduler, threadMonitor);
        this.cts = cts;
        this.tokenType = tokenType;
        this.scheduledExecutorService = scheduler;
        this.pollTask = new PollTask(scheduler, threadMonitor, pollIntervalMs);
        this.purgeDelayMs = purgeDelayMs;
        String localServerId;
        try {
            localServerId = serverConfig.getAMServerID();
        } catch (ServerEntryNotFoundException ignored) {
            //This can never happen as we are looking up the local server ID.
            localServerId = "";
        }
        this.localServerId = localServerId;
    }

    @Override
    public void blacklist(T entry) throws BlacklistException {
        DEBUG.message("CTSBlacklist: Blacklisting entry: {}", entry);

        try {
            final Token token = new Token(entry.getStableStorageID(), tokenType);
            token.setExpiryTimestamp(timeOf(entry.getBlacklistExpiryTime() + purgeDelayMs));
            token.setAttribute(BLACKLIST_TIME_FIELD, now());
            token.setAttribute(SERVER_ID_FIELD, localServerId);
            cts.create(token);
        } catch (CoreTokenException ex) {
            DEBUG.error("CTSBlacklist: Error blacklisting entry", ex);
            throw new BlacklistException(ex);
        }

        notifyListeners(entry);
    }

    @Override
    public boolean isBlacklisted(T entry) throws BlacklistException {
        try {
            return cts.read(entry.getStableStorageID()) != null;
        } catch (CoreTokenException ex) {
            DEBUG.error("CTSBlacklist: error checking blacklist", ex);
            throw new BlacklistException(ex);
        }
    }

    @Override
    public void subscribe(final Listener listener) {
    	if (StringUtils.startsWith(SystemProperties.get("org.forgerock.openam.sm.datalayer.module.CTS_ASYNC"),"org.openidentityplatform.openam.cassandra")){
    		DEBUG.message("CTSBlacklist: Blacklisting exclude by: {}", SystemProperties.get("org.forgerock.openam.sm.datalayer.module.CTS_ASYNC"));
    		return;
    	}
        pollTask.start();
        Reject.ifNull(listener);
        listeners.add(listener);

        // Schedule a task to update the listener with the current entry blacklist
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                // Replay the existing blacklisted entries for the listener
                for (PartialToken token : findEntriesBlacklistedSince(0)) {
                    listener.onBlacklisted(token.<String>getValue(CoreTokenField.TOKEN_ID),
                            token.<Calendar>getValue(CoreTokenField.EXPIRY_DATE).getTimeInMillis());
                }
            }
        });
    }

    private Calendar now() {
        return getCalendarInstance(TimeUtils.UTC, ROOT);
    }

    private Calendar timeOf(long utcMillis) {
        final Calendar calendar = now();
        calendar.setTimeInMillis(utcMillis);
        return calendar;
    }

    private void notifyListeners(T entry) throws BlacklistException {
        notifyListeners(entry.getStableStorageID(), entry.getBlacklistExpiryTime() + purgeDelayMs);
    }

    private void notifyListeners(String stableId, long expiryTime) {
        for (Listener listener : listeners) {
            listener.onBlacklisted(stableId, expiryTime);
        }
    }

    private Collection<PartialToken> findEntriesBlacklistedSince(long lastPollTime) {
        // Search for blacklist tokens that have been added since our last poll time, but not from this server (those
        // will already have been notified directly from the blacklist() method).
        final TokenFilter filter = new TokenFilterBuilder()
                .withQuery(and(equalTo(CoreTokenField.TOKEN_TYPE, tokenType),
                                greaterThanOrEqualTo(BLACKLIST_TIME_FIELD, timeOf(lastPollTime)),
                                not(equalTo(SERVER_ID_FIELD, localServerId))))
                .returnAttribute(CoreTokenField.TOKEN_ID)
                .returnAttribute(CoreTokenField.EXPIRY_DATE)
                .build();

        try {
            return cts.attributeQuery(filter);
        } catch (CoreTokenException e) {
            DEBUG.error("CTSBlacklist: CTS failure while polling entry blacklist: {}", e, e);
            return Collections.emptySet();
        }
    }

    /**
     * Periodic task that checks for newly black-listed entries.
     */
    private final class PollTask implements Runnable {
        private final ScheduledExecutorService scheduledExecutorService;
        private final ThreadMonitor threadMonitor;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final long pollIntervalMs;

        PollTask(ScheduledExecutorService scheduledExecutorService, ThreadMonitor threadMonitor, long pollIntervalMs) {
            this.scheduledExecutorService = scheduledExecutorService;
            this.threadMonitor = threadMonitor;
            this.pollIntervalMs = pollIntervalMs;
        }

        void start() {
            if (running.compareAndSet(false, true)) {
                DEBUG.message("CTSBlacklist: starting poll thread");
                threadMonitor.watchScheduledThread(scheduledExecutorService, this, pollIntervalMs, pollIntervalMs,
                        MILLISECONDS);
            }
        }

        @Override
        public void run() {
            DEBUG.message("CTSBlacklist: polling for new blacklisted entries");
            Collection<PartialToken> results =
                    findEntriesBlacklistedSince(lastPollTime.getAndSet(currentTimeMillis()));
            if (results != null) {
                DEBUG.message("CTSBlacklist: Processing {} entry blacklist notifications", results.size());
                for (PartialToken token : results) {
                    notifyListeners(token.<String>getValue(CoreTokenField.TOKEN_ID),
                            TimeUtils.toUnixTime(token.<Calendar>getValue(CoreTokenField.EXPIRY_DATE)));
                }
            }
        }
    }
}
