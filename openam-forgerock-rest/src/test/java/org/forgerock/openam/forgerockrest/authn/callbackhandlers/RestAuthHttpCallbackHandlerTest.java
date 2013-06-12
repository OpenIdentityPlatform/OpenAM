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

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.HttpCallback;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(httpCallback.getAuthorizationHeader()).willReturn("AUTHORIZATION");
        given(request.getHeader("AUTHORIZATION")).willReturn("HTTP_AUTHZ");

        //When
        boolean updated = restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, httpCallback, HttpMethod.POST);

        //Then
        verify(httpCallback).setAuthorization("HTTP_AUTHZ");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn(null);

        //When
        boolean exceptionCaught = false;
        RestAuthCallbackHandlerResponseException exception = null;
        try {
            restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                    jsonPostBody, httpCallback, HttpMethod.POST);
        } catch (RestAuthCallbackHandlerResponseException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getResponseStatus(), Response.Status.UNAUTHORIZED);
        assertEquals(exception.getResponseHeaders().size(), 1);
        assertTrue(exception.getResponseHeaders().containsKey("WWW-Authenticate"));
        assertTrue(exception.getResponseHeaders().containsValue("Negotiate"));
        assertEquals(exception.getJsonResponse().get("failure").asBoolean(), (Boolean)true);
        assertEquals(exception.getJsonResponse().get("reason").asString(), "iwa-failed");
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn("");

        //When
        boolean exceptionCaught = false;
        RestAuthCallbackHandlerResponseException exception = null;
        try {
            restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                    jsonPostBody, httpCallback, HttpMethod.POST);
        } catch (RestAuthCallbackHandlerResponseException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getResponseStatus(), Response.Status.UNAUTHORIZED);
        assertEquals(exception.getResponseHeaders().size(), 1);
        assertTrue(exception.getResponseHeaders().containsKey("WWW-Authenticate"));
        assertTrue(exception.getResponseHeaders().containsValue("Negotiate"));
        assertEquals(exception.getJsonResponse().get("failure").asBoolean(), (Boolean)true);
        assertEquals(exception.getJsonResponse().get("reason").asString(), "iwa-failed");
    }

    @Test
    public void shouldHandleCallbackAndSetIWAFailedIfReasonInPostBody() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback originalHttpCallback = mock(HttpCallback.class);
        Map<String, String> postBodyMap = new LinkedHashMap<String, String>();
        postBodyMap.put("reason", "iwa-failed");
        JsonValue jsonPostBody = new JsonValue(postBodyMap);

        //When
        HttpCallback httpCallback = restAuthHttpCallbackHandler.handle(headers, request, response, jsonPostBody,
                originalHttpCallback);

        //Then
        assertEquals(originalHttpCallback, httpCallback);
        verify(request).setAttribute("iwa-failed", true);
    }

    @Test
    public void shouldHandleCallbackWhereIWASuccessful() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = new JsonValue(new HashMap<String, String>());
        HttpCallback originalHttpCallback = mock(HttpCallback.class);

        //When
        HttpCallback httpCallback = restAuthHttpCallbackHandler.handle(headers, request, response, jsonPostBody,
                originalHttpCallback);

        //Then
        assertEquals(originalHttpCallback, httpCallback);
        verify(request, never()).setAttribute("iwa-failed", true);
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailConvertToJson() {

        //Given

        //When
        restAuthHttpCallbackHandler.convertToJson(null, 1);

        //Then
        fail();
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJson() {

        //Given

        //When
        restAuthHttpCallbackHandler.convertFromJson(null, null);

        //Then
        fail();
    }
}
