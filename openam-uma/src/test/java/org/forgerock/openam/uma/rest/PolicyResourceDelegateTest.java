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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.uma.rest.PolicyResourceDelegate;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PolicyResourceDelegateTest {

    private PolicyResourceDelegate delegate;

    private RequestHandler policyResource;

    @BeforeMethod
    public void setup() {
        policyResource = mock(RequestHandler.class);

        delegate = new PolicyResourceDelegate(policyResource);
    }

    @Test
    public void shouldCreatePolicies() throws ResourceException {

        //Given
        //Given
        Context context = mock(Context.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        List<ResourceResponse> createdPolicies = new ArrayList<ResourceResponse>();
        ResourceResponse createdPolicyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceResponse createdPolicyTwo = newResourceResponse("ID_2", "REVISION_2", json(object()));
        createdPolicies.add(createdPolicyOne);
        createdPolicies.add(createdPolicyTwo);
        Promise<ResourceResponse, ResourceException> createPolicyOnePromise = Promises.newResultPromise(createdPolicyOne);
        Promise<ResourceResponse, ResourceException> createPolicyTwoPromise = Promises.newResultPromise(createdPolicyTwo);

        given(policyResource.handleCreate(eq(context), Matchers.<CreateRequest>anyObject()))
                .willReturn(createPolicyOnePromise)
                .willReturn(createPolicyTwoPromise);

        //When
        List<ResourceResponse> returnedPolicies = delegate.createPolicies(context, policies).getOrThrowUninterruptibly();

        //Then
        verify(policyResource, never()).handleDelete(eq(context), Matchers.<DeleteRequest>anyObject());
        assertThat(returnedPolicies).isEqualTo(createdPolicies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToCreatePoliciesByDeletingCreatedPolicies() throws ResourceException {

        //Given
        //Given
        Context context = mock(Context.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        ResourceResponse createdPolicyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceException exception = mock(ResourceException.class);
        Promise<ResourceResponse, ResourceException> createPolicyOnePromise = Promises.newResultPromise(createdPolicyOne);
        Promise<ResourceResponse, ResourceException> createPolicyTwoPromise = Promises.newExceptionPromise(exception);
        Promise<ResourceResponse, ResourceException> deletePolicyOnePromise = Promises.newResultPromise(createdPolicyOne);

        given(policyResource.handleCreate(eq(context), Matchers.<CreateRequest>anyObject()))
                .willReturn(createPolicyOnePromise)
                .willReturn(createPolicyTwoPromise);
        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise);

        //When
        try {
            delegate.createPolicies(context, policies).getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            ArgumentCaptor<DeleteRequest> requestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
            verify(policyResource).handleDelete(eq(context), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getResourcePathObject().leaf()).isEqualTo("ID_1");
            throw e;
        }
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToDeleteFailedCreationOfPolicies() throws ResourceException {

        //Given
        //Given
        Context context = mock(Context.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        ResourceResponse createdPolicyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceException createException = mock(ResourceException.class);
        ResourceException deleteException = mock(ResourceException.class);
        Promise<ResourceResponse, ResourceException> createPolicyOnePromise = Promises.newResultPromise(createdPolicyOne);
        Promise<ResourceResponse, ResourceException> createPolicyTwoPromise = Promises.newExceptionPromise(createException);
        Promise<ResourceResponse, ResourceException> deletePolicyOnePromise = Promises.newExceptionPromise(deleteException);

        given(policyResource.handleCreate(eq(context), Matchers.<CreateRequest>anyObject()))
                .willReturn(createPolicyOnePromise)
                .willReturn(createPolicyTwoPromise);
        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise);

        //When
        try {
            delegate.createPolicies(context, policies).getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            ArgumentCaptor<DeleteRequest> requestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
            verify(policyResource).handleDelete(eq(context), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getResourcePathObject().leaf()).isEqualTo("ID_1");
            assertThat(e).isEqualTo(deleteException);
            throw e;
        }
    }

    @Test
    public void shouldUpdatePolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        List<ResourceResponse> updatedPolicies = new ArrayList<ResourceResponse>();
        ResourceResponse updatedPolicyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceResponse updatedPolicyTwo = newResourceResponse("ID_1", "REVISION_1", json(object()));
        updatedPolicies.add(updatedPolicyOne);
        updatedPolicies.add(updatedPolicyTwo);
        Promise<ResourceResponse, ResourceException> updatePolicyOnePromise = Promises.newResultPromise(updatedPolicyOne);
        Promise<ResourceResponse, ResourceException> updatePolicyTwoPromise = Promises.newResultPromise(updatedPolicyTwo);

        given(policyResource.handleUpdate(eq(context), Matchers.<UpdateRequest>anyObject()))
                .willReturn(updatePolicyOnePromise)
                .willReturn(updatePolicyTwoPromise);

        //When
        List<ResourceResponse> returnedPolicies = delegate.updatePolicies(context, policies).getOrThrowUninterruptibly();

        //Then
        assertThat(returnedPolicies).isEqualTo(updatedPolicies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToUpdatePolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        ResourceResponse updatedPolicyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceException exception = mock(ResourceException.class);
        Promise<ResourceResponse, ResourceException> updatePolicyOnePromise = Promises.newResultPromise(updatedPolicyOne);
        Promise<ResourceResponse, ResourceException> updatePolicyTwoPromise = Promises.newExceptionPromise(exception);

        given(policyResource.handleUpdate(eq(context), Matchers.<UpdateRequest>anyObject()))
                .willReturn(updatePolicyOnePromise)
                .willReturn(updatePolicyTwoPromise);

        //When
        delegate.updatePolicies(context, policies).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }

    @Test
    public void shouldQueryPolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        //When
        delegate.queryPolicies(context, request, handler);

        //Then
        verify(policyResource).handleQuery(context, request, handler);
    }

    @Test
    public void shouldQueryPoliciesWithHandler() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        //When
        delegate.queryPolicies(context, request, handler);

        //Then
        verify(policyResource).handleQuery(context, request, handler);
    }

    @Test
    public void shouldDeletePolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        List<ResourceResponse> policies = new ArrayList<ResourceResponse>();
        ResourceResponse policyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        ResourceResponse policyTwo = newResourceResponse("ID_2", "REVISION_2", json(object()));
        policies.add(policyOne);
        policies.add(policyTwo);
        Set<String> policyIds = new HashSet<String>();
        policyIds.add("ID_1");
        policyIds.add("ID_2");
        Promise<ResourceResponse, ResourceException> deletePolicyOnePromise = Promises.newResultPromise(policyOne);
        Promise<ResourceResponse, ResourceException> deletePolicyTwoPromise = Promises.newResultPromise(policyTwo);

        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise)
                .willReturn(deletePolicyTwoPromise);

        //When
        List<ResourceResponse> deletedPolicies = delegate.deletePolicies(context, policyIds).getOrThrowUninterruptibly();

        //Then
        assertThat(deletedPolicies).isEqualTo(policies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToDeletePolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        ResourceResponse policyOne = newResourceResponse("ID_1", "REVISION_1", json(object()));
        Set<String> policyIds = new HashSet<String>();
        policyIds.add("ID_1");
        policyIds.add("ID_2");
        ResourceException exception = mock(ResourceException.class);
        Promise<ResourceResponse, ResourceException> deletePolicyOnePromise = Promises.newResultPromise(policyOne);
        Promise<ResourceResponse, ResourceException> deletePolicyTwoPromise = Promises.newExceptionPromise(exception);

        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise)
                .willReturn(deletePolicyTwoPromise);

        //When
        delegate.deletePolicies(context, policyIds).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }
}
