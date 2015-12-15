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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.idm.AMIdentity;

import org.forgerock.openam.uma.ResourceSetSharedFilter;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.UmaProviderSettings;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilterVisitor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetServiceTest {

    private ResourceSetService service;

    private ResourceSetStore resourceSetStore;
    private UmaPolicyService policyService;
    private CoreWrapper coreWrapper;
    private UmaProviderSettings umaProviderSettings;

    @BeforeMethod
    public void setup() throws Exception {
        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        resourceSetStore = mock(ResourceSetStore.class);
        policyService = mock(UmaPolicyService.class);
        coreWrapper = mock(CoreWrapper.class);
        UmaProviderSettingsFactory umaProviderSettingsFactory = mock(UmaProviderSettingsFactory.class);
        umaProviderSettings = mock(UmaProviderSettings.class);

        service = new ResourceSetService(resourceSetStoreFactory, policyService, coreWrapper, umaProviderSettingsFactory);

        given(resourceSetStoreFactory.create("REALM")).willReturn(resourceSetStore);
        given(umaProviderSettingsFactory.get("REALM")).willReturn(umaProviderSettings);
    }

    @Test
    public void shouldGetResourceSetWithoutPolicy() throws Exception {

        //Given
        Context context = mock(Context.class);
        String realm = "REALM";
        String resourceSetId = "RESOURCE_SET_ID";
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicy = false;
        ResourceSetDescription resourceSetDescription = mock(ResourceSetDescription.class);

        given(resourceSetStore.read(eq(resourceSetId), any(ResourceSetSharedFilter.class)))
                .willReturn(resourceSetDescription);

        //When
        ResourceSetDescription resourceSet = service.getResourceSet(context, realm, resourceSetId, resourceOwnerId,
                augmentWithPolicy).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSet).isEqualTo(resourceSetDescription);
        verifyZeroInteractions(policyService);
        verify(resourceSet, never()).setPolicy(Matchers.<JsonValue>anyObject());
    }

    @Test
    public void shouldGetResourceWithPolicy() throws Exception {

        //Given
        Context context = mock(Context.class);
        String realm = "REALM";
        String resourceSetId = "RESOURCE_SET_ID";
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicy = true;
        ResourceSetDescription resourceSetDescription = mock(ResourceSetDescription.class);
        UmaPolicy policy = mock(UmaPolicy.class);
        Promise<UmaPolicy, ResourceException> policyPromise = Promises.newResultPromise(policy);
        JsonValue policyJson = mock(JsonValue.class);

        given(resourceSetStore.read(eq(resourceSetId), any(ResourceSetSharedFilter.class)))
                .willReturn(resourceSetDescription);
        given(policyService.readPolicy(context, resourceSetId)).willReturn(policyPromise);
        given(policy.asJson()).willReturn(policyJson);

        //When
        ResourceSetDescription resourceSet = service.getResourceSet(context, realm, resourceSetId, resourceOwnerId,
                augmentWithPolicy).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSet).isEqualTo(resourceSetDescription);
        verify(policyService).readPolicy(context, resourceSetId);
        verify(resourceSet).setPolicy(policyJson);
    }

    private Context mockContext(String realm) {
        RealmContext realmContext = mock(RealmContext.class);
        given(realmContext.getResolvedRealm()).willReturn(realm);
        return realmContext;
    }

    private void mockResourceOwnerIdentity(String resourceOwnerId, String realm) {
        AMIdentity identity = mock(AMIdentity.class);
        given(identity.getUniversalId()).willReturn(resourceOwnerId + "_UNIVERSAL_ID");
        given(coreWrapper.getIdentity(resourceOwnerId, realm)).willReturn(identity);
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenNoResourceSetsExist() throws Exception {

        //Given
        String realm = "REALM";
        Context context = mockContext(realm);
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = false;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        QueryFilter<JsonPointer> policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        given(resourceSetStore.query(any(QueryFilter.class))).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);

        mockResourceOwnerIdentity(resourceOwnerId, realm);
        mockPolicyEvaluator("RS_CLIENT_ID");
        mockFilteredResourceSetsQueryVisitor(resourceSetQuery, queriedResourceSets);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).isEmpty();
    }

    private Context createContext() {
        RealmContext realmContext = new RealmContext(new RootContext());
        realmContext.setDnsAlias("/", "REALM");
        return realmContext;
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistWithNoPolicyQuery() throws Exception {

        //Given
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = false;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());

        mockResourceOwnerIdentity(resourceOwnerId, realm);
        mockFilteredResourceSetsQueryVisitor(resourceSetQuery, queriedResourceSets);

        query.setResourceSetQuery(resourceSetQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);

        Collection<UmaPolicy> queriedPolicies = new HashSet<UmaPolicy>();
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(2).contains(resourceSetOne, resourceSetTwo);
        assertThat(resourceSetOne.getPolicy()).isNull();
        assertThat(resourceSetTwo.getPolicy()).isNull();
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistWithNoPolicyQueryWithPolicies() throws Exception {

        //Given
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = true;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        Promise<UmaPolicy, ResourceException> policyOnePromise = Promises.newResultPromise(policyOne);
        Promise<UmaPolicy, ResourceException> policyTwoPromise = Promises.newResultPromise(policyTwo);

        query.setResourceSetQuery(resourceSetQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(resourceSetStore.query(QueryFilter.and(
                resourceSetQuery,
                equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, "RESOURCE_OWNER_ID"))))
                .willReturn(queriedResourceSets);
        given(policyService.readPolicy(context, "RS_ID_ONE")).willReturn(policyOnePromise);
        given(policyService.readPolicy(context, "RS_ID_TWO")).willReturn(policyTwoPromise);

        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(Promises.<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException>newResultPromise(
                        Pair.<QueryResponse, Collection<UmaPolicy>>of(newQueryResponse(), new HashSet<UmaPolicy>())));
        mockResourceOwnerIdentity(resourceOwnerId, realm);
        mockFilteredResourceSetsQueryVisitor(resourceSetQuery, queriedResourceSets);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(2).contains(resourceSetOne, resourceSetTwo);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetTwo.getPolicy()).isEqualTo(policyTwoJson);
    }

    private void mockPolicyEvaluator(String clientId) throws EntitlementException {
        Evaluator policyEvaluator = mock(Evaluator.class);
        given(umaProviderSettings.getPolicyEvaluator(any(Subject.class), anyString())).willReturn(policyEvaluator);
        given(policyEvaluator.evaluate(any(String.class), any(Subject.class), any(String.class), anyMap(),
                any(Boolean.class))).willReturn(Collections.<Entitlement>emptyList());
        given(umaProviderSettings.getPolicyEvaluator(any(Subject.class), eq(clientId.toLowerCase())))
                .willReturn(policyEvaluator);
    }

    private void mockFilteredResourceSetsQueryVisitor(QueryFilter<String> resourceSetQuery,
            Set<ResourceSetDescription> queriedResourceSets) {
        given(resourceSetQuery.accept(any(QueryFilterVisitor.class), eq(queriedResourceSets)))
                .willReturn(queriedResourceSets);
    }

    @Test
    public void getResourceSetsShouldReturnSetWhenResourceSetsExistQueryingByOr() throws Exception {

        //Given
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        query.setOperator(AggregateQuery.Operator.OR);
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = false;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyTwo);

        mockResourceOwnerIdentity(resourceOwnerId, realm);
        mockFilteredResourceSetsQueryVisitor(resourceSetQuery, queriedResourceSets);

        given(policyOne.getResourceSet()).willReturn(resourceSetOne);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyTwo.getId()).willReturn("RS_ID_THREE");
        given(policyTwo.getResourceSet()).willReturn(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE", resourceOwnerId)).willReturn(resourceSetThree);

        mockPolicyEvaluator("RS_CLIENT_ID");

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query,
                resourceOwnerId, augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(3).contains(resourceSetOne, resourceSetTwo, resourceSetThree);
        assertThat(resourceSetOne.getPolicy()).isNull();
        assertThat(resourceSetTwo.getPolicy()).isNull();
        assertThat(resourceSetThree.getPolicy()).isNull();
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenResourceSetsExistQueryingByAnd() throws Exception {

        //Given
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = false;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        query.setOperator(AggregateQuery.Operator.AND);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyTwo);

        mockResourceOwnerIdentity(resourceOwnerId, realm);
        mockFilteredResourceSetsQueryVisitor(resourceSetQuery, queriedResourceSets);

        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyOne.getResourceSet()).willReturn(resourceSetOne);
        given(policyTwo.getId()).willReturn("RS_ID_THREE");
        given(policyTwo.getResourceSet()).willReturn(resourceSetTwo);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);

        mockPolicyEvaluator("RS_CLIENT_ID");

        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE", resourceOwnerId)).willReturn(resourceSetThree);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
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
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = true;

        QueryFilter<String> resourceSetQuery = QueryFilter.contains("name", "RS_THREE");
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", singletonMap("name", (Object) "RS_ONE"));
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", singletonMap("name", (Object) "RS_TWO"));
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", singletonMap("name", (Object) "RS_THREE"));
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        UmaPolicy policyThree = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        JsonValue policyThreeJson = mock(JsonValue.class);
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);
        Promise<UmaPolicy, ResourceException> policyOnePromise = Promises.newResultPromise(policyOne);
        Promise<UmaPolicy, ResourceException> policyTwoPromise = Promises.newResultPromise(policyTwo);

        mockResourceOwnerIdentity(resourceOwnerId, realm);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyThree);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyOne.getResourceSet()).willReturn(resourceSetOne);
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(policyTwo.getResourceSet()).willReturn(resourceSetTwo);
        given(policyThree.getId()).willReturn("RS_ID_THREE");
        given(policyThree.getResourceSet()).willReturn(resourceSetThree);
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyThree.asJson()).willReturn(policyThreeJson);
        given(resourceSetStore.query(QueryFilter.and(
                resourceSetQuery,
                equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, "RESOURCE_OWNER_ID"))))
                .willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_ONE", resourceOwnerId)).willReturn(resourceSetOne);
        given(resourceSetStore.read("RS_ID_THREE", resourceOwnerId)).willReturn(resourceSetThree);
        given(policyService.readPolicy(context, "RS_ID_ONE")).willReturn(policyOnePromise);
        given(policyService.readPolicy(context, "RS_ID_TWO")).willReturn(policyTwoPromise);


        Entitlement entitlement = new Entitlement();
        Map<String, Boolean> actionValues = new HashMap();
        actionValues.put("actionValueKey", true);
        entitlement.setActionValues(actionValues);
        Evaluator evaluator = mock(Evaluator.class);
        given(umaProviderSettings.getPolicyEvaluator(any(Subject.class), anyString())).willReturn(evaluator);
        given(evaluator.evaluate(eq(realm), any(Subject.class), eq("RS_ONE"), isNull(Map.class), eq(false)))
                .willReturn(singletonList(entitlement));
        given(evaluator.evaluate(eq(realm), any(Subject.class), eq("RS_TWO"), isNull(Map.class), eq(false)))
                .willReturn(singletonList(entitlement));
        given(evaluator.evaluate(eq(realm), any(Subject.class), eq("RS_THREE"), isNull(Map.class), eq(false)))
                .willReturn(Collections.<Entitlement>emptyList());

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(2).contains(resourceSetOne, resourceSetThree);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetThree.getPolicy()).isEqualTo(policyThreeJson);
    }

    @Test
    public void getResourceSetsShouldReturnEmptySetWhenResourceSetsExistQueryingByAndWithPolicies() throws Exception {

        //Given
        Context context = createContext();
        String realm = "REALM";
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        boolean augmentWithPolicies = true;
        QueryFilter<String> resourceSetQuery = mock(QueryFilter.class);
        QueryFilter policyQuery = QueryFilter.alwaysFalse();
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetThree = new ResourceSetDescription("RS_ID_THREE", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        UmaPolicy policyOne = mock(UmaPolicy.class);
        UmaPolicy policyTwo = mock(UmaPolicy.class);
        UmaPolicy policyThree = mock(UmaPolicy.class);
        JsonValue policyOneJson = mock(JsonValue.class);
        JsonValue policyTwoJson = mock(JsonValue.class);
        JsonValue policyThreeJson = mock(JsonValue.class);
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);

        query.setResourceSetQuery(resourceSetQuery);
        query.setPolicyQuery(policyQuery);
        query.setOperator(AggregateQuery.Operator.AND);
        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        queriedPolicies.add(policyOne);
        queriedPolicies.add(policyThree);
        given(policyOne.getId()).willReturn("RS_ID_ONE");
        given(policyOne.asJson()).willReturn(policyOneJson);
        given(policyOne.getResourceSet()).willReturn(resourceSetOne);
        given(policyTwo.getId()).willReturn("RS_ID_TWO");
        given(policyTwo.asJson()).willReturn(policyTwoJson);
        given(policyTwo.getResourceSet()).willReturn(resourceSetTwo);
        given(policyThree.getId()).willReturn("RS_ID_THREE");
        given(policyThree.asJson()).willReturn(policyThreeJson);
        given(policyThree.getResourceSet()).willReturn(resourceSetThree);
        given(resourceSetStore.query(resourceSetQuery)).willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(resourceSetStore.read("RS_ID_THREE", resourceOwnerId)).willReturn(resourceSetThree);

        mockPolicyEvaluator("RS_CLIENT_ID");

        AMIdentity amIdentity = mock(AMIdentity.class);
        given(amIdentity.getUniversalId()).willReturn("UNIVERSAL_ID");
        given(coreWrapper.getIdentity("RESOURCE_OWNER_ID", realm)).willReturn(amIdentity);

        given(resourceSetQuery.accept(any(QueryFilterVisitor.class), eq(queriedResourceSets))).willReturn(queriedResourceSets);

        //When
        Collection<ResourceSetDescription> resourceSets = service.getResourceSets(context, realm, query, resourceOwnerId,
                augmentWithPolicies).getOrThrowUninterruptibly();

        //Then
        assertThat(resourceSets).hasSize(1).contains(resourceSetOne);
        assertThat(resourceSetOne.getPolicy()).isEqualTo(policyOneJson);
        assertThat(resourceSetTwo.getPolicy()).isNull();
        assertThat(resourceSetThree.getPolicy()).isNull();
    }

    @Test
    public void shouldRevokeAllResourceSetPolicies() throws Exception {

        //Given
        String realm = "REALM";
        Context context = mockContext(realm);
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        Set<ResourceSetDescription> queriedResourceSets = new HashSet<>();
        ResourceSetDescription resourceSetOne = new ResourceSetDescription("RS_ID_ONE", "CLIENT_ID_ONE",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSetTwo = new ResourceSetDescription("RS_ID_TWO", "CLIENT_ID_TWO",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        Collection<UmaPolicy> queriedPolicies = new HashSet<>();
        Pair<QueryResponse, Collection<UmaPolicy>> queriedPoliciesPair = Pair.of(newQueryResponse(), queriedPolicies);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queriedPoliciesPromise
                = Promises.newResultPromise(queriedPoliciesPair);

        mockResourceOwnerIdentity(resourceOwnerId, realm);

        queriedResourceSets.add(resourceSetOne);
        queriedResourceSets.add(resourceSetTwo);
        given(resourceSetStore.query(Matchers.<QueryFilter<String>>anyObject()))
                .willReturn(queriedResourceSets);
        given(policyService.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queriedPoliciesPromise);
        given(policyService.deletePolicy(context, "RS_ID_ONE"))
                .willReturn(Promises.<Void, ResourceException>newResultPromise(null));
        given(policyService.deletePolicy(context, "RS_ID_TWO"))
                .willReturn(Promises.<Void, ResourceException>newResultPromise(null));

        //When
        service.revokeAllPolicies(context, realm, resourceOwnerId).getOrThrowUninterruptibly();

        //Then
        verify(policyService).deletePolicy(context, "RS_ID_ONE");
        verify(policyService).deletePolicy(context, "RS_ID_TWO");
    }
}
