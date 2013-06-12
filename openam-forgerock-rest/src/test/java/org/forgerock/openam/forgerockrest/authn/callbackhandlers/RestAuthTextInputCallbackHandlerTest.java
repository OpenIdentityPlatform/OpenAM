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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
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
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn("TEXT");

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, textInputCallback, HttpMethod.POST);

        //Then
        verify(textInputCallback).setText("TEXT");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenUsernameIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn(null);

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, textInputCallback, HttpMethod.POST);

        //Then
        verify(textInputCallback, never()).setText(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        given(request.getParameter("text")).willReturn("");

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, textInputCallback, HttpMethod.POST);

        //Then
        verify(textInputCallback, never()).setText(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextInputCallback originalTextInputCallback = mock(TextInputCallback.class);

        //When
        TextInputCallback textInputCallback = restAuthTextInputCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalTextInputCallback);

        //Then
        assertEquals(originalTextInputCallback, textInputCallback);
    }

    @Test
    public void shouldConvertToJson() {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");

        //When
        JsonValue jsonObject = restAuthTextInputCallbackHandler.convertToJson(textInputCallback, 1);

        //Then
        assertEquals("TextInputCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("output"));
        assertEquals(2, jsonObject.get("output").size());
        assertEquals("Enter text:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("DEFAULT_VALUE", jsonObject.get("output").get(1).get("value").asString());
        assertNotNull(jsonObject.get("input"));
        assertEquals(1, jsonObject.get("input").size());
        assertEquals("", jsonObject.get("input").get(0).get("value").asString());
    }

    @Test
    public void shouldConvertFromJson() {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JsonValue jsonTextInputCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "TEXT_VALUE").build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Enter text:").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "DEFAULT_VALUE").build())
                .put("type", "TextInputCallback")
                .build();

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
    public void shouldFailToConvertFromJsonWithInvalidType() {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JsonValue jsonTextInputCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "TEXT_VALUE").build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Enter text:").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "DEFAULT_VALUE").build())
                .put("type", "PasswordCallback")
                .build();

        //When
        restAuthTextInputCallbackHandler.convertFromJson(textInputCallback, jsonTextInputCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");
        JsonValue jsonTextInputCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "TEXT_VALUE").build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Enter text:").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "DEFAULT_VALUE").build())
                .put("type", "tExtinpuTcallback")
                .build();

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
