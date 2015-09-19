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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.entitlement.rest.EntitlementTestUtils.assertQueryPromiseFailedWithCodes;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.resource.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class PolicyResourceTest {

    @Mock
    private PolicyParser mockParser;

    @Mock
    private PolicyStoreProvider mockStoreProvider;

    @Mock
    private PolicyStore mockStore;

    private ExceptionMappingHandler resourceErrorHandler;

    @Mock
    private Context mockServerContext;

    @Mock
    private ResultHandler<Resource> mockResultHandler;

    @Mock
    private PolicyEvaluatorFactory mockFactory;

    @Mock
    private PolicyRequestFactory requestFactory;

    private PolicyResource policyResource;

    @BeforeClass
    public void mockPrivilegeClass() {
        System.setProperty(Privilege.PRIVILEGE_CLASS_PROPERTY, StubPrivilege.class.getName());
    }

    @AfterClass
    public void unmockPrivilegeClass() {
        System.clearProperty(Privilege.PRIVILEGE_CLASS_PROPERTY);
    }

    @BeforeMethod
    public void setupMocks() throws Exception {
        MockitoAnnotations.initMocks(this);

        given(mockStoreProvider.getPolicyStore(any(Context.class))).willReturn(mockStore);

        // Use a real error handler as this is a core part of the
        // functionality we are testing and doesn't need to be mocked
        resourceErrorHandler =
                new EntitlementsExceptionMappingHandler(EntitlementRestGuiceModule.getEntitlementsErrorHandlers());

        policyResource =
                new PolicyResource(mockFactory, requestFactory, mockParser, mockStoreProvider, resourceErrorHandler);

        when(mockServerContext.asContext(HttpContext.class)).thenThrow(new IllegalArgumentException());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullParser() {
        new PolicyResource(mockFactory, requestFactory, null, mockStoreProvider, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullRequestFactory() {
        new PolicyResource(mockFactory, null, mockParser, mockStoreProvider, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullStoreProvider() {
        new PolicyResource(mockFactory, requestFactory, mockParser, null, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullErrorHandler() {
        new PolicyResource(mockFactory, requestFactory, mockParser, mockStoreProvider, null);
    }

    @Test
    public void shouldReportBadCreateRequests() throws Exception {
        // Given
        String id = "uniqueId";
        JsonValue json = new JsonValue("");
        CreateRequest request = mockCreateRequest(id, json);

        given(mockParser.parsePolicy(id, json)).willThrow(new EntitlementException(EntitlementException.INVALID_JSON,
                "Mock error message"));

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.createInstance(mockServerContext, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldReportCreatePolicyStoreErrors() throws Exception {
        // Given
        String id = "uniqueId";
        JsonValue json = new JsonValue("");

        CreateRequest request = mockCreateRequest(id, json);
        Privilege policy = mockPrivilege(id, 123l);

        given(mockParser.parsePolicy(id, json)).willReturn(policy);
        willThrow(new EntitlementException(EntitlementException.INVALID_APPLICATION_CLASS))
                .given(mockStore).create(policy);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.createInstance(mockServerContext, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldAcceptPolicyNameFromUrl() throws Exception {
        // Given
        String policyName = "policyName";
        JsonValue json = new JsonValue("");

        CreateRequest request = mockCreateRequest(policyName, json);
        Privilege policy = mockPrivilege(policyName, 123l);
        given(mockParser.parsePolicy(policyName, json)).willReturn(policy);

        // When
        policyResource.createInstance(mockServerContext, request);

        // Then
        verify(mockParser).parsePolicy(policyName, json);
    }

    @Test
    public void shouldAcceptConsistentPolicyNamesFromURLandJSON() throws Exception {
        // Given
        String policyName = "policyName";
        // Policy name can be specified in *both* URL and JSON so long as it is equal
        JsonValue json = JsonValue.json(JsonValue.object(JsonValue.field("name", policyName)));

        CreateRequest request = mockCreateRequest(policyName, json);
        Privilege policy = mockPrivilege(policyName, 123l);
        given(mockParser.parsePolicy(policyName, json)).willReturn(policy);

        // When
        policyResource.createInstance(mockServerContext, request);

        // Then
        verify(mockParser).parsePolicy(policyName, json);
    }

    @Test
    public void shouldRejectMismatchedPolicyName() throws Exception {
        // Given
        String policyName = "policyName";
        String differentPolicyName = "Different!";
        JsonValue json = JsonValue.json(JsonValue.object(JsonValue.field("name", policyName)));
        CreateRequest request = mockCreateRequest(differentPolicyName, json);

        Privilege policy = mockPrivilege(policyName, 123l);
        given(mockParser.parsePolicy(differentPolicyName, json)).willReturn(policy);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.createInstance(mockServerContext, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldCreatePoliciesInStore() throws Exception {
        // Given
        String id = "uniqueId";
        long lastModified = 12345l;
        JsonValue json = new JsonValue("");

        CreateRequest request = mockCreateRequest(id, json);
        Privilege policy = mockPrivilege(id, lastModified);
        JsonValue result = new JsonValue("result");

        given(mockParser.parsePolicy(id, json)).willReturn(policy);
        given(mockParser.printPolicy(policy)).willReturn(result);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.createInstance(mockServerContext, request);

        // Then
        verify(mockStore).create(policy);
        assertThat(promise).succeeded().withId().isNotNull();
    }

    @Test
    public void shouldDeletePoliciesFromStore() throws Exception {
        // Given
        String id = "uniqueId";
        DeleteRequest request = mock(DeleteRequest.class);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.deleteInstance(mockServerContext, id, request);

        // Then
        verify(mockStore).delete(id);
        assertThat(promise).succeeded().withId().isNotNull();
    }

    @Test
    public void shouldRejectNullPolicyIdInDelete() throws Exception {
        // Given
        String id = null;
        DeleteRequest request = mock(DeleteRequest.class);
        willThrow(new EntitlementException(EntitlementException.MISSING_PRIVILEGE_NAME))
                .given(mockStore).delete(id);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.deleteInstance(mockServerContext, id, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldReportMissingPoliciesInDelete() throws Exception {
        // Given
        String id = "unknown";
        DeleteRequest request = mock(DeleteRequest.class);
        willThrow(new EntitlementException(EntitlementException.NO_SUCH_POLICY))
                .given(mockStore).delete(id);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.deleteInstance(mockServerContext, id, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldReadPoliciesFromStore() throws Exception {
        // Given
        String id = "testPolicy";
        long lastModified = 1234l;
        Privilege policy = mockPrivilege(id, lastModified);
        ReadRequest request = mock(ReadRequest.class);
        given(mockStore.read(id)).willReturn(policy);
        JsonValue content = new JsonValue("content");
        given(mockParser.printPolicy(policy)).willReturn(content);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.readInstance(mockServerContext, id, request);

        // Then
        assertThat(promise).succeeded().withId().isNotNull();
    }

    @Test
    public void shouldReportMissingPoliciesInRead() throws Exception {
        // Given
        String id = "unknown";
        given(mockStore.read(id)).willThrow(new EntitlementException(EntitlementException.NO_SUCH_POLICY));
        ReadRequest request = mock(ReadRequest.class);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.readInstance(mockServerContext, id, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldUpdatePoliciesInStore() throws Exception {
        // Given
        String id = "testPolicy";
        long lastModified = 1234l;
        UpdateRequest request = mock(UpdateRequest.class);
        JsonValue content = new JsonValue("content");
        given(request.getContent()).willReturn(content);
        Privilege privilege = mockPrivilege(id, lastModified);
        given(mockParser.parsePolicy(id, content)).willReturn(privilege);
        given(mockStore.update(id, privilege)).willReturn(privilege);

        // When
        Promise<ResourceResponse, ResourceException> promise =
                policyResource.updateInstance(mockServerContext, id, request);

        // Then
        assertThat(promise).succeeded().withId().isNotNull();
    }

    @Test
    public void shouldHandleQueryRequests() throws Exception {
        // Given
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        List<Privilege> policies = Arrays.<Privilege>asList(
                new StubPrivilege("one"),
                new StubPrivilege("two")
        );
        given(mockStore.query(request)).willReturn(policies);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When
        policyResource.queryCollection(mockServerContext, request, handler);

        // Then
        verify(handler, times(policies.size())).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void shouldHandleInvalidQueryErrors() throws Exception {
        // Given
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(mockStore.query(request)).willThrow(new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER));

        // When
        Promise<QueryResponse, ResourceException> promise =
                policyResource.queryCollection(mockServerContext, request, handler);

        // Then
        assertQueryPromiseFailedWithCodes(promise, ResourceException.BAD_REQUEST,
                EntitlementException.INVALID_SEARCH_FILTER);
    }

    private Privilege mockPrivilege(String name, long lastModified) throws EntitlementException {
        Privilege mock = new StubPrivilege();
        mock.setName(name);
        mock.setLastModifiedDate(lastModified);
        return mock;
    }

    private CreateRequest mockCreateRequest(String resourceName, JsonValue content) {
        CreateRequest mockRequest = mock(CreateRequest.class);
        given(mockRequest.getNewResourceId()).willReturn(resourceName);
        given(mockRequest.getContent()).willReturn(content);
        return mockRequest;
    }
}
