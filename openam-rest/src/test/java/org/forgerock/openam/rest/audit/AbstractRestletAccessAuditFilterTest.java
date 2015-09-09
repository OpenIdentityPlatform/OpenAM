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
package org.forgerock.openam.rest.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.USER_ID;
import static org.mockito.Mockito.*;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

public class AbstractRestletAccessAuditFilterTest {

    private AuditEventFactory eventFactory;
    private AuditEventPublisher eventPublisher;
    private MockAccessAuditFilter auditFilter;
    private Restlet restlet;

    @BeforeMethod
    public void setUp() {
        restlet = mock(Restlet.class);
        AuditServiceConfigurator auditServiceConfigurator = mock(AuditServiceConfigurator.class);
        when(auditServiceConfigurator.getAuditServiceConfiguration()).thenReturn(new AMAuditServiceConfiguration());
        eventFactory = new AuditEventFactory(auditServiceConfigurator);
        eventPublisher = mock(AuditEventPublisher.class);
        auditFilter = new MockAccessAuditFilter(restlet, eventPublisher, eventFactory);
    }

    @Test
    public void shouldHandleAuditException() throws AuditException {
        // Given
        Request request = mock(Request.class);
        Response response = new Response(request);
        Representation representation = mock(Representation.class);
        when(request.getEntity()).thenReturn(representation);
        when(request.getDate()).thenReturn(new Date());
        when(representation.isTransient()).thenReturn(false);
        AuditRequestContext.putProperty(USER_ID, "User 1");
        //AuditRequestContext.putProperty(CONTEXT_ID, "1234567890");
        when(eventPublisher.isAuditing(anyString())).thenReturn(true);
        when(eventPublisher.isSuppressExceptions()).thenReturn(false);
        doThrow(AuditException.class).when(eventPublisher).publish(anyString(), any(AuditEvent.class));

        // When
        auditFilter.handle(request, response);

        // Then
        verify(restlet, never()).handle(any(Request.class), any(Response.class));
        assertThat(response.getStatus()).isEqualTo(Status.SERVER_ERROR_INTERNAL);
    }

    @Test
    public void shouldCallHandleOnRestlet() {
        // Given
        Request request = mock(Request.class);
        Response response = new Response(request);
        Representation representation = mock(Representation.class);
        when(request.getEntity()).thenReturn(representation);
        when(representation.isTransient()).thenReturn(false);
        when(eventPublisher.isAuditing(anyString())).thenReturn(false);

        // When
        auditFilter.handle(request, response);

        // Then
        verify(restlet, times(1)).handle(any(Request.class), any(Response.class));
    }

    /**
     * Mock class to test AbstractRestletAccessAuditFilter.
     */
    private class MockAccessAuditFilter extends AbstractRestletAccessAuditFilter {

        public MockAccessAuditFilter(Restlet restlet, AuditEventPublisher publisher, AuditEventFactory factory) {
            super(AUTHENTICATION, restlet, publisher, factory);
        }
    }

}
