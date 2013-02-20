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

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.locale.L10NMessageImpl;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.AMKeyProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RestAuthenticationHandlerTest {

    private RestAuthenticationHandler restAuthenticationHandler;

    private AuthContext authContext;
    private AuthContextStateMap authContextStateMap;
    private AMKeyProvider amKeyProvider;
    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private JWTBuilderFactory jwtBuilderFactory;
    private SystemPropertiesManagerWrapper systemPropertiesManagerWrapper;

    @BeforeMethod
    public void setUp() {

        authContext = mock(AuthContext.class);
        authContextStateMap = mock(AuthContextStateMap.class);
        amKeyProvider = mock(AMKeyProvider.class);
        restAuthCallbackHandlerManager = mock(RestAuthCallbackHandlerManager.class);
        jwtBuilderFactory = mock(JWTBuilderFactory.class);
        systemPropertiesManagerWrapper = mock(SystemPropertiesManagerWrapper.class);

        restAuthenticationHandler = new RestAuthenticationHandler(authContextStateMap, amKeyProvider,
                restAuthCallbackHandlerManager, jwtBuilderFactory, systemPropertiesManagerWrapper) {
            @Override
            protected AuthContext createAuthContext(String realm) throws AuthLoginException {
                given(authContext.getOrganizationName()).willReturn(realm);
                return authContext;
            }
        };
    }

    @Test
    public void shouldAuthenticateSuccessfullyWithNoIndexTypeRealmOrReqs() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("/", authContext.getOrganizationName());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("tokenId"), "SSO_TOKEN_ID");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldAuthenticateSuccessfullyWithNoIndexTypeAndReqs() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, "REALM", null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("REALM", authContext.getOrganizationName());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("tokenId"), "SSO_TOKEN_ID");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    private void testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType indexType,
            String indexTypeString, String indexTypeValue) throws L10NMessageImpl, AuthLoginException, JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, indexTypeString, indexTypeValue);

        //Then
        verify(authContext, never()).login();
        verify(authContext).login(indexType, indexTypeValue);

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("tokenId"), "SSO_TOKEN_ID");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs() throws AuthLoginException,
            L10NMessageImpl, JSONException {

        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.MODULE_INSTANCE, "module",
                "MODULE_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.COMPOSITE_ADVICE, "composite",
                "COMPOSITE_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.LEVEL, "level", "LEVEL_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.RESOURCE, "resource",
                "RESOURCE_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.ROLE, "role", "ROLE_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.SERVICE, "service",
                "SERVICE_NAME");
        testAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs(AuthContext.IndexType.USER, "user", "USER_NAME");
    }

    @Test
    public void shouldAuthenticateSuccessfullyRequirementsInternally() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        Callback[] callbacks = new Callback[0];
        JSONArray jsonCallbacks = mock(JSONArray.class);

        given(authContext.hasMoreRequirements()).willReturn(true).willReturn(false);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, callbacks)).willReturn(jsonCallbacks);
        given(jsonCallbacks.length()).willReturn(0);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("/", authContext.getOrganizationName());
        verify(authContextStateMap, never()).addAuthContext(anyString(), Matchers.<AuthContext>anyObject());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("tokenId"), "SSO_TOKEN_ID");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldAuthenticateSuccessfullyRequirementsExternally() throws AuthLoginException, L10NMessageImpl,
            JSONException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        Callback[] callbacks = new Callback[0];
        JSONArray jsonCallbacks = mock(JSONArray.class);
        PrivateKey privateKey = mock(PrivateKey.class);
        JWTBuilder jwtBuilder = mock(JWTBuilder.class);

        given(authContext.hasMoreRequirements()).willReturn(true);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, callbacks)).willReturn(jsonCallbacks);
        given(jsonCallbacks.length()).willReturn(2);
        given(jsonCallbacks.toString()).willReturn("[CALLBACK1,CALLBACK2]");
        given(amKeyProvider.getPrivateKey(anyString())).willReturn(privateKey);
        given(jwtBuilderFactory.getJWTBuilder()).willReturn(jwtBuilder);
        given(jwtBuilder.setAlgorithm(anyString())).willReturn(jwtBuilder);
        given(jwtBuilder.addValuePair(eq("otk"), anyString())).willReturn(jwtBuilder);
        given(jwtBuilder.sign(privateKey)).willReturn(jwtBuilder);
        given(jwtBuilder.build()).willReturn("JWT_STRING");
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");
        given(systemPropertiesManagerWrapper.get("org.forgerock.keystore.alias")).willReturn("KEYSTORE_ALIAS");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("/", authContext.getOrganizationName());
        verify(authContextStateMap).addAuthContext("JWT_STRING", authContext);

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();
        JSONArray responseCallbacks = responseJson.getJSONArray("callbacks");

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("authId"), "JWT_STRING");
        assertEquals(responseCallbacks.toString(), "[\"CALLBACK1\",\"CALLBACK2\"]");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldFailAuthenticationWhenAuthStatusIsNotSuccessful() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.FAILED);
        given(authContext.getErrorCode()).willReturn(AMAuthErrorCode.AUTH_ERROR);
        given(authContext.getErrorMessage()).willReturn("ERROR_MESSAGE");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("/", authContext.getOrganizationName());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 401);
        assertEquals(responseJson.get("errorMessage"), "ERROR_MESSAGE");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldFailAuthenticationWhenL10MessageImplThrown() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        L10NMessageImpl l10NMessageException = mock(L10NMessageImpl.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(l10NMessageException.getLocalizedMessage()).willReturn("L10NMessageException Message");
        when(authContext.getSSOToken()).thenThrow(l10NMessageException);

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 401);
        assertEquals(responseJson.get("errorMessage"), "L10NMessageException Message");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldFailAuthenticationWhenAuthLoginExceptionThrown() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthLoginException authLoginException = mock(AuthLoginException.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authLoginException.getLocalizedMessage()).willReturn("AuthLoginException Message");
        doThrow(authLoginException).when(authContext).login();

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, null, null, null);

        //Then
        verify(authContext).login();
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 401);
        assertEquals(responseJson.get("errorMessage"), "AuthLoginException Message");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void shouldProcessAuthenticationRequirementsWithSuccessfulAuthentication() throws AuthLoginException,
            L10NMessageImpl, JSONException, SignatureException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        String messageBody = "{authId : \"AUTH_ID\", callbacks : [{type : \"CALLBACK1\"," +
                "output : [{name : \"prompt\",value : \"Enter Callback1:\"}]," +
                "input : [{key : \"cbk1\",value : \"\"}]}," +
                "{type : \"PasswordCallback\"," +
                "output : [{name : \"prompt\",value : \"Enter Callback2:\"}]," +
                "input : [{key : \"cbk2\",value : \"\"}]}]}";
        X509Certificate certificate = mock(X509Certificate.class);
        JWTBuilder jwtBuilder = mock(JWTBuilder.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        Callback[] responseCallbacks = new Callback[0];
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(amKeyProvider.getX509Certificate(anyString())).willReturn(certificate);
        given(jwtBuilderFactory.getJWTBuilder()).willReturn(jwtBuilder);
        given(authContextStateMap.getAuthContext("AUTH_ID")).willReturn(authContext);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(restAuthCallbackHandlerManager.handleJsonCallbacks(
                eq(callbacks), Matchers.<JSONArray>anyObject())).willReturn(responseCallbacks);
        given(authContext.hasMoreRequirements()).willReturn(false);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus())
                .willReturn(AuthContext.Status.IN_PROGRESS)
                .willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.processAuthenticationRequirements(headers, request, messageBody);

        //Then
        verify(jwtBuilder).verify("AUTH_ID", certificate);
        verify(authContext).submitRequirements(responseCallbacks);

        String entity = (String) response.getEntity();
        JSONObject responseJson = new JSONObject(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("tokenId"), "SSO_TOKEN_ID");
        assertTrue(metadata.containsKey("Content-Type"));
        assertEquals(metadata.get("Content-Type").size(), 1);
        assertEquals(metadata.get("Content-Type").get(0), MediaType.APPLICATION_JSON_TYPE);
    }
}
