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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.shared.locale.L10NMessageImpl;
import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.authn.core.AuthIndexType;
import org.forgerock.openam.core.rest.authn.core.LoginAuthenticator;
import org.forgerock.openam.core.rest.authn.core.LoginConfiguration;
import org.forgerock.openam.core.rest.authn.core.LoginProcess;
import org.forgerock.openam.core.rest.authn.core.LoginStage;
import org.forgerock.openam.core.rest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthErrorCodeException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RestAuthenticationHandlerTest {

    private RestAuthenticationHandler restAuthenticationHandler;

    private LoginAuthenticator loginAuthenticator;
    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;
    private AuthIdHelper authIdHelper;
    private CoreWrapper coreWrapper;

    @BeforeMethod
    public void setUp() {

        loginAuthenticator = mock(LoginAuthenticator.class);
        restAuthCallbackHandlerManager = mock(RestAuthCallbackHandlerManager.class);
        amAuthErrorCodeResponseStatusMapping = mock(AMAuthErrorCodeResponseStatusMapping.class);
        authIdHelper = mock(AuthIdHelper.class);
        coreWrapper = mock(CoreWrapper.class);

        restAuthenticationHandler = new RestAuthenticationHandler(loginAuthenticator, restAuthCallbackHandlerManager,
                amAuthErrorCodeResponseStatusMapping, authIdHelper, coreWrapper);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthException, RestAuthResponseException {

        //Given
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

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getSSOToken()).willReturn(ssoToken);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        //When
        JsonValue response = restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(response.get("tokenId").asString(), "SSO_TOKEN_ID");
        assertTrue(response.isDefined("successUrl"));

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
            JSONException, IOException, RestAuthException, RestAuthResponseException {

        //Given
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

        //When
        try {
            restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                    authIndexType, indexValue, sessionUpgradeSSOTokenId);
        } catch (RestAuthErrorCodeException e) {
            assertEquals(e.getStatusCode(), 401);
            ArgumentCaptor<LoginConfiguration> argumentCaptor = ArgumentCaptor.forClass(LoginConfiguration.class);
            verify(loginAuthenticator).getLoginProcess(argumentCaptor.capture());
            LoginConfiguration loginConfiguration = argumentCaptor.getValue();
            assertEquals(loginConfiguration.getHttpRequest(), request);
            assertEquals(loginConfiguration.getIndexType(), AuthIndexType.MODULE);
            assertEquals(loginConfiguration.getIndexValue(), "INDEX_VALUE");
            assertEquals(loginConfiguration.getSessionId(), "");
            assertEquals(loginConfiguration.getSSOTokenId(), "");
            return;
        }

        //Then
        fail();
    }

    @Test
    public void shouldInitiateAuthenticationViaGET2() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthResponseException, SignatureException, RestAuthException {

        //Given
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
        given(pagePropertiesCallback.getHeader()).willReturn("HEADER");
        given(pagePropertiesCallback.getInfoText()).willReturn(Collections.singletonList("MESSAGE"));

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.REQUIREMENTS_WAITING);
        given(loginProcess.getCallbacks()).willReturn(callbacks);
        given(loginProcess.getPagePropertiesCallback()).willReturn(pagePropertiesCallback);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        JsonValue jsonCallbacks = new JsonValue(new HashMap<String, Object>());
        jsonCallbacks.add("KEY", "VALUE");

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);
        given(restAuthCallbackHandlerManager.handleCallbacks(request, httpResponse, callbacks))
                .willReturn(jsonCallbacks);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        //When
        JsonValue response = restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(response.size(), 6);
        assertEquals(response.get("authId").asString(), "AUTH_ID");
        assertEquals(response.get("template").asString(), "TEMPLATE_NAME");
        assertEquals(response.get("stage").asString(), "MODULE_NAMEPAGE_STATE");
        assertEquals(response.get("header").asString(), "HEADER");
        assertEquals(response.get("callbacks").get("KEY").asString(), "VALUE");
        assertEquals(response.get("infoText").asList().size(), 1);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET3() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthResponseException, SignatureException, RestAuthException {

        //Given
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
        given(loginProcess.getSSOToken()).willReturn(ssoToken);
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
        given(restAuthCallbackHandlerManager.handleCallbacks(request, httpResponse, callbacks))
                .willReturn(jsonCallbacks);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        //When
        JsonValue response = restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                authIndexType, indexValue, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(response.get("tokenId").asString(), "SSO_TOKEN_ID");
        assertTrue(response.isDefined("successUrl"));
        verify(loginProcess).next(callbacks);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET4() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthResponseException, SignatureException, RestAuthException {

        //Given
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
        RestAuthResponseException restAuthResponseException =
                new RestAuthResponseException(999, responseHeaders, jsonResponse);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);
        given(restAuthCallbackHandlerManager.handleCallbacks(request, httpResponse, callbacks))
                .willThrow(restAuthResponseException);
        given(authIdHelper.createAuthId(Matchers.<LoginConfiguration>anyObject(), eq(authContextLocalWrapper)))
                .willReturn("AUTH_ID");

        //When
        try {
            restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                    authIndexType, indexValue, sessionUpgradeSSOTokenId);
        } catch (RestAuthResponseException e) {
            JsonValue response = e.getJsonResponse();
            assertEquals(response.size(), 2);
            assertEquals(response.get("authId").asString(), "AUTH_ID");
            assertEquals(response.get("KEY").asString(), "VALUE");
            Map<String, String> headers = e.getResponseHeaders();
            assertEquals(headers.get("HEADER_KEY"), "HEADER_VALUE");
            assertEquals(e.getStatusCode(), 999);
            return;
        }

        //Then
        fail();
    }

    @Test
    public void shouldInitiateAuthenticationViaGET5() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, RestAuthException, RestAuthResponseException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        String authIndexType = "UNKNOWN";
        String indexValue = null;
        String sessionUpgradeSSOTokenId = null;

        //When
        try {
            restAuthenticationHandler.initiateAuthentication(request, httpResponse,
                    authIndexType, indexValue, sessionUpgradeSSOTokenId);
        } catch (RestAuthException e) {
            assertEquals(e.getStatusCode(), 400);
            return;
        }

        //Then
        fail();
    }

    @Test
    public void shouldInitiateAuthenticationViaPOST() throws AuthLoginException, L10NMessageImpl,
            JSONException, IOException, SignatureException, RestAuthException, RestAuthResponseException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        JsonValue postBody = JsonValueBuilder.toJsonValue("{ \"authId\": \"AUTH_ID\" }");
        String sessionUpgradeSSOTokenId = "SSO_TOKEN_ID";

        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        given(authContextLocalWrapper.getSSOToken()).willReturn(ssoToken);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getSSOToken()).willReturn(ssoToken);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(signedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim("sessionId", String.class)).willReturn("SESSION_ID");
        given(claimsSet.getClaim("authIndexType", String.class))
                .willReturn(AuthIndexType.MODULE.getIndexType().toString());
        given(claimsSet.getClaim("authIndexValue", String.class)).willReturn("INDEX_VALUE");
        given(claimsSet.getClaim("realm", String.class)).willReturn("REALM_DN");

        given(authIdHelper.reconstructAuthId("AUTH_ID")).willReturn(signedJwt);

        //When
        JsonValue response = restAuthenticationHandler.continueAuthentication(request, httpResponse,
                postBody, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(response.get("tokenId").asString(), "SSO_TOKEN_ID");
        assertTrue(response.isDefined("successUrl"));

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
    public void shouldCleanupAfterAuthenticationComplete() throws Exception {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        String module = "LDAP";
        String existingSessionId = "session1";

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        // When
        restAuthenticationHandler.initiateAuthentication(request, response, "module", module, existingSessionId);

        // Then
        verify(loginProcess).cleanup();
    }

    @Test
    public void shouldReturnAbsoluteRealmInSuccessfulAuthenticationResponse() throws Exception {
        JsonValue response = performSuccessfulAuthentication();
        assertThat(response).stringAt("realm").isEqualTo("REALM");
    }

    private JsonValue performSuccessfulAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);

        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        LoginProcess loginProcess = mock(LoginProcess.class);
        given(loginProcess.getSSOToken()).willReturn(ssoToken);
        given(loginProcess.getLoginStage()).willReturn(LoginStage.COMPLETE);
        given(loginProcess.isSuccessful()).willReturn(true);
        given(loginProcess.getAuthContext()).willReturn(authContextLocalWrapper);

        given(loginAuthenticator.getLoginProcess(Matchers.<LoginConfiguration>anyObject())).willReturn(loginProcess);

        given(coreWrapper.convertOrgNameToRealmName(anyString())).willReturn("REALM");

        return restAuthenticationHandler.initiateAuthentication(request, httpResponse, null, null, null);
    }
}
