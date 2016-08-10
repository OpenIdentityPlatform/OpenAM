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

package com.iplanet.dpro.session.service;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;

import org.forgerock.openam.session.SessionMeta;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

public class InternalSessionTest {

    @Mock private SessionService mockSessionService;
    @Mock private SessionServiceConfig mockSessionServiceConfig;
    @Mock private SessionLogging mockSessionLogging;
    @Mock private SessionAuditor mockSessionAuditor;
    @Mock private Debug mockDebug;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
        FrozenTimeService.INSTANCE.setCurrentTimeMillis(0);
    }

    @AfterMethod
    public void tearDown() {
        InternalSession.setPurgeDelayInSeconds(SessionMeta.getPurgeDelay(SECONDS));
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
    }

    @Test
    public void shouldSetDefaultTimeoutsWhenConstructed() {
        // Give

        // When
        final InternalSession session = createSession();

        // Then
        assertThat(session.getMaxSessionTime()).isEqualTo(3);
        assertThat(session.getMaxIdleTime()).isEqualTo(3);
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(180);
        assertThat(session.getTimeLeft()).isEqualTo(180);
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(-1);
    }

    @Test
    public void shouldAllowTimeoutsToBeConfigured() {
        // Given
        final InternalSession session = createSession();

        // When
        session.setMaxSessionTime(30);
        session.setMaxIdleTime(5);

        // Then
        assertThat(session.getMaxSessionTime()).isEqualTo(30);
        assertThat(session.getMaxIdleTime()).isEqualTo(5);
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(300);
        assertThat(session.getTimeLeft()).isEqualTo(1800);
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(-1);
    }

    @Test
    public void shouldAdjustTimeRemainingBasedOnCurrentTime() {
        // Given
        final InternalSession session = createSession();

        // When
        FrozenTimeService.INSTANCE.fastForward(1, SECONDS);

        // Then
        assertThat(session.getIdleTime()).isEqualTo(1);
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(180);
        assertThat(session.getTimeLeft()).isEqualTo(179);
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(-1);
    }

    @Test
    public void shouldAdjustTimeRemainingWhenLatestAccessTimeIsUpdated() {
        // Given
        final InternalSession session = createSession();

        // When
        FrozenTimeService.INSTANCE.fastForward(1, SECONDS);
        session.setLatestAccessTime();
        FrozenTimeService.INSTANCE.fastForward(1, SECONDS);

        // Then
        assertThat(session.getIdleTime()).isEqualTo(1);
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(180);
        assertThat(session.getTimeLeft()).isEqualTo(178);
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(-1);
    }

    @Test
    public void shouldAddPurgeDelayToExpirationTime() {
        // Given
        InternalSession.setPurgeDelayInSeconds(MINUTES.toSeconds(10));
        final InternalSession session = createSession();
        session.setMaxSessionTime(30);
        session.setMaxIdleTime(5);

        // When
        FrozenTimeService.INSTANCE.fastForward(6, MINUTES);
        session.setTimedOutAt(MINUTES.toSeconds(5)); // simulate time-out after max idle time expires

        // Then
        assertThat(session.isTimedOut()).isTrue();
        assertThat(session.getIdleTime()).isEqualTo(MINUTES.toSeconds(6));
        long expectedExpirationTimeInSeconds = MILLISECONDS.toSeconds(FrozenTimeService.INSTANCE.plus(9, MINUTES));
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(expectedExpirationTimeInSeconds);
        assertThat(session.getTimeLeft()).isEqualTo(MINUTES.toSeconds(24));
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(MINUTES.toSeconds(9));
    }

    @Test
    public void expirationTimeShouldNotExceedMaxSessionTime() {
        // Given
        InternalSession.setPurgeDelayInSeconds(MINUTES.toSeconds(10));
        final InternalSession session = createSession();
        session.setMaxSessionTime(30);
        session.setMaxIdleTime(5);

        // When
        FrozenTimeService.INSTANCE.fastForward(26, MINUTES);
        session.setLatestAccessTime();

        // Then
        assertThat(session.isTimedOut()).isFalse();
        assertThat(session.getIdleTime()).isEqualTo(MINUTES.toSeconds(0));
        long expectedExpirationTimeInSeconds = MILLISECONDS.toSeconds(FrozenTimeService.INSTANCE.plus(4, MINUTES));
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(expectedExpirationTimeInSeconds);
        assertThat(session.getTimeLeft()).isEqualTo(MINUTES.toSeconds(4));
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(-1);
    }

    @Test
    public void calculatesMaxSessionExpirationTime() {
        // Given a session created at "10 minutes" with a max session time of 30 minutes...
        FrozenTimeService.INSTANCE.fastForward(10, MINUTES);
        final InternalSession session = createSession();
        session.setMaxSessionTime(30);

        // Then max session expiration time should occur at "40 minutes"
        assertThat(session.getMaxSessionExpirationTime(MINUTES)).isEqualTo(40);
    }

    @Test
    public void calculatesMaxIdleExpirationTime() {
        // Given a session created at "10 minutes" with a max idle time of 5 minutes...
        FrozenTimeService.INSTANCE.fastForward(10, MINUTES);
        final InternalSession session = createSession();
        session.setMaxIdleTime(5);

        // When current time is "16 minutes", and the session has not been idle for any time...
        FrozenTimeService.INSTANCE.fastForward(6, MINUTES);
        session.setLatestAccessTime();

        // Then idle expiration time should occur at "21 minutes"
        assertThat(session.getMaxIdleExpirationTime(MINUTES)).isEqualTo(21);
    }

    @Test
    public void calculatesPurgeDelayExpirationTime() {
        // Given a session created at "10 minutes" with a purge delay of 10 minutes and max idle time of 5 minutes...
        InternalSession.setPurgeDelayInSeconds(MINUTES.toSeconds(10));
        FrozenTimeService.INSTANCE.fastForward(10, MINUTES);
        final InternalSession session = createSession();
        session.setMaxIdleTime(5);

        // When current time is "16 minutes", and the session timed out 1 minute ago...
        FrozenTimeService.INSTANCE.fastForward(6, MINUTES);
        session.setTimedOutAt(MINUTES.toSeconds(15));

        // Then purge delay expiration should occur at time "25 minutes"
        assertThat(session.isTimedOut()).isTrue();
        assertThat(session.getPurgeDelayExpirationTime(MINUTES)).isEqualTo(25);
        assertThat(session.getTimeLeftBeforePurge()).isEqualTo(MINUTES.toSeconds(9));
    }

    private InternalSession createSession() {
        final SessionID sessionID = new SessionID("");
        return new InternalSession(
                sessionID,
                mockSessionService,
                mockSessionServiceConfig,
                mockSessionLogging,
                mockSessionAuditor,
                mockDebug);
    }

}