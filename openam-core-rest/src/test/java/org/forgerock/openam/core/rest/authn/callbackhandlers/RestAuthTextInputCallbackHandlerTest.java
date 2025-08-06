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

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import javax.security.auth.callback.TextInputCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestAuthTextInputCallbackHandlerTest {

    private RestAuthCallbackHandler<TextInputCallback> restAuthTextInputCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthTextInputCallbackHandler = new RestAuthTextInputCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthTextInputCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(TextInputCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthResponseException,
            RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TextInputCallback textInputCallback = mock(TextInputCallback.class);

        //When
        boolean updated = restAuthTextInputCallbackHandler.updateCallbackFromRequest(request, response,
                textInputCallback);

        //Then
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextInputCallback originalTextInputCallback = mock(TextInputCallback.class);

        //When
        TextInputCallback textInputCallback = restAuthTextInputCallbackHandler.handle(request, response,
                jsonPostBody, originalTextInputCallback);

        //Then
        assertEquals(originalTextInputCallback, textInputCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        TextInputCallback textInputCallback = new TextInputCallback("Enter text:", "DEFAULT_VALUE");

        //When
        JsonValue jsonObject = restAuthTextInputCallbackHandler.convertToJson(textInputCallback, 1);

        //Then
        assertThat(jsonObject).stringAt("type").isEqualTo("TextInputCallback");
        assertThat(jsonObject).hasArray("output").hasSize(2);
        assertEquals("Enter text:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("DEFAULT_VALUE", jsonObject.get("output").get(1).get("value").asString());
        assertThat(jsonObject).hasArray("input").hasSize(1);
        assertEquals("", jsonObject.get("input").get(0).get("value").asString());
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

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
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

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
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

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
