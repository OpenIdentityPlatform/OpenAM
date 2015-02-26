package org.forgerock.openam.uma;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.json.JsonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class AuthorizationRequestEndpointTest {

    private static final String RS_CLIENT_ID = "RS_CLIENT_ID";
    private static final String RS_ID = "RESOURCE_SET_ID";
    private static final String RS_DESCRIPTION_ID = "RESOURCE_SET_DESCRIPTION_ID";
    private static final String RESOURCE_NAME = UmaConstants.UMA_POLICY_SCHEME + RS_DESCRIPTION_ID;

    private AuthorizationRequestEndpoint endpoint;
    private UmaProviderSettings umaProviderSettings;
    private UmaProviderSettingsFactory umaProviderSettingsFactory;
    private OAuth2RequestFactory<Request> requestFactory;
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
    private Subject subject;
    private UmaAuditLogger umaAuditLogger;

    private class AuthorizationRequestEndpoint2 extends AuthorizationRequestEndpoint {

        public AuthorizationRequestEndpoint2(UmaProviderSettingsFactory umaProviderSettingsFactory,
                TokenStore oauth2TokenStore, OAuth2RequestFactory<Request> requestFactory,
                OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
                UmaAuditLogger auditLogger) {
            super(umaProviderSettingsFactory, oauth2TokenStore, requestFactory, oAuth2ProviderSettingsFactory, auditLogger);
        }

        @Override
        protected Subject createSubject(String username, String realm) {
            return new Subject();
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
        accessToken = mock(AccessToken.class);

        oauth2TokenStore = mock(TokenStore.class);
        given(oauth2TokenStore.readAccessToken(Matchers.<OAuth2Request>anyObject(), anyString())).willReturn(accessToken);
        given(accessToken.getClientId()).willReturn(RS_CLIENT_ID);

        subject = new Subject();
        umaAuditLogger = mock(UmaAuditLogger.class);

        umaTokenStore = mock(UmaTokenStore.class);
        rpt = mock(RequestingPartyToken.class);
        given(rpt.getId()).willReturn("1");
        permissionTicket = mock(PermissionTicket.class);
        given(permissionTicket.getExpiryTime()).willReturn(System.currentTimeMillis() + 10000);
        given(permissionTicket.getResourceSetId()).willReturn(RS_ID);
        given(permissionTicket.getClientId()).willReturn(RS_CLIENT_ID);
        given(umaTokenStore.readPermissionTicket(anyString())).willReturn(permissionTicket);
        given(umaTokenStore.createRPT(Matchers.<AccessToken>anyObject(), Matchers.<PermissionTicket>anyObject()))
                .willReturn(rpt);

        resourceSetStore = mock(ResourceSetStore.class);
        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setId(RS_DESCRIPTION_ID);
        given(resourceSetStore.read(RS_ID)).willReturn(resourceSet);

        umaProviderSettings = mock(UmaProviderSettings.class);
        policyEvaluator = mock(Evaluator.class);
        given(umaProviderSettings.getPolicyEvaluator(subject)).willReturn(policyEvaluator);
        given(umaProviderSettings.getUmaTokenStore()).willReturn(umaTokenStore);

        umaProviderSettingsFactory = mock(UmaProviderSettingsFactory.class);
        given(umaProviderSettingsFactory.get(Matchers.<Request>anyObject())).willReturn(umaProviderSettings);
        given(umaProviderSettings.getUmaTokenStore()).willReturn(umaTokenStore);

        OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings oauth2ProviderSettings = mock(OAuth2ProviderSettings.class);
        given(oauth2ProviderSettingsFactory.get(any(OAuth2Request.class))).willReturn(oauth2ProviderSettings);
        given(oauth2ProviderSettings.getResourceSetStore()).willReturn(resourceSetStore);

        endpoint = spy(new AuthorizationRequestEndpoint2(umaProviderSettingsFactory, oauth2TokenStore,
                requestFactory, oauth2ProviderSettingsFactory, umaAuditLogger));
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
    public void shouldThrowNotAuthorizedErrorWhenScopeDoesNotExistInPermissionTicket() throws Exception {
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
            assertThat(e.getStatusCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("not_authorised");
            throw e;
        }
    }

    @Test(expectedExceptions = UmaException.class)
    public void shouldThrowNotAuthorizedErrorWhenScopeDoesNotExistInPermissionTicket2() throws Exception {
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
            assertThat(e.getStatusCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("not_authorised");
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
        assertThat(endpoint.requestAuthorization(entity) != null);
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
        assertThat(endpoint.requestAuthorization(entity) != null);
    }

    private Entitlement createEntitlement(String action) {
        Entitlement entitlement = new Entitlement();
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put(action, true);
        entitlement.setActionValues(actionValues);
        return entitlement;
    }
}
