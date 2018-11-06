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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import com.sun.identity.shared.Constants;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTest;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.uma.ResourceSetAcceptAllFilter;
import org.forgerock.openam.uma.extensions.ResourceDelegationFilter;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaSettings;
import org.forgerock.openam.uma.UmaSettingsFactory;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

public class UmaPolicyServiceImplTest {

    static final String RESOURCE_OWNER_ID = "alice";
    private UmaPolicyServiceImpl policyService;

    private PolicyResourceDelegate policyResourceDelegate;
    private ResourceSetStore resourceSetStore;
    private ResourceSetDescription resourceSet;
    private UmaAuditLogger auditLogger;
    private ContextHelper contextHelper;
    private Evaluator policyEvaluator;
    private CoreServicesWrapper coreServicesWrapper;
    private UmaSettings umaSettings;
    private ResourceDelegationFilter resourceDelegationFilter;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setup() throws Exception {

        policyResourceDelegate = mock(PolicyResourceDelegate.class);
        final ResourceSetStoreFactory resourceSetStoreFactory = mock(ResourceSetStoreFactory.class);
        Config<UmaAuditLogger> lazyAuditLogger = mock(Config.class);
        auditLogger = mock(UmaAuditLogger.class);
        contextHelper = mock(ContextHelper.class);
        UmaPolicyEvaluatorFactory policyEvaluatorFactory = mock(UmaPolicyEvaluatorFactory.class);
        policyEvaluator = mock(Evaluator.class);
        given(policyEvaluatorFactory.getEvaluator(any(Subject.class), anyString())).willReturn(policyEvaluator);
        coreServicesWrapper = mock(CoreServicesWrapper.class);
        Debug debug = mock(Debug.class);
        UmaSettingsFactory umaSettingsFactory = mock(UmaSettingsFactory.class);
        UmaSettings umaSettings = mock(UmaSettings.class);
        given(umaSettingsFactory.create(anyString())).willReturn(umaSettings);

        ExtensionFilterManager extensionFilterManager = mock(ExtensionFilterManager.class);
        resourceDelegationFilter = mock(ResourceDelegationFilter.class);
        given(extensionFilterManager.getFilters(ResourceDelegationFilter.class))
                .willReturn(Collections.singleton(resourceDelegationFilter));

        policyService = new UmaPolicyServiceImpl(policyResourceDelegate, resourceSetStoreFactory, lazyAuditLogger,
                contextHelper, policyEvaluatorFactory, coreServicesWrapper, debug, umaSettingsFactory,
                extensionFilterManager);

        given(contextHelper.getRealm(Matchers.<Context>anyObject())).willReturn("REALM");
        given(contextHelper.getUserId(Matchers.<Context>anyObject())).willReturn(RESOURCE_OWNER_ID);
        given(contextHelper.getUserUid(Matchers.<Context>anyObject())).willReturn("RESOURCE_OWNER_UID");

        resourceSetStore = mock(ResourceSetStore.class);
        resourceSet = new ResourceSetDescription("RESOURCE_SET_ID",
                "CLIENT_ID", RESOURCE_OWNER_ID, Collections.<String, Object>emptyMap());
        resourceSet.setDescription(json(object(field("name", "NAME"), field("scopes", array("SCOPE_A", "SCOPE_B", "SCOPE_C")))));

        given(resourceSetStoreFactory.create(anyString())).willReturn(resourceSetStore);
        given(resourceSetStore.read("RESOURCE_SET_ID", RESOURCE_OWNER_ID)).willReturn(resourceSet);
        given(resourceSetStore.read(eq("RESOURCE_SET_ID"),
                any(ResourceSetAcceptAllFilter.class))).willReturn(resourceSet);
        given(resourceSetStore.query(QueryFilter.and(
                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, "RESOURCE_SET_ID"))))
                .willReturn(Collections.singleton(resourceSet));
        doThrow(org.forgerock.oauth2.core.exceptions.NotFoundException.class).when(resourceSetStore).read("OTHER_ID", RESOURCE_OWNER_ID);
        doThrow(org.forgerock.oauth2.core.exceptions.ServerException.class).when(resourceSetStore).read("FAILING_ID", RESOURCE_OWNER_ID);
        doThrow(org.forgerock.oauth2.core.exceptions.ServerException.class).when(resourceSetStore).query(QueryFilter.and(
                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, "FAILING_ID")));
        given(lazyAuditLogger.get()).willReturn(auditLogger);

        AMIdentity identity = mock(AMIdentity.class);
        given(identity.getUniversalId()).willReturn("uid=RESOURCE_OWNER_ID,ou=REALM,dc=openidentityplatform,dc=org");
        given(coreServicesWrapper.getIdentity(RESOURCE_OWNER_ID, "REALM")).willReturn(identity);

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    private Context createContext() throws SSOException {
        return createContextForLoggedInUser(RESOURCE_OWNER_ID);
    }

    private Context createContextForLoggedInUser(String userShortName) throws SSOException {
        SubjectContext subjectContext = mock(SSOTokenContext.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(subjectContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("id=" + userShortName + ",ou=REALM,dc=openidentityplatform,dc=org");
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn(userShortName);
        return ClientContext.newInternalClientContext(new RealmContext(subjectContext, Realm.root()));
    }

    private static JsonValue createUmaPolicyJson(String resourceSetId, String... subjectTwoScopes) {
        return json(object(
                field("policyId", resourceSetId),
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

    static JsonValue createUmaPolicyJson(String resourceSetId) {
        return createUmaPolicyJson(resourceSetId, "SCOPE_A");
    }

    static JsonValue createBackendSubjectOnePolicyJson() {
        return json(object(
                field("name", "RESOURCE_SET_ID - SUBJECT_ONE"),
                field("createdBy", RESOURCE_OWNER_ID),
                field("active", true),
                field("resources", array("uma://RESOURCE_SET_ID")),
                field("resourceTypeUuid", "76656a38-5f8e-401b-83aa-4ccb74ce88d2"),
                field("actionValues", object(
                        field("SCOPE_A", true),
                        field("SCOPE_B", true))),
                field("subject", object(
                        field("type", "JwtClaim"),
                        field("claimName", "sub"),
                        field("claimValue", "SUBJECT_ONE")
                ))
        ));
    }

    static JsonValue createBackendSubjectTwoPolicyJson() {
        return json(object(
                field("name", "RESOURCE_SET_ID - SUBJECT_TWO"),
                field("createdBy", RESOURCE_OWNER_ID),
                field("active", true),
                field("resources", array("uma://RESOURCE_SET_ID")),
                field("resourceTypeUuid", "76656a38-5f8e-401b-83aa-4ccb74ce88d2"),
                field("actionValues", object(
                        field("SCOPE_A", true))),
                field("subject", object(
                        field("type", "JwtClaim"),
                        field("claimName", "sub"),
                        field("claimValue", "SUBJECT_TWO")
                ))
        ));
    }

    static JsonValue createBackendSubjectOneUpdatedPolicyJson() {
        return json(object(
                field("name", "RESOURCE_SET_ID - SUBJECT_ONE"),
                field("createdBy", RESOURCE_OWNER_ID),
                field("active", true),
                field("resources", array("uma://RESOURCE_SET_ID")),
                field("resourceTypeUuid", "76656a38-5f8e-401b-83aa-4ccb74ce88d2"),
                field("actionValues", object(
                        field("SCOPE_A", true),
                        field("SCOPE_B", true),
                        field("SCOPE_C", true))),
                field("subject", object(
                        field("type", "JwtClaim"),
                        field("claimName", "sub"),
                        field("claimValue", "SUBJECT_ONE")
                ))
        ));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateUmaPolicy() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID");
        List<ResourceResponse> createdPolicies = new ArrayList<>();
        ResourceResponse createdPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse createdPolicy2 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectTwoPolicyJson());
        createdPolicies.add(createdPolicy1);
        createdPolicies.add(createdPolicy2);

        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                Promises.newExceptionPromise((ResourceException) new NotFoundException());
        setupQueries(queryPromise, createdPolicy1, createdPolicy2);

        Promise<List<ResourceResponse>, ResourceException> createPolicyPromise = newResultPromise(createdPolicies);
        given(policyResourceDelegate.createPolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(createPolicyPromise);

        //When
        UmaPolicy umaPolicy = policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        InOrder inOrder = inOrder(resourceDelegationFilter, policyResourceDelegate, resourceDelegationFilter);
        inOrder.verify(resourceDelegationFilter).beforeResourceShared(any(UmaPolicy.class));
        inOrder.verify(policyResourceDelegate).createPolicies(eq(context), anySetOf(JsonValue.class));
        inOrder.verify(resourceDelegationFilter).afterResourceShared(any(UmaPolicy.class));

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
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID");
        ResourceResponse policyResource = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                newResultPromise(
                        Pair.of(newQueryResponse(), Collections.singletonList(policyResource)));

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);

        //When
        try {
            policyService.createPolicy(context, policy).getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            //Then
            verify(policyResourceDelegate, never()).createPolicies(eq(context), anySetOf(JsonValue.class));
            throw e;
        }
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToCreateUnderlyingPolicies() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID");
        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                Promises.newExceptionPromise((ResourceException) new NotFoundException());
        Promise<List<ResourceResponse>, ResourceException> createPoliciesPromise = Promises.newExceptionPromise(exception);

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);
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
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID", "SCOPE_D");

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotCreateUmaPolicyIfResourceSetNotFound() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID").put("policyId", "OTHER_ID");

        //When
        policyService.createPolicy(context, policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void shouldNotCreateUmaPolicyWhenFailToReadResourceSet() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID").put("policyId", "FAILING_ID");

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
        Context context = createContext();

        QueryResponse queryResult = newQueryResponse();
        List<ResourceResponse> policies = new ArrayList<>();
        ResourceResponse readPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse readPolicy2 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectTwoPolicyJson());
        policies.add(readPolicy1);
        policies.add(readPolicy2);
        UmaPolicy expectedUmaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, policies);

        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                newResultPromise(Pair.of(queryResult, policies));

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);

        //When
        UmaPolicy umaPolicy = policyService.readPolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();

        //Then
        assertThat(umaPolicy).isEqualTo(expectedUmaPolicy);
    }

    @Test(expectedExceptions = ResourceException.class)
    @SuppressWarnings("unchecked")
    public void shouldHandleReadFailureToQueryUnderlyingPolicies() throws Exception {

        //Given
        Context context = createContext();

        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPromise =
                Promises.newExceptionPromise(exception);

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(queryPromise);

        //When
        policyService.readPolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();

        //Then
        failBecauseExceptionWasNotThrown(ResourceException.class);
    }

    @Test
    public void shouldUpdateUmaPolicy() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID", "SCOPE_A", "SCOPE_C");
        policy.remove(new JsonPointer("/permissions/0/scopes/1"));
        List<ResourceResponse> updatedPolicies = new ArrayList<>();
        ResourceResponse updatedPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOneUpdatedPolicyJson());
        ResourceResponse updatedPolicy2 = newResourceResponse("ID_2", "REVISION_1", createBackendSubjectTwoPolicyJson());
        updatedPolicies.add(updatedPolicy1);
        updatedPolicies.add(updatedPolicy2);
        Promise<List<ResourceResponse>, ResourceException> updatePolicyPromise = newResultPromise(updatedPolicies);


        List<ResourceResponse> currentPolicies = new ArrayList<>();
        ResourceResponse currentPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse currentPolicy2 = newResourceResponse("ID_2", "REVISION_1", createBackendSubjectTwoPolicyJson());
        currentPolicies.add(currentPolicy1);
        currentPolicies.add(currentPolicy2);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> currentPolicyPromise
                = newResultPromise(Pair.of((QueryResponse) null, currentPolicies));

        setupQueries(currentPolicyPromise, updatedPolicy1, updatedPolicy2);

        given(policyResourceDelegate.updatePolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePolicyPromise);

        //When
        UmaPolicy umaPolicy = policyService.updatePolicy(context, "RESOURCE_SET_ID", policy)
                .getOrThrowUninterruptibly();

        //Then
        InOrder inOrder = inOrder(resourceDelegationFilter, policyResourceDelegate);
        inOrder.verify(resourceDelegationFilter).beforeResourceSharedModification(any(UmaPolicy.class),
                any(UmaPolicy.class));
        inOrder.verify(policyResourceDelegate, times(2)).updatePolicies(any(Context.class), anySetOf(JsonValue.class));

        assertThat(umaPolicy.getId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        JsonValue expectedPolicyJson = createUmaPolicyJson("RESOURCE_SET_ID", "SCOPE_A", "SCOPE_C");
        expectedPolicyJson.remove(new JsonPointer("/permissions/0/scopes/1"));
        assertThat(umaPolicy.asJson().asMap()).isEqualTo(expectedPolicyJson.asMap());
    }

    @Test(expectedExceptions = ResourceException.class)
    public void shouldHandleFailureToUpdateUnderlyingPolicies() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID", "SCOPE_A", "SCOPE_B");
        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> currentPolicyPromise
                = newResultPromise(Pair.of((QueryResponse) null, Collections.<ResourceResponse>emptyList()));
        Promise<List<ResourceResponse>, ResourceException> updatePoliciesPromise = newExceptionPromise(exception);

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(currentPolicyPromise);
        given(policyResourceDelegate.updatePolicies(eq(context), Matchers.<Set<JsonValue>>anyObject()))
                .willReturn(updatePoliciesPromise);

        //When
        policyService.updatePolicy(context, "RESOURCE_SET_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateUmaPolicyIfContainsInvalidRequestedScope() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID", "SCOPE_D");

        //When
        policyService.updatePolicy(context, "RESOURCE_SET_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateUmaPolicyIfResourceSetNotFound() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID");

        //When
        policyService.updatePolicy(context, "OTHER_ID", policy).getOrThrowUninterruptibly();

        //Then
        //Expected ResourceException
        verifyZeroInteractions(policyResourceDelegate);
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void shouldNotUpdateUmaPolicyWhenFailToReadResourceSet() throws Exception {

        //Given
        Context context = createContext();
        JsonValue policy = createUmaPolicyJson("RESOURCE_SET_ID");

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
        Context context = createContext();
        List<ResourceResponse> readPolicies = new ArrayList<ResourceResponse>();
        ResourceResponse readPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse readPolicy2 = newResourceResponse("ID_2", "REVISION_2", createBackendSubjectTwoPolicyJson());
        readPolicies.add(readPolicy1);
        readPolicies.add(readPolicy2);

        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> currentPolicyPromise
                = newResultPromise(Pair.of((QueryResponse) null, readPolicies));
        setupQueries(currentPolicyPromise);

        Promise<List<ResourceResponse>, ResourceException> deletePoliciesPromise = newResultPromise(readPolicies);

        given(policyResourceDelegate.deletePolicies(eq(context), anyListOf(String.class)))
                .willReturn(deletePoliciesPromise);

        //When
        policyService.deletePolicy(context, "RESOURCE_SET_ID").getOrThrowUninterruptibly();

        //Then
        InOrder inOrder = inOrder(resourceDelegationFilter, policyResourceDelegate);
        inOrder.verify(resourceDelegationFilter).onResourceSharedDeletion(any(UmaPolicy.class));
        inOrder.verify(policyResourceDelegate).deletePolicies(eq(context), anyListOf(String.class));
    }

    @Test(expectedExceptions = ResourceException.class)
    @SuppressWarnings("unchecked")
    public void shouldHandleDeleteFailureToQueryUnderlyingPolicies() throws Exception {

        //Given
        Context context = createContext();
        ResourceException exception = mock(ResourceException.class);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> readPoliciesPromise =
                Promises.newExceptionPromise(exception);

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
        Context context = createContext();
        ResourceException exception = mock(ResourceException.class);
        List<ResourceResponse> readPolicies = new ArrayList<>();
        ResourceResponse readPolicy1 = newResourceResponse("ID_1", "REVISION_1", createBackendSubjectOnePolicyJson());
        ResourceResponse readPolicy2 = newResourceResponse("ID_2", "REVISION_2", createBackendSubjectTwoPolicyJson());
        readPolicies.add(readPolicy1);
        readPolicies.add(readPolicy2);
        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> currentPolicyPromise
                = newResultPromise(Pair.of((QueryResponse) null, readPolicies));
        Promise<List<ResourceResponse>, ResourceException> deletePoliciesPromise = newExceptionPromise(exception);

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(currentPolicyPromise);
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

    private void setupQueries(Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> initialQuery,
            final ResourceResponse... updatedPolicies) {
        given(policyResourceDelegate.queryPolicies(any(Context.class), any(QueryRequest.class)))
                .willReturn(initialQuery);
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

    private void mockBackendQuery(Context context, JsonValue... policies) {
        QueryResponse queryResult = newQueryResponse();
        List<ResourceResponse> policyResources = new ArrayList<>();
        for (JsonValue policy : policies) {
            policyResources.add(newResourceResponse("ID_1", "REVISION_1", policy));
        }

        Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> backendQueryPromise
                = newResultPromise(Pair.of(queryResult, policyResources));

        given(policyResourceDelegate.queryPolicies(eq(context), Matchers.<QueryRequest>anyObject()))
                .willReturn(backendQueryPromise);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubject() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_ONE"));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubject() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_OTHER"));

        mockBackendQuery(context);

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer("resourceServer"), "CLIENT_ID"));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer("resourceServer"), "OTHER_CLIENT_ID"));

        mockBackendQuery(context);

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectAndResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_ONE"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectAndResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "OTHER_SUBJECT"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectAndUnknownResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_ONE"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "OTHER_CLIENT_ID")
                ));

        mockBackendQuery(context);

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectAndUnknownResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_OTHER"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "OTHER_CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectOrResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_ONE"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectOrResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_OTHER"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesBySubjectOrUnknownResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_ONE"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "OTHER_CLIENT_ID")
                ));

        mockBackendQuery(context, createBackendSubjectOnePolicyJson(), createBackendSubjectTwoPolicyJson());

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryUmaPoliciesByUnknownSubjectOrUnknownResourceServer() throws Exception {

        //Given
        Context context = createContext();
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.or(
                        QueryFilter.equalTo(new JsonPointer("permissions/subject"), "SUBJECT_OTHER"),
                        QueryFilter.equalTo(new JsonPointer("resourceServer"), "OTHER_CLIENT_ID")
                ));

        mockBackendQuery(context);

        //When
        Pair<QueryResponse, Collection<UmaPolicy>> queryResult = policyService.queryPolicies(context, request)
                .getOrThrowUninterruptibly();

        //Then
        assertThat(queryResult.getSecond()).hasSize(0);
    }
}
