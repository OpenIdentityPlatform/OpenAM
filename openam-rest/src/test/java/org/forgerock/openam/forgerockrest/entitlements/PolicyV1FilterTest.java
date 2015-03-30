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
package org.forgerock.openam.forgerockrest.entitlements;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sun.identity.entitlement.Application;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.CollectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;

/**
 * Exercises the policy filter to handle version 1.0.
 *
 * @since 13.0.0
 */
public class PolicyV1FilterTest {

    @Mock
    private ApplicationServiceFactory applicationServiceFactory;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ServerContext context;
    @Mock
    private ResultHandler<JsonValue> jsonResultHandler;
    @Mock
    private ResultHandler<Resource> resourceResultHandler;
    @Mock
    private QueryResultHandler queryResultHandler;
    @Mock
    private RequestHandler requestHandler;
    @Mock
    private ContextHelper contextHelper;
    @Mock
    private Debug debug;

    @Captor
    private ArgumentCaptor<ResultHandler<Resource>> resourceResultHandlerCaptor;
    @Captor
    private ArgumentCaptor<QueryResultHandler> queryResultHandlerCaptor;

    private PolicyV1Filter filter;
    private Subject subject;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        EntitlementsExceptionMappingHandler resourceErrorHandler =
                new EntitlementsExceptionMappingHandler(
                        ForgerockRestGuiceModule.getEntitlementsErrorHandlers());
        filter = new PolicyV1Filter(applicationServiceFactory, resourceErrorHandler, contextHelper, debug);
        subject = new Subject();
    }

    /**
     * Verify that action requests are forwarded on.
     */
    @Test
    public void forwardOnAction() {
        // Given
        ActionRequest actionRequest = mock(ActionRequest.class);

        // When
        filter.filterAction(context, actionRequest, jsonResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleAction(context, actionRequest, jsonResultHandler);
    }

    /**
     * Verifies that the appropriate resource type is associated with the policy being created.
     */
    @Test
    public void resourceTypeAssociationOnCreate() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        CreateRequest createRequest = mock(CreateRequest.class);
        JsonValue jsonValue = json(object(field("applicationName", "testApp")));
        given(createRequest.getContent()).willReturn(jsonValue);

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        Application application = mock(Application.class);
        given(applicationService.getApplication("testApp")).willReturn(application);

        Set<String> resourceTypeUUIDs = new HashSet<String>(CollectionUtils.asSet("abc-def-hij"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        filter.filterCreate(context, createRequest, resourceResultHandler, requestHandler);

        // Then
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        verify(requestHandler).handleCreate(eq(context), eq(createRequest), resourceResultHandlerCaptor.capture());
        assertThat(jsonValue.get("resourceTypeUuid").asString()).isEqualTo("abc-def-hij");

        // Now exercise the internal static result handler.
        exerciseTransformationHandler(resourceResultHandlerCaptor.getValue());
    }

    /**
     * Verifies that the application name missing from the request is handled appropriately.
     */
    @Test
    public void createFailsWhenMissingApplication() {
        // Given
        CreateRequest createRequest = mock(CreateRequest.class);
        JsonValue jsonValue = json(object());
        given(createRequest.getContent()).willReturn(jsonValue);

        // When
        filter.filterCreate(context, createRequest, resourceResultHandler, requestHandler);

        verify(resourceResultHandler).handleError(isNotNull(ResourceException.class));
        // Request should not be forwarded.
        verifyNoMoreInteractions(requestHandler);
    }

    /**
     * Verifies that an application that cannot be found is handled appropriately.
     */
    @Test
    public void createFailsWhenApplicationLookupFails() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        CreateRequest createRequest = mock(CreateRequest.class);
        JsonValue jsonValue = json(object(field("applicationName", "testApp")));
        given(createRequest.getContent()).willReturn(jsonValue);

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        given(applicationService.getApplication("testApp")).willReturn(null);

        // When
        filter.filterCreate(context, createRequest, resourceResultHandler, requestHandler);

        // Then
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        verify(resourceResultHandler).handleError(isNotNull(ResourceException.class));
        // Request should not be forwarded.
        verifyNoMoreInteractions(requestHandler);
    }

    /**
     * Verifies that an application with more than one resource type is handled appropriately.
     */
    @Test
    public void createFailsWhenMultipleResourceTypesFound() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        CreateRequest createRequest = mock(CreateRequest.class);
        JsonValue jsonValue = json(object(field("applicationName", "testApp")));
        given(createRequest.getContent()).willReturn(jsonValue);

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        Application application = mock(Application.class);
        given(applicationService.getApplication("testApp")).willReturn(application);

        Set<String> resourceTypeUUIDs = new HashSet<String>(CollectionUtils.asSet("abc-def-hij", "123-456-789"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        filter.filterCreate(context, createRequest, resourceResultHandler, requestHandler);

        // Then
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        verify(resourceResultHandler).handleError(isNotNull(ResourceException.class));
        // Request should not be forwarded.
        verifyNoMoreInteractions(requestHandler);
    }

    /**
     * Verify that update requests are forwarded on and that the response is tailored to represent v1.0 policy.
     */
    @Test
    public void forwardOnUpdate() {
        // Given
        UpdateRequest updateRequest = mock(UpdateRequest.class);

        // When
        filter.filterUpdate(context, updateRequest, resourceResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleUpdate(eq(context), eq(updateRequest), resourceResultHandlerCaptor.capture());

        // Now exercise the internal static result handler.
        exerciseTransformationHandler(resourceResultHandlerCaptor.getValue());
    }

    /**
     * Verify that delete requests are forwarded on.
     */
    @Test
    public void forwardOnDelete() {
        // Given
        DeleteRequest deleteRequest = mock(DeleteRequest.class);

        // When
        filter.filterDelete(context, deleteRequest, resourceResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleDelete(context, deleteRequest, resourceResultHandler);
    }

    /**
     * Verify that query requests are forwarded on and that the response is tailored to represent v1.0 policy.
     */
    @Test
    public void forwardOnQuery() {
        // Given
        QueryRequest queryRequest = mock(QueryRequest.class);

        // When
        filter.filterQuery(context, queryRequest, queryResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleQuery(eq(context), eq(queryRequest), queryResultHandlerCaptor.capture());

        // Now exercise the internal static result handler.
        exerciseTransformationHandler(queryResultHandlerCaptor.getValue());
    }

    /**
     * Verify that read requests are forwarded on and that the response is tailored to represent v1.0 policy.
     */
    @Test
    public void forwardOnRead() {
        // Given
        ReadRequest readRequest = mock(ReadRequest.class);

        // When
        filter.filterRead(context, readRequest, resourceResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleRead(eq(context), eq(readRequest), resourceResultHandlerCaptor.capture());

        // Now exercise the internal static result handler.
        exerciseTransformationHandler(resourceResultHandlerCaptor.getValue());
    }

    /**
     * Exercises the captured handler to ensure the JSON response is modified such that is represents a v1.0 policy.
     *
     * @param capturedHandler
     */
    private void exerciseTransformationHandler(ResultHandler<Resource> capturedHandler) {
        // Given
        JsonValue jsonValue = JsonValue.json(object(field("resourceTypeUuid", "abc-def-hij")));
        Resource resource = new Resource("testId", "1.2.3", jsonValue);

        // When
        capturedHandler.handleResult(resource);

        // Then
        verify(resourceResultHandler).handleResult(resource);
        assertThat(jsonValue.contains("resourceTypeUuid")).isFalse();
    }

    /**
     * Exercises the captured handler to ensure the JSON response is modified such that is represents a v1.0 policy.
     *
     * @param capturedHandler
     */
    private void exerciseTransformationHandler(QueryResultHandler capturedHandler) {
        // Given
        JsonValue jsonValue = JsonValue.json(object(field("resourceTypeUuid", "abc-def-hij")));
        Resource resource = new Resource("testId", "1.2.3", jsonValue);
        QueryResult queryResult = new QueryResult(null, 2);

        // When
        capturedHandler.handleResource(resource);
        capturedHandler.handleResult(queryResult);

        // Then
        verify(queryResultHandler).handleResource(resource);
        verify(queryResultHandler).handleResult(queryResult);
        assertThat(jsonValue.contains("resourceTypeUuid")).isFalse();
    }

}