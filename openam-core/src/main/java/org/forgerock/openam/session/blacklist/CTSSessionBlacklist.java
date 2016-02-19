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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.session.blacklist;

import static java.util.Locale.*;
import static java.util.TimeZone.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.query.QueryFilter.*;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.CoreTokenConstants;
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

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Session blacklist that stores blacklisted sessions in the CTS until they expire. A stable ID is stored in the CTS
 * for each session that has been blacklisted. Normal CTS reaper process will remove sessions from the blacklist
 * after they have expired.
 * <p/>
 * The fields used by this class are:
 * <ul>
 *     <li>{@link CoreTokenField#TOKEN_TYPE} - always {@link TokenType#SESSION_BLACKLIST}</li>
 *     <li>{@link CoreTokenField#TOKEN_ID} - the {@link Session#getStableStorageID()} (a random UUID that never
 *     changes for the lifetime of the session).</li>
 *     <li>{@link CoreTokenField#DATE_ONE} - the timestamp (millisecond precision) at which the session was first
 *     blacklisted. Used to discover newly blacklisted sessions from other servers.</li>
 *     <li>{@link CoreTokenField#STRING_ONE} - the ID of the <em>server</em> on which the session was first
 *     blacklisted. From {@link SessionServerConfig#getLocalServerID()}.</li>
 * </ul>
 * <p/>
 * In addition to blacklisting sessions and checking the blacklist, this class also periodically polls the CTS for
 * blacklist changes made on other servers since the last check. This is used to send local notifications to
 * subscribed blacklist {@link Listener}s for <em>all</em> blacklist entries, not just local ones. This feature is
 * essential for correct operation of the {@link BloomFilterSessionBlacklist}, which would otherwise report false
 * negatives.
 *
 * @since 13.0.0
 */
public final class CTSSessionBlacklist implements SessionBlacklist {
    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final AtomicLong lastPollTime = new AtomicLong(0);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<Listener>();

    /**
     * CTS field to store the time at which each session was blacklisted.
     */
    private static final CoreTokenField BLACKLIST_TIME_FIELD = CoreTokenField.DATE_ONE;
    private static final CoreTokenField SERVER_ID_FIELD = CoreTokenField.STRING_ONE;

    private final CTSPersistentStore cts;
    private final PollTask pollTask;
    private final ScheduledExecutorService scheduledExecutorService;
    private final String localServerId;
    private final long purgeDelayMs;

    @Inject
    public CTSSessionBlacklist(final CTSPersistentStore cts,
                               final @Named(CoreTokenConstants.CTS_SCHEDULED_SERVICE) ScheduledExecutorService scheduler,
                               final ThreadMonitor threadMonitor,
                               final SessionServerConfig serverConfig,
                               final SessionServiceConfig serviceConfig) {
        Reject.ifNull(cts, scheduler, threadMonitor);
        this.cts = cts;
        this.scheduledExecutorService = scheduler;
        this.pollTask = new PollTask(scheduler, threadMonitor, serviceConfig.getSessionBlacklistPollInterval(MILLISECONDS));
        this.localServerId = serverConfig.getLocalServerID();
        this.purgeDelayMs = serviceConfig.getSessionBlacklistPurgeDelay(MILLISECONDS);
    }

    @Override
    public void blacklist(final Session session) throws SessionException {
        DEBUG.message("CTSSessionBlacklist: Blacklisting session: {}", session);

        try {
            final Token token = new Token(session.getStableStorageID(), TokenType.SESSION_BLACKLIST);
            token.setExpiryTimestamp(timeOf(session.getBlacklistExpiryTime(purgeDelayMs)));
            token.setAttribute(BLACKLIST_TIME_FIELD, now());
            token.setAttribute(SERVER_ID_FIELD, localServerId);
            cts.create(token);
        } catch (CoreTokenException ex) {
            DEBUG.error("CTSSessionBlacklist: Error blacklisting session", ex);
            throw new SessionException(ex);
        }

        notifyListeners(session);
    }

    @Override
    public boolean isBlacklisted(final Session session) throws SessionException {
        try {
            return cts.read(session.getStableStorageID()) != null;
        } catch (CoreTokenException ex) {
            DEBUG.error("CTSSessionBlacklist: error checking blacklist", ex);
            throw new SessionException(ex);
        }
    }

    @Override
    public void subscribe(final Listener listener) {
        pollTask.start();
        Reject.ifNull(listener);
        listeners.add(listener);

        // Schedule a task to update the listener with the current session blacklist
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                // Replay the existing blacklisted sessions for the listener
                for (PartialToken token : findSessionsBlacklistedSince(0)) {
                    listener.onBlacklisted(token.<String>getValue(CoreTokenField.TOKEN_ID),
                            token.<Calendar>getValue(CoreTokenField.EXPIRY_DATE).getTimeInMillis());
                }
            }
        });
    }

    private Calendar now() {
        return getCalendarInstance(getTimeZone("UTC"), ROOT);
    }

    private Calendar timeOf(final long utcMillis) {
        final Calendar calendar = now();
        calendar.setTimeInMillis(utcMillis);
        return calendar;
    }

    private void notifyListeners(Session session) throws SessionException {
        notifyListeners(session.getStableStorageID(), session.getBlacklistExpiryTime(purgeDelayMs));
    }

    private void notifyListeners(String stableId, long expiryTime) {
        for (Listener listener : listeners) {
            listener.onBlacklisted(stableId, expiryTime);
        }
    }

    private Collection<PartialToken> findSessionsBlacklistedSince(final long lastPollTime) {
        // Search for blacklist tokens that have been added since our last poll time, but not from this server (those
        // will already have been notified directly from the blacklist() method).
        final TokenFilter filter = new TokenFilterBuilder()
                .withQuery(and(equalTo(CoreTokenField.TOKEN_TYPE, TokenType.SESSION_BLACKLIST),
                                greaterThanOrEqualTo(BLACKLIST_TIME_FIELD, timeOf(lastPollTime)),
                                not(equalTo(SERVER_ID_FIELD, localServerId))))
                .returnAttribute(CoreTokenField.TOKEN_ID)
                .returnAttribute(CoreTokenField.EXPIRY_DATE)
                .build();

        try {
            return cts.attributeQuery(filter);
        } catch (CoreTokenException e) {
            DEBUG.error("CTSSessionBlacklist: CTS failure while polling session blacklist: {}", e, e);
            return Collections.emptySet();
        }
    }

    /**
     * Periodic task that checks for newly black-listed sessions.
     */
    private final class PollTask implements Runnable {
        private final ScheduledExecutorService scheduledExecutorService;
        private final ThreadMonitor threadMonitor;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final long pollIntervalMs;

        PollTask(final ScheduledExecutorService scheduledExecutorService, final ThreadMonitor threadMonitor,
                 final long pollIntervalMs) {
            this.scheduledExecutorService = scheduledExecutorService;
            this.threadMonitor = threadMonitor;
            this.pollIntervalMs = pollIntervalMs;
        }

        void start() {
            if (running.compareAndSet(false, true)) {
                DEBUG.message("CTSSessionBlacklist: starting poll thread");
                threadMonitor.watchScheduledThread(scheduledExecutorService, this, pollIntervalMs, pollIntervalMs,
                        MILLISECONDS);
            }
        }

        @Override
        public void run() {
            DEBUG.message("CTSSessionBlacklist: polling for new blacklisted sessions");
            final Collection<PartialToken> results =
                    findSessionsBlacklistedSince(lastPollTime.getAndSet(currentTimeMillis()));
            if (results != null) {
                DEBUG.message("CTSSessionBlacklist: Processing {} session blacklist notifications", results.size());
                for (PartialToken token : results) {
                    notifyListeners(token.<String>getValue(CoreTokenField.TOKEN_ID),
                            TimeUtils.toUnixTime(token.<Calendar>getValue(CoreTokenField.EXPIRY_DATE)));
                }
            }

        }
    }

}
