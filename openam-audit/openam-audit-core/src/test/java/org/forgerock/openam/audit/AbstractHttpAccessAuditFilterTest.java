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
package org.forgerock.openam.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.SESSION;
import static org.forgerock.openam.audit.AuditConstants.EventName;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_ACCESS_ATTEMPT;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_ACCESS_OUTCOME;
import static org.forgerock.openam.audit.AuditConstants.USER_ID;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AbstractHttpAccessAuditFilterTest {

    private final String realm = "testRealm";

    private MockAccessAuditFilter auditFilter;

    @Mock
    private AuditEventPublisher eventPublisher;
    @Mock
    private AuditServiceProvider auditServiceProvider;
    @Mock
    private AMAuditService auditService;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        when(auditServiceProvider.getDefaultAuditService()).thenReturn(auditService);
        when(auditServiceProvider.getAuditService(realm)).thenReturn(auditService);
        AuditEventFactory eventFactory = new AuditEventFactory();

        auditFilter = new MockAccessAuditFilter(eventPublisher, eventFactory);

        AuditRequestContext.putProperty(USER_ID, "USER_ID");
        AuditRequestContext.putProperty(SESSION.toString(), "value");
    }

    @DataProvider
    private Object[][] handlerResponses() {
        return new Object[][]{
            {Status.OK},
            {Status.BAD_REQUEST},
        };
    }

    @Test(dataProvider = "handlerResponses")
    public void shouldNotAuditIfAuditingIsNotEnabledForAccessTopic(Status responseStatus) throws AuditException {

        //Given
        Context context = new RequestAuditContext(mockContext());
        Request request = new Request()
                .setUri(URI.create("http://example.com"));

        disableAccessTopicAuditing();
        Handler handler = mockHandler(context, request, responseStatus);

        //When
        auditFilter.filter(context, request, handler);

        //Then
        verify(eventPublisher, never()).tryPublish(anyString(), any(AuditEvent.class));
    }

    @Test(dataProvider = "handlerResponses")
    public void shouldAuditAccessAttemptAndResult(Status responseStatus) throws AuditException {

        //Given
        Context context = new RequestAuditContext(mockContext());
        Request request = new Request()
                .setMethod("GET")
                .setUri(URI.create("http://example.com:8080?query=value"));
        request.getHeaders().put(ContentTypeHeader.valueOf("CONTENT_TYPE"));

        enableAccessTopicAuditing();
        Handler handler = mockHandler(context, request, responseStatus);

        //When
        auditFilter.filter(context, request, handler);

        //Then
        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher, times(2)).tryPublish(eq(AuditConstants.ACCESS_TOPIC), auditEventCaptor.capture());

        verifyAccessAttemptAuditEvent(auditEventCaptor.getAllValues().get(0).getValue());
        if (responseStatus.isSuccessful()) {
            verifyAccessSuccessAuditEvent(auditEventCaptor.getAllValues().get(1).getValue());
        } else {
            verifyAccessFailedAuditEvent(auditEventCaptor.getAllValues().get(1).getValue());
        }
    }

    @Test
    public void shouldGetUserIdForAccessAttemptIfNotSet() {

        //Given
        Request request = new Request();
        AuditRequestContext.putProperty(USER_ID, null);

        //When
        String userId = auditFilter.getUserIdForAccessAttempt(request);

        //Then
        assertThat(userId).isEmpty();
    }

    @Test
    public void shouldGetUserIdForAccessOutcomeIfNotSet() {

        //Given
        Response response = new Response();
        AuditRequestContext.putProperty(USER_ID, null);

        //When
        String userId = auditFilter.getUserIdForAccessOutcome(response);

        //Then
        assertThat(userId).isEmpty();
    }

    private Context mockContext() {
        return new AttributesContext(new SessionContext(
                ClientContext.buildExternalClientContext(new RootContext())
                        .certificates()
                        .userAgent("USER_AGENT")
                        .remoteAddress("REMOTE_ADDRESS")
                        .remotePort(9000)
                        .remoteUser("REMOTE_USER")
                        .build(),
                mock(Session.class)));
    }

    private void disableAccessTopicAuditing() {
        given(eventPublisher
                .isAuditing(eq(realm), anyString(), any(EventName.class)))
                .willReturn(true);
        given(eventPublisher
                .isAuditing(eq(realm), eq(AuditConstants.ACCESS_TOPIC), any(EventName.class)))
                .willReturn(false);
    }

    private void enableAccessTopicAuditing() {
        given(eventPublisher
                .isAuditing(eq(realm), anyString(), any(EventName.class)))
                .willReturn(false);
        given(eventPublisher
                .isAuditing(eq(realm), eq(AuditConstants.ACCESS_TOPIC), any(EventName.class)))
                .willReturn(true);
    }

    private Handler mockHandler(Context context, Request request, Status status) {
        Handler handler = mock(Handler.class);
        Promise<Response, NeverThrowsException> promise = newResultPromise(new Response(status));
        given(handler.handle(context, request)).willReturn(promise);
        return handler;
    }

    private void verifyAccessAttemptAuditEvent(JsonValue auditEvent) {
        verifyAccessAuditEvent(auditEvent);
        assertThat(auditEvent).stringAt("eventName").isEqualTo(AM_ACCESS_ATTEMPT.toString());
    }

    private void verifyAccessSuccessAuditEvent(JsonValue auditEvent) {
        verifyAccessAuditEvent(auditEvent);
        assertThat(auditEvent).stringAt("eventName").isEqualTo(AM_ACCESS_OUTCOME.toString());
        assertThat(auditEvent).stringAt("response/status").isEqualTo("SUCCESSFUL");
        assertThat(auditEvent).longAt("response/elapsedTime").isNotNull();
    }

    private void verifyAccessFailedAuditEvent(JsonValue auditEvent) {
        verifyAccessAuditEvent(auditEvent);
        assertThat(auditEvent).stringAt("eventName").isEqualTo(AM_ACCESS_OUTCOME.toString());
        assertThat(auditEvent).stringAt("response/status").isEqualTo("FAILED");
        assertThat(auditEvent).longAt("response/elapsedTime").isNotNull();
        assertThat(auditEvent).stringAt("response/detail/reason").isNotNull();
    }

    private void verifyAccessAuditEvent(JsonValue auditEvent) {
        assertThat(auditEvent).stringAt("timestamp").isNotNull();
        assertThat(auditEvent).stringAt("transactionId").isNotNull();
        assertThat(auditEvent).stringAt("component").isEqualTo(AUTHENTICATION.toString());
        assertThat(auditEvent).stringAt("userId").isEqualTo("USER_ID");
        assertThat(auditEvent).hasArray("trackingIds").contains("value");
        assertThat(auditEvent).stringAt("client/ip").isEqualTo("REMOTE_ADDRESS");
        assertThat(auditEvent).integerAt("client/port").isEqualTo(9000);
        assertThat(auditEvent).stringAt("http/request/method").isEqualTo("GET");
        assertThat(auditEvent).stringAt("http/request/path").isEqualTo("http://example.com:8080");
        assertThat(auditEvent).hasArray("http/request/queryParameters/query").contains("value");
        assertThat(auditEvent).hasArray("http/request/headers/" + ContentTypeHeader.NAME).contains("CONTENT_TYPE");
    }

    private class MockAccessAuditFilter extends AbstractHttpAccessAuditFilter {
        public MockAccessAuditFilter(AuditEventPublisher publisher, AuditEventFactory factory) {
            super(AUTHENTICATION, publisher, factory);
        }

        @Override
        protected String getRealm(Context context) {
            return realm;
        }
    }
}
