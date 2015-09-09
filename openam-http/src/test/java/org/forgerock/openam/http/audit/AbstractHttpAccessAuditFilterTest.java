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

package org.forgerock.openam.http.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.Context.SESSION;
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
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.Session;
import org.forgerock.http.context.ClientContext;
import org.forgerock.http.context.AttributesContext;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.SessionContext;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AbstractHttpAccessAuditFilterTest {

    private MockAccessAuditFilter auditFilter;

    @Mock
    private AuditEventPublisher eventPublisher;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        AuditServiceConfigurator auditServiceConfigurator = mock(AuditServiceConfigurator.class);
        when(auditServiceConfigurator.getAuditServiceConfiguration()).thenReturn(new AMAuditServiceConfiguration());
        AuditEventFactory eventFactory = new AuditEventFactory(auditServiceConfigurator);

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
        Context context = mockContext();
        Request request = new Request()
                .setTime(System.currentTimeMillis())
                .setUri(URI.create("http://example.com"));

        disableAccessTopicAuditing();
        Handler handler = mockHandler(context, request, responseStatus);

        //When
        auditFilter.filter(context, request, handler);

        //Then
        verify(eventPublisher, never()).publish(anyString(), any(AuditEvent.class));
    }

    @Test
    public void shouldReturnInternalServerErrorResponseWhenAuditingFails() throws AuditException {

        //Given
        Context context = mockContext();
        Request request = new Request()
                .setTime(System.currentTimeMillis())
                .setUri(URI.create("http://example.com"));

        enableAccessTopicAuditing();
        Handler handler = mockHandler(context, request, Status.OK);

        AuditException auditException = mock(AuditException.class);
        doThrow(auditException).when(eventPublisher).publish(anyString(), any(AuditEvent.class));

        //When
        Promise<Response, NeverThrowsException> promise = auditFilter.filter(context, request, handler);

        //Then
        Response response = promise.getOrThrowUninterruptibly();
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        assertThat(response.getCause()).isSameAs(auditException);
    }

    @Test(dataProvider = "handlerResponses")
    public void shouldAuditAccessAttemptAndResult(Status responseStatus) throws AuditException {

        //Given
        Context context = mockContext();
        Request request = new Request()
                .setTime(System.currentTimeMillis())
                .setMethod("GET")
                .setUri(URI.create("http://example.com:8080?query=value"));
        request.getHeaders().putSingle(ContentTypeHeader.valueOf("CONTENT_TYPE"));

        enableAccessTopicAuditing();
        Handler handler = mockHandler(context, request, responseStatus);

        //When
        auditFilter.filter(context, request, handler);

        //Then
        ArgumentCaptor<AuditEvent> accessAttemptAuditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        ArgumentCaptor<AuditEvent> accessResultAuditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).publish(eq(AuditConstants.ACCESS_TOPIC), accessAttemptAuditEventCaptor.capture());
        verify(eventPublisher).tryPublish(eq(AuditConstants.ACCESS_TOPIC), accessResultAuditEventCaptor.capture());

        verifyAccessAttemptAuditEvent(accessAttemptAuditEventCaptor.getValue().getValue());
        if (responseStatus.isSuccessful()) {
            verifyAccessSuccessAuditEvent(accessResultAuditEventCaptor.getValue().getValue());
        } else {
            verifyAccessFailedAuditEvent(accessResultAuditEventCaptor.getValue().getValue());
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
                        .remoteHost("REMOTE_HOST")
                        .remotePort(9000)
                        .remoteUser("REMOTE_USER")
                        .build(),
                mock(Session.class)));
    }

    private void disableAccessTopicAuditing() {
        given(eventPublisher.isAuditing(anyString())).willReturn(true);
        given(eventPublisher.isAuditing(AuditConstants.ACCESS_TOPIC)).willReturn(false);
    }

    private void enableAccessTopicAuditing() {
        given(eventPublisher.isAuditing(anyString())).willReturn(false);
        given(eventPublisher.isAuditing(AuditConstants.ACCESS_TOPIC)).willReturn(true);
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
        assertThat(auditEvent).stringAt("response/status").isEqualTo("SUCCESS");
        assertThat(auditEvent).longAt("response/elapsedTime").isNotNull();
    }

    private void verifyAccessFailedAuditEvent(JsonValue auditEvent) {
        verifyAccessAuditEvent(auditEvent);
        assertThat(auditEvent).stringAt("eventName").isEqualTo(AM_ACCESS_OUTCOME.toString());
        assertThat(auditEvent).stringAt("response/status").startsWith("FAILED");
        assertThat(auditEvent).longAt("response/elapsedTime").isNotNull();
        assertThat(auditEvent).stringAt("response/message").isNotNull();
    }

    private void verifyAccessAuditEvent(JsonValue auditEvent) {
        assertThat(auditEvent).stringAt("timestamp").isNotNull();
        assertThat(auditEvent).stringAt("transactionId").isNotNull();
        assertThat(auditEvent).stringAt("component").isEqualTo(AUTHENTICATION.toString());
        assertThat(auditEvent).stringAt("authentication/id").isEqualTo("USER_ID");
        assertThat(auditEvent).stringAt("contexts").isEqualTo("{\"Session token\" : \"value\"}");
        assertThat(auditEvent).stringAt("client/host").isEqualTo("");
        assertThat(auditEvent).stringAt("client/ip").isEqualTo("REMOTE_ADDRESS");
        assertThat(auditEvent).integerAt("client/port").isEqualTo(9000);
        assertThat(auditEvent).stringAt("http/method").isEqualTo("GET");
        assertThat(auditEvent).stringAt("http/path").isEqualTo("http://example.com:8080");
        assertThat(auditEvent).stringAt("http/queryString").isEqualTo("query=value");
        assertThat(auditEvent).hasArray("http/headers/" + ContentTypeHeader.NAME).contains("CONTENT_TYPE");
    }

    private class MockAccessAuditFilter extends AbstractHttpAccessAuditFilter {
        public MockAccessAuditFilter(AuditEventPublisher publisher, AuditEventFactory factory) {
            super(AUTHENTICATION, publisher, factory);
        }
    }
}
