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

import static org.fest.assertions.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyCollectionOf;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sun.identity.entitlement.Application;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.entitlement.rest.ApplicationV1Filter;
import org.forgerock.openam.entitlement.rest.ApplicationV1FilterTransformer;
import org.forgerock.openam.entitlement.rest.EntitlementsExceptionMappingHandler;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
* Exercises the application filter to handle version 1.0.
*
* @since 13.0.0
*/
public class ApplicationV1FilterTest {

    @Mock
    private ResourceTypeService resourceTypeService;
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
    @Mock
    private Promise<QueryResponse, ResourceException> mockQueryResponse;
    @Mock
    private Promise<ResourceResponse, ResourceException> mockPromise;
    @Mock
    private ApplicationV1FilterTransformer applicationTransformer;

    @Captor
    private ArgumentCaptor<QueryResourceHandler> queryResultHandlerCaptor;
    @Captor
    private ArgumentCaptor<QueryFilter<SmsAttribute>> queryFilterCaptor;
    @Captor
    private ArgumentCaptor<ResourceType> resourceTypeCaptor;

    private ApplicationV1Filter filter;
    private Subject subject;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        EntitlementsExceptionMappingHandler resourceErrorHandler =
                new EntitlementsExceptionMappingHandler(
                        EntitlementRestGuiceModule.getEntitlementsErrorHandlers());
        filter = new ApplicationV1Filter(resourceTypeService,
                applicationServiceFactory, resourceErrorHandler, contextHelper, applicationTransformer, debug);
        subject = new Subject();
    }

    /**
     * Verifies that the appropriate resource type is associated with the application being created.
     */
    @Test
    public void resourceTypeAssociationOnCreate() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson(),
                        TestData.DATA_SET_1.getActions().asJson(),
                        field("realm", "/abc")
                )
        );

        CreateRequest createRequest = mock(CreateRequest.class);
        given(createRequest.getContent()).willReturn(jsonValue);

        Set<ResourceType> resourceTypes = CollectionUtils
                .asSet(ResourceType
                                .builder()
                                .setName("test")
                                .setUUID(TestData.DATA_SET_1.getResourceTypeUuid())
                                .build()
                );
        given(resourceTypeService.getResourceTypes(
                queryFilterCaptor.capture(), eq(subject), eq("/abc"))).willReturn(resourceTypes);

        given(requestHandler.handleCreate(eq(context), eq(createRequest))).willReturn(mockPromise);

        // When
        Promise<ResourceResponse, ResourceException> result =
            filter.filterCreate(context, createRequest, requestHandler);

        // Then
        assertThat(jsonValue.get("resourceTypeUuids").asSet(String.class))
                .containsOnly(TestData.DATA_SET_1.getResourceTypeUuid());
        verify(requestHandler).handleCreate(eq(context), eq(createRequest));
    }

    /**
     * Verifies that the appropriate resource type is created for the application being created.
     */
    @Test
    public void resourceTypeCreationOnCreate() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        field("name", "testApplication"),
                        TestData.DATA_SET_1.getResources().asJson(),
                        TestData.DATA_SET_1.getActions().asJson(),
                        field("realm", "/abc")
                )
        );

        CreateRequest createRequest = mock(CreateRequest.class);
        given(createRequest.getContent()).willReturn(jsonValue);

        Set<ResourceType> resourceTypes = Collections.emptySet();
        given(resourceTypeService.getResourceTypes(
                queryFilterCaptor.capture(), eq(subject), eq("/abc"))).willReturn(resourceTypes);

        ResourceType resourceType = ResourceType
                .builder()
                .setName("test")
                .setUUID("some-test-uuid")
                .setActions(TestData.DATA_SET_1.getActions().getUnderlyingMap())
                .setPatterns(TestData.DATA_SET_1.getResources().getUnderlyingSet())
                .build();
        given(resourceTypeService.saveResourceType(eq(subject), eq("/abc"), resourceTypeCaptor.capture()))
                .willReturn(resourceType);


        // When
        filter.filterCreate(context, createRequest, requestHandler);

        // Then
        assertThat(jsonValue.get("resourceTypeUuids").asSet(String.class)).containsOnly("some-test-uuid");
        verify(requestHandler).handleCreate(eq(context), eq(createRequest));

        ResourceType capturedResourceType = resourceTypeCaptor.getValue();
        assertThat(capturedResourceType.getName()).startsWith("testApplicationResourceType");
        assertThat(capturedResourceType.getActions()).isEqualTo(TestData.DATA_SET_1.getActions().getUnderlyingMap());
        assertThat(capturedResourceType.getPatterns()).isEqualTo(TestData.DATA_SET_1.getResources().getUnderlyingSet());
    }

    /**
     * Verifies that creation fails when no actions are defined.
     */
    @Test (expectedExceptions = BadRequestException.class)
    public void createFailsWhenNoActionsDefined() throws ResourceException {
        // Given
        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson()
                )
        );

        CreateRequest createRequest = mock(CreateRequest.class);
        given(createRequest.getContent()).willReturn(jsonValue);

        // When
        Promise<ResourceResponse, ResourceException> result =
            filter.filterCreate(context, createRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
    }

    /**
     * Verifies that creation fails when no resources are defined.
     */
    @Test (expectedExceptions = BadRequestException.class)
    public void createFailsWhenNoResourcesDefined() throws ResourceException {
        // Given
        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getActions().asJson()
                )
        );

        CreateRequest createRequest = mock(CreateRequest.class);
        given(createRequest.getContent()).willReturn(jsonValue);

        // When
        Promise<ResourceResponse, ResourceException> result =
            filter.filterCreate(context, createRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
    }

    /**
     * Verifies that the underlying associated resource type is updated to reflect changes in a v1.0 application.
     */
    @Test
    public void updateModifiesUnderlyingResourceType() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson(),
                        TestData.DATA_SET_1.getActions().asJson(),
                        field("realm", "/abc")
                )
        );

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(jsonValue);
        given(updateRequest.getResourcePath()).willReturn("testApplication");

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        Application application = mock(Application.class);
        given(applicationService.getApplication("testApplication")).willReturn(application);

        Set<String> resourceTypeUUIDs = new HashSet<>(CollectionUtils.asSet("abc-def-ghi"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        ResourceType resourceType = ResourceType
                .builder()
                .setName("test")
                .setUUID("abc-def-ghi")
                .setActions(TestData.DATA_SET_2.getActions().getUnderlyingMap())
                .setPatterns(TestData.DATA_SET_2.getResources().getUnderlyingSet())
                .build();
        given(resourceTypeService.getResourceType(subject, "/abc", "abc-def-ghi")).willReturn(resourceType);

        // When
        filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        assertThat(jsonValue.get("resourceTypeUuids").asSet(String.class)).containsOnly("abc-def-ghi");
        verify(resourceTypeService).updateResourceType(eq(subject), eq("/abc"), resourceTypeCaptor.capture());
        verify(requestHandler).handleUpdate(eq(context), eq(updateRequest));

        ResourceType capturedResourceType = resourceTypeCaptor.getValue();
        assertThat(capturedResourceType.getUUID()).isEqualTo("abc-def-ghi");
        assertThat(capturedResourceType.getActions())
                .isEqualTo(TestData.DATA_SET_1.getActions().getUnderlyingMap());
        assertThat(capturedResourceType.getPatterns())
                .isEqualTo(TestData.DATA_SET_1.getResources().getUnderlyingSet());
    }

    /**
     * Verifies that update fails when no actions are defined.
     */
    @Test (expectedExceptions = BadRequestException.class)
    public void updateFailsWhenNoActionsDefined() throws ResourceException {
        // Given
        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson()
                )
        );

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(jsonValue);

        // When
        Promise<ResourceResponse, ResourceException> result =
                filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
    }

    /**
     * Verifies that update fails when no resources are defined.
     */
    @Test (expectedExceptions = BadRequestException.class)
    public void updateFailsWhenNoResourcesDefined() throws ResourceException {
        // Given
        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getActions().asJson()
                )
        );

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(jsonValue);

        // When
        Promise<ResourceResponse, ResourceException> result =
                filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
    }

    /**
     * Verifies that update fails when the selected application cannot be found.
     */
    @Test (expectedExceptions = NotFoundException.class)
    public void updateFailsWhenApplicationMissing() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson(),
                        TestData.DATA_SET_1.getActions().asJson(),
                        field("realm", "/abc")
                )
        );

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(jsonValue);
        given(updateRequest.getResourcePath()).willReturn("testApplication");

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        given(applicationService.getApplication("testApplication")).willReturn(null);

        // When
        Promise<ResourceResponse, ResourceException> result =
                filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
    }

    /**
     * Verifies that update fails when the mentioned application has more than one resource type.
     */
    @Test (expectedExceptions = BadRequestException.class)
    public void updateFailsWithManyResourceTypes() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        // Build application JSON representation.
        JsonValue jsonValue = json(
                object(
                        TestData.DATA_SET_1.getResources().asJson(),
                        TestData.DATA_SET_1.getActions().asJson(),
                        field("realm", "/abc")
                )
        );

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(jsonValue);

        given(applicationServiceFactory.create(subject, "/abc")).willReturn(applicationService);
        Application application = mock(Application.class);
        given(updateRequest.getResourcePath()).willReturn("testApplication");
        given(applicationService.getApplication("testApplication")).willReturn(application);

        Set<String> resourceTypeUUIDs = new HashSet<>(CollectionUtils.asSet("abc-def-ghi", "jkl-mno-qrs"));
        given(application.getResourceTypeUuids()).willReturn(resourceTypeUUIDs);

        // When
        Promise<ResourceResponse, ResourceException> result =
                filter.filterUpdate(context, updateRequest, requestHandler);

        // Then
        result.getOrThrowUninterruptibly();
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
     * Verify that query requests are forwarded on and that the response is tailored to represent v1.0 application.
     */
    @Test
    public void forwardOnQuery() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);

        QueryRequest queryRequest = mock(QueryRequest.class);

        given(requestHandler.handleQuery(eq(context), eq(queryRequest), any(QueryResourceHandler.class)))
                .willReturn(mockQueryResponse);

        // When
        filter.filterQuery(context, queryRequest, queryResultHandler, requestHandler);

        // Then
        verify(requestHandler).handleQuery(eq(context), eq(queryRequest), queryResultHandlerCaptor.capture());
        verify(applicationTransformer).transform(eq(mockQueryResponse), eq(context), eq(queryRequest), eq(queryResultHandler),
                anyCollectionOf(ResourceResponse.class));
    }

    /**
     * Verify that read requests are forwarded on and that the response is tailored to represent v1.0 application.
     */
    @Test
    public void forwardOnRead() throws Exception {
        // Given
        given(contextHelper.getRealm(context)).willReturn("/abc");
        given(contextHelper.getSubject(context)).willReturn(subject);
        ReadRequest readRequest = mock(ReadRequest.class);
        given(requestHandler.handleRead(context, readRequest)).willReturn(mockPromise);

        // When
        filter.filterRead(context, readRequest, requestHandler);

        // Then
        verify(requestHandler).handleRead(eq(context), eq(readRequest));
        verify(applicationTransformer).transform(mockPromise, context);
    }

    /**
     * Enum for different test data configurations.
     */
    private static enum TestData {

        DATA_SET_1(
                "abc-def-ghi",
                newMapBuilder()
                        .put("GET", true)
                        .put("POST", true),
                newSetBuilder()
                        .add("a://b.c:d/e")
        ),
        DATA_SET_2(
                "jkl-mno-pqr",
                newMapBuilder()
                        .put("DELETE", true)
                        .put("PATCH", true),
                newSetBuilder()
                        .add("b://c.d:e/f")
        ),
        DATA_SET_3(
                "stu-vwz-yz1",
                newMapBuilder()
                        .put("UPDATE", true)
                        .put("ACTION", true),
                newSetBuilder()
                        .add("c://d.e:f/g")
                        .add("d://e.f:g/h")
        );

        private final String resourceTypeUuid;
        private final ActionMap actions;
        private final ResourceSet resources;

        private TestData(String resourceTypeUuid,
                         ActionMap actions,
                         ResourceSet resources) {
            this.resourceTypeUuid = resourceTypeUuid;
            this.actions = actions;
            this.resources = resources;
        }

        String getResourceTypeUuid() {
            return resourceTypeUuid;
        }

        ActionMap getActions() {
            return actions;
        }

        ResourceSet getResources() {
            return resources;
        }

    }

    private static ActionMap newMapBuilder() {
        return new ActionMap();
    }

    private static ResourceSet newSetBuilder() {
        return new ResourceSet();
    }

    private static final class ActionMap {

        private final Map<String, Boolean> underlyingMap;

        private ActionMap() {
            underlyingMap = new HashMap<>();
        }

        ActionMap put(String key, Boolean value) {
            underlyingMap.put(key, value);
            return this;
        }

        Map.Entry<String, Object> asJson() {
            return field("actions", object(
                    underlyingMap.entrySet().toArray(new Map.Entry[underlyingMap.size()])));
        }

        Map<String, Boolean> getUnderlyingMap() {
            return underlyingMap;
        }

    }

    private static final class ResourceSet {

        private final Set<String> underlyingSet;

        private ResourceSet() {
            underlyingSet = new HashSet<>();
        }

        ResourceSet add(String value) {
            underlyingSet.add(value);
            return this;
        }

        Map.Entry<String, Object> asJson() {
            return field("resources", array(underlyingSet.toArray()));
        }

        Set<String> getUnderlyingSet() {
            return underlyingSet;
        }

    }

}