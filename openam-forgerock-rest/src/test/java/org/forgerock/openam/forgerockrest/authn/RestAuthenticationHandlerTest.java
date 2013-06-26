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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.locale.L10NMessageImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jwt.PlaintextJwt;
import org.forgerock.json.jwt.SignedJwt;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandlerResponseException;
import org.forgerock.openam.forgerockrest.authn.core.AuthIndexType;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.core.LoginAuthenticator;
import org.forgerock.openam.forgerockrest.authn.core.LoginConfiguration;
import org.forgerock.openam.forgerockrest.authn.core.LoginProcess;
import org.forgerock.openam.forgerockrest.authn.core.LoginStage;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.json.JSONException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestAuthenticationHandlerTest {

    private RestAuthenticationHandler restAuthenticationHandler;

    private LoginAuthenticator loginAuthenticator;
    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;
    private AuthIdHelper authIdHelper;
    private CoreServicesWrapper coreServicesWrapper;

    @BeforeMethod
    public void setUp() {

        loginAuthenticator = mock(LoginAuthenticator.class);
        restAuthCallbackHandlerManager = mock(RestAuthCallbackHandlerManager.class);
        amAuthErrorCodeResponseStatusMapping = mock(AMAuthErrorCodeResponseStatusMapping.class);
        authIdHelper = mock(AuthIdHelper.class);
        coreServicesWrapper = mock(CoreServicesWrapper.class);

        restAuthenticationHandler = new RestAuthenticationHandler(loginAuthenticator, restAuthCallbackHandlerManager,
                amAuthErrorCodeResponseStatusMapping, authIdHelper, coreServicesWrapper);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        given(authContextLocalWrapper.getSSOToken()).willReturn(ssoToken);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 200);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertEquals(jsonValue.get("tokenId").asString(), "SSO_TOKEN_ID");

        ArgumentCaptor<LoginConfiguration> argumentCaptor = ArgumentCaptor.forClass(LoginConfiguration.class);
        verify(loginAuthenticator).getLoginProcess(argumentCaptor.capture());
        LoginConfiguration loginConfiguration = argumentCaptor.getValue();
        assertEquals(loginConfiguration.getHttpRequest(), request);
        assertEquals(loginConfiguration.getIndexType(), AuthIndexType.NONE);
        assertEquals(loginConfiguration.getIndexValue(), null);
        assertEquals(loginConfiguration.getSessionId(), "");
        assertEquals(loginConfiguration.getSSOTokenId(), "");
    }

    @Test
    public void shouldInitiateAuthenticationViaGET1() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = AuthIndexType.MODULE.toString();
        String indexValue = "INDEX_VALUE";
        String sessionUpgradeSSOTokenId = null;

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        given(authContextLocalWrapper.getErrorCode()).willReturn("ERROR_CODE");
        given(authContextLocalWrapper.getErrorMessage()).willReturn("ERROR_MESSAGE");

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(false);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 401);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertEquals(jsonValue.get("errorMessage").asString(), "ERROR_MESSAGE");

        ArgumentCaptor<LoginConfiguration> argumentCaptor = ArgumentCaptor.forClass(LoginConfiguration.class);
        verify(loginAuthenticator).getLoginProcess(argumentCaptor.capture());
        LoginConfiguration loginConfiguration = argumentCaptor.getValue();
        assertEquals(loginConfiguration.getHttpRequest(), request);
        assertEquals(loginConfiguration.getIndexType(), AuthIndexType.MODULE);
        assertEquals(loginConfiguration.getIndexValue(), "INDEX_VALUE");
        assertEquals(loginConfiguration.getSessionId(), "");
        assertEquals(loginConfiguration.getSSOTokenId(), "");
    }

    @Test
    public void shouldInitiateAuthenticationViaGET2() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthCallbackHandlerResponseException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        Callback callbackOne = mock(Callback.class);
        Callback callbackTwo = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo};

        PagePropertiesCallback pagePropertiesCallback = mock(PagePropertiesCallback.class);
        given(pagePropertiesCallback.getTemplateName()).willReturn("TEMPLATE_NAME");
        given(pagePropertiesCallback.getModuleName()).willReturn("MODULE_NAME");
        given(pagePropertiesCallback.getPageState()).willReturn("PAGE_STATE");

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.REQUIREMENTS_WAITING);
        given(loginProcess.getCallbacks()).willReturn(callbacks);
        given(loginProcess.getPagePropertiesCallback()).willReturn(pagePropertiesCallback);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        JsonValue jsonCallbacks = new JsonValue(new HashMap<String, Object>());
        jsonCallbacks.add("KEY", "VALUE");

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, httpResponse, null, callbacks,
                HttpMethod.GET)).willReturn(jsonCallbacks);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 200);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 4);
        assertEquals(jsonValue.get("authId").asString(), "AUTH_ID");
        assertEquals(jsonValue.get("template").asString(), "TEMPLATE_NAME");
        assertEquals(jsonValue.get("stage").asString(), "MODULE_NAMEPAGE_STATE");
        assertEquals(jsonValue.get("callbacks").get("KEY").asString(), "VALUE");
    }

    @Test
    public void shouldInitiateAuthenticationViaGET3() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthCallbackHandlerResponseException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        Callback callbackOne = mock(Callback.class);
        Callback callbackTwo = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo};

        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        given(authContextLocalWrapper.getSSOToken()).willReturn(ssoToken);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage())
                .willReturn(LoginStage.REQUIREMENTS_WAITING)
                .willReturn(LoginStage.COMPLETE);
        given(loginProcess.getCallbacks()).willReturn(callbacks);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);
        given(loginProcess.next(callbacks)).willReturn(loginProcess);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);


        JsonValue jsonCallbacks = new JsonValue(new HashMap<String, Object>());

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, httpResponse, null, callbacks,
                HttpMethod.GET)).willReturn(jsonCallbacks);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 200);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertEquals(jsonValue.get("tokenId").asString(), "SSO_TOKEN_ID");
        verify(loginProcess).next(callbacks);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET4() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthCallbackHandlerResponseException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        Callback[] callbacks = new Callback[0];

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.REQUIREMENTS_WAITING);
        given(loginProcess.getCallbacks()).willReturn(callbacks);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        PagePropertiesCallback pagePropertiesCallback = mock(PagePropertiesCallback.class);
        given(pagePropertiesCallback.getTemplateName()).willReturn("TEMPLATE_NAME");
        given(pagePropertiesCallback.getModuleName()).willReturn("MODULE_NAME");
        given(pagePropertiesCallback.getPageState()).willReturn("PAGE_STATE");

        JsonValue jsonCallbacks = new JsonValue(new HashMap<String, Object>());
        jsonCallbacks.add("KEY", "VALUE");

        Map<String, String> responseHeaders = new HashMap<String, String>();
        responseHeaders.put("HEADER_KEY", "HEADER_VALUE");
        JsonValue jsonResponse = new JsonValue(new HashMap<String, Object>());
        jsonResponse.add("KEY", "VALUE");
        RestAuthCallbackHandlerResponseException restAuthCallbackHandlerResponseException =
                new RestAuthCallbackHandlerResponseException(Response.Status.ACCEPTED, responseHeaders, jsonResponse);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, httpResponse, null, callbacks,
                HttpMethod.GET)).willThrow(restAuthCallbackHandlerResponseException);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 202);
        assertTrue(response.getMetadata().get("HEADER_KEY").contains("HEADER_VALUE"));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 2);
        assertEquals(jsonValue.get("authId").asString(), "AUTH_ID");
        assertEquals(jsonValue.get("KEY").asString(), "VALUE");
    }

    @Test
    public void shouldInitiateAuthenticationViaGET5() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = "UNKNOWN";
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
    }

    @Test
    public void shouldInitiateAuthenticationViaPOST() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String postBody = "{ \"authId\": \"AUTH_ID\" }";
        String sessionUpgradeSSOTokenId = "SSO_TOKEN_ID";

        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        given(authContextLocalWrapper.getSSOToken()).willReturn(ssoToken);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        PlaintextJwt plaintextJwt = mock(PlaintextJwt.class);
        given(plaintextJwt.getContent("sessionId", String.class)).willReturn("SESSION_ID");
        given(plaintextJwt.getContent("authIndexType", String.class))
                .willReturn(AuthIndexType.MODULE.getIndexType().toString());
        given(plaintextJwt.getContent("authIndexValue", String.class)).willReturn("INDEX_VALUE");
        given(plaintextJwt.getContent("realm", String.class)).willReturn("REALM_DN");

        SignedJwt signedJwt = mock(SignedJwt.class);
        given(signedJwt.getJwt()).willReturn(plaintextJwt);

        given(authIdHelper.reconstructAuthId("AUTH_ID")).willReturn(signedJwt);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");

        //When
        Response response = restAuthenticationHandler.continueAuthentication(headers, request, httpResponse,
                postBody, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(response.getStatus(), 200);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertEquals(jsonValue.get("tokenId").asString(), "SSO_TOKEN_ID");

        verify(authIdHelper).verifyAuthId("REALM_DN", "AUTH_ID");

        ArgumentCaptor<LoginConfiguration> argumentCaptor = ArgumentCaptor.forClass(LoginConfiguration.class);
        verify(loginAuthenticator).getLoginProcess(argumentCaptor.capture());
        LoginConfiguration loginConfiguration = argumentCaptor.getValue();
        assertEquals(loginConfiguration.getHttpRequest(), request);
        assertEquals(loginConfiguration.getIndexType(), AuthIndexType.MODULE);
        assertEquals(loginConfiguration.getIndexValue(), "INDEX_VALUE");
        assertEquals(loginConfiguration.getSessionId(), "SESSION_ID");
        assertEquals(loginConfiguration.getSSOTokenId(), "SSO_TOKEN_ID");
    }

    @Test
    public void shouldGetLoginProcessAndThrow400ExceptionWithOrgDNNotValidReturningNull() throws SSOException,
            AuthException, AuthLoginException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn(null);

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
    }

    @Test
    public void shouldGetLoginProcessAndThrow400ExceptionWithOrgDNNotValidReturningEmptyString() throws SSOException,
            AuthException, AuthLoginException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("");

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
    }

    @Test
    public void shouldGetLoginProcessAndThrow400ExceptionWithOrgDNNotValid() throws SSOException, AuthException,
            AuthLoginException, IdRepoException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = null;
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.isOrganizationActive("ORG_DN")).willThrow(IdRepoException.class);

        //When
        Response response = restAuthenticationHandler.initiateAuthentication(headers, request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);

        //Then
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
    }
}
