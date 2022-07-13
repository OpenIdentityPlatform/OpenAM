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

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.audit.AuditConstants.ACTIVITY_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.security.PrivilegedAction;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.services.TransactionId;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;

public class SessionAuditorTest {

    private static final long FAKE_TIME = 1478012083903L;
    private static final String FAKE_TIMESTAMP = "2016-11-01T14:54:43.903Z";
    private static final String FAKE_ORG_NAME = "ORG";
    private static final String FAKE_REALM_NAME = "REALM";
    private static final String FAKE_SESSION_CONTEXT_ID = "contextId";
    private static final String FAKE_AUDIT_TRANSACTION_ID = "transactionId";
    private static final String FAKE_UNIVERSAL_ID = "userId";

    private AuditEventPublisher auditEventPublisher;
    private AuditEventFactory auditEventFactory;
    private PrivilegedAction<SSOToken> adminTokenAction;
    private DNWrapper dnWrapper;
    private InternalSession session;

    private SessionAuditor sessionAuditor;

    @BeforeMethod
    public void setUp() {
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditEventFactory = new AuditEventFactory();
        adminTokenAction = mock(PrivilegedAction.class);
        dnWrapper = mock(DNWrapper.class);

        sessionAuditor = new SessionAuditor(auditEventPublisher, auditEventFactory, adminTokenAction, dnWrapper);

        session = mock(InternalSession.class);
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setClientDomain(FAKE_ORG_NAME);
        sessionInfo.getProperties().put(Constants.AM_CTX_ID, FAKE_SESSION_CONTEXT_ID);
        sessionInfo.getProperties().put(Constants.UNIVERSAL_IDENTIFIER, FAKE_UNIVERSAL_ID);
        given(session.toSessionInfo()).willReturn(sessionInfo);
        given(dnWrapper.orgNameToRealmName(FAKE_ORG_NAME)).willReturn(FAKE_REALM_NAME);
    }

    @Test
    public void shouldAddTransactionIdToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);
        AuditRequestContext.set(new AuditRequestContext(new TransactionId(FAKE_AUDIT_TRANSACTION_ID)));

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("transactionId").isEqualTo(FAKE_AUDIT_TRANSACTION_ID);
    }

    @Test
    public void shouldAddTimestampToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("timestamp").isEqualTo(FAKE_TIMESTAMP);
    }

    @Test
    public void shouldAddRealmNameToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("realm").isEqualTo(FAKE_REALM_NAME);
    }

    @Test
    public void shouldAddUserIdToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("userId").isEqualTo(FAKE_UNIVERSAL_ID);
    }

    @Test
    public void shouldAddComponentToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("component").isEqualTo("Session");
    }

    @Test
    public void shouldAddTrackingIdToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).hasArray("trackingIds").containsExactly(FAKE_SESSION_CONTEXT_ID);
    }

    @Test
    public void shouldAddObjectIdToAuditEvent() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("objectId").isEqualTo(FAKE_SESSION_CONTEXT_ID);
    }

    @Test
    public void shouldAuditSessionCreation() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.SESSION_CREATION);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-CREATED");
        assertThat(auditEvent).stringAt("operation").isEqualTo("CREATE");
    }

    @Test
    public void shouldAuditSessionIdleTimeOut() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_IDLE_TIMED_OUT)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.IDLE_TIMEOUT);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-IDLE_TIMED_OUT");
        assertThat(auditEvent).stringAt("operation").isEqualTo("DELETE");
    }

    @Test
    public void shouldAuditSessionMaxTimeOut() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_MAX_TIMED_OUT)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.MAX_TIMEOUT);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-MAX_TIMED_OUT");
        assertThat(auditEvent).stringAt("operation").isEqualTo("DELETE");
    }

    @Test
    public void shouldAuditSessionLogout() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_LOGGED_OUT)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.LOGOUT);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-LOGGED_OUT");
        assertThat(auditEvent).stringAt("operation").isEqualTo("DELETE");
    }

    @Test
    public void shouldAuditSessionDestroy() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_DESTROYED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.DESTROY);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-DESTROYED");
        assertThat(auditEvent).stringAt("operation").isEqualTo("DELETE");
    }

    @Test
    public void shouldAuditSessionPropertyChanged() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_PROPERTY_CHANGED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.PROPERTY_CHANGED);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-PROPERTY_CHANGED");
        assertThat(auditEvent).stringAt("operation").isEqualTo("UPDATE");
    }

    @Test
    public void shouldAuditSessionEventUrlAdded() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_EVENT_URL_ADDED)).willReturn(true);

        // When
        JsonValue auditEvent = fireSessionEvent(SessionEventType.EVENT_URL_ADDED);

        // Then
        assertThat(auditEvent).stringAt("eventName").isEqualTo("AM-SESSION-EVENT_URL_ADDED");
        assertThat(auditEvent).stringAt("operation").isEqualTo("UPDATE");
    }

    @Test
    public void shouldIgnoreQuotaExhaustedEvent() {
        // Given

        // When
        sessionAuditor.onEvent(new InternalSessionEvent(session, SessionEventType.QUOTA_EXHAUSTED,
                System.currentTimeMillis()));

        // Then
        verifyZeroInteractions(auditEventPublisher);
    }

    @Test
    public void shouldIgnoreProtectedPropertyEvent() {
        // Given

        // When
        sessionAuditor.onEvent(new InternalSessionEvent(session, SessionEventType.PROTECTED_PROPERTY,
                System.currentTimeMillis()));

        // Then
        verifyZeroInteractions(auditEventPublisher);
    }

    @Test
    public void shouldAllowAuditingToBeDisabled() {
        // Given
        given(auditEventPublisher.isAuditing(FAKE_REALM_NAME, ACTIVITY_TOPIC, AM_SESSION_CREATED)).willReturn(false);

        // When
        sessionAuditor.onEvent(new InternalSessionEvent(session, SessionEventType.SESSION_CREATION,
                System.currentTimeMillis()));

        // Then
        verify(auditEventPublisher, times(0)).tryPublish(any(String.class), any(AuditEvent.class));
    }

    private JsonValue fireSessionEvent(SessionEventType sessionEventType) {
        InternalSessionEvent event = new InternalSessionEvent(session, sessionEventType, FAKE_TIME);
        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        doNothing().when(auditEventPublisher).tryPublish(eq(ACTIVITY_TOPIC), auditEventCaptor.capture());

        sessionAuditor.onEvent(event);

        return auditEventCaptor.getValue().getValue();
    }

}
