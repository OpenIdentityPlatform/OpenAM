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
package org.forgerock.openam.rest.batch.helpers;

import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import javax.inject.Provider;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.CrestRealmRouter;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Intentionally strict unit tests.
 * The class under test will be used by SDK component authors.
 */
public class RequesterTest {

    CrestRealmRouter mockRealmRouter;
    SDKResultHandlerFactory resultHandlerFactory;
    ServerContext mockServerContext;
    Requester requester;

    @BeforeTest
    private void theSetUp() { //you need this
        mockRealmRouter = mock(CrestRealmRouter.class);
        resultHandlerFactory = mock(SDKResultHandlerFactory.class);
        Provider<CrestRouter> mockRealmRouterProvider = mock(Provider.class);
        given(mockRealmRouterProvider.get()).willReturn(mockRealmRouter);

        requester = new Requester(mockRealmRouterProvider, resultHandlerFactory);

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("REALM", "REALM");
        mockServerContext = new ServerContext(realmContext);
    }

    //create

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksCreate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.create(location, resourceId, payload, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksCreate() throws ResourceException {
        //given
        String location = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.create(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksCreate() throws ResourceException {
        //given
        String location = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.create(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void nullPayloadBreaksCreate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = null;

        //when
        requester.create(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteCreateRequest() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue result = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        SDKServerResultHandler<Resource> mockResultHandler = mock(SDKServerResultHandler.class);
        given(resultHandlerFactory.getResourceResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(new Resource("Id", "1.0", result));

        //when
        JsonValue created = requester.create(location, resourceId, payload, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleCreate(eq(mockServerContext), any(CreateRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();
        assertEquals(result, created);
    }

    //read

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksRead() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.read(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksRead() throws ResourceException {
        //given
        String location = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.create(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksRead() throws ResourceException {
        //given
        String location = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.create(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksRead() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "";

        //when
        requester.read(location, resourceId, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksRead() throws ResourceException {
        //given
        String location = "location";
        String resourceId = null;

        //when
        requester.read(location, resourceId, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteReadOperation() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";

        SDKServerResultHandler<Resource> mockResultHandler = mock(SDKServerResultHandler.class);
        given(resultHandlerFactory.getResourceResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(new Resource(null, "", JsonValueBuilder.jsonValue().build()));

        //when
        requester.read(location, resourceId, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleRead(eq(mockServerContext), any(ReadRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();
    }

    //update

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksUpdate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.update(location, resourceId, payload, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void nullPayloadBreaksUpdate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = null;

        //when
        requester.update(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksUpdate() throws ResourceException {
        //given
        String location = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.update(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksUpdate() throws ResourceException {
        //given
        String location = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.update(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksUpdate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.update(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksUpdate() throws ResourceException {
        //given
        String location = "location";
        String resourceId = null;
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.update(location, resourceId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteUpdateOperation() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue result = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        SDKServerResultHandler<Resource> mockResultHandler = mock(SDKServerResultHandler.class);
        given(resultHandlerFactory.getResourceResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(new Resource("result", "1.0", result));

        //when
        JsonValue updated = requester.update(location, resourceId, payload, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleUpdate(eq(mockServerContext), any(UpdateRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();

        assertEquals(updated, result);
    }

    //delete

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksDelete() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.delete(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksDelete() throws ResourceException {
        //given
        String location = "";
        String resourceId = "resourceId";
        Context fakeContext = mock(Context.class);

        //when
        requester.delete(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksDelete() throws ResourceException {
        //given
        String location = null;
        String resourceId = "resourceId";
        Context fakeContext = mock(Context.class);

        //when
        requester.delete(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }


    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceIdBreaksDelete() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "";
        Context fakeContext = mock(Context.class);

        //when
        requester.delete(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullResourceIdBreaksDelete() throws ResourceException {
        //given
        String location = "location";
        String resourceId = null;
        Context fakeContext = mock(Context.class);

        //when
        requester.delete(location, resourceId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteDeleteRequest() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        JsonValue result = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        SDKServerResultHandler<Resource> mockResultHandler = mock(SDKServerResultHandler.class);
        given(resultHandlerFactory.getResourceResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(new Resource("result", "1.0", result));

        //when
        JsonValue deleted = requester.delete(location, resourceId, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleDelete(eq(mockServerContext), any(DeleteRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();
        assertEquals(result, deleted);
    }

    //patch

    //action

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksAction() throws ResourceException {
        //given
        String location = "location";
        String actionId = "actionId";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.action(location, resourceId, actionId, payload, fakeContext);

        //then -- matched by expectedExceptions
    }


    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullActionIdBreaksAction() throws ResourceException {
        //given
        String location = "location";
        String actionId = null;
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyActionIdBreaksAction() throws ResourceException {
        //given
        String location = "location";
        String actionId = "";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksAction() throws ResourceException {
        //given
        String location = null;
        String actionId = "actionId";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksAction() throws ResourceException {
        //given
        String location = "";
        String actionId = "actionId";
        String resourceId = "resourceId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();

        //when
        requester.action(location, resourceId, actionId, payload, mockServerContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteActionOperation() throws ResourceException {
        //given
        String location = "location";
        String resourceId = "resourceId";
        String actionId = "actionId";
        JsonValue payload = JsonValueBuilder.jsonValue().build();
        JsonValue result = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        SDKServerResultHandler<JsonValue> mockResultHandler = mock(SDKServerResultHandler.class);
        given(resultHandlerFactory.getJsonValueResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(result);

        //when
        JsonValue actioned = requester.action(location, resourceId, actionId, payload, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleAction(eq(mockServerContext), any(ActionRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();
        assertEquals(actioned, result);
    }

    //query

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void invalidContextBreaksQuery() throws ResourceException {
        //given
        String location = "location";
        String queryId = "queryId";
        Context fakeContext = mock(AbstractContext.class);

        //when
        requester.query(location, queryId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void emptyLocationBreaksQuery() throws ResourceException {
        //given
        String location = "";
        String queryId = "queryId";
        Context fakeContext = mock(Context.class);

        //when
        requester.query(location, queryId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void nullLocationBreaksQuery() throws ResourceException {
        //given
        String location = null;
        String queryId = "queryId";
        Context fakeContext = mock(Context.class);

        //when
        requester.query(location, queryId, fakeContext);

        //then -- matched by expectedExceptions
    }

    @Test
    public void shouldExecuteQueryOperation() throws ResourceException {
        //given
        String location = "location";
        String queryId = "queryId";
        JsonValue result = JsonValueBuilder.toJsonValue("{ \"lemon\" : \"custard\" } ");

        SDKServerQueryResultHandler mockResultHandler = mock(SDKServerQueryResultHandler.class);
        given(resultHandlerFactory.getQueryResultHandler()).willReturn(mockResultHandler);
        given(mockResultHandler.getResource()).willReturn(result);

        //when
        JsonValue queried = requester.query(location, queryId, mockServerContext);

        //then
        verify(mockRealmRouter, times(1)).handleQuery(eq(mockServerContext), any(QueryRequest.class),
                eq(mockResultHandler));
        verify(mockResultHandler, times(1)).getResource();
        assertEquals(queried, result);
    }

}
