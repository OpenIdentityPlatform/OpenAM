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
package org.forgerock.openam.rest.audit;

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AMAuditService;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.audit.DefaultAuditServiceProxy;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.mockito.ArgumentCaptor;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractRestletAccessAuditFilterTest {

    private AuditEventFactory eventFactory;
    private AuditEventPublisher eventPublisher;
    private RestletAccessAuditFilterTest auditFilter;
    private AuditServiceProvider auditServiceProvider;
    private Restlet restlet;

    @BeforeMethod
    public void setUp() throws Exception {
        restlet = mock(Restlet.class);

        AMAuditServiceConfiguration serviceConfig = new AMAuditServiceConfiguration(true);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService().withConfiguration(serviceConfig);
        AMAuditService auditService = new DefaultAuditServiceProxy(builder.build(), serviceConfig);
        auditService.startup();
        auditServiceProvider = mock(AuditServiceProvider.class);
        when(auditServiceProvider.getDefaultAuditService()).thenReturn(auditService);

        eventFactory = new AuditEventFactory();
        eventPublisher = mock(AuditEventPublisher.class);
        auditFilter = new RestletAccessAuditFilterTest(restlet, eventPublisher, eventFactory, null, null);
    }

    @Test
    public void shouldCallHandleOnRestlet() {
        // Given
        Request request = mock(Request.class);
        Response response = new Response(request);
        Representation representation = mock(Representation.class);
        when(request.getEntity()).thenReturn(representation);
        when(request.getAttributes()).thenReturn(new ConcurrentHashMap<String, Object>());
        when(representation.isTransient()).thenReturn(false);
        when(eventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).thenReturn(false);

        // When
        auditFilter.handle(request, response);

        // Then
        verify(restlet, times(1)).handle(any(Request.class), any(Response.class));
    }

    @Test
    public void shouldCaptureRequestBodyProperties() throws Exception {
        // Given
        auditFilter = new RestletAccessAuditFilterTest(restlet, eventPublisher, eventFactory,
                RestletBodyAuditor.jsonAuditor("fred"), RestletBodyAuditor.jsonAuditor("gary"));
        Request request = new Request();
        request.setDate(newDate());
        Response response = new Response(request);
        request.setEntity(new JsonRepresentation((Map<String, Object>) object(field("fred", "v"), field("gary", 7))));
        when(eventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).thenReturn(true);

        // When
        auditFilter.beforeHandle(request, response);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).tryPublish(anyString(), captor.capture());
        assertThat(captor.getValue().getValue()).isObject()
                .hasObject("request")
                .hasObject("detail")
                .contains("fred", "v");
    }

    @Test
    public void shouldCaptureResponseBodyProperties() throws Exception {
        // Given
        auditFilter = new RestletAccessAuditFilterTest(restlet, eventPublisher, eventFactory,
                RestletBodyAuditor.jsonAuditor("fred"), RestletBodyAuditor.jsonAuditor("gary"));
        Request request = new Request();
        request.setDate(newDate());
        Response response = new Response(request);
        response.setEntity(new JsonRepresentation((Map<String, Object>) object(field("fred", "v"), field("gary", 7))));
        when(eventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).thenReturn(true);

        // When
        auditFilter.afterHandle(request, response);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).tryPublish(anyString(), captor.capture());
        assertThat(captor.getValue().getValue()).isObject()
                .hasObject("response")
                .hasObject("detail")
                .contains("gary", 7);
    }

    /**
     * Class to test AbstractRestletAccessAuditFilter.
     */
    private class RestletAccessAuditFilterTest extends AbstractRestletAccessAuditFilter {

        public RestletAccessAuditFilterTest(Restlet restlet, AuditEventPublisher publisher, AuditEventFactory factory,
                RestletBodyAuditor requestBodyAuditor, RestletBodyAuditor responseBodyAuditor) {
            super(AUTHENTICATION, restlet, publisher, factory, requestBodyAuditor, responseBodyAuditor);
        }
    }

}
