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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.uma.rest.UmaPolicyServiceImplTest.*;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;

import com.sun.identity.shared.Constants;
import org.assertj.core.api.Assertions;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.ContextHelper;
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
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.JwtPrincipal;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

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
    private RealmTestHelper realmTestHelper;

    private AMIdentity loggedInUser;
    private String loggedInUserId;
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

        ExtensionFilterManager extensionFilterManager = mock(ExtensionFilterManager.class);

        policyService = new UmaPolicyServiceImpl(policyResourceDelegate, resourceSetStoreFactory, lazyAuditLogger,
                contextHelper, policyEvaluatorFactory, coreServicesWrapper, debug, umaSettingsFactory,
                extensionFilterManager);

        given(contextHelper.getRealm(Matchers.<Context>anyObject())).willReturn("REALM");

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
        loggedInUser = null;
        loggedInUserId = null;
        loggedInRealm = null;
        userInUri = null;
        createdPolicies = null;
    }

    @Test
    public void aliceShouldBeAbleToCreatePolicyForResource() throws Exception {

        //Given
        AMIdentity loggedInUser = userIsLoggedIn("alice", "REALM");
        accessingUriForUser("alice");
        String resourceSetId = registerResourceSet("alice");
        JsonValue policy = policyToCreate(resourceSetId);
        Context context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        promise.getOrThrow();
        assertThat(promise).succeeded();
        verifyPolicyIsCreatedForLoggedInUser();
        verifyAuditLogCreatedForLoggedInUser(resourceSetId, loggedInUser);
    }

    @Test
    public void bobShouldBeAbleToCreatePolicyForResourceSharedByAlice() throws Exception {

        //Given
        AMIdentity loggedInUser = userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        createPolicyFor("bob", resourceSetId, "SCOPE_A", "SCOPE_B");
        JsonValue policy = policyToCreate(resourceSetId);
        setResharingModeToImplicit();
        Context context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.createPolicy(context, policy);

        //Then
        assertThat(promise).succeeded();
        verifyPolicyIsCreatedForLoggedInUser();
        verifyAuditLogCreatedForLoggedInUser(resourceSetId, loggedInUser);
    }

    @Test
    public void bobShouldNotBeAbleToCreatePolicyForResourceNotSharedByAlice() throws Exception {

        //Given
        userIsLoggedIn("bob", "REALM");
        accessingUriForUser("bob");
        String resourceSetId = registerResourceSet("alice");
        JsonValue policy = policyToCreate(resourceSetId);
        Context context = getContext();

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
        Context context = getContext();

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
        Context context = getContext();

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
        Context context = getContext();

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
        Context context = getContext();

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
        Context context = getContext();

        //When
        Promise<UmaPolicy, ResourceException> promise = policyService.updatePolicy(context, resourceSetId, policy);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    //Set up helper methods

    private AMIdentity userIsLoggedIn(String username, String realm) {
        loggedInUserId = username;
        loggedInRealm = realm;
        return setupIdentityForUser(username, loggedInRealm);
    }

    private AMIdentity setupIdentityForUser(String username, String realm) {
        AMIdentity loggedInUser = mock(AMIdentity.class);
        given(loggedInUser.getUniversalId()).willReturn("id=" + username + ",ou=" + realm + ",dc=openidentityplatform,dc=org");
        given(coreServicesWrapper.getIdentity(username, realm)).willReturn(loggedInUser);
        return loggedInUser;
    }

    private void accessingUriForUser(String username) {
        userInUri = username;
        given(contextHelper.getUserId(Matchers.<Context>anyObject())).willReturn(userInUri);
        given(contextHelper.getUserUid(Matchers.<Context>anyObject())).willReturn("uid=" + userInUri + ",ou="
                + loggedInRealm + ",dc=openidentityplatform,dc=org");
    }

    private String registerResourceSet(String resourceOwner) throws Exception {
        setupIdentityForUser(resourceOwner, loggedInRealm);
        String resourceSetId = UUID.randomUUID().toString();
        ResourceSetDescription resourceSet = new ResourceSetDescription(resourceSetId, "CLIENT_ID", resourceOwner,
                json(object(field("name", "RESOURCE_SET_NAME"), field("scopes", array("SCOPE_A", "SCOPE_B")))).asMap());
        given(resourceSetStore.read(resourceSetId, resourceOwner)).willReturn(resourceSet);
        given(resourceSetStore.query(QueryFilter.and(
                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId))))
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
        final List<ResourceResponse> createdPolicies = new ArrayList<>();
        ResourceResponse createdPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse createdPolicy2 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectTwoPolicyJson());
        createdPolicies.add(createdPolicy1);
        createdPolicies.add(createdPolicy2);
        Promise<List<ResourceResponse>, ResourceException> createPolicyPromise = newResultPromise(createdPolicies);
        given(policyResourceDelegate.createPolicies(any(Context.class), anySetOf(JsonValue.class)))
                .willReturn(createPolicyPromise);

        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                new NotFoundException().asPromise();
        given(policyResourceDelegate.queryPolicies(any(Context.class), any(QueryRequest.class)))
                .willReturn(queryPromise);
        given(policyResourceDelegate.queryPolicies(any(Context.class), any(QueryRequest.class),
                any(QueryResourceHandler.class))).willAnswer(new Answer<Promise<QueryResponse, ResourceException>>() {
            @Override
            public Promise<QueryResponse, ResourceException> answer(InvocationOnMock invocation) throws Throwable {
                final PolicyGraph policyGraph = (PolicyGraph) invocation.getArguments()[2];
                for (ResourceResponse r : createdPolicies) {
                    policyGraph.handleResource(r);
                }
                policyGraph.handleResult(newQueryResponse());
                return newResultPromise(newQueryResponse());
            }
        });
    }

    private void mockPolicyResourceDelegateForUpdatedPolicy() {
        List<ResourceResponse> currentPolicies = new ArrayList<>();
        ResourceResponse currentPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse currentPolicy2 = newResourceResponse("ID_2", "REVISION_1", createBackendSubjectTwoPolicyJson());
        currentPolicies.add(currentPolicy1);
        currentPolicies.add(currentPolicy2);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise
                = newResultPromise(Pair.of((QueryResponse) null, currentPolicies));
        given(policyResourceDelegate.queryPolicies(any(Context.class), any(QueryRequest.class)))
                .willReturn(queryPromise);

        final List<ResourceResponse> updatedPolicies = new ArrayList<>();
        ResourceResponse updatedPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse updatedPolicy3 = newResourceResponse("ID_3", "REVISION_1", createBackendSubjectTwoPolicyJson());
        updatedPolicies.add(updatedPolicy1);
        updatedPolicies.add(updatedPolicy3);
        Promise<List<ResourceResponse>, ResourceException> updatePolicyPromise = newResultPromise(updatedPolicies);
        given(policyResourceDelegate.updatePolicies(any(Context.class), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePolicyPromise);
        given(policyResourceDelegate.queryPolicies(any(Context.class), any(QueryRequest.class),
                any(QueryResourceHandler.class))).willAnswer(new Answer<Promise<QueryResponse, ResourceException>>() {
            @Override
            public Promise<QueryResponse, ResourceException> answer(InvocationOnMock invocation) throws Throwable {
                final PolicyGraph policyGraph = (PolicyGraph) invocation.getArguments()[2];
                for (ResourceResponse r : updatedPolicies) {
                    policyGraph.handleResource(r);
                }
                policyGraph.handleResult(newQueryResponse());
                return newResultPromise(newQueryResponse());
            }
        });
    }

    private void setResharingModeToImplicit() throws ServerException {
        given(umaSettings.getResharingMode()).willReturn(ResharingMode.IMPLICIT);
    }

    private Context getContext() throws Exception {
        SubjectContext subjectContext = mock(SSOTokenContext.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(subjectContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("id=" + loggedInUserId + ",ou=REALM,dc=openidentityplatform,dc=org");
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn(loggedInUserId);
        return ClientContext.newInternalClientContext(new RealmContext(subjectContext, Realm.root()));
    }

    @SuppressWarnings("unchecked")
    private void captureCreatedPolicies() {
        if (createdPolicies == null) {
            ArgumentCaptor<Set> policyCaptor = ArgumentCaptor.forClass(Set.class);
            verify(policyResourceDelegate).createPolicies(any(Context.class), policyCaptor.capture());
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
        verify(policyResourceDelegate, never()).createPolicies(any(Context.class), anySetOf(JsonValue.class));
    }

    private void verifyAuditLogCreatedForLoggedInUser(String resourceSetId, AMIdentity loggedInUser) {
        ArgumentCaptor<AMIdentity> loggedInUserCaptor = ArgumentCaptor.forClass(AMIdentity.class);
        verify(auditLogger).log(eq(resourceSetId), anyString(), loggedInUserCaptor.capture(), eq(UmaAuditType.POLICY_CREATED), eq("id=" + loggedInUserId + ",ou=REALM,dc=openidentityplatform,dc=org"));
        Assertions.assertThat(loggedInUserCaptor.getValue().getUniversalId()).isEqualTo(loggedInUser.getUniversalId());
    }

    private void verifyAuditLogNotCreatedForLoggedInUser(String resourceSetId) {
        verify(auditLogger, never()).log(eq(resourceSetId), anyString(), eq(loggedInUser), eq(UmaAuditType.POLICY_CREATED), eq("id=" + loggedInUserId + ",ou=REALM,dc=openidentityplatform,dc=org"));
    }
}
