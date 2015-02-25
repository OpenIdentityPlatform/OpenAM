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

package org.forgerock.openam.rest.oauth2;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.UmaPolicyService;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetResourceTest {

    private ResourceSetResource resource;

    private ResourceSetStore resourceSetStore;

    @BeforeMethod
    public void setup() {

        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        resourceSetStore = mock(ResourceSetStore.class);
        UmaPolicyService policyService = mock(UmaPolicyService.class);

        resource = new ResourceSetResource(resourceSetStoreFactory, policyService);

        given(resourceSetStoreFactory.create("REALM")).willReturn(resourceSetStore);
    }

    @Test
    public void shouldReadResourceSet() throws Exception {

        //Given
        RealmContext realmContext = new RealmContext(new RootContext());
        realmContext.addDnsAlias("", "REALM");
        ServerContext context = new ServerContext(realmContext);
        ReadRequest request = mock(ReadRequest.class);
        given(request.getFields()).willReturn(Arrays.asList(new JsonPointer("/fred")));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setDescription(json(object()));

        given(resourceSetStore.read("RESOURCE_SET_UID")).willReturn(resourceSet);

        //When
        resource.readInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleResult(Matchers.<Resource>anyObject());
    }

    @Test
    public void createShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        CreateRequest request = mock(CreateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        resource.createInstance(context, request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void updateShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        resource.updateInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void deleteShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        resource.deleteInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void patchShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        PatchRequest request = mock(PatchRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        resource.patchInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void actionInstanceShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = mock(ActionRequest.class);
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        resource.actionInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void actionCollectionShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = mock(ActionRequest.class);
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void queryShouldNotBeSupported() {

        //Given
        ServerContext context = mock(ServerContext.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(new JsonPointer("/fred"), 5));

        //When
        resource.queryCollection(context, request, handler);

        //Then
        verify(handler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    @Test
    public void nameQueryShouldBeSupported() throws Exception {

        //Given
        RealmContext realmContext = new RealmContext(new RootContext());
        realmContext.addDnsAlias("", "REALM");
        ServerContext context = new ServerContext(realmContext);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getFields()).willReturn(Arrays.asList(new JsonPointer("/fred")));
        QueryResultHandler handler = mock(QueryResultHandler.class);
        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setId("abc123");
        resourceSet.setClientId("myclient");
        resourceSet.setDescription(json(object()));

        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(new JsonPointer("/name"), 5));
        given(resourceSetStore.query(any(org.forgerock.util.query.QueryFilter.class))).willReturn(asSet(resourceSet));

        //When
        resource.queryCollection(context, request, handler);

        //Then
        verify(handler).handleResult(any(QueryResult.class));
    }
}
