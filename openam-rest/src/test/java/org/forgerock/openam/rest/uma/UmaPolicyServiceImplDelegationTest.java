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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.rest.uma.UmaPolicyServiceImplTest.*;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.JwtPrincipal;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.context.ServerContext;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.ResharingMode;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaSettings;
import org.forgerock.openam.uma.UmaSettingsFactory;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyServiceImplDelegationTest {

    private UmaPolicyServiceImpl policyService;

    @Mock
    private PolicyResourceDelegate policyResourceDelegate;
    @Mock
    private ResourceSetStore resourceSetStore;
    @Mock
    private UmaAuditLogger auditLogger;
    @Mock
    private ContextHelper contextHelper;
    @Mock
    private Evaluator policyEvaluator;
    @Mock
    private CoreServicesWrapper coreServicesWrapper;
    @Mock
    UmaSettings umaSettings;

    private String loggedInUser;
    private String loggedInRealm;
    private String userInUri;
    private Set<JsonValue> createdPolicies;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        //Stub getting ResourceSetStore
        ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        given(resourceSetStoreFactory.create(anyString())).willReturn(resourceSetStore);

        //Stub getting UmaAuditLogger
        Config<UmaAuditLogger> lazyAuditLogger = mock(Config.class);
        given(lazyAuditLogger.get()).willReturn(auditLogger);

        //Stub getting Evaluator
        UmaPolicyEvaluatorFactory policyEvaluatorFactory = mock(UmaPolicyEvaluatorFactory.class);
        given(policyEvaluatorFactory.getEvaluator(any(Subject.class), anyString())).willReturn(policyEvaluator);

        Debug debug = mock(Debug.class);
        UmaSettingsFactory umaSettingsFactory = mock(UmaSettingsFactory.class);
        given(umaSettingsFactory.create(anyString())).willReturn(umaSettings);

        policyService = new UmaPolicyServiceImpl(policyResourceDelegate, resourceSetStoreFactory, lazyAuditLogger,
                contextHelper, policyEvaluatorFactory, coreServicesWrapper, debug, umaSettingsFactory);

        given(contextHelper.getRealm(Matchers.<ServerContext>anyObject())).willReturn("REALM");
    }

    @AfterMethod
    public void tearDown() {
        loggedInUser = null;
        loggedInRealm = null;
        userInUri = null;
        createdPolicies = null;
    }

    @Test
    public void aliceShouldBeAbleToCreatePolicyForResource() throws Exception {

        //Given
        userIsLoggedIn("alice", "REALM");
        accessingUriForUser("alice");
        String resourceSetId = registerResourceSet("alice");
        JsonValue policy = policyToCreate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        promise.getOrThrow();
        assertThat(promise).succeeded();
        verifyPolicyIsCreatedForLoggedInUser();
        verifyAuditLogCreatedForLoggedInUser(resourceSetId);
    }

    @Test
    public void bobShouldBeAbleToCreatePolicyForResourceSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_A", "SCOPE_B");
        JsonValue policy = policyToCreate(resourceSetId);
        setResharingModeToImplicit();
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        assertThat(promise).succeeded();
        verifyPolicyIsCreatedForLoggedInUser();
        verifyAuditLogCreatedForLoggedInUser(resourceSetId);
    }

    @Test
    public void bobShouldNotBeAbleToCreatePolicyForResourceNotSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        JsonValue policy = policyToCreate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
        verifyPolicyIsNotCreatedForLoggedInUser();
        verifyAuditLogNotCreatedForLoggedInUser(resourceSetId);
    }

    @Test
    public void bobShouldNotBeAbleToCreatePolicyForResourceWithMoreScopesThanSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_A");
        JsonValue policy = policyToCreate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
        verifyPolicyIsNotCreatedForLoggedInUser();
        verifyAuditLogNotCreatedForLoggedInUser(resourceSetId);
    }

    @Test
    public void aliceShouldBeAbleToUpdatePolicyForResource() throws Exception {

        //Given
        userIsLoggedIn("alice", "REALM");
        accessingUriForUser("alice");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_A", "SCOPE_B");
        JsonValue policy = policyToUpdate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.updatePolicy(context, resourceSetId, policy);

        //Then
        assertThat(promise).succeeded();
    }

    @Test
    public void bobShouldBeAbleToReadPolicyForResourceSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_A", "SCOPE_B");
        createPolicyFor("charlie", resourceSetId, "SCOPE_A", "SCOPE_B");
        setResharingModeToImplicit();
        JsonValue policy = policyToUpdate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.updatePolicy(context, resourceSetId, policy);

        //Then
        assertThat(promise).succeeded();
    }

    @Test
    public void bobShouldNotBeAbleToUpdatePolicyForResourceNotSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("charlie", resourceSetId, "SCOPE_A", "SCOPE_B");
        JsonValue policy = policyToUpdate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.updatePolicy(context, resourceSetId, policy);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void bobShouldNotBeAbleToUpdatePolicyForResourceWithMoreScopesThanSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_B");
        createPolicyFor("charlie", resourceSetId, "SCOPE_A", "SCOPE_B");
        JsonValue policy = policyToUpdate(resourceSetId);
        ServerContext context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.updatePolicy(context, resourceSetId, policy);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    //Set up helper methods

    private void userIsLoggedIn(String username, String realm) {
        loggedInUser = username;
        loggedInRealm = realm;
        setupIdentityForUser(loggedInUser, loggedInRealm);
    }

    private void setupIdentityForUser(String username, String realm) {
        AMIdentity identity = mock(AMIdentity.class);
        given(identity.getUniversalId()).willReturn("uid=" + username + ",ou=" + realm + ",dc=forgerock,dc=org");
        given(coreServicesWrapper.getIdentity(username, realm)).willReturn(identity);
    }

    private void accessingUriForUser(String username) {
        userInUri = username;
        given(contextHelper.getUserId(Matchers.<ServerContext>anyObject())).willReturn(userInUri);
        given(contextHelper.getUserUid(Matchers.<ServerContext>anyObject())).willReturn("uid=" + userInUri + ",ou="
                + loggedInRealm + ",dc=forgerock,dc=org");
    }

    private String registerResourceSet(String resourceOwner) throws Exception {
        setupIdentityForUser(resourceOwner, loggedInRealm);
        String resourceSetId = UUID.randomUUID().toString();
        ResourceSetDescription resourceSet = new ResourceSetDescription(resourceSetId, "CLIENT_ID", resourceOwner,
                json(object(field("name", "RESOURCE_SET_NAME"), field("scopes", array("SCOPE_A", "SCOPE_B")))).asMap());
        given(resourceSetStore.read(resourceSetId)).willReturn(resourceSet);
        given(resourceSetStore.query(org.forgerock.util.query.QueryFilter.and(
                org.forgerock.util.query.QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId))))
                .willReturn(Collections.singleton(resourceSet));
        return resourceSetId;
    }

    private void createPolicyFor(String requestingParty, String resourceSetId, String... scopes)
            throws EntitlementException {
        Map<String, Boolean> actionValues = new HashMap<>();
        for (String scope : scopes) {
            actionValues.put(scope, true);
        }
        given(policyEvaluator.evaluate(loggedInRealm, createSubject(requestingParty),
                UmaConstants.UMA_POLICY_SCHEME + resourceSetId, null, false))
                .willReturn(Collections.singletonList(new Entitlement(resourceSetId, actionValues)));
    }

    private Subject createSubject(String username) {
        setupIdentityForUser(username, loggedInRealm);
        AMIdentity identity = coreServicesWrapper.getIdentity(username, loggedInRealm);
        JwtPrincipal principal = new JwtPrincipal(json(object(field("sub", identity.getUniversalId()))));
        Set<Principal> principals = new HashSet<>();
        principals.add(principal);
        return new Subject(false, principals, Collections.emptySet(), Collections.emptySet());
    }

    private JsonValue policyToCreate(String resourceSetId) {
        mockPolicyResourceDelegateForNewPolicy();
        return createUmaPolicyForResourceSet(resourceSetId);
    }

    private JsonValue policyToUpdate(String resourceSetId) {
        mockPolicyResourceDelegateForUpdatedPolicy();
        JsonValue umaPolicy = createUmaPolicyForResourceSet(resourceSetId);
        umaPolicy.remove(new JsonPointer("/permissions/0/scopes/1"));
        return umaPolicy;
    }

    private JsonValue createUmaPolicyForResourceSet(String resourceSetId) {
        return createUmaPolicyJson(resourceSetId);
    }

    private void mockPolicyResourceDelegateForNewPolicy() {
        final List<Resource> createdPolicies = new ArrayList<>();
        Resource createdPolicy1 = new Resource("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        Resource createdPolicy2 = new Resource("ID_1", "REVISION_1", createBackendSubjectTwoPolicyJson());
        createdPolicies.add(createdPolicy1);
        createdPolicies.add(createdPolicy2);
        Promise<List<Resource>, ResourceException> createPolicyPromise = newResultPromise(createdPolicies);
        given(policyResourceDelegate.createPolicies(any(ServerContext.class), anySetOf(JsonValue.class)))
                .willReturn(createPolicyPromise);

        Promise<Pair<QueryResult, List<Resource>>, ResourceException> queryPromise =
                newExceptionPromise((ResourceException) new NotFoundException());
        given(policyResourceDelegate.queryPolicies(any(ServerContext.class), any(QueryRequest.class)))
                .willReturn(queryPromise);
        given(policyResourceDelegate.queryPolicies(any(ServerContext.class), any(QueryRequest.class),
                any(QueryResultHandler.class))).willAnswer(new Answer<Promise<QueryResult, ResourceException>>() {
            @Override
            public Promise<QueryResult, ResourceException> answer(InvocationOnMock invocation) throws Throwable {
                final PolicyGraph policyGraph = (PolicyGraph) invocation.getArguments()[2];
                for (Resource r : createdPolicies) {
                    policyGraph.handleResource(r);
                }
                policyGraph.handleResult(new QueryResult());
                return newResultPromise(new QueryResult());
            }
        });
    }

    private void mockPolicyResourceDelegateForUpdatedPolicy() {
        List<Resource> currentPolicies = new ArrayList<>();
        Resource currentPolicy1 = new Resource("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        Resource currentPolicy2 = new Resource("ID_2", "REVISION_1", createBackendSubjectTwoPolicyJson());
        currentPolicies.add(currentPolicy1);
        currentPolicies.add(currentPolicy2);
        Promise<Pair<QueryResult, List<Resource>>, ResourceException> queryPromise
                = newResultPromise(Pair.of((QueryResult) null, currentPolicies));
        given(policyResourceDelegate.queryPolicies(any(ServerContext.class), any(QueryRequest.class)))
                .willReturn(queryPromise);

        final List<Resource> updatedPolicies = new ArrayList<>();
        Resource updatedPolicy1 = new Resource("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        Resource updatedPolicy3 = new Resource("ID_3", "REVISION_1", createBackendSubjectTwoPolicyJson());
        updatedPolicies.add(updatedPolicy1);
        updatedPolicies.add(updatedPolicy3);
        Promise<List<Resource>, ResourceException> updatePolicyPromise = newResultPromise(updatedPolicies);
        given(policyResourceDelegate.updatePolicies(any(ServerContext.class), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePolicyPromise);
        given(policyResourceDelegate.queryPolicies(any(ServerContext.class), any(QueryRequest.class),
                any(QueryResultHandler.class))).willAnswer(new Answer<Promise<QueryResult, ResourceException>>() {
            @Override
            public Promise<QueryResult, ResourceException> answer(InvocationOnMock invocation) throws Throwable {
                final PolicyGraph policyGraph = (PolicyGraph) invocation.getArguments()[2];
                for (Resource r : updatedPolicies) {
                    policyGraph.handleResource(r);
                }
                policyGraph.handleResult(new QueryResult());
                return newResultPromise(new QueryResult());
            }
        });
    }

    private void setResharingModeToImplicit() throws ServerException {
        given(umaSettings.getResharingMode()).willReturn(ResharingMode.IMPLICIT);
    }

    private ServerContext getContext() throws Exception {
        SubjectContext subjectContext = mock(SSOTokenContext.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(subjectContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn(loggedInUser);
        return new ServerContext(new RealmContext(subjectContext));
    }

    @SuppressWarnings("unchecked")
    private void captureCreatedPolicies() {
        if (createdPolicies == null) {
            ArgumentCaptor<Set> policyCaptor = ArgumentCaptor.forClass(Set.class);
            verify(policyResourceDelegate).createPolicies(any(ServerContext.class), policyCaptor.capture());
            createdPolicies = policyCaptor.getValue();
        }
    }

    private void verifyPolicyIsCreatedForLoggedInUser() {
        captureCreatedPolicies();
        for (JsonValue policy : createdPolicies) {
            Assertions.assertThat(policy.get("name").asString().contains("RESOURCE_SET_NAME-" + loggedInUser));
        }
    }

    private void verifyPolicyIsNotCreatedForLoggedInUser() {
        verify(policyResourceDelegate, never()).createPolicies(any(ServerContext.class), anySetOf(JsonValue.class));
    }

    private void verifyAuditLogCreatedForLoggedInUser(String resourceSetId) {
        verify(auditLogger).log(eq(resourceSetId), anyString(), eq(loggedInUser), eq(UmaAuditType.POLICY_CREATED), eq(loggedInUser));
    }

    private void verifyAuditLogNotCreatedForLoggedInUser(String resourceSetId) {
        verify(auditLogger, never()).log(eq(resourceSetId), anyString(), eq(loggedInUser), eq(UmaAuditType.POLICY_CREATED), eq(loggedInUser));
    }
}
