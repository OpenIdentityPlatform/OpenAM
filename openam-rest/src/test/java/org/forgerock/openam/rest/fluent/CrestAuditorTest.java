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
package org.forgerock.openam.rest.fluent;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.rest.fluent.AuditTestUtils.*;
import static org.mockito.BDDMockito.*;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class CrestAuditorTest {

    private Debug debug;
    private AuditEventFactory auditEventFactory;
    private AuditEventPublisher auditEventPublisher;
    private Context context;
    private CrestAuditor auditor;

    @BeforeMethod
    public void setUp() throws Exception {
        debug = mock(Debug.class);
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditEventFactory = mockAuditEventFactory();
        context = mockAuditContext();
    }

    @DataProvider(name = "CRESTRequests")
    public Object[][] getRequests() {
        return new Object[][]{
                {newCreateRequest("mockResource", json(object()))},
                {newReadRequest("mockResource")},
                {newUpdateRequest("mockResource", json(object()))},
                {newDeleteRequest("mockResource")},
                {newPatchRequest("mockResource")},
                {newActionRequest("mockResource", "actionId")},
                {newQueryRequest("mockResource")}
        };
    }

    @Test(dataProvider = "CRESTRequests")
    public void auditAccessShouldPublishEvents(Request request) throws Exception {
        given(auditEventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).willReturn(true);
        auditor = new CrestAuditor(debug, auditEventPublisher, auditEventFactory, context, request);
        givenAccessAuditingEnabled(auditEventPublisher);

        auditor.auditAccessAttempt();

        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).tryPublish(eq(ACCESS_TOPIC), auditEventCaptor.capture());
        assertThat(getField(auditEventCaptor, EVENT_NAME).asString()).isEqualTo(EventName.AM_ACCESS_ATTEMPT.toString());
    }

    @Test(dataProvider = "CRESTRequests")
    public void auditSuccessShouldPublishEvents(Request request) throws Exception {
        given(auditEventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).willReturn(true);
        auditor = new CrestAuditor(debug, auditEventPublisher, auditEventFactory, context, request);
        givenAccessAuditingEnabled(auditEventPublisher);

        final JsonValue detail = json(object(field("foo", "bar")));
        auditor.auditAccessSuccess(detail);

        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).tryPublish(eq(ACCESS_TOPIC), auditEventCaptor.capture());
        assertThat(getField(auditEventCaptor, EVENT_NAME).asString()).isEqualTo(EventName.AM_ACCESS_OUTCOME.toString());
        assertThat(getField(auditEventCaptor, RESPONSE + "/" + DETAIL).asMap()).isEqualTo(detail.asMap());
    }

    @Test(dataProvider = "CRESTRequests")
    public void auditFailureShouldPublishEvents(Request request) throws Exception {
        given(auditEventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).willReturn(true);
        auditor = new CrestAuditor(debug, auditEventPublisher, auditEventFactory, context, request);
        givenAccessAuditingEnabled(auditEventPublisher);

        auditor.auditAccessFailure(500, "my bad");

        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).tryPublish(eq(ACCESS_TOPIC), auditEventCaptor.capture());
        assertThat(getField(auditEventCaptor, EVENT_NAME).asString()).isEqualTo(EventName.AM_ACCESS_OUTCOME.toString());
        assertThat(getField(auditEventCaptor, RESPONSE + "/" + STATUS_CODE).asString()).isEqualTo("500");
        assertThat(getField(auditEventCaptor, RESPONSE + "/" + DETAIL + "/" + ACCESS_RESPONSE_DETAIL_REASON).asString())
                .isEqualTo("my bad");
    }

    @Test
    public void publishesNoAuditEventsIfAccessAuditingDisabled() throws Exception {
        givenAccessAuditingDisabled(auditEventPublisher);

        auditor.auditAccessAttempt();
        auditor.auditAccessSuccess(json(object()));
        auditor.auditAccessFailure(500, "sorry");

        // Then
        verifyZeroInteractions(auditEventPublisher);
    }

    private void givenAccessAuditingEnabled(AuditEventPublisher auditEventPublisher) {
        given(auditEventPublisher.isAuditing(NO_REALM, ACCESS_TOPIC, null)).willReturn(true);
    }

    private void givenAccessAuditingDisabled(AuditEventPublisher auditEventPublisher) {
        given(auditEventPublisher.isAuditing(NO_REALM, ACCESS_TOPIC, null)).willReturn(false);
    }

    private JsonValue getField(ArgumentCaptor<AuditEvent> captor, String pointer) {
        return captor.getValue().getValue().get(new JsonPointer(pointer));
    }
}
