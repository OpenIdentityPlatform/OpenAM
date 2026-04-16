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

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.PendingRequestsService;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PendingRequestResourceTest {

    private PendingRequestResource resource;

    @Mock
    private PendingRequestsService service;
    @Mock
    private ContextHelper contextHelper;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        resource = new PendingRequestResource(service, contextHelper);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionCollectionShouldHandleApproveAction() throws Exception {

        //Given
        Context context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "approve");

        mockPendingRequestsForUser("alice", "/REALM", 2);
        mockPendingRequestApprovalService();

        //When
        Promise<ActionResponse, ResourceException> promise = resource.approveAll(context, request);

        //Then
        verify(service, times(2)).approvePendingRequest(eq(context), anyString(), any(JsonValue.class), anyString());
        assertThat(promise).succeeded();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionCollectionShouldHandleDenyAction() throws Exception {

        //Given
        Context context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "deny");

        mockPendingRequestsForUser("alice", "/REALM", 2);

        //When
        Promise<ActionResponse, ResourceException> promise = resource.denyAll(context, request);

        //Then
        verify(service, times(2)).denyPendingRequest(anyString(), anyString());
        assertThat(promise).succeeded();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionInstanceShouldHandleApproveAction() throws Exception {

        //Given
        Context context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "approve");

        mockPendingRequestsForUser("alice", "/REALM", 1);
        mockPendingRequestApprovalService();

        //When
        Promise<ActionResponse, ResourceException> promise = resource.approveAll(context, request);

        //Then
        verify(service).approvePendingRequest(eq(context), anyString(), any(JsonValue.class), anyString());
        assertThat(promise).succeeded();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void actionInstanceShouldHandleDenyAction() throws Exception {

        //Given
        Context context = mockContext("REALM");
        ActionRequest request = Requests.newActionRequest("", "deny");

        mockPendingRequestsForUser("alice", "/REALM", 1);

        //When
        Promise<ActionResponse, ResourceException> promise = resource.denyAll(context, request);

        //Then
        verify(service).denyPendingRequest(anyString(), anyString());
        assertThat(promise).succeeded();
    }

    @Test
    public void shouldQueryPendingRequests() throws Exception {

        //Given
        Context context = mockContext("REALM");
        QueryRequest request = Requests.newQueryRequest("").setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        mockPendingRequestsForUser("alice", "/REALM", 2);

        //When
        Promise<QueryResponse, ResourceException> promise = resource.queryCollection(context, request, handler);

        //Then
        verify(handler, times(2)).handleResource(any(ResourceResponse.class));
        assertThat(promise).succeeded();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadPendingRequest() throws Exception {

        //Given
        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest("");

        String id = mockPendingRequest();

        //When
        Promise<ResourceResponse, ResourceException> promise = resource.readInstance(context, id, request);

        //Then
        assertThat(promise).succeeded();
    }

    private Context mockContext(String realmName) {
        Realm realm = realmTestHelper.mockRealm(realmName);
        RealmContext realmContext = mock(RealmContext.class);
        given(realmContext.getRealm()).willReturn(realm);
        return realmContext;
    }

    private void mockPendingRequestsForUser(String username, String realm, int numberOfPendingRequests)
            throws ResourceException {
        given(contextHelper.getUserId(any(Context.class))).willReturn(username);
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

    private void mockPendingRequestApprovalService() throws ResourceException {
        Promise<Void, ResourceException> promise = Promises.newResultPromise(null);
        given(service.approvePendingRequest(any(Context.class), anyString(), any(JsonValue.class), anyString()))
                .willReturn(promise);
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(PendingRequestResource.class).hasValidAnnotations();
    }
}
