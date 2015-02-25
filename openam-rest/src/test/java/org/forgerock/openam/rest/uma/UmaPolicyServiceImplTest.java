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
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyStore;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyServiceImplTest {

    private UmaPolicyServiceImpl policyService;

    private UmaPolicyStore umaPolicyStore;
    private PolicyResourceDelegate policyResourceDelegate;
    private ResourceSetDescription resourceSet;

    @BeforeMethod
    public void setup() throws org.forgerock.oauth2.core.exceptions.NotFoundException, ServerException {

        umaPolicyStore = mock(UmaPolicyStore.class);
        policyResourceDelegate = mock(PolicyResourceDelegate.class);
        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);

        policyService = new UmaPolicyServiceImpl(umaPolicyStore, policyResourceDelegate, resourceSetStoreFactory);

        ResourceSetStore resourceSetStore = mock(ResourceSetStore.class);
        resourceSet = new ResourceSetDescription("RESOURCE_SET_ID",
                "CLIENT_ID", "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        resourceSet.setDescription(json(object(field("name", "NAME"), field("scopes", array("SCOPE_A", "SCOPE_B")))));

        given(resourceSetStoreFactory.create(anyString())).willReturn(resourceSetStore);
        given(resourceSetStore.read("POLICY_ID")).willReturn(resourceSet);
        doThrow(org.forgerock.oauth2.core.exceptions.NotFoundException.class).when(resourceSetStore).read("OTHER_ID");
        doThrow(ServerException.class).when(resourceSetStore).read("FAILING_ID");
    }

    private ServerContext createContext() throws SSOException {
        SubjectContext subjectContext = mock(SSOTokenContext.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(subjectContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn("RESOURCE_OWNER_ID");
        return new ServerContext(new RealmContext(subjectContext));
    }

    private JsonValue createUmaPolicyJson(String... subjectTwoScopes) {
        return json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", Arrays.asList(subjectTwoScopes)))
                ))
        ));
    }

    private JsonValue createUmaPolicyJson() {
        return createUmaPolicyJson("SCOPE_A");
    }

    private JsonValue createBackendScopeAPolicyJson() {
        return json(object(
                field("name", "POLICY_ID - SCOPE_A"),
                field("resources", array("uma://POLICY_ID")),
                field("resourceTypeUuid", "76656a38-5f8e-401b-83aa-4ccb74ce88d2"),
                field("actionValues", object(field("SCOPE_A", true))),
                field("subject", object(
                        field("type", "OR"),
                        field("subjects", array(
                                object(
                                        field("type", "JwtClaim"),
                                        field("claimName", "sub"),
                                        field("claimValue", "SUBJECT_ONE")
                                ), object(
                                        field("type", "JwtClaim"),
                                        field("claimName", "sub"),
                                        field("claimValue", "SUBJECT_TWO")
                                )))
                ))
        ));
    }

    private JsonValue createBackendScopeBPolicyJson() {
        return json(object(
                field("name", "POLICY_ID - SCOPE_B"),
                field("resources", array("uma://POLICY_ID")),
                field("resourceTypeUuid", "76656a38-5f8e-401b-83aa-4ccb74ce88d2"),
                field("actionValues", object(field("SCOPE_B", true))),
                field("subject", object(
                        field("type", "OR"),
                        field("subjects", array(
                                object(
                                        field("type", "JwtClaim"),
                                        field("claimName", "sub"),
                                        field("claimValue", "SUBJECT_ONE")
                                )))
                ))
        ));
    }

    private void mockLoadUserUmaPolicies(ServerContext context) {
        QueryResult queryResult = new QueryResult();
        List<Resource> policies = new ArrayList<Resource>();
        Resource readPolicy1 = new Resource("ID_1", "REVISION_1", createBackendScopeAPolicyJson());
        Resource readPolicy2 = new Resource("ID_1", "REVISION_1", createBackendScopeBPolicyJson());
        policies.add(readPolicy1);
        policies.add(readPolicy2);

        Promise<Pair<QueryResult, List<Resource>>, ResourceException> queryPromise =
                Promises.newSuccessfulPromise(Pair.of(queryResult, policies));

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateUmaPolicy() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        List<Resource> createdPolicies = new ArrayList<Resource>();
        Resource createdPolicy1 = new Resource("ID_1", "REVISION_1", createBackendScopeAPolicyJson());
        Resource createdPolicy2 = new Resource("ID_1", "REVISION_1", createBackendScopeBPolicyJson());
        createdPolicies.add(createdPolicy1);
        createdPolicies.add(createdPolicy2);
        Promise<List<Resource>, ResourceException> createPolicyPromise = Promises.newSuccessfulPromise(createdPolicies);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID")).willReturn(null);
        given(policyResourceDelegate.createPolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(createPolicyPromise);

        //When
        UmaPolicy umaPolicy = policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        verify(umaPolicyStore).getUserCache("RESOURCE_OWNER_ID", "/");
        verify(umaPolicyStore).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"), Matchers.<UmaPolicy>anyObject());
        assertThat(umaPolicy.getId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        assertThat(umaPolicy.asJson().asMap()).hasSize(3)
                .contains(entry("policyId", "RESOURCE_SET_ID"), entry("name", "NAME"));
        JsonValue permissions = umaPolicy.asJson().get("permissions");
        assertThat(permissions.asList()).hasSize(2);
        assertThat(permissions.get(0).asMap()).contains(entry("subject", "SUBJECT_ONE"));
        assertThat(permissions.get(0).get("scopes").asList()).containsOnly("SCOPE_A", "SCOPE_B");
        assertThat(permissions.get(1).asMap()).contains(entry("subject", "SUBJECT_TWO"));
        assertThat(permissions.get(1).get("scopes").asList()).containsOnly("SCOPE_A");
    }

    @Test(expectedExceptions = ConflictException.class)
    @SuppressWarnings("unchecked")
    public void shouldNotCreateUmaPolicyIfAlreadyExists() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        UmaPolicy cacheEntry = mock(UmaPolicy.class);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID")).willReturn(cacheEntry);

        //When
        try {
            policyService.createPolicy(context, policy).getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            verify(umaPolicyStore).getUserCache("RESOURCE_OWNER_ID", "/");
            verify(umaPolicyStore, never()).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"),
                    Matchers.<UmaPolicy>anyObject());
            verifyZeroInteractions(policyResourceDelegate);
            throw e;
        }
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToCreateUnderlyingPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson();
        ResourceException exception = mock(ResourceException.class);
        Promise<List<Resource>, ResourceException> createPoliciesPromise = Promises.newFailedPromise(exception);

        given(policyResourceDelegate.createPolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(createPoliciesPromise);

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotCreateUmaPolicyIfContainsInvalidRequestedScope() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson("SCOPE_C");

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotCreateUmaPolicyIfResourceSetNotFound() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson().put("policyId", "OTHER_ID");

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void shouldNotCreateUmaPolicyWhenFailToReadResourceSet() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson().put("policyId", "FAILING_ID");

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReadUmaPolicy() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        UmaPolicy cacheEntry = mock(UmaPolicy.class);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("POLICY_ID"))
                .willReturn(null)
                .willReturn(cacheEntry);
        mockLoadUserUmaPolicies(context);

        //When
        UmaPolicy umaPolicy = policyService.readPolicy(context, "POLICY_ID").getOrThrowUninterruptibly();

        //Then
        verify(umaPolicyStore, times(2)).getUserCache("RESOURCE_OWNER_ID", "/");
        verify(umaPolicyStore).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"),
                Matchers.<UmaPolicy>anyObject());
        assertThat(umaPolicy).isEqualTo(cacheEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReadUmaPolicyFromCache() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        UmaPolicy cacheEntry = mock(UmaPolicy.class);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID")).willReturn(cacheEntry);

        //When
        UmaPolicy umaPolicy = policyService.readPolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();

        //Then
        verifyZeroInteractions(policyResourceDelegate);
        verify(umaPolicyStore).getUserCache("RESOURCE_OWNER_ID", "/");
        verify(umaPolicyStore, never()).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"),
                Matchers.<UmaPolicy>anyObject());
        assertThat(umaPolicy).isEqualTo(cacheEntry);
    }

    @Test(expectedExceptions = ResourceException.class)
    @SuppressWarnings("unchecked")
    public void shouldHandleReadFailureToQueryUnderlyingPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID")).willReturn(null);
        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResult, List<Resource>>, ResourceException> queryPromise =
                Promises.newFailedPromise(exception);

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);

        //When
        try {
            policyService.readPolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            verify(umaPolicyStore).getUserCache("RESOURCE_OWNER_ID", "/");
            verify(umaPolicyStore, never()).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"),
                    Matchers.<UmaPolicy>anyObject());
            throw e;
        }
    }

    @Test
    public void shouldUpdateUmaPolicy() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson("SCOPE_A", "SCOPE_B");
        List<Resource> updatedPolicies = new ArrayList<Resource>();
        Resource updatedPolicy1 = new Resource("ID_1", "REVISION_1", createBackendScopeAPolicyJson());
        Resource updatedPolicy2 = new Resource("ID_1", "REVISION_1", createBackendScopeBPolicyJson());
        updatedPolicies.add(updatedPolicy1);
        updatedPolicies.add(updatedPolicy2);
        Promise<List<Resource>, ResourceException> updatePolicyPromise = Promises.newSuccessfulPromise(updatedPolicies);

        given(policyResourceDelegate.updatePolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePolicyPromise);

        //When
        UmaPolicy umaPolicy = policyService.updatePolicy(context, "POLICY_ID", policy)
                .getOrThrowUninterruptibly();

        //Then
        verify(umaPolicyStore).addToUserCache(eq("RESOURCE_OWNER_ID"), eq("/"), eq("RESOURCE_SET_ID"), Matchers.<UmaPolicy>anyObject());
        assertThat(umaPolicy.getId()).isEqualTo("POLICY_ID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        assertThat(umaPolicy.asJson().asMap()).isEqualTo(createUmaPolicyJson("SCOPE_A", "SCOPE_B").asMap());
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToUpdateUnderlyingPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson("SCOPE_A", "SCOPE_B");
        ResourceException exception = mock(ResourceException.class);
        Promise<List<Resource>, ResourceException> updatePoliciesPromise = Promises.newFailedPromise(exception);

        given(policyResourceDelegate.updatePolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePoliciesPromise);

        //When
        policyService.updatePolicy(context, "POLICY_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateUmaPolicyIfContainsInvalidRequestedScope() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson("SCOPE_C");

        //When
        policyService.updatePolicy(context, "POLICY_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateUmaPolicyIfResourceSetNotFound() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson();

        //When
        policyService.updatePolicy(context, "OTHER_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void shouldNotUpdateUmaPolicyWhenFailToReadResourceSet() throws Exception {

        //Given
        ServerContext context = createContext();
        JsonValue policy = createUmaPolicyJson();

        //When
        policyService.updatePolicy(context, "FAILING_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldDeleteUmaPolicy() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        UmaPolicy cacheEntry = mock(UmaPolicy.class);
        List<Resource> readPolicies = new ArrayList<Resource>();
        Resource readPolicy1 = new Resource("ID_1", "REVISION_1", createBackendScopeAPolicyJson());
        Resource readPolicy2 = new Resource("ID_2", "REVISION_2", createBackendScopeBPolicyJson());
        readPolicies.add(readPolicy1);
        readPolicies.add(readPolicy2);
        Promise<List<Resource>, ResourceException> deletePoliciesPromise = Promises.newSuccessfulPromise(readPolicies);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID"))
                .willReturn(null)
                .willReturn(cacheEntry);
        mockLoadUserUmaPolicies(context);
        given(policyResourceDelegate.deletePolicies(eq(context), anyListOf(String.class)))
                .willReturn(deletePoliciesPromise);

        //When
        policyService.deletePolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();

        //Then
        verify(policyResourceDelegate).deletePolicies(eq(context), anyListOf(String.class));
        verify(userCache).invalidate("RESOURCE_SET_ID");
    }

    @Test(expectedExceptions = ResourceException.class)
    @SuppressWarnings("unchecked")
    public void shouldHandleDeleteFailureToQueryUnderlyingPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResult, List<Resource>>, ResourceException> readPoliciesPromise =
                Promises.newFailedPromise(exception);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID"))
                .willReturn(null);
        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(readPoliciesPromise);

        //When
        try {
            policyService.deletePolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            verify(policyResourceDelegate, never()).deletePolicies(eq(context), anyListOf(String.class));
            throw e;
        }
    }

    @Test(expectedExceptions = ResourceException.class)
    @SuppressWarnings("unchecked")
    public void shouldHandleFailureToDeleteUnderlyingPolicies() throws Exception {

        //Given
        ServerContext context = createContext();
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        UmaPolicy cacheEntry = mock(UmaPolicy.class);
        ResourceException exception = mock(ResourceException.class);
        Promise<List<Resource>, ResourceException> deletePoliciesPromise = Promises.newFailedPromise(exception);

        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.getIfPresent("RESOURCE_SET_ID"))
                .willReturn(null)
                .willReturn(cacheEntry);
        mockLoadUserUmaPolicies(context);
        given(policyResourceDelegate.deletePolicies(eq(context), anyListOf(String.class)))
                .willReturn(deletePoliciesPromise);

        //When
        try {
            policyService.deletePolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            verify(policyResourceDelegate).deletePolicies(eq(context), anyListOf(String.class));
            throw e;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubject() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo("/permissions/subject", "SUBJECT_ONE"));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubject() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo("/permissions/subject", "SUBJECT_OTHER"));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo("resourceServer", "CLIENT_ID"));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo("resourceServer", "OTHER_CLIENT_ID"));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectAndResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_ONE"),
                        QueryFilter.equalTo("resourceServer", "CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectAndResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_SUBJECT"),
                        QueryFilter.equalTo("resourceServer", "CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectAndUnknownResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_ONE"),
                        QueryFilter.equalTo("resourceServer", "OTHER_CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectAndUnknownResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_OTHER"),
                        QueryFilter.equalTo("resourceServer", "OTHER_CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectOrResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_ONE"),
                        QueryFilter.equalTo("resourceServer", "CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectOrResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_OTHER"),
                        QueryFilter.equalTo("resourceServer", "CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectOrUnknownResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_ONE"),
                        QueryFilter.equalTo("resourceServer", "OTHER_CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectOrUnknownResourceServer() throws Exception {

        //Given
        ServerContext context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo("/permissions/subject", "SUBJECT_OTHER"),
                        QueryFilter.equalTo("resourceServer", "OTHER_CLIENT_ID")
                ));
        Cache<String, UmaPolicy> userCache = mock(Cache.class);
        ConcurrentHashMap<String, UmaPolicy> policyMap = new ConcurrentHashMap<String, UmaPolicy>();
        policyMap.put("RESOURCE_SET_ID", UmaPolicy.valueOf(resourceSet, createUmaPolicyJson()));

        mockLoadUserUmaPolicies(context);
        given(umaPolicyStore.getUserCache("RESOURCE_OWNER_ID", "/")).willReturn(userCache);
        given(userCache.asMap()).willReturn(policyMap);

        //When
        Pair<QueryResult, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }
}
