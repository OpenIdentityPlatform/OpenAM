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
package org.forgerock.openam.entitlement.rest;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.entitlement.rest.EntitlementTestUtils.assertResourcePromiseFailedWithCodes;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryException;
import org.forgerock.openam.rest.query.QueryResponseHandler;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ResourceTypesResourceTest {

    private final String jsonResourceType =
            "{" +
                    "    \"actions\": {" +
                    "            \"ACTION\": true," +
                    "            \"CREATE\": true," +
                    "            \"DELETE\": true," +
                    "            \"PATCH\": true," +
                    "            \"QUERY\": true," +
                    "            \"READ\": true," +
                    "            \"UPDATE\": true" +
                    "    }," +
                    "    \"description\": \"An example resource type\"," +
                    "    \"name\": \"myResourceType\"," +
                    "    \"patterns\": [" +
                    "            \"http://example.com:80/*\"" +
                    "    ]" +
                    "}" +
                    ";";

    private final Map<String, Set<String>> rawData = new HashMap<String, Set<String>>();

    private Context mockServerContext;
    private ResourceTypesResource resourceTypesResource;
    private ResourceTypeService resourceTypeService;
    private Subject callerSubject;

    private abstract class MockResourceTypeService implements ResourceTypeService {
        ResourceType resourceType;

        @Override
        public ResourceType saveResourceType(Subject subject, String realm, ResourceType resourceType)
                throws EntitlementException {
            this.resourceType = resourceType;

            return resourceType;
        }

        @Override
        public ResourceType getResourceType(Subject subject, String realm, String uuid) throws EntitlementException {
            assertThat(uuid).isEqualTo(resourceType.getUUID());

            return resourceType;
        }

        @Override
        public ResourceType updateResourceType(Subject subject, String realm, ResourceType resourceType)
                throws EntitlementException {
            assertThat(this.resourceType.getUUID()).isEqualTo(resourceType.getUUID());

            return resourceType;
        }

        @Override
        public void deleteResourceType(Subject subject, String realm, String uuid) throws EntitlementException {
            assertThat(uuid).isEqualTo(resourceType.getUUID());
        }
    }

    @BeforeMethod
    public void setUp() throws ResourceException {

        callerSubject = new Subject();

        // to mock the HTTP method, we need the following contexts
        Context httpContext = new HttpContext(json(object(
                field(HttpContext.ATTR_HEADERS, Collections.singletonMap("method", Arrays.asList("PUT"))),
                field(HttpContext.ATTR_PARAMETERS, Collections.emptyMap()))), null);

        Context securityContext = new SecurityContext(httpContext, null, null);

        Context subjectContext = new SSOTokenContext(mock(Debug.class), null, securityContext) {
            @Override
            public Subject getCallerSubject() {
                return callerSubject;
            }
        };

        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("/", "/");

        mockServerContext = ClientContext.newInternalClientContext(realmContext);

        resourceTypeService = mock(MockResourceTypeService.class);

        Debug debug = mock(Debug.class);
        resourceTypesResource = new ResourceTypesResource(debug,
                new EntitlementsExceptionMappingHandler(EntitlementRestGuiceModule.getEntitlementsErrorHandlers()),
                resourceTypeService);

        rawData.put("name", Collections.singleton("myResourceType"));
        rawData.put("description", Collections.singleton("myResourceType"));
        rawData.put("realm", Collections.singleton("/"));
        rawData.put("actions", Collections.singleton("CREATE"));
        rawData.put("patterns", Collections.singleton("http://example.com:80/*"));
        rawData.put("creationDate", Collections.singleton(String.valueOf(newDate().getTime())));
        rawData.put("lastModifiedDate", Collections.singleton(String.valueOf(newDate().getTime())));
    }

    @Test
    public void shouldCreateResourceType() throws EntitlementException {

        //given
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        JsonValue content = mock(JsonValue.class);

        given(mockCreateRequest.getContent()).willReturn(content);
        given(content.toString()).willReturn(jsonResourceType);

        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertThat(promise).succeeded().withContent().stringAt("name").isEqualTo("myResourceType");
    }

    @Test
    public void createShouldFailIfCallerSubjectNotPresent() {

        //given
        CreateRequest mockCreateRequest = mock(CreateRequest.class);

        // subject is null, which will represent a broken SSOTokenContext
        callerSubject = null;

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.INTERNAL_ERROR,
                EntitlementException.INTERNAL_ERROR);
    }

    @Test
    public void createShouldFailIfJsonResourceTypeInvalid() throws EntitlementException {
        //given
        JsonValue content = mock(JsonValue.class);

        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        given(mockCreateRequest.getContent()).willReturn(content);

        // the name attribute is required, but here replaced by an unrecognised attribute
        given(content.toString()).willReturn(jsonResourceType.replaceAll("\"name\"", "\"id\""));
        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.BAD_REQUEST,
                EntitlementException.INVALID_CLASS);
    }

    /*
     * This seems to throw an unchecked exception (NPE) when the name is not present
     */
    @Test
    public void createShouldFailIfResourceTypeNameAbsent() throws EntitlementException {
       //given
        JsonValue content = mock(JsonValue.class);

        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        given(mockCreateRequest.getContent()).willReturn(content);

        // the name attribute is required, but here replaced by an unrecognised attribute
        given(content.toString()).willReturn(jsonResourceType.replaceAll("\"name\":.*,", ""));
        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.BAD_REQUEST,
                EntitlementException.MISSING_RESOURCE_TYPE_NAME);
    }

    /*
     * This expected failure when the newResourceId in the create request is not the resource type name.
     *
     * Now the new resource id is not used.
     *
     */
    @Test
    public void createShouldIgnoreNewResourceId() throws EntitlementException {
        //given
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        JsonValue content = mock(JsonValue.class);
        given(mockCreateRequest.getContent()).willReturn(content);

        // the resource ID in the request is differs form anything in the JSON resource type
        given(mockCreateRequest.getNewResourceId()).willReturn("ignored-new-resource-id");
        given(content.toString()).willReturn(jsonResourceType);

        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertThat(promise).succeeded();
    }

    /*
     * A UUID is supplied on create, but it is ignored
     */
    @Test
    public void createShouldIgnoreUUIDInJson() throws EntitlementException {
        //given
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        JsonValue content = mock(JsonValue.class);

        given(mockCreateRequest.getContent()).willReturn(content);
        given(mockCreateRequest.getNewResourceId()).willReturn(null);

        String jsonWithUuid =
                "{" +
                        "    \"actions\": {" +
                        "            \"ACTION\": true," +
                        "            \"CREATE\": true," +
                        "            \"DELETE\": true," +
                        "            \"PATCH\": true," +
                        "            \"QUERY\": true," +
                        "            \"READ\": true," +
                        "            \"UPDATE\": true" +
                        "    }," +
                        "    \"description\": \"An example resource type\"," +
                        "    \"uuid\": \"123.456.789\"," +
                        "    \"name\": \"myResourceType\"," +
                        "    \"patterns\": [" +
                        "            \"http://example.com:80/*\"" +
                        "    ]" +
                        "}" +
                        ";";

        // Json has unnecessary UUID
        given(content.toString()).willReturn(jsonWithUuid);

        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(), any
                (ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertThat(promise).succeeded();
    }

    @Test
    public void createShouldHandleResourceTypeServiceFailure() throws EntitlementException {
        //given
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        JsonValue content = mock(JsonValue.class);

        given(mockCreateRequest.getContent()).willReturn(content);
        given(mockCreateRequest.getNewResourceId()).willReturn(null);

        given(content.toString()).willReturn(jsonResourceType);

        // simulate failure in the resource type service
        Throwable t = new EntitlementException(EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
        Mockito.doThrow(t).when(resourceTypeService).saveResourceType(any(Subject.class), anyString(), any
                (ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.CONFLICT,
                EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
    }

    private ResourceResponse setupExistingResourceTypeFromJson(String jsonResourceType)
            throws EntitlementException, ExecutionException, InterruptedException {

        //given
        CreateRequest createRequest = mock(CreateRequest.class);
        JsonValue requestContent = mock(JsonValue.class);
        given(createRequest.getContent()).willReturn(requestContent);
        given(requestContent.toString()).willReturn(jsonResourceType);

        doCallRealMethod().when(resourceTypeService).saveResourceType(any(Subject.class), anyString(), any
                (ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.createInstance(mockServerContext, createRequest);

        //then
        assertThat(promise).succeeded().withId();

        return promise.get();
    }

    @Test
    public void shouldReadResourceTypes() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        ReadRequest readRequest = mock(ReadRequest.class);
        when(resourceTypeService.getResourceType(any(Subject.class), anyString(), anyString())).thenCallRealMethod();

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.readInstance(mockServerContext, createdResource.getId(), readRequest);

        //then
        assertThat(promise).succeeded();
    }

    @Test
    public void readShouldHandleServiceException() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        ReadRequest readRequest = mock(ReadRequest.class);
        Throwable t = new EntitlementException(EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
        when(resourceTypeService.getResourceType(any(Subject.class), anyString(), anyString())).thenThrow(t);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.readInstance(mockServerContext, createdResource.getId(), readRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.CONFLICT,
                EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
    }

    @Test
    public void shouldUpdateResourceTypes() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(createdResource.getContent());
        doCallRealMethod().when(resourceTypeService).updateResourceType(any(Subject.class), anyString(), any
                (ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.updateInstance(mockServerContext, createdResource.getId(), updateRequest);

        //then
        assertThat(promise).succeeded();
    }

    @Test
    public void shouldUpdateResourceTypeName() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        JsonValue v = createdResource.getContent();
        v.remove("name");
        v.add("name", "modifiedName");

        UpdateRequest updateRequest = mock(UpdateRequest.class);

        given(updateRequest.getContent()).willReturn(v);
        doCallRealMethod().when(resourceTypeService).updateResourceType(any(Subject.class), anyString(), any
                (ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.updateInstance(mockServerContext, createdResource.getId(), updateRequest);

        //then
        assertThat(promise).succeeded().withContent().stringAt("name").isEqualTo("modifiedName");
    }

    @Test
    public void updateShouldHandleServiceException() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(createdResource.getContent());
        Throwable t = new EntitlementException(EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
        doThrow(t).when(resourceTypeService).updateResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.updateInstance(mockServerContext, createdResource.getId(), updateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.CONFLICT,
                EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
    }

    @Test
    public void updateShouldFailWhenJsonInvalid() throws Exception {
        //given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        // modify value created to contain invalid Json resource type
        JsonValue modifiedValue = createdResource.getContent();
        modifiedValue.remove("name");
        modifiedValue.add("x-y", "z");

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        given(updateRequest.getContent()).willReturn(modifiedValue);

        doCallRealMethod().when(resourceTypeService).updateResourceType(any(Subject.class), anyString(),
                any(ResourceType.class));

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.updateInstance(mockServerContext, createdResource.getId(), updateRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.BAD_REQUEST,
                EntitlementException.INVALID_CLASS);
    }

    @Test
    public void shouldDeleteResourceTypes() throws Exception {
        //Given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        doCallRealMethod().when(resourceTypeService).deleteResourceType(any(Subject.class), anyString(), anyString());

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.deleteInstance(mockServerContext, createdResource.getId(), mockDeleteRequest);

        //then
        assertThat(promise).succeeded().withId().isEqualTo(createdResource.getId());
        assertThat(promise).succeeded().withContent().isObject().containsOnly();
    }

    @Test
    public void deleteShouldHandleServiceException() throws Exception {
        //Given
        ResourceResponse createdResource = setupExistingResourceTypeFromJson(jsonResourceType);

        DeleteRequest deleteRequest = mock(DeleteRequest.class);
        Throwable t = new EntitlementException(EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);
        doThrow(t).when(resourceTypeService).deleteResourceType(any(Subject.class), anyString(), anyString());

        //when
        Promise<ResourceResponse, ResourceException> promise =
                resourceTypesResource.deleteInstance(mockServerContext, createdResource.getId(), deleteRequest);

        //then
        assertResourcePromiseFailedWithCodes(promise, ResourceException.CONFLICT,
                EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS);

    }

    @Test
    public void queryShouldHandleAllResults() throws Exception {
        //given
        QueryRequest queryRequest = makeMockQueryRequest();
        QueryResponseHandler queryHandler = makeQueryResponseHandler();

        Map<String, Map<String, Set<String>>> resourceTypes = new HashMap<>();
        final int resultSize = 10;
        for (int i = 0; i < resultSize; i++) {
            resourceTypes.put(UUID.randomUUID().toString(), rawData);
        }

        when(resourceTypeService.getResourceTypesData(any(Subject.class), anyString())).thenReturn(resourceTypes);
        when(resourceTypeService.getResourceType(any(Subject.class), anyString(), anyString())).thenReturn
                (EntitlementUtils.resourceTypeFromMap(UUID.randomUUID().toString(), rawData));

        //when
        Promise<QueryResponse, ResourceException> promise =
                resourceTypesResource.queryCollection(mockServerContext, queryRequest, queryHandler);

        //then
        QueryResponse queryResponse = promise.getOrThrowUninterruptibly();
        verify(queryHandler, times(resultSize)).handleResource(any(ResourceResponse.class));
        assertThat(queryResponse.getRemainingPagedResults()).isEqualTo(0);
    }

    @Test
    public void queryShouldPageResults() throws Exception {
        //given
        QueryRequest queryRequest = makeMockQueryRequest();

        QueryResponseHandler queryHandler = makeQueryResponseHandler();

        final int firstPageSize = 2;
        final int pageOffset = 0;
        final int resultSize = 10;
        Map<String, Map<String, Set<String>>> resourceTypes = new HashMap<>();
        for (int i = 0; i < resultSize; i++) {
           resourceTypes.put(UUID.randomUUID().toString(), rawData);
        }

        when(resourceTypeService.getResourceTypesData(any(Subject.class), anyString())).thenReturn(resourceTypes);
        when(resourceTypeService.getResourceType(any(Subject.class), anyString(), anyString())).thenReturn
               (EntitlementUtils.resourceTypeFromMap(UUID.randomUUID().toString(), rawData));
        when(queryRequest.getPagedResultsOffset()).thenReturn(pageOffset);
        when(queryRequest.getPageSize()).thenReturn(firstPageSize);

        Answer<Boolean> onlyFirstPage = new Answer<Boolean>() {
            int count = 0;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                return count++ < firstPageSize;
            }
        };

        when(queryHandler.handleResource(any(ResourceResponse.class))).thenAnswer(onlyFirstPage);

        //when
        Promise<QueryResponse, ResourceException> promise =
                resourceTypesResource.queryCollection(mockServerContext, queryRequest, queryHandler);

        //then
        QueryResponse queryResponse = promise.getOrThrowUninterruptibly();
        verify(queryHandler, times(firstPageSize)).handleResource(any(ResourceResponse.class));
        assertThat(queryResponse.getRemainingPagedResults()).isEqualTo(resultSize - firstPageSize);
    }

    @Test
    public void shouldHandleQueryPageLargerThanResults() throws Exception {
        //given
        QueryRequest queryRequest = makeMockQueryRequest();

        QueryResponseHandler queryHandler = makeQueryResponseHandler();

        final int lastPageSize = 5;
        final int pageOffset = 10;
        final int resultSize = 11;
        Map<String, Map<String, Set<String>>> resourceTypes = new HashMap<>();
        for (int i = 0; i < resultSize; i++) {
            resourceTypes.put(UUID.randomUUID().toString(), rawData);
        }

        when(resourceTypeService.getResourceTypesData(any(Subject.class), anyString())).thenReturn(resourceTypes);
        when(resourceTypeService.getResourceType(any(Subject.class), anyString(), anyString())).thenReturn
                (EntitlementUtils.resourceTypeFromMap(UUID.randomUUID().toString(), rawData));
        when(queryRequest.getPagedResultsOffset()).thenReturn(pageOffset);
        when(queryRequest.getPageSize()).thenReturn(lastPageSize);

        Answer<Boolean> onlyFirstPage = new Answer<Boolean>() {
            int count = pageOffset;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                return count++ < resultSize;
            }
        };

        when(queryHandler.handleResource(any(ResourceResponse.class))).thenAnswer(onlyFirstPage);

        //when
        Promise<QueryResponse, ResourceException> promise =
                resourceTypesResource.queryCollection(mockServerContext, queryRequest, queryHandler);

        //then
        QueryResponse queryResponse = promise.getOrThrowUninterruptibly();
        verify(queryHandler, times(resultSize - pageOffset)).handleResource(any(ResourceResponse.class));
        assertThat(queryResponse.getRemainingPagedResults()).isEqualTo(0);
    }


    @Test
    public void queryShouldHandleQueryException() throws Exception {
        //given
        setupExistingResourceTypeFromJson(jsonResourceType);

        QueryRequest queryRequest = mock(QueryRequest.class);
        QueryResponseHandler queryHandler = makeQueryResponseHandler();
        Throwable t = new QueryException(QueryException.QueryErrorCode.FILTER_BOOLEAN_LITERAL_FALSE);
        when(resourceTypeService.getResourceTypesData(any(Subject.class), anyString())).thenThrow(t);

        //when
        Promise<QueryResponse, ResourceException> promise =
                resourceTypesResource.queryCollection(mockServerContext, queryRequest, queryHandler);

        //then
        assertThat(promise).failedWithResourceException().withCode(ResourceException.BAD_REQUEST);
    }

    /**
     * Generates a mock request which will simulate the required behaviour of requesting
     * the now deprecated behaviour of returning the remaining results.
     *
     * @return Non null.
     */
    private static QueryRequest makeMockQueryRequest() {
        QueryRequest mockRequest = mock(QueryRequest.class);
        given(mockRequest.getAdditionalParameter(QueryResponsePresentation.REMAINING)).willReturn("true");
        return mockRequest;
    }

    private static QueryResponseHandler makeQueryResponseHandler() {
        QueryResponseHandler mock = mock(QueryResponseHandler.class);
        given(mock.handleResource(any(ResourceResponse.class))).willReturn(true);
        return mock;
    }

}
