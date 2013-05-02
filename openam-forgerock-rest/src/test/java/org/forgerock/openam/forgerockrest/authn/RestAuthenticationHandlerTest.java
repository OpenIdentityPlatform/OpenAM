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
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.shared.locale.L10NMessageImpl;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandlerResponseException;
import org.forgerock.openam.forgerockrest.jwt.JwsAlgorithm;
import org.forgerock.openam.forgerockrest.jwt.JwtBuilder;
import org.forgerock.openam.forgerockrest.jwt.PlaintextJwt;
import org.forgerock.openam.forgerockrest.jwt.SignedJwt;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestAuthenticationHandlerTest {

    private RestAuthenticationHandler restAuthenticationHandler;

    private AuthContext authContext;
    private AuthContextStateMap authContextStateMap;
    private AMKeyProvider amKeyProvider;
    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private JwtBuilder jwtBuilder;
    private ServiceConfigManager serviceConfigManager;
    private SSOToken ssoToken;
    private AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;

    @BeforeMethod
    public void setUp() {

        authContext = mock(AuthContext.class);
        authContextStateMap = mock(AuthContextStateMap.class);
        amKeyProvider = mock(AMKeyProvider.class);
        restAuthCallbackHandlerManager = mock(RestAuthCallbackHandlerManager.class);
        jwtBuilder = mock(JwtBuilder.class);
        serviceConfigManager = mock(ServiceConfigManager.class);
        ssoToken = mock(SSOToken.class);
        amAuthErrorCodeResponseStatusMapping = mock(AMAuthErrorCodeResponseStatusMapping.class);


        restAuthenticationHandler = new RestAuthenticationHandler(authContextStateMap, amKeyProvider,
                restAuthCallbackHandlerManager, jwtBuilder, amAuthErrorCodeResponseStatusMapping) {
            @Override
            protected AuthContext createAuthContext(String realm) throws AuthLoginException {
                given(authContext.getOrganizationName()).willReturn(realm);
                return authContext;
            }

            @Override
            protected ServiceConfigManager getServiceConfigManager(String serviceName, SSOToken token) {
                ServiceConfig serviceConfig = mock(ServiceConfig.class);
                Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
                Set<String> values = new HashSet<String>();
                values.add("test");
                attributes.put("iplanet-am-auth-jwt-signing-key-alias", values);
                try {
                    given(serviceConfigManager.getOrganizationConfig(anyString(), anyString())).willReturn(serviceConfig);
                    given(serviceConfig.getAttributes()).willReturn(attributes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return serviceConfigManager;
            }

            @Override
            protected SSOToken getAdminToken() {
                return ssoToken;
            }
        };
    }

    @Test
    public void shouldAuthenticateSuccessfullyWithNoIndexTypeRealmOrReqs() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, "REALM", null,
                null, HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null,
                indexTypeString, indexTypeValue, HttpMethod.GET);

        //Then
        verify(authContext, never()).login();
        verify(authContext).login(indexType, indexTypeValue, null, request, httpResponse);

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
            JSONException, RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        Callback[] callbacks = new Callback[0];
        Callback[] allCallbacks = new Callback[1];
        PagePropertiesCallback pagePropertiesCallback = mock(PagePropertiesCallback.class);
        allCallbacks[0] = pagePropertiesCallback;
        JsonValue jsonCallbacks = mock(JsonValue.class);

        given(authContext.hasMoreRequirements()).willReturn(true).willReturn(false);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(authContext.getRequirements(true)).willReturn(allCallbacks);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, httpResponse, null,
                callbacks, HttpMethod.GET)).willReturn(jsonCallbacks);
        given(jsonCallbacks.size()).willReturn(0);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
            JSONException, SignatureException, RestAuthCallbackHandlerResponseException, IOException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);
        Callback[] callbacks = new Callback[0];
        Callback[] allCallbacks = new Callback[1];
        PagePropertiesCallback pagePropertiesCallback = mock(PagePropertiesCallback.class);
        allCallbacks[0] = pagePropertiesCallback;
        List<String> jsonCallbacksList = new ArrayList<String>();
        jsonCallbacksList.add("CALLBACK1");
        jsonCallbacksList.add("CALLBACK2");
        JsonValue jsonCallbacks = new JsonValue(jsonCallbacksList);
        PrivateKey privateKey = mock(PrivateKey.class);
        PlaintextJwt plaintextJwt = mock(PlaintextJwt.class);
        SignedJwt signedJwt = mock(SignedJwt.class);

        given(authContext.hasMoreRequirements()).willReturn(true);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(authContext.getRequirements(true)).willReturn(allCallbacks);
        given(restAuthCallbackHandlerManager.handleCallbacks(headers, request, httpResponse, null,
                callbacks, HttpMethod.GET)).willReturn(jsonCallbacks);
