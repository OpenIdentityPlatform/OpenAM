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
 * Copyright 2013-2016 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations.strategies;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import java.util.concurrent.TimeUnit;

import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.session.service.access.SessionQueryManager;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.util.time.TimeService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionEvent;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.sun.identity.shared.debug.Debug;

public class LocalOperationsTest {

    private LocalOperations local;
    @Mock private Session mockRequester;
    @Mock private Session mockSession;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockInternalSession;
    @Mock private SessionAccessManager sessionAccessManager;
    @Mock private SessionInfoFactory sessionInfoFactory;
    @Mock private SessionServerConfig serverConfig;
    @Mock private SessionServiceConfig serviceConfig;
    @Mock private SessionCookies sessionCookies;
    @Mock private SessionChangeAuthorizer sessionChangeAuthorizer;
    @Mock private InternalSessionEventBroker internalSessionEventBroker;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TimeTravelUtil.setBackingTimeService(TimeTravelUtil.FrozenTimeService.INSTANCE);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(mockInternalSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getSessionID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);

        local = new LocalOperations(mock(Debug.class), sessionAccessManager, mock(SessionQueryManager.class),
                sessionInfoFactory, serverConfig, internalSessionEventBroker, sessionChangeAuthorizer);
    }

    @AfterMethod
    public void tearDown() {
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
    }

    @Test
    public void shouldNotSetLastAccessTimeWhenResetFlagIsFalse() throws SessionException {
        // Given
        boolean reset = false;
        // When
        local.refresh(mockSession, reset);
        // Then
        verify(mockInternalSession, times(0)).setLatestAccessTime();
    }

    @Test
    public void shouldSetLastAccessTimeWhenResetFlagIsTrue() throws SessionException {
        // Given
        boolean reset = true;
        // When
        local.refresh(mockSession, reset);
        // Then
        verify(mockInternalSession).setLatestAccessTime();
    }

    @Test
    public void shouldReturnSessionInfoOnRefresh() throws SessionException {
        // Given
        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(sessionInfoFactory.getSessionInfo(mockInternalSession, mockSessionID)).willReturn(mockSessionInfo);
        // When
        SessionInfo result = local.refresh(mockSession, true);
        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldRemoveSessionFromSessionAccessManagerOnLogout() throws Exception {
        // Given
        // When
        local.logout(mockSession);
        // Then
        verify(sessionAccessManager).removeInternalSession(mockInternalSession);
    }

    @Test
    public void firesInternalSessionEventWhenLoggingOutSession() throws Exception {
        // Given
        // When
        local.logout(mockSession);
        // Then
        verifyEvent(SessionEventType.LOGOUT);
    }

    @Test
    public void shouldRemoveSessionFromSessionAccessManagerOnDestroy() throws SessionException {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);
        // When
        local.destroy(mockRequester, mockSession);
        // Then
        verify(sessionAccessManager).removeSessionId(eq(mockSessionID));
    }

    @Test
    public void firesInternalSessionEventWhenDestroyingSession() throws Exception {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);
        // When
        local.destroy(mockRequester, mockSession);
        // Then
        verifyEvent(SessionEventType.DESTROY);
    }

    @Test
    public void shouldSetTimeoutStateOfInternalSessionOnSessionTimeout() throws Exception {
        // Given
        TimeTravelUtil.FrozenTimeService.INSTANCE.fastForward(10, MINUTES);
        // When
        local.timeout(mockInternalSession, SessionEventType.MAX_TIMEOUT);
        // Then
        verify(mockInternalSession).setTimedOutTime(TimeUnit.MINUTES.toMillis(10));
    }

    @DataProvider(name = "timeoutTypes")
    public Object[][] getTimeoutTypes() {
        return new Object[][] {
                { SessionEventType.MAX_TIMEOUT },
                { SessionEventType.IDLE_TIMEOUT }
        };
    }

    @Test(dataProvider = "timeoutTypes")
    public void firesInternalSessionEventsOnSessionTimeout(SessionEventType eventType) throws Exception {
        // Given
        // When
        local.timeout(mockInternalSession, eventType);
        // Then
        verify(mockInternalSession).setState(eq(SessionState.DESTROYED));
        verifyEvents(eventType, SessionEventType.DESTROY);
    }

    @Test(dataProvider = "timeoutTypes", expectedExceptions = IllegalStateException.class)
    public void willNotTimeoutSessionWhichIsStored(SessionEventType eventType) throws Exception {
        // Given
        given(mockInternalSession.isStored()).willReturn(true);
        // When
        local.timeout(mockInternalSession, eventType);
        // Then
        // expect exception
    }

    private void verifyEvent(SessionEventType eventType) {
        verifyEvents(eventType);
    }

    private void verifyEvents(SessionEventType... eventTypes) {
        ArgumentCaptor<InternalSessionEvent> eventCaptor = ArgumentCaptor.forClass(InternalSessionEvent.class);
        verify(internalSessionEventBroker, times(eventTypes.length)).onEvent(eventCaptor.capture());
        for (int i = 0; i < eventTypes.length; i++) {
            SessionEventType expectedEventType = eventTypes[i];
            InternalSessionEvent actualEvent = eventCaptor.getAllValues().get(i);
            assertThat(actualEvent.getType()).isEqualTo(expectedEventType);
            assertThat(actualEvent.getInternalSession()).isEqualTo(mockInternalSession);
            assertThat(actualEvent.getTime()).isEqualTo(TimeTravelUtil.FrozenTimeService.INSTANCE.now());
        }
    }

}
