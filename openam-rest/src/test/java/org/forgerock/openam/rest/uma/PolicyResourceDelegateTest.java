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
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.PromisedRequestHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PolicyResourceDelegateTest {

    private PolicyResourceDelegate delegate;

    private PromisedRequestHandler policyResource;

    @BeforeMethod
    public void setup() {
        policyResource = mock(PromisedRequestHandler.class);

        delegate = new PolicyResourceDelegate(policyResource);
    }

    @Test
    public void shouldCreatePolicies() throws ResourceException {

        //Given
        //Given
        ServerContext context = mock(ServerContext.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        List<Resource> createdPolicies = new ArrayList<Resource>();
        Resource createdPolicyOne = new Resource("ID_1", "REVISION_1", json(object()));
        Resource createdPolicyTwo = new Resource("ID_2", "REVISION_2", json(object()));
        createdPolicies.add(createdPolicyOne);
        createdPolicies.add(createdPolicyTwo);
        Promise<Resource, ResourceException> createPolicyOnePromise = Promises.newSuccessfulPromise(createdPolicyOne);
        Promise<Resource, ResourceException> createPolicyTwoPromise = Promises.newSuccessfulPromise(createdPolicyTwo);

        given(policyResource.handleCreate(eq(context), Matchers.<CreateRequest>anyObject()))
                .willReturn(createPolicyOnePromise)
                .willReturn(createPolicyTwoPromise);

        //When
        List<Resource> returnedPolicies = delegate.createPolicies(context, policies).getOrThrowUninterruptibly();

        //Then
        verify(policyResource, never()).handleDelete(eq(context), Matchers.<DeleteRequest>anyObject());
        assertThat(returnedPolicies).isEqualTo(createdPolicies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToCreatePoliciesByDeletingCreatedPolicies() throws ResourceException {

        //Given
        //Given
        ServerContext context = mock(ServerContext.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        Resource createdPolicyOne = new Resource("ID_1", "REVISION_1", json(object()));
        ResourceException exception = mock(ResourceException.class);
        Promise<Resource, ResourceException> createPolicyOnePromise = Promises.newSuccessfulPromise(createdPolicyOne);
        Promise<Resource, ResourceException> createPolicyTwoPromise = Promises.newFailedPromise(exception);
        Promise<Resource, ResourceException> deletePolicyOnePromise = Promises.newSuccessfulPromise(createdPolicyOne);

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
            assertThat(requestCaptor.getValue().getResourceNameObject().leaf()).isEqualTo("ID_1");
            throw e;
        }
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToDeleteFailedCreationOfPolicies() throws ResourceException {

        //Given
        //Given
        ServerContext context = mock(ServerContext.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        Resource createdPolicyOne = new Resource("ID_1", "REVISION_1", json(object()));
        ResourceException createException = mock(ResourceException.class);
        ResourceException deleteException = mock(ResourceException.class);
        Promise<Resource, ResourceException> createPolicyOnePromise = Promises.newSuccessfulPromise(createdPolicyOne);
        Promise<Resource, ResourceException> createPolicyTwoPromise = Promises.newFailedPromise(createException);
        Promise<Resource, ResourceException> deletePolicyOnePromise = Promises.newFailedPromise(deleteException);

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
            assertThat(requestCaptor.getValue().getResourceNameObject().leaf()).isEqualTo("ID_1");
            assertThat(e).isEqualTo(deleteException);
            throw e;
        }
    }

    @Test
    public void shouldUpdatePolicies() throws ResourceException {

        //Given
        ServerContext context = mock(ServerContext.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        List<Resource> updatedPolicies = new ArrayList<Resource>();
        Resource updatedPolicyOne = new Resource("ID_1", "REVISION_1", json(object()));
        Resource updatedPolicyTwo = new Resource("ID_1", "REVISION_1", json(object()));
        updatedPolicies.add(updatedPolicyOne);
        updatedPolicies.add(updatedPolicyTwo);
        Promise<Resource, ResourceException> updatePolicyOnePromise = Promises.newSuccessfulPromise(updatedPolicyOne);
        Promise<Resource, ResourceException> updatePolicyTwoPromise = Promises.newSuccessfulPromise(updatedPolicyTwo);

        given(policyResource.handleUpdate(eq(context), Matchers.<UpdateRequest>anyObject()))
                .willReturn(updatePolicyOnePromise)
                .willReturn(updatePolicyTwoPromise);

        //When
        List<Resource> returnedPolicies = delegate.updatePolicies(context, policies).getOrThrowUninterruptibly();

        //Then
        assertThat(returnedPolicies).isEqualTo(updatedPolicies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToUpdatePolicies() throws ResourceException {

        //Given
        ServerContext context = mock(ServerContext.class);
        Set<JsonValue> policies = new HashSet<JsonValue>();
        JsonValue policyOne = json(object(field("name", "POLICY_ONE")));
        JsonValue policyTwo = json(object(field("name", "POLICY_TWO")));
        policies.add(policyOne);
        policies.add(policyTwo);
        Resource updatedPolicyOne = new Resource("ID_1", "REVISION_1", json(object()));
        ResourceException exception = mock(ResourceException.class);
        Promise<Resource, ResourceException> updatePolicyOnePromise = Promises.newSuccessfulPromise(updatedPolicyOne);
        Promise<Resource, ResourceException> updatePolicyTwoPromise = Promises.newFailedPromise(exception);

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
        ServerContext context = mock(ServerContext.class);
        QueryRequest request = mock(QueryRequest.class);

        //When
        delegate.queryPolicies(context, request);

        //Then
        verify(policyResource).handleQuery(context, request);
    }

    @Test
    public void shouldDeletePolicies() throws ResourceException {

        //Given
        ServerContext context = mock(ServerContext.class);
        List<Resource> policies = new ArrayList<Resource>();
        Resource policyOne = new Resource("ID_1", "REVISION_1", json(object()));
        Resource policyTwo = new Resource("ID_2", "REVISION_2", json(object()));
        policies.add(policyOne);
        policies.add(policyTwo);
        Set<String> policyIds = new HashSet<String>();
        policyIds.add("ID_1");
        policyIds.add("ID_2");
        Promise<Resource, ResourceException> deletePolicyOnePromise = Promises.newSuccessfulPromise(policyOne);
        Promise<Resource, ResourceException> deletePolicyTwoPromise = Promises.newSuccessfulPromise(policyTwo);

        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise)
                .willReturn(deletePolicyTwoPromise);

        //When
        List<Resource> deletedPolicies = delegate.deletePolicies(context, policyIds).getOrThrowUninterruptibly();

        //Then
        assertThat(deletedPolicies).isEqualTo(policies);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToDeletePolicies() throws ResourceException {

        //Given
        ServerContext context = mock(ServerContext.class);
        Resource policyOne = new Resource("ID_1", "REVISION_1", json(object()));
        Set<String> policyIds = new HashSet<String>();
        policyIds.add("ID_1");
        policyIds.add("ID_2");
        ResourceException exception = mock(ResourceException.class);
        Promise<Resource, ResourceException> deletePolicyOnePromise = Promises.newSuccessfulPromise(policyOne);
        Promise<Resource, ResourceException> deletePolicyTwoPromise = Promises.newFailedPromise(exception);

        given(policyResource.handleDelete(eq(context), Matchers.<DeleteRequest>anyObject()))
                .willReturn(deletePolicyOnePromise)
                .willReturn(deletePolicyTwoPromise);

        //When
        delegate.deletePolicies(context, policyIds).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }
}
