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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetServiceTest {

    private ResourceSetService service;

    private ResourceSetStore resourceSetStore;
    private UmaPolicyService policyService;

    @BeforeMethod
    public void setup() {
        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        resourceSetStore = mock(ResourceSetStore.class);
        policyService = mock(UmaPolicyService.class);

        service = new ResourceSetService(resourceSetStoreFactory, policyService);

        given(resourceSetStoreFactory.create("REALM")).willReturn(resourceSetStore);
    }

    @Test
    public void shouldGetResourceSetWithoutPolicy() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        String realm = "REALM";
        String resourceSetId = "RESOURCE_SET_ID";
        boolean augmentWithPolicy = false;
        ResourceSetDescription resourceSetDescription = mock(ResourceSetDescription.class);

        given(resourceSetStore.read(resourceSetId)).willReturn(resourceSetDescription);

        //When
        ResourceSetDescription resourceSet = service.getResourceSet(context, realm, resourceSetId, augmentWithPolicy)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSet).isEqualTo(resourceSetDescription);
        verifyZeroInteractions(policyService);
        verify(resourceSet, never()).setPolicy(Matchers.<JsonValue>anyObject());
    }

    @Test
    public void shouldGetResourceWithPolicy() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        String realm = "REALM";
        String resourceSetId = "RESOURCE_SET_ID";
        boolean augmentWithPolicy = true;
        ResourceSetDescription resourceSetDescription = mock(ResourceSetDescription.class);
        UmaPolicy policy = mock(UmaPolicy.class);
        Promise<UmaPolicy, ResourceException> policyPromise = Promises.newSuccessfulPromise(policy);
        JsonValue policyJson = mock(JsonValue.class);

        given(resourceSetStore.read(resourceSetId)).willReturn(resourceSetDescription);
        given(policyService.readPolicy(context, resourceSetId)).willReturn(policyPromise);
        given(policy.asJson()).willReturn(policyJson);

        //When
        ResourceSetDescription resourceSet = service.getResourceSet(context, realm, resourceSetId, augmentWithPolicy)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSet).isEqualTo(resourceSetDescription);
        verify(policyService).readPolicy(context, resourceSetId);
        verify(resourceSet).setPolicy(policyJson);
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenNoResourceSetsExist() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = false;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        Pair<QueryResult, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(new QueryResult(), queriedPolicies);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newSuccessfulPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).isEmpty();
    }

    private ServerContext createContext() {
        RealmContext realmContext = new RealmContext(new RootContext());
        realmContext.addDnsAlias("/", "REALM");
        return realmContext;
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistWithNoPolicyQuery() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = false;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());

        query.setResourceSetQuery(resourceSetQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(2).contains(resourceSetOne, resourceSetTwo);
        assertThat(resourceSetOne.getPolicy()).isNull();
        assertThat(resourceSetTwo.getPolicy()).isNull();
        verifyZeroInteractions(policyService);
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistWithNoPolicyQueryWithPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = true;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        Promise<UmaPolicy, ResourceException> policyOnePromise = Promises.newSuccessfulPromise(policyOne);
        Promise<UmaPolicy, ResourceException> policyTwoPromise = Promises.newSuccessfulPromise(policyTwo);

        query.setResourceSetQuery(resourceSetQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.readPolicy(context, "RS_ID_ONE")).willReturn(policyOnePromise);
        given(policyService.readPolicy(context, "RS_ID_TWO")).willReturn(policyTwoPromise);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(2).contains(resourceSetOne, resourceSetTwo);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetTwo.getPolicy()).isEqualTo(policyTwoJson);
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistQueryingByOr() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = false;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        Pair<QueryResult, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(new QueryResult(), queriedPolicies);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newSuccessfulPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyTwo);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_THREE");
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE")).willReturn(resourceSetThree);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(3).contains(resourceSetOne, resourceSetTwo, resourceSetThree);
        assertThat(resourceSetOne.getPolicy()).isNull();
        assertThat(resourceSetTwo.getPolicy()).isNull();
        assertThat(resourceSetThree.getPolicy()).isNull();
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenResourceSetsExistQueryingByAnd() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = false;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        Pair<QueryResult, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(new QueryResult(), queriedPolicies);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newSuccessfulPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        query.setOperator("AND");
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyTwo);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_THREE");
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE")).willReturn(resourceSetThree);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(1).contains(resourceSetOne);
        assertThat(resourceSetOne.getPolicy()).isNull();
        assertThat(resourceSetTwo.getPolicy()).isNull();
        assertThat(resourceSetThree.getPolicy()).isNull();
    }

    @Test
    public void shouldGetResourceSetsWhenResourceSetsExistQueryingByOrWithPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = true;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        UmaPolicy policyThree = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        JsonValue policyThreeJson = mock(JsonValue.class);
        Pair<QueryResult, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(new QueryResult(), queriedPolicies);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newSuccessfulPromise(queriedPoliciesPair);
        Promise<UmaPolicy, ResourceException> policyOnePromise = Promises.newSuccessfulPromise(policyOne);
        Promise<UmaPolicy, ResourceException> policyTwoPromise = Promises.newSuccessfulPromise(policyTwo);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyThree);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(policyThree.getId()).willReturn("RS_ID_THREE");
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyThree.asJson()).willReturn(policyThreeJson);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE")).willReturn(resourceSetThree);
        given(policyService.readPolicy(context, "RS_ID_ONE")).willReturn(policyOnePromise);
        given(policyService.readPolicy(context, "RS_ID_TWO")).willReturn(policyTwoPromise);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(3).contains(resourceSetOne, resourceSetTwo, resourceSetThree);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetTwo.getPolicy()).isEqualTo(policyTwoJson);
        assertThat(resourceSetThree.getPolicy()).isEqualTo(policyThreeJson);
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenResourceSetsExistQueryingByAndWithPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        boolean augmentWithPolicies = true;
        org.forgerock.util.query.QueryFilter<String> resourceSetQuery
                = mock(org.forgerock.util.query.QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        UmaPolicy policyThree = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        JsonValue policyThreeJson = mock(JsonValue.class);
        Pair<QueryResult, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(new QueryResult(), queriedPolicies);
        Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newSuccessfulPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        query.setOperator("AND");
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyThree);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(policyThree.getId()).willReturn("RS_ID_THREE");
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyThree.asJson()).willReturn(policyThreeJson);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE")).willReturn(resourceSetThree);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(1).contains(resourceSetOne);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetTwo.getPolicy()).isNull();
        assertThat(resourceSetThree.getPolicy()).isNull();
    }
}
