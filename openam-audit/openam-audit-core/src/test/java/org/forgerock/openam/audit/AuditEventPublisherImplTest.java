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

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.audit.AuditConstants.EventName;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @since 13.0.0
 */
@SuppressWarnings("unchecked")
public class AuditEventPublisherImplTest {

    private static final String FAILURE_SUPPRESSED_REALM = "realm1";
    private static final String FAILURE_NOT_SUPPRESSED_REALM = "realm2";

    private AuditEventHandler mockHandler;
    private AuditEventPublisher auditEventPublisher;
    private AuditServiceProvider auditServiceProvider;
    private ArgumentCaptor<JsonValue> auditEventCaptor;
    private Promise<ResourceResponse, ResourceException> dummyPromise;

    @BeforeMethod
    protected void setUp() throws Exception {
        mockHandler = mock(AuditEventHandler.class);
        when(mockHandler.getHandledTopics()).thenReturn(asSet("access"));
        when(mockHandler.isEnabled()).thenReturn(true);
        auditServiceProvider = mock(AuditServiceProvider.class);
        auditEventPublisher = new AuditEventPublisherImpl(auditServiceProvider);
        auditEventCaptor = ArgumentCaptor.forClass(JsonValue.class);
        dummyPromise = newResultPromise(newResourceResponse("", "", json(object())));
    }

    @Test
    public void publishesProvidedAuditEventToAuditService() throws Exception {
        // Given
        AuditEvent auditEvent = getAuditEvent(null);
        givenDefaultAuditService();

        when(mockHandler.publishEvent(
                any(Context.class), eq("access"), auditEventCaptor.capture())).thenReturn(dummyPromise);

        // When
        auditEventPublisher.tryPublish("access", auditEvent);

        // Then
        verify(mockHandler, times(1)).publishEvent(any(Context.class), any(String.class), any(JsonValue.class));
        assertThat(auditEventCaptor.getValue()).isEqualTo(auditEvent.getValue());
    }

    @Test
    public void shouldSuppressExceptionsOnPublish() throws Exception {
        // Given
        AuditEvent auditEvent = getAuditEvent(FAILURE_SUPPRESSED_REALM);
        givenSuppressedFailureAuditService();

        // When
        auditEventPublisher.tryPublish("unknownTopic", auditEvent);
    }

    @Test
    public void shouldFallBackToDefaultAuditServiceWhenRealmHasShutDown() throws Exception {
        // Given
        AuditEvent auditEvent = getAuditEvent("deadRealm");
        givenDefaultAuditService();
        when(mockHandler.publishEvent(
                any(Context.class), eq("access"), auditEventCaptor.capture())).thenReturn(dummyPromise);

        AMAuditServiceConfiguration serviceConfig = new AMAuditServiceConfiguration(true);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withConfiguration(serviceConfig)
                .withAuditEventHandler(mock(AuditEventHandler.class));
        AMAuditService auditService = new RealmAuditServiceProxy(builder.build(), mock(AMAuditService.class),
                serviceConfig);
        auditService.startup();
        auditService.shutdown();
        when(auditServiceProvider.getAuditService("deadRealm")).thenReturn(auditService);

        // When
        auditEventPublisher.tryPublish("access", auditEvent);

        // Then
        assertThat(auditEventCaptor.getValue()).isEqualTo(auditEvent.getValue());
    }

    private AuditEvent getAuditEvent(String realm) {
        return new AMAccessAuditEventBuilder()
                .eventName(EventName.AM_ACCESS_OUTCOME)
                .transactionId(UUID.randomUUID().toString())
                .userId("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org")
                .client("172.16.101.7", 62375)
                .server("216.58.208.36", 80)
                .request("CREST", "READ")
                .httpRequest(false, "GET", "/some/path", getQueryParameters(),
                        Collections.<String, List<String>>emptyMap())
                .response(SUCCESSFUL, "200", 42, MILLISECONDS)
                .realm(realm)
                .toEvent();
    }

    private void givenSuppressedFailureAuditService() throws ServiceUnavailableException, AuditException {
        AMAuditServiceConfiguration serviceConfig = new AMAuditServiceConfiguration(true);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withConfiguration(serviceConfig)
                .withAuditEventHandler(mockHandler);
        AMAuditService auditService = new RealmAuditServiceProxy(builder.build(), mock(AMAuditService.class),
                serviceConfig);
        auditService.startup();
        when(auditServiceProvider.getAuditService(FAILURE_SUPPRESSED_REALM)).thenReturn(auditService);
    }

    private void givenDefaultAuditService() throws ServiceUnavailableException, AuditException {
        AMAuditServiceConfiguration serviceConfig = new AMAuditServiceConfiguration(true);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withConfiguration(serviceConfig)
                .withAuditEventHandler(mockHandler);
        AMAuditService auditService = new DefaultAuditServiceProxy(builder.build(), serviceConfig);
        auditService.startup();
        when(auditServiceProvider.getDefaultAuditService()).thenReturn(auditService);
    }

    private Map<String, List<String>> getQueryParameters() {
        HashMap<String, List<String>> queryParameters = new LinkedHashMap<>();
        queryParameters.put("p1", asList("v1"));
        queryParameters.put("p2", asList("v2"));
        return queryParameters;
    }
}
