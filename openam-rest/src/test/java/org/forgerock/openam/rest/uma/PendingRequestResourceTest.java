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

package org.forgerock.openam.rest.uma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.PendingRequestsService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PendingRequestResourceTest {

    private PendingRequestResource resource;

    @Mock
    private PendingRequestsService service;
    @Mock
    private ContextHelper contextHelper;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        resource = new PendingRequestResource(service, contextHelper);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionCollectionShouldReturnNotSupportedExceptionForUnsupportedAction() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("", "other");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionCollectionShouldHandleApproveAction() throws Exception {

        //Given
        ServerContext context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "approve");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        mockPendingRequestsForUser("alice", "REALM", 2);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        verify(service, times(2)).approvePendingRequest(anyString());
        verify(handler).handleResult(any(JsonValue.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionCollectionShouldHandleDenyAction() throws Exception {

        //Given
        ServerContext context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "deny");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        mockPendingRequestsForUser("alice", "REALM", 2);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        verify(service, times(2)).denyPendingRequest(anyString());
        verify(handler).handleResult(any(JsonValue.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionInstanceShouldReturnNotSupportedExceptionForUnsupportedAction() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("", "other");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionInstanceShouldHandleApproveAction() throws Exception {

        //Given
        ServerContext context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "approve");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        mockPendingRequestsForUser("alice", "REALM", 1);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        verify(service).approvePendingRequest(anyString());
        verify(handler).handleResult(any(JsonValue.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionInstanceShouldHandleDenyAction() throws Exception {

        //Given
        ServerContext context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "deny");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        mockPendingRequestsForUser("alice", "REALM", 1);

        //When
        resource.actionCollection(context, request, handler);

        //Then
        verify(service).denyPendingRequest(anyString());
        verify(handler).handleResult(any(JsonValue.class));
    }

    @Test
    public void shouldQueryPendingRequests() throws Exception {

        //Given
        ServerContext context = mockContext("REALM");
        QueryRequest request = Requests.newQueryRequest("").setQueryFilter(QueryFilter.alwaysTrue());
        QueryResultHandler handler = mock(QueryResultHandler.class);

        mockPendingRequestsForUser("alice", "REALM", 2);

        //When
        resource.queryCollection(context, request, handler);

        //Then
        verify(handler, times(2)).handleResource(any(Resource.class));
        verify(handler).handleResult(any(QueryResult.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadPendingRequest() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("");
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        String id = mockPendingRequest();

        //When
        resource.readInstance(context, id, request, handler);

        //Then
        verify(handler).handleResult(any(Resource.class));
    }

    private ServerContext mockContext(String realm) {
        RealmContext realmContext = mock(RealmContext.class);
        given(realmContext.getResolvedRealm()).willReturn(realm);
        return realmContext;
    }

    private void mockPendingRequestsForUser(String username, String realm, int numberOfPendingRequests)
            throws ResourceException {
        given(contextHelper.getUserId(any(ServerContext.class))).willReturn(username);
        Set<UmaPendingRequest> pendingRequests = new HashSet<>();
        for (int i = 0; i < numberOfPendingRequests; i++) {
            UmaPendingRequest pendingRequest = new UmaPendingRequest();
            pendingRequest.setId("ID" + i);
            pendingRequests.add(pendingRequest);
        }
        given(service.queryPendingRequests(username, realm)).willReturn(pendingRequests);
    }

    private String mockPendingRequest() throws ResourceException {
        UmaPendingRequest pendingRequest = new UmaPendingRequest("RESOURCE_SET_ID", "RESOURCE_SET_NAME",
                "RESOURCE_OWNER_ID", "REALM", "REQUESTING_PARTY_ID", Collections.singleton("SCOPE"));
        pendingRequest.setId("PENDING_REQUEST_ID");
        given(service.readPendingRequest("PENDING_REQUEST_ID")).willReturn(pendingRequest);
        return "PENDING_REQUEST_ID";
    }
}
