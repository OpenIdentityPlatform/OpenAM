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

import junit.framework.Assert;
import org.forgerock.openam.forgerockrest.authn.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RestAuthPasswordCallbackHandlerTest {

    private RestAuthCallbackHandler<PasswordCallback> restAuthPasswordCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthPasswordCallbackHandler = new RestAuthPasswordCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthPasswordCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(PasswordCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getParameter("password")).willReturn("PASSWORD");

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, passwordCallback, HttpMethod.POST);

        //Then
        verify(passwordCallback).setPassword("PASSWORD".toCharArray());
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getParameter("password")).willReturn(null);

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, passwordCallback, HttpMethod.POST);

        //Then
        verify(passwordCallback, never()).setPassword(Matchers.<char[]>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getParameter("password")).willReturn("");

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, passwordCallback, HttpMethod.POST);

        //Then
        verify(passwordCallback, never()).setPassword(Matchers.<char[]>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        PasswordCallback originalPasswordCallback = mock(PasswordCallback.class);

        //When
        PasswordCallback passwordCallback = restAuthPasswordCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalPasswordCallback);

        //Then
        assertEquals(originalPasswordCallback, passwordCallback);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);

        //When
        JSONObject jsonObject = restAuthPasswordCallbackHandler.convertToJson(passwordCallback, 1);

        //Then
        assertEquals("PasswordCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        Assert.assertEquals(1, jsonObject.getJSONArray("output").length());
        Assert.assertEquals("Enter password:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertNotNull(jsonObject.getJSONArray("input"));
        Assert.assertEquals(1, jsonObject.getJSONArray("input").length());
        Assert.assertEquals("", jsonObject.getJSONArray("input").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JSONObject jsonPasswordCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("value", "PASSWORD")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("value", "Enter password:")))
                .put("type", "PasswordCallback");

        //When
        PasswordCallback convertedPasswordCallback = restAuthPasswordCallbackHandler.convertFromJson(passwordCallback,
                jsonPasswordCallback);

        //Then
        Assert.assertEquals(passwordCallback, convertedPasswordCallback);
        Assert.assertEquals("Enter password:", convertedPasswordCallback.getPrompt());
        Assert.assertEquals("PASSWORD", new String(convertedPasswordCallback.getPassword()));
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JSONObject jsonPasswordCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("value", "PASSWORD")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("value", "Enter password:")))
                .put("type", "NameCallback");

        //When
        restAuthPasswordCallbackHandler.convertFromJson(passwordCallback, jsonPasswordCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JSONObject jsonPasswordCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("value", "PASSWORD")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("value", "Enter password:")))
                .put("type", "passwordcallback");

        //When
        PasswordCallback convertedPasswordCallback = restAuthPasswordCallbackHandler.convertFromJson(passwordCallback,
                jsonPasswordCallback);

        //Then
        Assert.assertEquals(passwordCallback, convertedPasswordCallback);
        Assert.assertEquals("Enter password:", convertedPasswordCallback.getPrompt());
        Assert.assertEquals("PASSWORD", new String(convertedPasswordCallback.getPassword()));
    }
}
