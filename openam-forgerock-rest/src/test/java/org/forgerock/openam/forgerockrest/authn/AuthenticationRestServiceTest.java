package org.forgerock.openam.forgerockrest.authn;///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2013 ForgeRock Inc.
// */
//
//package org.forgerock.openam.forgerockrest.authn;
//
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.ws.rs.core.HttpHeaders;
//import javax.ws.rs.core.Response;
//
//import static org.mockito.Mockito.*;
//import static org.mockito.BDDMockito.*;
//import static org.testng.Assert.assertEquals;
//
//public class AuthenticationRestServiceTest {
//
//    private AuthenticationRestService authenticationRestService;
//
//    private RestAuthenticationHandler restAuthenticationHandler;
//
//    @BeforeClass
//    public void setUp() {
//
//        restAuthenticationHandler = mock(RestAuthenticationHandler.class);
//
//        authenticationRestService = new AuthenticationRestService(restAuthenticationHandler) {
//            @Override
//            String getRealm(HttpServletRequest request) {
//                return "REALM";
//            }
//        };
//    }
//
//    @Test
//    public void shouldInitiateAuthenticationViaGET() {
//
//        //Given
//        HttpHeaders headers = mock(HttpHeaders.class);
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        String authIndexType = "AUTH_INDEX_TYPE";
//        String authIndexValue = "AUTH_INDEX_VALUE";
//        Response jaxrsResponse = mock(Response.class);
//
//        given(restAuthenticationHandler.authenticate(headers, request, response, "REALM", authIndexType,
//                authIndexValue, HttpMethod.GET)).willReturn(jaxrsResponse);
//
//        //When
//        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType, authIndexValue);
//
//        //Then
//        assertEquals(jaxrsResponse, resp);
//        verify(restAuthenticationHandler).authenticate(headers, request, response, "REALM", authIndexType,
//                authIndexValue, HttpMethod.GET);
//    }
//
//    @Test
//    public void shouldInitiateAuthenticationViaPOST() {
//
//        //Given
//        HttpHeaders headers = mock(HttpHeaders.class);
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        String authIndexType = "AUTH_INDEX_TYPE";
//        String authIndexValue = "AUTH_INDEX_VALUE";
//        Response jaxrsResponse = mock(Response.class);
//
//        given(restAuthenticationHandler.authenticate(headers, request, response, "REALM", authIndexType,
//                authIndexValue, HttpMethod.POST)).willReturn(jaxrsResponse);
//
//        //When
//        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType, authIndexValue,
//                null);
//
//        //Then
//        assertEquals(jaxrsResponse, resp);
//        verify(restAuthenticationHandler).authenticate(headers, request, response, "REALM", authIndexType,
//                authIndexValue, HttpMethod.POST);
//    }
//
//    @Test
//    public void shouldContinueAuthenticationViaPOST() {
//
//        //Given
//        HttpHeaders headers = mock(HttpHeaders.class);
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        String authIndexType = "AUTH_INDEX_TYPE";
//        String authIndexValue = "AUTH_INDEX_VALUE";
//        String postBody = "POST_BODY";
//        Response jaxrsResponse = mock(Response.class);
//
//        given(restAuthenticationHandler.processAuthenticationRequirements(headers, request, response, postBody,
//                HttpMethod.POST)).willReturn(jaxrsResponse);
//
//        //When
//        Response resp = authenticationRestService.authenticate(headers, request, response, authIndexType, authIndexValue,
//                postBody);
//
//        //Then
//        assertEquals(jaxrsResponse, resp);
//        verify(restAuthenticationHandler).processAuthenticationRequirements(headers, request, response, postBody,
//                HttpMethod.POST);
//    }
//}
