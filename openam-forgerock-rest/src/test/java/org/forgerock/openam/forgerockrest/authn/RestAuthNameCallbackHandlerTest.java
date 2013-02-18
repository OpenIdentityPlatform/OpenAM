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

import junit.framework.Assert;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RestAuthNameCallbackHandlerTest {

    private RestAuthCallbackHandler<NameCallback> restAuthNameCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthNameCallbackHandler = new RestAuthNameCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthNameCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(NameCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn("USERNAME");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request,
                nameCallback);

        //Then
        verify(nameCallback).setName("USERNAME");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenUsernameIsNull() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn(null);

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request,
                nameCallback);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn("");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request,
                nameCallback);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");

        //When
        JSONObject jsonObject = restAuthNameCallbackHandler.convertToJson(nameCallback);

        //Then
        assertEquals("NameCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        Assert.assertEquals(1, jsonObject.getJSONArray("output").length());
        Assert.assertEquals("prompt", jsonObject.getJSONArray("output").getJSONObject(0).getString("name"));
        Assert.assertEquals("Enter username:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertNotNull(jsonObject.getJSONArray("input"));
        Assert.assertEquals(1, jsonObject.getJSONArray("input").length());
        Assert.assertEquals("name", jsonObject.getJSONArray("input").getJSONObject(0).getString("name"));
        Assert.assertEquals("", jsonObject.getJSONArray("input").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("name", "name")
                                .put("value", "USERNAME")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter username:")))
                .put("type", "NameCallback");

        //When
        NameCallback convertedNameCallback = restAuthNameCallbackHandler.convertFromJson(nameCallback,
                jsonNameCallback);

        //Then
        Assert.assertEquals(nameCallback, convertedNameCallback);
        Assert.assertEquals("Enter username:", convertedNameCallback.getPrompt());
        Assert.assertEquals("USERNAME", convertedNameCallback.getName());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("name", "name")
                                .put("value", "USERNAME")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter username:")))
                .put("type", "PasswordCallback");

        //When
        restAuthNameCallbackHandler.convertFromJson(nameCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray().put(
                        new JSONObject()
                                .put("name", "name")
                                .put("value", "USERNAME")))
                .put("output", new JSONArray().put(
                        new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter username:")))
                .put("type", "namecallback");

        //When
        NameCallback convertedNameCallback = restAuthNameCallbackHandler.convertFromJson(nameCallback,
                jsonNameCallback);

        //Then
        Assert.assertEquals(nameCallback, convertedNameCallback);
        Assert.assertEquals("Enter username:", convertedNameCallback.getPrompt());
        Assert.assertEquals("USERNAME", convertedNameCallback.getName());
    }
}
