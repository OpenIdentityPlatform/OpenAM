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
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyResourceTest {

    private UmaPolicyResource policyResource;

    private UmaPolicyService policyService;

    @BeforeMethod
    public void setup() {

        policyService = mock(UmaPolicyService.class);

        policyResource = new UmaPolicyResource(policyService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyCreatePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        CreateRequest request = Requests.newCreateRequest("/policies", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        UmaPolicy policy = mock(UmaPolicy.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newSuccessfulPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policyService.createPolicy(context, request.getContent())).willReturn(promise);

        //When
        policyResource.createInstance(context, request, handler);

        //Then
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getId()).isEqualTo("ID");
        assertThat(resourceCaptor.getValue().getRevision()).isEqualTo("REVISION");
        assertThat(resourceCaptor.getValue().getContent().asMap()).isEqualTo(Collections.emptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedCreatePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        CreateRequest request = Requests.newCreateRequest("/policies", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newFailedPromise(resourceException);

        given(policyService.createPolicy(context, request.getContent())).willReturn(promise);

        //When
        policyResource.createInstance(context, request, handler);

        //Then
        verify(handler).handleError(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyReadPolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("/policies");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        UmaPolicy policy = mock(UmaPolicy.class);
        JsonValue policyJson = json(object());
        Promise<UmaPolicy, ResourceException> promise = Promises.newSuccessfulPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policy.asJson()).willReturn(policyJson);
        given(policyService.readPolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        policyResource.readInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getId()).isEqualTo("ID");
        assertThat(resourceCaptor.getValue().getRevision()).isEqualTo("REVISION");
        assertThat(resourceCaptor.getValue().getContent()).isEqualTo(policyJson);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedReadPolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("/policies");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newFailedPromise(resourceException);

        given(policyService.readPolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        policyResource.readInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyUpdatePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = Requests.newUpdateRequest("/policies", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        UmaPolicy policy = mock(UmaPolicy.class);
        JsonValue policyJson = json(object());
        Promise<UmaPolicy, ResourceException> promise = Promises.newSuccessfulPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policy.asJson()).willReturn(policyJson);
        given(policyService.updatePolicy(context, "RESOURCE_SET_UID", request.getContent())).willReturn(promise);

        //When
        policyResource.updateInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getId()).isEqualTo("ID");
        assertThat(resourceCaptor.getValue().getRevision()).isEqualTo("REVISION");
        assertThat(resourceCaptor.getValue().getContent()).isEqualTo(policyJson);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedUpdatePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = Requests.newUpdateRequest("/policies", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newFailedPromise(resourceException);

        given(policyService.updatePolicy(context, "RESOURCE_SET_UID", request.getContent())).willReturn(promise);

        //When
        policyResource.updateInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyDeletePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = Requests.newDeleteRequest("/policies");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<Void, ResourceException> promise = Promises.newSuccessfulPromise(null);

        given(policyService.deletePolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        policyResource.deleteInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getId()).isEqualTo("RESOURCE_SET_UID");
        assertThat(resourceCaptor.getValue().getRevision()).isEqualTo("0");
        assertThat(resourceCaptor.getValue().getContent().asMap()).isEqualTo(Collections.emptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedDeletePolicy() {

        //Given
        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = Requests.newDeleteRequest("/policies");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<Void, ResourceException> promise = Promises.newFailedPromise(resourceException);

        given(policyService.deletePolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        policyResource.deleteInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        verify(handler).handleError(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowNotSupportedExceptionForPatchInstance() {

        //Given
        ServerContext context = mock(ServerContext.class);
        PatchRequest request = Requests.newPatchRequest("/policies");
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        policyResource.patchInstance(context, "RESOURCE_SET_UID", request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowNotSupportedExceptionForActionCollection() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("/policies", "ACTION_ID");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        policyResource.actionCollection(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowNotSupportedExceptionForActionInstance() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("/policies", "ACTION_ID");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        policyResource.actionInstance(context, "RESOURCE_SET_ID", request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldSuccessfullyQueryPolicies() {

        //Given
        ServerContext context = mock(ServerContext.class);
        QueryRequest request = Requests.newQueryRequest("/policies");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        QueryResult queryResult = new QueryResult();
        Collection<UmaPolicy> umaPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policy1 = mock(UmaPolicy.class);
        UmaPolicy policy2 = mock(UmaPolicy.class);
        umaPolicies.add(policy1);
        umaPolicies.add(policy2);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> promise =
                Promises.newSuccessfulPromise(Pair.of(queryResult, umaPolicies));

        given(policyService.queryPolicies(context, request)).willReturn(promise);

        //When
        policyResource.queryCollection(context, request, handler);

        //Then
        verify(handler, times(2)).handleResource(Matchers.<Resource>anyObject());
        verify(handler).handleResult(queryResult);
    }

    @Test
    public void shouldHandleFailedQueryOfPolicies() {

        //Given
        ServerContext context = mock(ServerContext.class);
        QueryRequest request = Requests.newQueryRequest("/policies");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> promise =
                Promises.newFailedPromise(resourceException);

        given(policyService.queryPolicies(context, request)).willReturn(promise);

        //When
        policyResource.queryCollection(context, request, handler);

        //Then
        verify(handler).handleError(resourceException);
    }
}