//        given(jsonCallbacks.size()).willReturn(2);
//        given(jsonCallbacks.toString()).willReturn("[CALLBACK1,CALLBACK2]");
        given(amKeyProvider.getPrivateKey(anyString())).willReturn(privateKey);
        given(jwtBuilder.jwt()).willReturn(plaintextJwt);
        given(plaintextJwt.header(anyString(), anyString())).willReturn(plaintextJwt);
        given(plaintextJwt.content(eq("otk"), anyString())).willReturn(plaintextJwt);
        given(plaintextJwt.content(Matchers.<Map<String, Object>>anyObject())).willReturn(plaintextJwt);
        given(plaintextJwt.sign(JwsAlgorithm.HS256, privateKey)).willReturn(signedJwt);
        given(signedJwt.build()).willReturn("JWT_STRING");
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
        verify(authContext, never()).login(Matchers.<AuthContext.IndexType>anyObject(), anyString());
        assertEquals("/", authContext.getOrganizationName());
        verify(authContextStateMap).addAuthContext("JWT_STRING", authContext);

        String entity = (String) response.getEntity();
        JsonValue responseJson = JsonValueBuilder.toJsonValue(entity);
        MultivaluedMap<String,Object> metadata = response.getMetadata();
        JsonValue responseCallbacks = responseJson.get("callbacks");

        assertEquals(response.getStatus(), 200);
        assertEquals(responseJson.get("authId").asString(), "JWT_STRING");
        assertEquals(responseCallbacks.get(0).asString(), "CALLBACK1");
        assertEquals(responseCallbacks.get(1).asString(), "CALLBACK2");
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.FAILED);
        given(authContext.getErrorCode()).willReturn(AMAuthErrorCode.AUTH_ERROR);
        given(authContext.getErrorMessage()).willReturn("ERROR_MESSAGE");

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        L10NMessageImpl l10NMessageException = mock(L10NMessageImpl.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);
        given(l10NMessageException.getLocalizedMessage()).willReturn("L10NMessageException Message");
        when(authContext.getSSOToken()).thenThrow(l10NMessageException);

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        AuthLoginException authLoginException = mock(AuthLoginException.class);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus()).willReturn(AuthContext.Status.FAILED);
        given(authContext.getErrorCode()).willReturn(AMAuthErrorCode.AUTH_INVALID_PASSWORD);
        given(authContext.getErrorMessage()).willReturn("AuthLoginException Message");
        given(authLoginException.getLocalizedMessage()).willReturn("AuthLoginException Message");
        doThrow(authLoginException).when(authContext).login();

        //When
        Response response = restAuthenticationHandler.authenticate(headers, request, httpResponse, null, null, null,
                HttpMethod.GET);

        //Then
        verify(authContext).login(request, httpResponse);
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
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        JsonValue jv = JsonValueBuilder.jsonValue()
                .put("authId", "AUTH_ID")
                .array("callbacks")
                    .add(JsonValueBuilder.jsonValue()
                            .put("type", "CALLBACK1")
                            .array("output")
                                .addLast(JsonValueBuilder.jsonValue()
                                        .put("name", "prompt")
                                        .put("value", "Enter Callback1:")
                                        .build())
                            .array("input")
                                .addLast(JsonValueBuilder.jsonValue()
                                        .put("key", "cbk1")
                                        .put("value", "")
                                        .build())
                            .build())
                    .addLast(JsonValueBuilder.jsonValue()
                            .put("type", "PasswordCallback")
                            .array("output")
                                .addLast(JsonValueBuilder.jsonValue()
                                        .put("name", "prompt")
                                        .put("value", "Enter Callback2:")
                                        .build())
                            .array("input")
                                .addLast(JsonValueBuilder.jsonValue()
                                        .put("key", "cbk2")
                                        .put("value", "")
                                        .build())
                            .build())
                .build();

          String messageBody = jv.toString();

//        String messageBody = "{authId : \"AUTH_ID\", callbacks : [{type : \"CALLBACK1\"," +
//                "output : [{name : \"prompt\",value : \"Enter Callback1:\"}]," +
//                "input : [{key : \"cbk1\",value : \"\"}]}," +
//                "{type : \"PasswordCallback\"," +
//                "output : [{name : \"prompt\",value : \"Enter Callback2:\"}]," +
//                "input : [{key : \"cbk2\",value : \"\"}]}]}";
        PrivateKey privateKey = mock(PrivateKey.class);
        X509Certificate certificate = mock(X509Certificate.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        Callback[] responseCallbacks = new Callback[0];
        SSOToken ssoToken = mock(SSOToken.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(amKeyProvider.getPrivateKey(anyString())).willReturn(privateKey);
        given(amKeyProvider.getX509Certificate(anyString())).willReturn(certificate);
        given(jwtBuilder.recontructJwt("AUTH_ID")).willReturn(signedJwt);
        given(authContextStateMap.getAuthContext("AUTH_ID")).willReturn(authContext);
        given(signedJwt.verify(privateKey, certificate)).willReturn(true);
        given(authContext.getRequirements()).willReturn(callbacks);
        given(restAuthCallbackHandlerManager.handleJsonCallbacks(
                eq(callbacks), Matchers.<JsonValue>anyObject())).willReturn(responseCallbacks);
        given(authContext.hasMoreRequirements()).willReturn(false);

        given(authContext.hasMoreRequirements()).willReturn(false);
        given(authContext.getStatus())
                .willReturn(AuthContext.Status.IN_PROGRESS)
                .willReturn(AuthContext.Status.SUCCESS);
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(ssoTokenID.toString()).willReturn("SSO_TOKEN_ID");

        //When
        Response response = restAuthenticationHandler.processAuthenticationRequirements(headers, request, httpResponse,
                messageBody, HttpMethod.GET);

        //Then
        verify(signedJwt).verify(privateKey, certificate);
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
