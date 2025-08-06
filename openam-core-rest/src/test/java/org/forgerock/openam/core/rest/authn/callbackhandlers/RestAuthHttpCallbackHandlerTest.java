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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.HttpCallback;

public class RestAuthHttpCallbackHandlerTest {

    private RestAuthCallbackHandler<HttpCallback> restAuthHttpCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthHttpCallbackHandler = new RestAuthHttpCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthHttpCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(HttpCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(httpCallback.getAuthorizationHeader()).willReturn("AUTHORIZATION");
        given(request.getHeader("AUTHORIZATION")).willReturn("HTTP_AUTHZ");

        //When
        boolean updated = restAuthHttpCallbackHandler.updateCallbackFromRequest(request, response, httpCallback);

        //Then
        verify(httpCallback).setAuthorization("HTTP_AUTHZ");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsNull()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn(null);
        given(httpCallback.getNegotiationHeaderName()).willReturn("WWW-Authenticate");
        given(httpCallback.getNegotiationHeaderValue()).willReturn("Negotiate");

        //When
        boolean exceptionCaught = false;
        RestAuthResponseException exception = null;
        try {
            restAuthHttpCallbackHandler.updateCallbackFromRequest(request, response, httpCallback);
        } catch (RestAuthResponseException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getStatusCode(), 401);
        assertEquals(exception.getResponseHeaders().size(), 1);
        assertTrue(exception.getResponseHeaders().containsKey("WWW-Authenticate"));
        assertTrue(exception.getResponseHeaders().containsValue("Negotiate"));
        assertEquals(exception.getJsonResponse().get("failure").asBoolean(), (Boolean) true);
        assertEquals(exception.getJsonResponse().get("reason").asString(), "http-auth-failed");
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsEmptyString()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn("");
        given(httpCallback.getNegotiationHeaderName()).willReturn("WWW-Authenticate");
        given(httpCallback.getNegotiationHeaderValue()).willReturn("Negotiate");

        //When
        boolean exceptionCaught = false;
        RestAuthResponseException exception = null;
        try {
            restAuthHttpCallbackHandler.updateCallbackFromRequest(request, response, httpCallback);
        } catch (RestAuthResponseException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getStatusCode(), 401);
        assertEquals(exception.getResponseHeaders().size(), 1);
        assertTrue(exception.getResponseHeaders().containsKey("WWW-Authenticate"));
        assertTrue(exception.getResponseHeaders().containsValue("Negotiate"));
        assertEquals(exception.getJsonResponse().get("failure").asBoolean(), (Boolean) true);
        assertEquals(exception.getJsonResponse().get("reason").asString(), "http-auth-failed");
    }

    @Test
    public void shouldHandleCallbackAndSetHttpAuthFailedIfReasonInPostBody() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback originalHttpCallback = mock(HttpCallback.class);
        Map<String, String> postBodyMap = new LinkedHashMap<String, String>();
        postBodyMap.put("reason", "http-auth-failed");
        JsonValue jsonPostBody = new JsonValue(postBodyMap);

        //When
        HttpCallback httpCallback = restAuthHttpCallbackHandler.handle(request, response, jsonPostBody,
                originalHttpCallback);

        //Then
        Assert.assertEquals(originalHttpCallback, httpCallback);
        verify(request).setAttribute("http-auth-failed", "true");
    }

    @Test
    public void shouldHandleCallbackWhereIWASuccessful() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = new JsonValue(new HashMap<String, String>());
        HttpCallback originalHttpCallback = mock(HttpCallback.class);

        //When
        HttpCallback httpCallback = restAuthHttpCallbackHandler.handle(request, response, jsonPostBody,
                originalHttpCallback);

        //Then
        Assert.assertEquals(originalHttpCallback, httpCallback);
        verify(request, never()).setAttribute("http-auth-failed", "true");
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailConvertToJson() throws RestAuthException {

        //Given

        //When
        restAuthHttpCallbackHandler.convertToJson(null, 1);
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJson() throws RestAuthException {

        //Given

        //When
        restAuthHttpCallbackHandler.convertFromJson(null, null);
    }
}
