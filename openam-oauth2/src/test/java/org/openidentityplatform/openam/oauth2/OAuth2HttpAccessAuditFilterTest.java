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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.oauth2;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.List;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OAuth2HttpAccessAuditFilterTest {

    private RealmTestHelper realmTestHelper;
    private AuditEventPublisher eventPublisher;
    private OAuth2HttpAccessAuditFilter filter;

    @BeforeMethod
    public void setUp() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        eventPublisher = mock(AuditEventPublisher.class);
        given(eventPublisher.isAuditing(any(), eq(AuditConstants.ACCESS_TOPIC), any(EventName.class)))
                .willReturn(true);
        filter = new OAuth2HttpAccessAuditFilter(eventPublisher, new AuditEventFactory());
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void auditsComponentOAuthAndTheRealmOfTheRealmContext() throws Exception {
        Context context = new RealmContext(requestContext(), realmTestHelper.mockRealm("sub"));

        List<JsonValue> events = auditedEvents(context, Status.OK);

        for (JsonValue event : events) {
            assertThat(event.get("component").asString()).isEqualTo(AuditConstants.Component.OAUTH.toString());
            assertThat(event.get("realm").asString()).isEqualTo("/sub");
        }
        assertThat(events.get(1).get("response").get("status").asString()).isEqualTo("SUCCESSFUL");
    }

    @Test
    public void auditsAFailureWithoutARealmWhenNoRealmContextExists() throws Exception {
        List<JsonValue> events = auditedEvents(requestContext(), Status.METHOD_NOT_ALLOWED);

        for (JsonValue event : events) {
            assertThat(event.get("component").asString()).isEqualTo(AuditConstants.Component.OAUTH.toString());
            assertThat(event.isDefined("realm")).isFalse();
        }
        assertThat(events.get(1).get("response").get("status").asString()).isEqualTo("FAILED");
        assertThat(events.get(1).get("response").get("statusCode").asString()).isEqualTo("405");
    }

    /** Runs the filter around a handler that answers the status, and returns the attempt and outcome events. */
    private List<JsonValue> auditedEvents(Context context, Status status) throws Exception {
        Request request = new Request().setMethod("GET")
                .setUri(URI.create("http://openam.example.org:8080/openam/.well-known/webfinger"));
        filter.filter(context, request, (ctx, req) -> newResultPromise(new Response(status)))
                .getOrThrowUninterruptibly();
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher, times(2)).tryPublish(eq(AuditConstants.ACCESS_TOPIC), events.capture());
        return events.getAllValues().stream().map(AuditEvent::getValue).collect(toList());
    }

    /** The contexts HttpFrameworkServlet creates: request audit, attributes, session, client. */
    private static Context requestContext() {
        return new RequestAuditContext(new AttributesContext(new SessionContext(
                ClientContext.buildExternalClientContext(new RootContext()).remoteAddress("127.0.0.1").build(),
                mock(Session.class))));
    }
}
