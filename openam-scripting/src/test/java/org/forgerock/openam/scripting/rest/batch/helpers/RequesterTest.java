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
package org.forgerock.openam.scripting.rest.batch.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import javax.inject.Provider;

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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Intentionally strict unit tests. The class under test will be used by SDK component authors.
 */
public class RequesterTest {

    Context mockServerContext;
    Requester requester;
    Provider<Router> mockRealmRouterProvider;
    Router mockRouter;

    @BeforeTest
    private void theSetUp() { // you need this
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("REALM", "REALM");
        mockServerContext = mock(Context.class);
    }

    @BeforeMethod
    private void setUpTestMethod() {
        mockRealmRouterProvider = mock(Provider.class);
        mockRouter = mock(Router.class);

        requester = new Requester(mockRealmRouterProvider);
    }

    //
    // test create()
    //

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksCreate() throws ResourceException {
        // given
        String location = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.create(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksCreate() throws ResourceException {
        // given
        String location = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.create(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullPayloadBreaksCreate() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = null;

        // when
        requester.create(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteCreateRequest() throws ResourceException {

        // *** GIVEN ***
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Create mocks
        Promise<ResourceResponse, ResourceException> mockResponse = mock(Promise.class);
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        ;

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleCreate(any(Context.class), any(CreateRequest.class))).willReturn(mockResponse);
        given(mockResponse.getOrThrowUninterruptibly()).willReturn(mockResourceResponse);
        given(mockResourceResponse.getContent()).willReturn(expectedResult);

        // *** WHEN ***
        JsonValue actualResult = requester.create(location, resourceId, payload, mockServerContext);

        // *** THEN ***
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleCreate(any(Context.class), any(CreateRequest.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResponse);

        verify(mockResourceResponse, times(1)).getContent();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    //
    // Test read()
    //

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksRead() throws ResourceException {
        // given
        String location = null;
        String resourceId = "resourceId";

        // when
        requester.read(location, resourceId, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksRead() throws ResourceException {
        // given
        String location = "";
        String resourceId = "resourceId";

        // when
        requester.read(location, resourceId, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksRead() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "";

        // when
        requester.read(location, resourceId, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksRead() throws ResourceException {
        // given
        String location = "location";
        String resourceId = null;

        // when
        requester.read(location, resourceId, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteReadOperation() throws ResourceException {
        // *** GIVEN ***
        String location = "location";
        String resourceId = "resourceId";
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Set up mocks
        Promise<ResourceResponse, ResourceException> mockResponse = mock(Promise.class);
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleRead(any(Context.class), any(ReadRequest.class))).willReturn(mockResponse);
        given(mockResponse.getOrThrowUninterruptibly()).willReturn(mockResourceResponse);
        given(mockResourceResponse.getContent()).willReturn(expectedResult);

        // *** WHEN ***
        JsonValue actualResult = requester.read(location, resourceId, mockServerContext);

        // *** THEN ***
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleRead(any(Context.class), any(ReadRequest.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResponse);

        verify(mockResourceResponse, times(1)).getContent();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    //
    // Test update()
    //

    @Test(expectedExceptions = NullPointerException.class)
    public void nullPayloadBreaksUpdate() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = null;

        // when
        requester.update(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksUpdate() throws ResourceException {
        // given
        String location = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.update(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksUpdate() throws ResourceException {
        // given
        String location = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.update(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksUpdate() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.update(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksUpdate() throws ResourceException {
        // given
        String location = "location";
        String resourceId = null;
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.update(location, resourceId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteUpdateOperation() throws ResourceException {
        // *** GIVEN ***
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Set up mocks
        Promise<ResourceResponse, ResourceException> mockResponse = mock(Promise.class);
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleUpdate(any(Context.class), any(UpdateRequest.class))).willReturn(mockResponse);
        given(mockResponse.getOrThrowUninterruptibly()).willReturn(mockResourceResponse);
        given(mockResourceResponse.getContent()).willReturn(expectedResult);

        // *** WHEN ***
        JsonValue actualResult = requester.update(location, resourceId, payload, mockServerContext);

        // *** THEN ***
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleUpdate(any(Context.class), any(UpdateRequest.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResponse);

        verify(mockResourceResponse, times(1)).getContent();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);

    }

    //
    // test delete()
    //

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksDelete() throws ResourceException {
        // given
        String location = "";
        String resourceId = "resourceId";
        Context fakeContext = mock(Context.class);

        // when
        requester.delete(location, resourceId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksDelete() throws ResourceException {
        // given
        String location = null;
        String resourceId = "resourceId";
        Context fakeContext = mock(Context.class);

        // when
        requester.delete(location, resourceId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksDelete() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "";
        Context fakeContext = mock(Context.class);

        // when
        requester.delete(location, resourceId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksDelete() throws ResourceException {
        // given
        String location = "location";
        String resourceId = null;
        Context fakeContext = mock(Context.class);

        // when
        requester.delete(location, resourceId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteDeleteRequest() throws ResourceException {
        // given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Set up mocks
        Promise<ResourceResponse, ResourceException> mockResponse = mock(Promise.class);
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleDelete(any(Context.class), any(DeleteRequest.class))).willReturn(mockResponse);
        given(mockResponse.getOrThrowUninterruptibly()).willReturn(mockResourceResponse);
        given(mockResourceResponse.getContent()).willReturn(expectedResult);

        // when
        JsonValue actualResult = requester.delete(location, resourceId, mockServerContext);

        // then
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleDelete(any(Context.class), any(DeleteRequest.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResponse);

        verify(mockResourceResponse, times(1)).getContent();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    //
    // test action()
    //

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullActionIdBreaksAction() throws ResourceException {
        // given
        String location = "location";
        String actionId = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyActionIdBreaksAction() throws ResourceException {
        // given
        String location = "location";
        String actionId = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksAction() throws ResourceException {
        // given
        String location = null;
        String actionId = "actionId";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksAction() throws ResourceException {
        // given
        String location = "";
        String actionId = "actionId";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        // when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteActionOperation() throws ResourceException {
        // GIVEN
        String location = "location";
        String resourceId = "resourceId";
        String actionId = "actionId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Set up mocks
        Promise<ActionResponse, ResourceException> mockResponse = mock(Promise.class);
        ActionResponse mockResourceResponse = mock(ActionResponse.class);

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleAction(any(Context.class), any(ActionRequest.class))).willReturn(mockResponse);
        given(mockResponse.getOrThrowUninterruptibly()).willReturn(mockResourceResponse);
        given(mockResourceResponse.getJsonContent()).willReturn(expectedResult);

        // WHEN
        JsonValue actualResult = requester.action(location, resourceId, actionId, payload, mockServerContext);

        // THEN
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleAction(any(Context.class), any(ActionRequest.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResponse);

        verify(mockResourceResponse, times(1)).getJsonContent();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    //
    // test query()
    //

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksQuery() throws ResourceException {
        // given
        String location = "";
        String queryId = "queryId";
        Context fakeContext = mock(Context.class);

        // when
        requester.query(location, queryId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksQuery() throws ResourceException {
        // given
        String location = null;
        String queryId = "queryId";
        Context fakeContext = mock(Context.class);

        // when
        requester.query(location, queryId, fakeContext);

        // then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteQueryOperation() throws ResourceException {
        // given
        String location = "location";
        String queryId = "queryId";
        JsonValue expectedResult = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        // Create mocks
        Promise<QueryResponse, ResourceException> mockResponse = mock(Promise.class);
        QueryResponse mockResourceResponse = mock(QueryResponse.class);
        Promise<JsonValue, ResourceException> asyncQueryResponse = mock(Promise.class);

        // Stub mocks
        given(mockRealmRouterProvider.get()).willReturn(mockRouter);
        given(mockRouter.handleQuery(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
                .willReturn(mockResponse);
        given(mockResponse.thenAsync(any(AsyncFunction.class))).willReturn(asyncQueryResponse);
        given(asyncQueryResponse.getOrThrowUninterruptibly()).willReturn(expectedResult);

        // when
        JsonValue actualResult = requester.query(location, queryId, mockServerContext);

        // then
        verify(mockRealmRouterProvider, times(1)).get();
        verifyNoMoreInteractions(mockRealmRouterProvider);

        verify(mockRouter, times(1)).handleQuery(any(Context.class), any(QueryRequest.class),
                any(QueryResourceHandler.class));
        verifyNoMoreInteractions(mockRouter);

        verify(mockResponse, times(1)).thenAsync(any(AsyncFunction.class));
        verifyNoMoreInteractions(mockResponse);

        verify(asyncQueryResponse, times(1)).getOrThrowUninterruptibly();
        verifyNoMoreInteractions(mockResourceResponse);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

}
