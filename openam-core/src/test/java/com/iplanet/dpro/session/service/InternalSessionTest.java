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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.SessionUtilsWrapper;
import com.sun.identity.shared.debug.Debug;

public class InternalSessionTest {

    @Mock private SessionService mockSessionService;
    @Mock private SessionServiceConfig mockSessionServiceConfig;
    @Mock private InternalSessionEventBroker mockInternalSessionEventBroker;
    @Mock private SessionUtilsWrapper mockSessionUtils;
    @Mock private SessionConstraint sessionConstraint;
    @Mock private Debug mockDebug;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
        FrozenTimeService.INSTANCE.setCurrentTimeMillis(0);
    }

    @AfterMethod
    public void tearDown() {
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
    }

    // Session events

    @Test
    public void firesInternalSessionEventWhenAttemptToSetProtectedPropertyIsBlocked() throws Exception {
        // Given
        final InternalSession session = createSession();
        final SSOToken clientToken = mock(SSOToken.class);
        doThrow(SessionException.class)
                .when(mockSessionUtils).checkPermissionToSetProperty(eq(clientToken), eq("key"), eq("value"));

        // When
        try {
            session.putExternalProperty(clientToken, "key", "value");
        } catch (SessionException se) {
            // expected
        }

        // Then
        verifyEvent(session, SessionEventType.PROTECTED_PROPERTY);
    }

    @Test
    public void firesInternalSessionEventWhenProtectedPropertyIsSet() throws Exception {
        // Given
        final InternalSession session = createSession();
        final SSOToken clientToken = mock(SSOToken.class);
        doNothing().when(mockSessionUtils).checkPermissionToSetProperty(eq(clientToken), eq("key"), eq("value"));
        session.setState(SessionState.VALID);
        given(mockSessionServiceConfig.isSendPropertyNotification(eq("key"))).willReturn(true);

        // When
        session.putExternalProperty(clientToken, "key", "value");

        // Then
        verifyEvent(session, SessionEventType.PROPERTY_CHANGED);
    }

    @Test
    public void firesInternalSessionEventWhenActivatingSessionAndUserSessionQuotaExhausted() throws Exception {
        // Given
        final InternalSession session = createSession();
        given(mockSessionServiceConfig.isSessionConstraintEnabled()).willReturn(true);
        given(mockSessionService.isSuperUser(anyString())).willReturn(false);
        given(sessionConstraint.checkQuotaAndPerformAction(eq(session))).willReturn(true);

        // When
        boolean activated = session.activate("userDN");

        // Then
        assertThat(activated).as("session should not be activated").isEqualTo(false);
        verifyEvent(session, SessionEventType.QUOTA_EXHAUSTED);
    }

    @Test
    public void firesInternalSessionEventWhenActivatingSession() throws Exception {
        // Given
        final InternalSession session = createSession();
        given(mockSessionServiceConfig.isSessionConstraintEnabled()).willReturn(false);

        // When
        boolean activated = session.activate("userDN");

        // Then
        assertThat(activated).as("session should be activated").isEqualTo(true);
        verifyEvent(session, SessionEventType.SESSION_CREATION);
    }

    // State management

    @Test
    public void shouldSetTimedOutStateAndProperty() throws Exception {
        // Given
        final InternalSession session = createSession();

        // When
        session.setTimedOutTime(SECONDS.toMillis(99));

        // Then
        assertThat(session.isTimedOut()).as("session should be timed out").isTrue();
        assertThat(session.getProperty("SessionTimedOut"))
                .as("session should expose SessionTimedOut as property").isEqualTo("99");
    }

    // Timeout calculations

    @Test
    public void shouldSetDefaultTimeoutsWhenConstructed() {
        // Given

        // When
        final InternalSession session = createSession();

        // Then
        assertThat(session.getMaxSessionTime()).isEqualTo(3);
        assertThat(session.getMaxIdleTime()).isEqualTo(3);
        assertThat(session.getExpirationTime(SECONDS)).isEqualTo(180);
        assertThat(session.getTimeLeft()).isEqualTo(180);
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
    }

    @Test
    public void expirationTimeShouldNotExceedMaxSessionTime() {
        // Given
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

    private void verifyEvent(InternalSession session, SessionEventType eventType) {
        ArgumentCaptor<InternalSessionEvent> eventCaptor = ArgumentCaptor.forClass(InternalSessionEvent.class);
        verify(mockInternalSessionEventBroker, times(1)).onEvent(eventCaptor.capture());
        InternalSessionEvent event = eventCaptor.getValue();
        assertThat(event.getInternalSession()).isSameAs(session);
        assertThat(event.getType()).isEqualTo(eventType);
    }

    private InternalSession createSession() {
        final SessionID sessionID = new SessionID("");
        return new InternalSession(
                sessionID,
                mockSessionService,
                mockSessionServiceConfig,
                mockInternalSessionEventBroker,
                mockSessionUtils,
                sessionConstraint,
                mockDebug);
    }

}