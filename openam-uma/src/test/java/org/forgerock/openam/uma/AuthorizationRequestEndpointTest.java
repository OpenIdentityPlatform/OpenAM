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

package org.forgerock.openam.uma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.eq;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.uma.extensions.RequestAuthorizationFilter;
import org.forgerock.util.query.QueryFilter;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthorizationRequestEndpointTest {

    private static final String RS_CLIENT_ID = "RS_CLIENT_ID";
    private static final String RS_ID = "RESOURCE_SET_ID";
    private static final String RESOURCE_OWNER_ID = "RESOURCE_OWNER_ID";
    private static final String RS_DESCRIPTION_ID = "RESOURCE_SET_DESCRIPTION_ID";
    private static final String REQUESTING_PARTY_ID = "REQUESTING_PARTY_ID";
    private static final String RESOURCE_NAME = UmaConstants.UMA_POLICY_SCHEME + RS_DESCRIPTION_ID;

    private AuthorizationRequestEndpoint endpoint;
    private UmaProviderSettings umaProviderSettings;
    private UmaProviderSettingsFactory umaProviderSettingsFactory;
    private OAuth2RequestFactory<?, Request> requestFactory;
    private TokenStore oauth2TokenStore;
    private Evaluator policyEvaluator;
    private UmaTokenStore umaTokenStore;
    private Request request;
    private AccessToken accessToken;
    private PermissionTicket permissionTicket;
    private JsonRepresentation entity;
    private JSONObject requestBody;
    private RequestingPartyToken rpt;
    private Response response;
    private ResourceSetStore resourceSetStore;
    private Subject subject = new Subject();
    private UmaAuditLogger umaAuditLogger;
    private PendingRequestsService pendingRequestsService;
    private IdTokenClaimGatherer idTokenClaimGatherer;
    private RequestAuthorizationFilter requestAuthorizationFilter;
    private JacksonRepresentationFactory jacksonRepresentationFactory =
            new JacksonRepresentationFactory(new ObjectMapper());

    private class AuthorizationRequestEndpoint2 extends AuthorizationRequestEndpoint {

        public AuthorizationRequestEndpoint2(UmaProviderSettingsFactory umaProviderSettingsFactory,
                TokenStore oauth2TokenStore, OAuth2RequestFactory<?, Request> requestFactory,
                OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
                OAuth2UrisFactory<RealmInfo> oAuth2UrisFactory,
                UmaAuditLogger auditLogger, PendingRequestsService pendingRequestsService,
                Map<String, ClaimGatherer> claimGatherers, ExtensionFilterManager extensionFilterManager,
                UmaExceptionHandler exceptionHandler, JacksonRepresentationFactory jacksonRepresentationFactory) {
            super(umaProviderSettingsFactory, oauth2TokenStore, requestFactory, oAuth2ProviderSettingsFactory,
                    oAuth2UrisFactory, auditLogger, pendingRequestsService, claimGatherers, extensionFilterManager,
                    exceptionHandler, jacksonRepresentationFactory);
        }

        @Override
        protected AMIdentity createIdentity(final String username, final String realm) {
            AMIdentity identity = mock(AMIdentity.class);
            if (!RESOURCE_OWNER_ID.equals(username)) {
                given(identity.getUniversalId()).willReturn("RESOURCE_OWNER_ID");
            } else {
                given(identity.getUniversalId()).willReturn(realm + ":" + username);
            }
            return identity;
        }

        @Override
        protected AccessToken getAuthorisationApiToken() throws ServerException {
            return accessToken;
        }
    }

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setup() throws ServerException, InvalidGrantException, NotFoundException, EntitlementException, JSONException {
        requestFactory = mock(OAuth2RequestFactory.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);
        given(requestFactory.create(any(Request.class))).willReturn(oAuth2Request);
        given(oAuth2Request.getParameter("realm")).willReturn("REALM");
        accessToken = mock(AccessToken.class);

        oauth2TokenStore = mock(TokenStore.class);
        given(oauth2TokenStore.readAccessToken(Matchers.<OAuth2Request>anyObject(), anyString())).willReturn(accessToken);
        given(accessToken.getClientId()).willReturn(RS_CLIENT_ID);
        given(accessToken.getResourceOwnerId()).willReturn(REQUESTING_PARTY_ID);

        umaAuditLogger = mock(UmaAuditLogger.class);

        umaTokenStore = mock(UmaTokenStore.class);
        rpt = mock(RequestingPartyToken.class);
        given(rpt.getId()).willReturn("1");
        permissionTicket = mock(PermissionTicket.class);
        given(permissionTicket.getExpiryTime()).willReturn(currentTimeMillis() + 10000);
        given(permissionTicket.getResourceSetId()).willReturn(RS_ID);
        given(permissionTicket.getResourceServerClientId()).willReturn(RS_CLIENT_ID);
        given(permissionTicket.getRealm()).willReturn("REALM");
        given(umaTokenStore.readPermissionTicket(anyString())).willReturn(permissionTicket);
        given(umaTokenStore.createRPT(Matchers.<PermissionTicket>anyObject())).willReturn(rpt);

        resourceSetStore = mock(ResourceSetStore.class);
        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setId(RS_DESCRIPTION_ID);
        resourceSet.setResourceOwnerId(RESOURCE_OWNER_ID);
        given(resourceSetStore.query(QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, RS_ID)))
                .willReturn(Collections.singleton(resourceSet));

        umaProviderSettings = mock(UmaProviderSettings.class);
        policyEvaluator = mock(Evaluator.class);
        given(umaProviderSettings.getPolicyEvaluator(any(Subject.class), eq(RS_CLIENT_ID.toLowerCase()))).willReturn(policyEvaluator);
        given(umaProviderSettings.getUmaTokenStore()).willReturn(umaTokenStore);

        umaProviderSettingsFactory = mock(UmaProviderSettingsFactory.class);
        given(umaProviderSettingsFactory.get(Matchers.<Request>anyObject())).willReturn(umaProviderSettings);
        given(umaProviderSettings.getUmaTokenStore()).willReturn(umaTokenStore);

        OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings oauth2ProviderSettings = mock(OAuth2ProviderSettings.class);
        given(oauth2ProviderSettingsFactory.get(any(OAuth2Request.class))).willReturn(oauth2ProviderSettings);
        given(oauth2ProviderSettings.getResourceSetStore()).willReturn(resourceSetStore);

        OAuth2UrisFactory<RealmInfo> oauth2UrisFactory = mock(OAuth2UrisFactory.class);
        OAuth2Uris oauth2Uris = mock(OAuth2Uris.class);
        given(oauth2UrisFactory.get(any(OAuth2Request.class))).willReturn(oauth2Uris);
        given(oauth2Uris.getIssuer()).willReturn("ISSUER");

        pendingRequestsService = mock(PendingRequestsService.class);

        Map<String, ClaimGatherer> claimGatherers = new HashMap<>();
        idTokenClaimGatherer = mock(IdTokenClaimGatherer.class);
        claimGatherers.put(IdTokenClaimGatherer.FORMAT, idTokenClaimGatherer);

        ExtensionFilterManager extensionFilterManager = mock(ExtensionFilterManager.class);
        requestAuthorizationFilter = mock(RequestAuthorizationFilter.class);
        given(extensionFilterManager.getFilters(RequestAuthorizationFilter.class))
                .willReturn(Collections.singletonList(requestAuthorizationFilter));

        UmaExceptionHandler exceptionHandler = mock(UmaExceptionHandler.class);

        endpoint = spy(new AuthorizationRequestEndpoint2(umaProviderSettingsFactory, oauth2TokenStore,
                requestFactory, oauth2ProviderSettingsFactory, oauth2UrisFactory, umaAuditLogger,
                pendingRequestsService, claimGatherers, extensionFilterManager, exceptionHandler,
                jacksonRepresentationFactory));
        request = mock(Request.class);
        given(endpoint.getRequest()).willReturn(request);

        response = mock(Response.class);
        endpoint.setResponse(response);

        requestBody = mock(JSONObject.class);
        given(requestBody.toString()).willReturn("{\"ticket\": \"016f84e8-f9b9-11e0-bd6f-0021cc6004de\"}");

        entity = mock(JsonRepresentation.class);
        given(entity.getJsonObject()).willReturn(requestBody);
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowRequestSubmittedErrorWhenScopeDoesNotExistInPermissionTicket() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<Entitlement>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<String>();
        requestedScopes.add("Read");
        requestedScopes.add("Write");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            InOrder inOrder = inOrder(requestAuthorizationFilter, policyEvaluator, requestAuthorizationFilter);
            inOrder.verify(requestAuthorizationFilter).beforeAuthorization(eq(permissionTicket), any(Subject.class),
                    any(Subject.class));
            inOrder.verify(policyEvaluator).evaluate(anyString(), any(Subject.class), anyString(), anyMap(), eq(false));
            inOrder.verify(requestAuthorizationFilter).afterFailedAuthorization(eq(permissionTicket),
                    any(Subject.class), any(Subject.class));
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("request_submitted");
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowRequestSubmittedErrorWhenScopeDoesNotExistInPermissionTicket2() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<Entitlement>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<String>();
        requestedScopes.add("Write");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            InOrder inOrder = inOrder(requestAuthorizationFilter, policyEvaluator, requestAuthorizationFilter);
            inOrder.verify(requestAuthorizationFilter).beforeAuthorization(eq(permissionTicket), any(Subject.class),
                    any(Subject.class));
            inOrder.verify(policyEvaluator).evaluate(anyString(), any(Subject.class), anyString(), anyMap(), eq(false));
            inOrder.verify(requestAuthorizationFilter).afterFailedAuthorization(eq(permissionTicket),
                    any(Subject.class), any(Subject.class));
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("request_submitted");
            throw e;
        }
    }

    @Test
    public void shouldReturnTrueWhenRequestedScopesExactlyMatchesEntitlements() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<Entitlement>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<String>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        //Then
        assertThat(endpoint.requestAuthorization(entity)).isNotNull();
        InOrder inOrder = inOrder(requestAuthorizationFilter, policyEvaluator, requestAuthorizationFilter);
        inOrder.verify(requestAuthorizationFilter).beforeAuthorization(eq(permissionTicket), any(Subject.class),
                any(Subject.class));
        inOrder.verify(policyEvaluator).evaluate(anyString(), any(Subject.class), anyString(), anyMap(), eq(false));
        inOrder.verify(requestAuthorizationFilter).afterSuccessfulAuthorization(eq(permissionTicket),
                any(Subject.class), any(Subject.class));
    }

    @Test
    public void shouldReturnTrueWhenRequestedScopesSubsetOfEntitlements() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<Entitlement>();
        entitlements.add(createEntitlement("Read"));
        entitlements.add(createEntitlement("Create"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<String>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        //Then
        assertThat(endpoint.requestAuthorization(entity)).isNotNull();
        InOrder inOrder = inOrder(requestAuthorizationFilter, policyEvaluator, requestAuthorizationFilter);
        inOrder.verify(requestAuthorizationFilter).beforeAuthorization(eq(permissionTicket), any(Subject.class),
                any(Subject.class));
        inOrder.verify(policyEvaluator).evaluate(anyString(), any(Subject.class), anyString(), anyMap(), eq(false));
        inOrder.verify(requestAuthorizationFilter).afterSuccessfulAuthorization(eq(permissionTicket),
                any(Subject.class), any(Subject.class));
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowNeedInfoExceptionWhenTrustElevationRequiredButClaimNotPresent() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME),
                Matchers.<Map<String, Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        enableRequiredTrustElevation();
        mockIdTokenClaimGatherRequiredClaimsDetails();

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("need_info");
            assertThat(e.getDetail()).hasArray("requesting_party_claims");
            assertThat(e.getDetail()).booleanAt("requesting_party_claims/0/CLAIMS_REQUIRED").isTrue();
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowNeedInfoExceptionWhenTrustElevationRequiredAndUnknownClaimPresent() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME),
                Matchers.<Map<String, Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        enableRequiredTrustElevation();
        mockRequestBodyWithUnknownClaim();

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("need_info");
            assertThat(e.getDetail()).hasArray("requesting_party_claims");
            assertThat(e.getDetail()).booleanAt("requesting_party_claims/0/CLAIMS_REQUIRED").isTrue();
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowNeedInfoExceptionWhenTrustElevationRequiredAndClaimGatheringFails() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME),
                Matchers.<Map<String, Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        enableRequiredTrustElevation();
        mockRequestBodyWithInvalidIdTokenClaim();

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("need_info");
            assertThat(e.getDetail()).hasArray("requesting_party_claims");
            assertThat(e.getDetail()).booleanAt("requesting_party_claims/0/CLAIMS_REQUIRED").isTrue();
            throw e;
        }
    }

    @Test
    public void shouldReturnRptWhenTrustElevationRequiredAndIdTokenClaimPresent() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(createEntitlement("Read"));

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME),
                Matchers.<Map<String, Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        enableRequiredTrustElevation();
        mockRequestBodyWithIdTokenClaim();

        //Then
        assertThat(endpoint.requestAuthorization(entity)).isNotNull();
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldCreatePendingRequestAndThrowRequestSubmittedExceptionWhenNoEntitlementsForRequestedScopes()
            throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        requestedScopes.add("Create");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        given(permissionTicket.getResourceSetId()).willReturn("RESOURCE_SET_ID");
        given(permissionTicket.getRealm()).willReturn("REALM");

        mockPendingRequestsQuery();

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            verify(pendingRequestsService).createPendingRequest(any(HttpServletRequest.class), eq("RESOURCE_SET_ID"),
                    anyString(), anyString(), anyString(), eq("REALM"), eq(requestedScopes));
            verify(umaAuditLogger).log(eq("RESOURCE_SET_ID"), any(AMIdentity.class), eq(UmaAuditType.REQUEST_SUBMITTED),
                    any(Request.class), anyString());
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("request_submitted");
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldCreatePendingRequestForAuthorizationRequestWithDifferentScopes() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        requestedScopes.add("Delete");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        given(permissionTicket.getResourceSetId()).willReturn("RESOURCE_SET_ID");
        given(permissionTicket.getRealm()).willReturn("REALM");

        mockPendingRequestsQuery(createPendingRequest("Read", "Create"));

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            verify(pendingRequestsService).createPendingRequest(any(HttpServletRequest.class), eq("RESOURCE_SET_ID"),
                    anyString(), anyString(), anyString(), eq("REALM"), eq(requestedScopes));
            verify(umaAuditLogger).log(eq("RESOURCE_SET_ID"), any(AMIdentity.class), eq(UmaAuditType.REQUEST_SUBMITTED),
                    any(Request.class), anyString());
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("request_submitted");
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowRequestSubmittedExceptionWhenNoEntitlementsForRequestedScopes() throws Exception {
        //Given
        ArrayList<Entitlement> entitlements = new ArrayList<>();

        given(policyEvaluator.evaluate(anyString(), Matchers.<Subject>anyObject(), eq(RESOURCE_NAME), Matchers.<Map<String,
                Set<String>>>anyObject(), anyBoolean())).willReturn(entitlements);

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add("Read");
        requestedScopes.add("Create");
        given(permissionTicket.getScopes()).willReturn(requestedScopes);

        given(permissionTicket.getResourceSetId()).willReturn("RESOURCE_SET_ID");
        given(permissionTicket.getRealm()).willReturn("REALM");

        mockPendingRequestsQuery(createPendingRequest("Read", "Create"));

        //Then
        try {
            endpoint.requestAuthorization(entity);
        } catch (UmaException e) {
            verify(pendingRequestsService, never()).createPendingRequest(any(HttpServletRequest.class),
                    eq("RESOURCE_SET_ID"), anyString(), anyString(), anyString(), eq("REALM"), eq(requestedScopes));
            verify(umaAuditLogger, never()).log(eq("RESOURCE_SET_ID"), any(AMIdentity.class), eq(UmaAuditType.REQUEST_SUBMITTED),
                    any(Request.class), anyString());
            assertThat(e.getStatusCode()).isEqualTo(403);
            assertThat(e.getError()).isEqualTo("request_submitted");
            throw e;
        }
    }

    private Entitlement createEntitlement(String action) {
        Entitlement entitlement = new Entitlement();
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put(action, true);
        entitlement.setActionValues(actionValues);
        return entitlement;
    }

    private UmaPendingRequest createPendingRequest(String... scopes) {
        UmaPendingRequest pendingRequest = mock(UmaPendingRequest.class);
        given(pendingRequest.getScopes()).willReturn(new HashSet<>(Arrays.asList(scopes)));
        return pendingRequest;
    }

    private void mockPendingRequestsQuery(UmaPendingRequest... pendingRequests)
            throws org.forgerock.openam.sm.datalayer.store.ServerException {
        given(pendingRequestsService.queryPendingRequests(anyString(), anyString(), anyString(), anyString()))
                .willReturn(new HashSet<>(Arrays.asList(pendingRequests)));
    }

    private void enableRequiredTrustElevation() throws ServerException {
        given(umaProviderSettings.isTrustElevationRequired()).willReturn(true);
    }

    private void mockRequestBodyWithIdTokenClaim() {
        Mockito.reset(requestBody, idTokenClaimGatherer);
        given(requestBody.toString()).willReturn("{\"ticket\": \"016f84e8-f9b9-11e0-bd6f-0021cc6004de\", "
                + "\"claim_tokens\": [{"
                + "\"format\": \"" + IdTokenClaimGatherer.FORMAT + "\", "
                + "\"token\": \"ID_TOKEN\"}]}");
        given(idTokenClaimGatherer.getRequestingPartyId(any(OAuth2Request.class), any(AccessToken.class),
                any(JsonValue.class))).willReturn("REQUESTING_PARTY_ID");
        mockIdTokenClaimGatherRequiredClaimsDetails();
    }

    private void mockRequestBodyWithInvalidIdTokenClaim() {
        Mockito.reset(requestBody, idTokenClaimGatherer);
        given(requestBody.toString()).willReturn("{\"ticket\": \"016f84e8-f9b9-11e0-bd6f-0021cc6004de\", "
                + "\"claim_tokens\": [{"
                + "\"format\": \"" + IdTokenClaimGatherer.FORMAT + "\", "
                + "\"token\": \"ID_TOKEN\"}]}");
        mockIdTokenClaimGatherRequiredClaimsDetails();
    }

    private void mockIdTokenClaimGatherRequiredClaimsDetails() {
        given(idTokenClaimGatherer.getRequiredClaimsDetails(anyString())).willReturn(json(object(
                field("CLAIMS_REQUIRED", true))));
    }

    private void mockRequestBodyWithUnknownClaim() {
        Mockito.reset(requestBody);
        given(requestBody.toString()).willReturn("{\"ticket\": \"016f84e8-f9b9-11e0-bd6f-0021cc6004de\", "
                + "\"claim_tokens\": [{"
                + "\"format\": \"UNKNOWN_FORMAT\", "
                + "\"token\": \"TOKEN\"}]}");
        mockIdTokenClaimGatherRequiredClaimsDetails();
    }
}
