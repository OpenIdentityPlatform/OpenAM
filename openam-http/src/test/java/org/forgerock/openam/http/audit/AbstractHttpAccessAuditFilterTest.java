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

import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.Session;
import org.forgerock.http.context.ClientInfoContext;
import org.forgerock.http.context.HttpRequestContext;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractHttpAccessAuditFilterTest {

    private AuditEventFactory eventFactory;
    private AuditEventPublisher eventPublisher;
    private MockAccessAuditFilter auditFilter;
    private Handler nextHandler;

    @BeforeMethod
    public void setUp() {
        nextHandler = mock(Handler.class);
        AuditServiceConfigurator auditServiceConfigurator = mock(AuditServiceConfigurator.class);
        when(auditServiceConfigurator.getAuditServiceConfiguration()).thenReturn(new AMAuditServiceConfiguration());
        eventFactory = new AuditEventFactory(auditServiceConfigurator);
        eventPublisher = mock(AuditEventPublisher.class);
        auditFilter = new MockAccessAuditFilter(eventPublisher, eventFactory);
    }

    @Test
    public void shouldHandleAuditException() throws Exception {
        // Given
        Request request = new Request()
                .setTime(System.currentTimeMillis())
                .setUri("http://example.com");
        Context context = new HttpRequestContext(ClientInfoContext.builder(new RootContext()).certificates().build(), mock(Session.class));
        AuditRequestContext.putProperty(USER_ID, "User 1");
        AuditRequestContext.putProperty(CONTEXT_ID, "1234567890");
        when(eventPublisher.isAuditing(anyString())).thenReturn(true);
        when(eventPublisher.isSuppressExceptions()).thenReturn(false);
        doThrow(AuditException.class).when(eventPublisher).publish(anyString(), any(AuditEvent.class));

        // When
        Promise<Response, NeverThrowsException> result = auditFilter.filter(context, request, nextHandler);

        // Then
        verify(nextHandler, never()).handle(any(Context.class), any(Request.class));
        Assertions.assertThat(result);
    }

    @Test
    public void shouldCallHandleOnHandler() {
        // Given
        Request request = new Request();
        Context context = new HttpRequestContext(new RootContext(), mock(Session.class));
        when(eventPublisher.isAuditing(anyString())).thenReturn(false);
        final Promise<Response, NeverThrowsException> responseExceptionPromise = newResultPromise(new Response());
        when(nextHandler.handle(context, request)).thenReturn(responseExceptionPromise);

        // When
        Promise<Response, NeverThrowsException> result = auditFilter.filter(context, request, nextHandler);

        // Then
        verify(nextHandler, times(1)).handle(any(Context.class), any(Request.class));
    }

    /**
     * Mock class to test AbstractRestletAccessAuditFilter.
     */
    private class MockAccessAuditFilter extends AbstractHttpAccessAuditFilter {

        public MockAccessAuditFilter(AuditEventPublisher publisher, AuditEventFactory factory) {
            super(AUTHENTICATION, publisher, factory);
        }
    }

}
