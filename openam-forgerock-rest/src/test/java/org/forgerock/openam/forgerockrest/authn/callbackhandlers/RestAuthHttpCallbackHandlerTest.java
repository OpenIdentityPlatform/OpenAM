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
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
    public void shouldUpdateCallbackFromRequest() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(httpCallback.getAuthorizationHeader()).willReturn("AUTHORIZATION");
        given(request.getHeader("AUTHORIZATION")).willReturn("HTTP_AUTHZ");

        //When
        boolean updated = restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                httpCallback);

        //Then
        verify(httpCallback).setAuthorization("HTTP_AUTHZ");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsNull() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn(null);

        //When
        boolean updated = restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                httpCallback);

        //Then
        verify(httpCallback, never()).setAuthorization(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenHttpAuthorizationIsEmptyString() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = mock(HttpCallback.class);

        given(request.getParameter("httpAuthorization")).willReturn("");

        //When
        boolean updated = restAuthHttpCallbackHandler.updateCallbackFromRequest(headers, request, response,
                httpCallback);

        //Then
        verify(httpCallback, never()).setAuthorization(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        HttpCallback httpCallback = new HttpCallback("AUTHZ_HEADER", "NEGO_NAME", "NEGO_VALUE", 0);

        //When
        JSONObject jsonObject = restAuthHttpCallbackHandler.convertToJson(httpCallback);

        //Then
        assertEquals("HttpCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(4, jsonObject.getJSONArray("output").length());
        assertEquals("authorizationHeader", jsonObject.getJSONArray("output").getJSONObject(0).getString("name"));
        assertEquals("AUTHZ_HEADER", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals("negotiationCode", jsonObject.getJSONArray("output").getJSONObject(1).getString("name"));
        assertEquals(0, jsonObject.getJSONArray("output").getJSONObject(1).getInt("value"));
        assertEquals("negotiationHeaderName", jsonObject.getJSONArray("output").getJSONObject(2).getString("name"));
        assertEquals("NEGO_NAME", jsonObject.getJSONArray("output").getJSONObject(2).getString("value"));
        assertEquals("negotiationHeaderValue", jsonObject.getJSONArray("output").getJSONObject(3).getString("name"));
        assertEquals("NEGO_VALUE", jsonObject.getJSONArray("output").getJSONObject(3).getString("value"));
        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(1, jsonObject.getJSONArray("input").length());
        assertEquals("authorization", jsonObject.getJSONArray("input").getJSONObject(0).getString("name"));
        assertEquals("", jsonObject.getJSONArray("input").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        HttpCallback httpCallback = new HttpCallback("AUTHZ_HEADER", "NEGO_NAME", "NEGO_VALUE", 0);
        JSONObject jsonHttpCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorization")
                                .put("value", "AUTHZ")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorizationHeader")
                                .put("value", "AUTHZ_HEADER"))
                        .put(new JSONObject()
                                .put("name", "negotiationCode")
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderName")
                                .put("value", "NEGO_NAME"))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderValue")
                                .put("value", "NEGO_VALUE")))
                .put("type", "HttpCallback");

        //When
        HttpCallback convertedHttpCallback = restAuthHttpCallbackHandler.convertFromJson(httpCallback,
                jsonHttpCallback);

        //Then
        assertEquals(httpCallback, convertedHttpCallback);
        assertEquals("AUTHZ_HEADER", convertedHttpCallback.getAuthorizationHeader());
        assertEquals(0, convertedHttpCallback.getNegotiationCode());
        assertEquals("NEGO_NAME", convertedHttpCallback.getNegotiationHeaderName());
        assertEquals("NEGO_VALUE", convertedHttpCallback.getNegotiationHeaderValue());
        assertEquals("AUTHZ", convertedHttpCallback.getAuthorization());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        HttpCallback httpCallback = new HttpCallback("AUTHZ_HEADER", "NEGO_NAME", "NEGO_VALUE", 0);
        JSONObject jsonHttpCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorization")
                                .put("value", "AUTHZ")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorizationHeader")
                                .put("value", "AUTHZ_HEADER"))
                        .put(new JSONObject()
                                .put("name", "negotiationCode")
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderName")
                                .put("value", "NEGO_NAME"))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderValue")
                                .put("value", "NEGO_VALUE")))
                .put("type", "PasswordCallback");

        //When
        restAuthHttpCallbackHandler.convertFromJson(httpCallback, jsonHttpCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        HttpCallback httpCallback = new HttpCallback("AUTHZ_HEADER", "NEGO_NAME", "NEGO_VALUE", 0);
        JSONObject jsonHttpCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorization")
                                .put("value", "AUTHZ")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "authorizationHeader")
                                .put("value", "AUTHZ_HEADER"))
                        .put(new JSONObject()
                                .put("name", "negotiationCode")
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderName")
                                .put("value", "NEGO_NAME"))
                        .put(new JSONObject()
                                .put("name", "negotiationHeaderValue")
                                .put("value", "NEGO_VALUE")))
                .put("type", "htTpcaLlback");

        //When
        HttpCallback convertedHttpCallback = restAuthHttpCallbackHandler.convertFromJson(httpCallback,
                jsonHttpCallback);

        //Then
        assertEquals(httpCallback, convertedHttpCallback);
        assertEquals("AUTHZ_HEADER", convertedHttpCallback.getAuthorizationHeader());
        assertEquals(0, convertedHttpCallback.getNegotiationCode());
        assertEquals("NEGO_NAME", convertedHttpCallback.getNegotiationHeaderName());
        assertEquals("NEGO_VALUE", convertedHttpCallback.getNegotiationHeaderValue());
        assertEquals("AUTHZ", convertedHttpCallback.getAuthorization());
    }
}
