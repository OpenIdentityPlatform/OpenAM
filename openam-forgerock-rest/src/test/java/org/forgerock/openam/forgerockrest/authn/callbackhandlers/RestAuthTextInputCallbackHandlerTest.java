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

import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.TextInputCallback;
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

public class RestAuthTextInputCallbackHandlerTest {

    private RestAuthCallbackHandler<TextInputCallback> restAuthTextInputCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthTextInputCallbackHandler = new RestAuthTextInputCallbackHandler();
    }

    @Test    //TODO
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthTextInputCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(TextInputCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn("TEXT");

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                textInputCallback);

        //Then
        verify(textInputCallback).setText("TEXT");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenUsernameIsNull() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn(null);

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                textInputCallback);

        //Then
        verify(textInputCallback, never()).setText(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn("");

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                textInputCallback);

        //Then
        verify(textInputCallback, never()).setText(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");

        //When
        JSONObject jsonObject = restAuthTextInputCallbackHandler.convertToJson(textInputCallback);

        //Then
        assertEquals("TextInputCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(2, jsonObject.getJSONArray("output").length());
        assertEquals("prompt", jsonObject.getJSONArray("output").getJSONObject(0).getString("name"));
        assertEquals("Enter text:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals("defaultText", jsonObject.getJSONArray("output").getJSONObject(1).getString("name"));
        assertEquals("DEFAULT_VALUE", jsonObject.getJSONArray("output").getJSONObject(1).getString("value"));
        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(1, jsonObject.getJSONArray("input").length());
        assertEquals("text", jsonObject.getJSONArray("input").getJSONObject(0).getString("name"));
        assertEquals("", jsonObject.getJSONArray("input").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JSONObject jsonTextInputCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "text")
                                .put("value", "TEXT_VALUE")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter text:"))
                        .put(new JSONObject()
                                .put("name", "defaultText")
                                .put("value", "DEFAULT_VALUE")))
                .put("type", "TextInputCallback");

        //When
        TextInputCallback convertedTextInputCallback = restAuthTextInputCallbackHandler.convertFromJson(
                textInputCallback, jsonTextInputCallback);

        //Then
        assertEquals(textInputCallback, convertedTextInputCallback);
        assertEquals("Enter text:", convertedTextInputCallback.getPrompt());
        assertEquals("DEFAULT_VALUE", convertedTextInputCallback.getDefaultText());
        assertEquals("TEXT_VALUE", convertedTextInputCallback.getText());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JSONObject jsonTextInputCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "text")
                                .put("value", "TEXT_VALUE")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter text:"))
                        .put(new JSONObject()
                                .put("name", "defaultText")
                                .put("value", "DEFAULT_VALUE")))
                .put("type", "PasswordCallback");

        //When
        restAuthTextInputCallbackHandler.convertFromJson(textInputCallback, jsonTextInputCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JSONObject jsonTextInputCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "text")
                                .put("value", "TEXT_VALUE")))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "prompt")
                                .put("value", "Enter text:"))
                        .put(new JSONObject()
                                .put("name", "defaultText")
                                .put("value", "DEFAULT_VALUE")))
                .put("type", "tExtinpuTcallback");

        //When
        TextInputCallback convertedTextInputCallback = restAuthTextInputCallbackHandler.convertFromJson(
                textInputCallback, jsonTextInputCallback);

        //Then
        assertEquals(textInputCallback, convertedTextInputCallback);
        assertEquals("Enter text:", convertedTextInputCallback.getPrompt());
        assertEquals("DEFAULT_VALUE", convertedTextInputCallback.getDefaultText());
        assertEquals("TEXT_VALUE", convertedTextInputCallback.getText());
    }
}
