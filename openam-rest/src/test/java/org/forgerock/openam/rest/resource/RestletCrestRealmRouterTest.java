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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.resource;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class RestletCrestRealmRouterTest {


    private Connection connection;

    private CollectionResourceProvider usersProvider;
    private CollectionResourceProvider groupsProvider;

    @BeforeClass
    private void setUpClass() {
        usersProvider = mock(CollectionResourceProvider.class);
        groupsProvider = mock(CollectionResourceProvider.class);
    }

    @BeforeMethod
    public void setUp() {
        reset(usersProvider, groupsProvider);

        RestRealmValidator realmValidator = mock(RestRealmValidator.class);
        CrestRealmRouter router = new CrestRealmRouter(realmValidator);

        connection = Resources.newInternalConnection(router);

        router.addRoute("/users", collection(usersProvider));
        router.addRoute("/groups", collection(groupsProvider));

        given(realmValidator.isRealm(anyString())).willReturn(true);
    }

    private Context ctx() {
        SecurityContext securityContext = new SecurityContext(new RootContext(), "",
                Collections.<String, Object>emptyMap());
        return new ServerContext(securityContext);
    }

    @DataProvider(name = "data")
    private Object[][] dataProvider() {
        return new Object[][]{
                {"users", "alice", "/", usersProvider},
                {"/realm1/realm2/users", "alice", "/realm1/realm2", usersProvider},
                {"groups", "admin", "/", groupsProvider},
                {"/realm1/realm2/groups", "admin", "/realm1/realm2", groupsProvider},
        };
    }

    @Test (dataProvider = "data")
    public void shouldCreateFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.create(ctx(), Requests.newCreateRequest(resourceContainer, resourceName, json(object())));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).createInstance(contextCaptor.capture(), Matchers.<CreateRequest>anyObject(),
                Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldReadFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.read(ctx(), Requests.newReadRequest(resourceContainer, resourceName));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).readInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<ReadRequest>anyObject(), Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldUpdateFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.update(ctx(), Requests.newUpdateRequest(resourceContainer, resourceName, json(object())));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).updateInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<UpdateRequest>anyObject(), Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldDeleteFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.delete(ctx(), Requests.newDeleteRequest(resourceContainer, resourceName));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).deleteInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<DeleteRequest>anyObject(), Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldPatchFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.patch(ctx(), Requests.newPatchRequest(resourceContainer, resourceName));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).patchInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<PatchRequest>anyObject(), Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldActionFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given

        //When
        connection.action(ctx(), Requests.newActionRequest(resourceContainer, resourceName, "ACTION_ID"));

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).actionInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<ActionRequest>anyObject(), Matchers.<ResultHandler<JsonValue>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    @Test (dataProvider = "data")
    public void shouldQueryFromRealm(String resourceContainer, String resourceName, String expectedRealm,
            CollectionResourceProvider expectedProvider) throws ResourceException {

        //Given
        QueryResultHandler handler = mock(QueryResultHandler.class);

        //When
        connection.query(ctx(), Requests.newQueryRequest(resourceContainer), handler);

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).queryCollection(contextCaptor.capture(), Matchers.<QueryRequest>anyObject(),
                Matchers.<QueryResultHandler>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(expectedRealm);
    }

    private static CollectionResourceProvider collection(final CollectionResourceProvider provider) {
        return new CollectionResourceProvider() {

            @Override
            public void actionCollection(ServerContext context, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionCollection(context, request, handler);
                handler.handleResult(json(object()));
            }

            @Override
            public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionInstance(context, resourceId, request, handler);
                handler.handleResult(json(object()));
            }

            @Override
            public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
                provider.createInstance(context, request, handler);
                handler.handleResult(new Resource("ID", "1", json(object())));
            }

            @Override
            public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                    ResultHandler<Resource> handler) {
                provider.deleteInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                    ResultHandler<Resource> handler) {
                provider.patchInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
                provider.queryCollection(context, request, handler);
                handler.handleResult(new QueryResult());
            }

            @Override
            public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                    ResultHandler<Resource> handler) {
                provider.readInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                    ResultHandler<Resource> handler) {
                provider.updateInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }
        };
    }
}
