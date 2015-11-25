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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.entitlement.rest;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.sun.identity.entitlement.Application;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.entitlement.rest.EntitlementsExceptionMappingHandler;
import org.forgerock.openam.entitlement.rest.PolicyV1Filter;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
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
    private Context context;
    @Mock
    private QueryResourceHandler queryResultHandler;
    @Mock
    private RequestHandler requestHandler;
    @Mock
    private ContextHelper contextHelper;
    @Mock
    private Debug debug;

    @Captor
    private ArgumentCaptor<QueryResourceHandler> queryResultHandlerCaptor;

    private PolicyV1Filter filter;
    private Subject subject;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        EntitlementsExceptionMappingHandler resourceErrorHandler =
                new EntitlementsExceptionMappingHandler(
                        EntitlementRestGuiceModule.getEntitlementsErrorHandlers());
        filter = new PolicyV1Filter(applicationServiceFactory, resourceErrorHandler, contextHelper, debug);
        subject = new Subject();

        when(requestHandler.handleAction(any(Context.class), any(ActionRequest.class))).thenReturn(
                Promises.<ActionResponse, ResourceException>newResultPromise(newActionResponse(
                        (json(object(field("ttl", "1234567890")))))));

        when(requestHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(
                Promises.<ResourceResponse, ResourceException>newResultPromise(newResourceResponse("A", "1",
                        (json(object(field("resourceTypeUuid", "abc-def-hij")))))));

        when(requestHandler.handleUpdate(any(Context.class), any(UpdateRequest.class))).thenReturn(Promises
                .<ResourceResponse, ResourceException>newResultPromise(newResourceResponse("A", "1", (json(object
                        (field("resourceTypeUuid", "abc-def-hij")))))));

        when(requestHandler.handleRead(any(Context.class), any(ReadRequest.class))).thenReturn(Promises
                .<ResourceResponse, ResourceException>newResultPromise(newResourceResponse("A", "1", (json(object
                        (field("resourceTypeUuid", "abc-def-hij")))))));
    }

    /**
     * Verify that action requests are forwarded on.
     */
    @Test
    public void forwardOnAction() throws Exception {
        // Given
        ActionRequest actionRequest = mock(ActionRequest.class);

        // When
        Promise<ActionResponse, ResourceException> promise =
                filter.filterAction(context, actionRequest, requestHandler);

        // Then
        assertThat(promise).succeeded();
        assertThat(promise.get().getJsonContent().contains("ttl")).isFalse();
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

        Set<String> resourceTypeUUIDs = new HashSet<>(CollectionUtils.asSet("abc-def-hij"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                filter.filterCreate(context, createRequest, requestHandler);

        // Then
        assertThat(promise).succeeded();
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        assertThat(jsonValue.get("resourceTypeUuid").asString()).isEqualTo("abc-def-hij");
        assertThat(promise.get().getContent().contains("resourceTypeUuid")).isFalse();
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
        Promise<ResourceResponse, ResourceException> promise =
                filter.filterCreate(context, createRequest, requestHandler);

        // Then
        assertThat(promise).failedWithResourceException().withCode(ResourceException.BAD_REQUEST);
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
        Promise<ResourceResponse, ResourceException> promise =
                filter.filterCreate(context, createRequest, requestHandler);

        // Then
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        assertThat(promise).failedWithResourceException().withCode(ResourceException.NOT_FOUND);
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

        Set<String> resourceTypeUUIDs = new HashSet<>(CollectionUtils.asSet("abc-def-hij", "123-456-789"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                filter.filterCreate(context, createRequest, requestHandler);

        // Then
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        assertThat(promise).failedWithResourceException().withCode(ResourceException.BAD_REQUEST);
        // Request should not be forwarded.
        verifyNoMoreInteractions(requestHandler);
    }

    /**
     * Verifies that the appropriate resource type is associated with the policy being updated.
     */
    @Test
    public void resourceTypeAssociationOnUpdate() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        JsonValue jsonValue = json(object(field("applicationName", "testApp")));
        given(updateRequest.getContent()).willReturn(jsonValue);

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        Application application = mock(Application.class);
        given(applicationService.getApplication("testApp")).willReturn(application);

        Set<String> resourceTypeUUIDs = new HashSet<>(CollectionUtils.asSet("abc-def-hij"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        assertThat(promise).succeeded();
        verify(applicationServiceFactory).create(subject, "/abc");
        verify(applicationService).getApplication("testApp");
        assertThat(jsonValue.get("resourceTypeUuid").asString()).isEqualTo("abc-def-hij");
        assertThat(promise.get().getContent().contains("resourceTypeUuid")).isFalse();
    }

    /**
     * Verify that delete requests are forwarded on.
     */
    @Test
    public void forwardOnDelete() {
        // Given
        DeleteRequest deleteRequest = mock(DeleteRequest.class);

        // When
        filter.filterDelete(context, deleteRequest, requestHandler);

        // Then
        verify(requestHandler).handleDelete(context, deleteRequest);
    }

    /**
     * Verify that query requests are forwarded on and that the response is tailored to represent v1.0 policy.
     */
    @Test
    public void forwardOnQuery() {
        // Given
        QueryRequest queryRequest = mock(QueryRequest.class);

        when(requestHandler.handleQuery(any(Context.class), any(QueryRequest.class),
                queryResultHandlerCaptor.capture())).thenReturn(Promises.<QueryResponse,
                ResourceException>newResultPromise(newQueryResponse()));

        // When
        Promise<QueryResponse, ResourceException> promise =
                filter.filterQuery(context, queryRequest, queryResultHandler, requestHandler);

        // Then
        assertThat(promise).succeeded();
        verify(requestHandler).handleQuery(eq(context), eq(queryRequest), queryResultHandlerCaptor.capture());

        // Now exercise the internal static result handler.
        exerciseTransformationHandler(queryResultHandlerCaptor.getValue());
    }

    /**
     * Verify that read requests are forwarded on and that the response is tailored to represent v1.0 policy.
     */
    @Test
    public void forwardOnRead() throws Exception {
        // Given
        ReadRequest readRequest = mock(ReadRequest.class);

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterRead(context, readRequest, requestHandler);

        // Then
        assertThat(promise).succeeded();
        verify(requestHandler).handleRead(eq(context), eq(readRequest));
        assertThat(promise.get().getContent().contains("resourceTypeUuid")).isFalse();
    }

    /**
     * Exercises the captured handler to ensure the JSON response is modified such that is represents a v1.0 policy.
     *
     * @param capturedHandler
     */
    private void exerciseTransformationHandler(QueryResourceHandler capturedHandler) {
        // Given
        JsonValue jsonValue = JsonValue.json(object(field("resourceTypeUuid", "abc-def-hij")));
        ResourceResponse resource = newResourceResponse("testId", "1.2.3", jsonValue);

        // When
        capturedHandler.handleResource(resource);

        // Then
        verify(queryResultHandler).handleResource(resource);
        assertThat(jsonValue.contains("resourceTypeUuid")).isFalse();
    }

}