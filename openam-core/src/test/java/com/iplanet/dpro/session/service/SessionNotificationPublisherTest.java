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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.NotificationsConfig;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.session.SessionEventType;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

public class SessionNotificationPublisherTest {

    private InternalSession session;
    private NotificationBroker notificationBroker;
    private NotificationsConfig notificationsConfig;

    private SessionNotificationPublisher sessionNotificationPublisher;

    @BeforeMethod
    public void setUp() {
        notificationBroker = mock(NotificationBroker.class);
        notificationsConfig = mock(NotificationsConfig.class);
        session = mock(InternalSession.class);
        sessionNotificationPublisher =
                new SessionNotificationPublisher(mock(Debug.class), notificationsConfig, notificationBroker);
    }

    @Test
    public void shouldPublishSessionNotificationForMasterSessionIdAndRestrictedSessionIds() {
        // Given
        given(session.getSessionID()).willReturn(new SessionID("masterSessionId"));
        given(session.getRestrictedTokens()).willReturn(restrictedTokens(
                "restrictedToken1", "restrictedToken2", "restrictedToken3"));

        // When
        List<JsonValue> notifications = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(notifications.get(0)).stringAt("tokenId").isEqualTo("masterSessionId");
        assertThat(notifications.get(1)).stringAt("tokenId").isEqualTo("restrictedToken1");
        assertThat(notifications.get(2)).stringAt("tokenId").isEqualTo("restrictedToken2");
        assertThat(notifications.get(3)).stringAt("tokenId").isEqualTo("restrictedToken3");
    }

    /**
     * Returns Set of SessionIDs which will be iterated in the order they were provided.
     * <p>
     * This allows notifications to be accessed by index.
     */
    private Set<SessionID> restrictedTokens(String... sessionIdStrings) {
        LinkedHashMap<SessionID, SessionID> restrictedTokens = new LinkedHashMap<>();
        for (final String sessionIdString : sessionIdStrings) {
            SessionID sessionId = new SessionID(sessionIdString);
            restrictedTokens.put(sessionId, sessionId);
        }
        return restrictedTokens.keySet();
    }

    @Test
    public void shouldSendNotificationForSessionCreation() {
        assertSendsNotificationForSessionEvent(SessionEventType.SESSION_CREATION);
    }

    @Test
    public void shouldSendNotificationForIdleTimeOut() {
        assertSendsNotificationForSessionEvent(SessionEventType.IDLE_TIMEOUT);
    }

    @Test
    public void shouldSendNotificationForMaxTimeOut() {
        assertSendsNotificationForSessionEvent(SessionEventType.MAX_TIMEOUT);
    }

    @Test
    public void shouldSendNotificationForLogout() {
        assertSendsNotificationForSessionEvent(SessionEventType.LOGOUT);
    }

    @Test
    public void shouldSendNotificationForDestroy() {
        assertSendsNotificationForSessionEvent(SessionEventType.DESTROY);
    }

    @Test
    public void shouldSendNotificationForPropertyChanged() {
        assertSendsNotificationForSessionEvent(SessionEventType.PROPERTY_CHANGED);
    }

    @Test
    public void shouldIgnoreQuotaExhaustedEvent() {
        assertIgnoresSessionEvent(SessionEventType.QUOTA_EXHAUSTED);
    }

    @Test
    public void shouldIgnoreProtectedPropertyEvent() {
        assertIgnoresSessionEvent(SessionEventType.PROTECTED_PROPERTY);
    }

    private void assertSendsNotificationForSessionEvent(SessionEventType sessionEventType) {
        // Given
        given(session.getSessionID()).willReturn(new SessionID("masterSessionId"));

        // When
        List<JsonValue> notifications = fireSessionEvent(sessionEventType);

        // Then
        SessionEventType eventType = (SessionEventType) notifications.get(0).get("eventType").getObject();
        assertThat(eventType).isEqualTo(sessionEventType);
    }

    private void assertIgnoresSessionEvent(SessionEventType sessionEventType) {
        // Given

        // When
        sessionNotificationPublisher.onEvent(new InternalSessionEvent(session, sessionEventType,
                System.currentTimeMillis()));

        // Then
        verifyZeroInteractions(notificationBroker);
    }

    private List<JsonValue> fireSessionEvent(SessionEventType sessionEventType) {
        given(notificationsConfig.isAgentsEnabled()).willReturn(true);
        ArgumentCaptor<JsonValue> notificationCaptor = ArgumentCaptor.forClass(JsonValue.class);
        given(notificationBroker.publish(eq(Topic.of("/agent/session")), notificationCaptor.capture())).willReturn(true);

        sessionNotificationPublisher.onEvent(new InternalSessionEvent(session, sessionEventType,
                System.currentTimeMillis()));

        return notificationCaptor.getAllValues();
    }

}
