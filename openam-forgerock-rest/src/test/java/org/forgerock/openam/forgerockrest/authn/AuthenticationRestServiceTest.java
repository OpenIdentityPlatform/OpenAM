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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AuthenticationRestServiceTest {

    private AuthenticationRestService authenticationRestService;

    private RestAuthenticationHandler restAuthenticationHandler;

    @BeforeClass
    public void setUp() {

        restAuthenticationHandler = mock(RestAuthenticationHandler.class);

        authenticationRestService = new AuthenticationRestService(restAuthenticationHandler);
    }

    @Test
    public void shouldInitiateAuthenticationViaGET() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        String authIndexType = "AUTH_INDEX_TYPE";
        String authIndexValue = "AUTH_INDEX_VALUE";
        Response jaxrsResponse = mock(Response.class);
        String sessionUpgradeSSOTokenId = "SSO_TOKEN_ID";

        given(restAuthenticationHandler.initiateAuthentication(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, HttpMethod.GET)).willReturn(jaxrsResponse);

        //When
        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId);

        //Then
        assertEquals(jaxrsResponse, resp);
        verify(restAuthenticationHandler).initiateAuthentication(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, HttpMethod.GET);
    }

    @Test
    public void shouldInitiateAuthenticationViaPOST() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        String authIndexType = "AUTH_INDEX_TYPE";
        String authIndexValue = "AUTH_INDEX_VALUE";
        Response jaxrsResponse = mock(Response.class);
        String sessionUpgradeSSOTokenId = "SSO_TOKEN_ID";
        List<String> contentTypeHeader = new ArrayList<String>();
        contentTypeHeader.add(MediaType.APPLICATION_JSON);

        given(headers.getRequestHeader(HttpHeaders.CONTENT_TYPE)).willReturn(contentTypeHeader);

        given(restAuthenticationHandler.initiateAuthentication(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, HttpMethod.POST)).willReturn(jaxrsResponse);

        //When
        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, null);

        //Then
        assertEquals(jaxrsResponse, resp);
        verify(restAuthenticationHandler).initiateAuthentication(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, HttpMethod.POST);
    }

    @Test
    public void shouldContinueAuthenticationViaPOST() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        String authIndexType = "AUTH_INDEX_TYPE";
        String authIndexValue = "AUTH_INDEX_VALUE";
        String postBody = "POST_BODY";
        Response jaxrsResponse = mock(Response.class);
        String sessionUpgradeSSOTokenId = "SSO_TOKEN_ID";
        List<String> contentTypeHeader = new ArrayList<String>();
        contentTypeHeader.add(MediaType.APPLICATION_JSON);

        given(headers.getRequestHeader(HttpHeaders.CONTENT_TYPE)).willReturn(contentTypeHeader);

        given(restAuthenticationHandler.continueAuthentication(headers, request, response, postBody,
                sessionUpgradeSSOTokenId)).willReturn(jaxrsResponse);

        //When
        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType,
                authIndexValue, sessionUpgradeSSOTokenId, postBody);

        //Then
        assertEquals(jaxrsResponse, resp);
        verify(restAuthenticationHandler).continueAuthentication(headers, request, response, postBody,
                sessionUpgradeSSOTokenId);
    }

    @Test
    public void shouldFailToAuthenticateViaPOSTWhenContentTypeHeaderNotSet() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);

        //When
        Response resp = authenticationRestService.authenticate(headers, null, null, null, null, null, null);

        //Then
        assertEquals(resp.getStatus(), 415);
        JsonValue jsonValue = JsonValueBuilder.toJsonValue((String) resp.getEntity());
        assertTrue(jsonValue.isDefined("errorMessage"));
    }
}
