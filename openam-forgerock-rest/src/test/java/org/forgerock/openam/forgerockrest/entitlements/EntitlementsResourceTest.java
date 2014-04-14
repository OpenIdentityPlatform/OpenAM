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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class EntitlementsResourceTest {

    private PolicyParser mockParser;
    private PolicyStoreProvider mockStoreProvider;
    private PolicyStore mockStore;
    private ResourceErrorHandler resourceErrorHandler;
    private ServerContext mockServerContext;
    private ResultHandler<Resource> mockResultHandler;

    private EntitlementsResource entitlementsResource;

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
        mockParser = mock(PolicyParser.class);
        mockStoreProvider = mock(PolicyStoreProvider.class);
        mockStore = mock(PolicyStore.class);
        mockServerContext = mock(ServerContext.class);
        mockResultHandler = mock(ResultHandler.class);

        given(mockStoreProvider.getPolicyStore(any(ServerContext.class))).willReturn(mockStore);

        // Use a real error handler as this is a core part of the functionality we are testing and doesn't need to be mocked
        resourceErrorHandler = new EntitlementsResourceErrorHandler(ForgerockRestGuiceModule.getEntitlementsErrorHandlers());

        entitlementsResource = new EntitlementsResource(mockParser, mockStoreProvider, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullParser() {
        new EntitlementsResource(null, mockStoreProvider, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullStoreProvider() {
        new EntitlementsResource(mockParser, null, resourceErrorHandler);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullErrorHandler() {
        new EntitlementsResource(mockParser, mockStoreProvider, null);
    }

    @Test
    public void shouldReportBadCreateRequests() throws Exception {
        // Given
        String id = "uniqueId";
        JsonValue json = new JsonValue("");
        CreateRequest request = mockCreateRequest(id, json);

        given(mockParser.parsePolicy(id, json)).willThrow(new EntitlementException(EntitlementException.INVALID_JSON));

        // When
        entitlementsResource.createInstance(mockServerContext, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleError(isA(BadRequestException.class));
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
        entitlementsResource.createInstance(mockServerContext, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleError(isA(InternalServerErrorException.class));
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
        entitlementsResource.createInstance(mockServerContext, request, mockResultHandler);

        // Then
        verify(mockStore).create(policy);
        verify(mockResultHandler).handleResult(new Resource(id, Long.toString(lastModified), result));
    }

    @Test
    public void shouldDeletePoliciesFromStore() throws Exception {
        // Given
        String id = "uniqueId";
        DeleteRequest request = mock(DeleteRequest.class);

        // When
        entitlementsResource.deleteInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockStore).delete(id);
        verify(mockResultHandler).handleResult(new Resource(id, null, JsonValue.json("Deleted")));
    }

    @Test
    public void shouldRejectNullPolicyIdInDelete() throws Exception {
        // Given
        String id = null;
        DeleteRequest request = mock(DeleteRequest.class);
        willThrow(new EntitlementException(EntitlementException.MISSING_PRIVILEGE_NAME))
                .given(mockStore).delete(id);

        // When
        entitlementsResource.deleteInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleError(isA(BadRequestException.class));
    }

    @Test
    public void shouldReportMissingPoliciesInDelete() throws Exception {
        // Given
        String id = "unknown";
        DeleteRequest request = mock(DeleteRequest.class);
        willThrow(new EntitlementException(EntitlementException.NO_SUCH_POLICY))
                .given(mockStore).delete(id);

        // When
        entitlementsResource.deleteInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleError(isA(NotFoundException.class));
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
        entitlementsResource.readInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleResult(new Resource(id, Long.toString(lastModified), content));
    }

    @Test
    public void shouldReportMissingPoliciesInRead() throws Exception {
        // Given
        String id = "unknown";
        given(mockStore.read(id)).willThrow(new EntitlementException(EntitlementException.NO_SUCH_POLICY));
        ReadRequest request = mock(ReadRequest.class);

        // When
        entitlementsResource.readInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleError(isA(NotFoundException.class));
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
        given(mockStore.update(privilege)).willReturn(privilege);

        // When
        entitlementsResource.updateInstance(mockServerContext, id, request, mockResultHandler);

        // Then
        verify(mockResultHandler).handleResult(new Resource(id, Long.toString(lastModified), content));
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
        given(mockRequest.getResourceName()).willReturn("");
        given(mockRequest.getContent()).willReturn(content);
        return mockRequest;
    }
}
