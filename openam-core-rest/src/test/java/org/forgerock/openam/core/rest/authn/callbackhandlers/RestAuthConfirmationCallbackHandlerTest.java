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

import javax.security.auth.callback.ConfirmationCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestAuthConfirmationCallbackHandlerTest {

    private RestAuthCallbackHandler<ConfirmationCallback> restAuthConfirmationCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthConfirmationCallbackHandler = new RestAuthConfirmationCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthConfirmationCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(ConfirmationCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthResponseException,
            RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);

        //When
        boolean updated = restAuthConfirmationCallbackHandler.updateCallbackFromRequest(request, response,
                confirmationCallback);

        //Then
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        ConfirmationCallback originalConfirmationCallback = mock(ConfirmationCallback.class);

        //When
        ConfirmationCallback confirmationCallback = restAuthConfirmationCallbackHandler.handle(request,
                response, jsonPostBody, originalConfirmationCallback);

        //Then
        assertEquals(originalConfirmationCallback, confirmationCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);

        //When
        JsonValue jsonObject = restAuthConfirmationCallbackHandler.convertToJson(confirmationCallback, 1);

        //Then
        assertEquals("ConfirmationCallback", jsonObject.get("type").asString());
        assertThat(jsonObject).hasArray("output").hasSize(5);
        assertEquals("Select confirmation:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals(ConfirmationCallback.INFORMATION, (int)jsonObject.get("output").get(1).get("value").asInteger());
        assertEquals("OK", jsonObject.get("output").get(2).get("value").get(0).asString());
        assertEquals("NO", jsonObject.get("output").get(2).get("value").get(1).asString());
        assertEquals("CANCEL", jsonObject.get("output").get(2).get("value").get(2).asString());
        assertEquals(-1, (int) jsonObject.get("output").get(3).get("value").asInteger());
        assertEquals(0, (int) jsonObject.get("output").get(4).get("value").asInteger());
        assertThat(jsonObject).hasArray("input").hasSize(1);
        assertEquals(0, (int) jsonObject.get("input").get(0).get("value").asInteger());
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JsonValue jsonConfirmationCallback = JsonValueBuilder.jsonValue()
                .array("input")
                .addLast(JsonValueBuilder.jsonValue().put("value", 2).build())
                .array("output")
                .add(JsonValueBuilder.jsonValue().put("value", "Select confirmation:").build())
                .add(JsonValueBuilder.jsonValue().put("value", 0).build())
                .add(JsonValueBuilder.jsonValue().put("value", new String[]{"OK", "NO", "CANCEL"}).build())
                .add(JsonValueBuilder.jsonValue().put("value", -1).build())
                .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "ConfirmationCallback")
                .build();

        //When
        ConfirmationCallback convertedConfirmationCallback = restAuthConfirmationCallbackHandler.convertFromJson(
                confirmationCallback, jsonConfirmationCallback);

        //Then
        assertEquals(confirmationCallback, convertedConfirmationCallback);
        assertEquals("Select confirmation:", convertedConfirmationCallback.getPrompt());
        assertEquals(ConfirmationCallback.INFORMATION, convertedConfirmationCallback.getMessageType());
        assertEquals("OK", convertedConfirmationCallback.getOptions()[0]);
        assertEquals("NO", convertedConfirmationCallback.getOptions()[1]);
        assertEquals("CANCEL", convertedConfirmationCallback.getOptions()[2]);
        assertEquals(-1, convertedConfirmationCallback.getOptionType());
        assertEquals(0, convertedConfirmationCallback.getDefaultOption());
        assertEquals(2, convertedConfirmationCallback.getSelectedIndex());
    }

    @Test
    public void shouldConvertFromJsonWithStringChoice() throws RestAuthException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JsonValue jsonConfirmationCallback = JsonValueBuilder.jsonValue()
                .array("input")
                .addLast(JsonValueBuilder.jsonValue().put("value", 2).build())
                .array("output")
                .add(JsonValueBuilder.jsonValue().put("value", "Select confirmation:").build())
                .add(JsonValueBuilder.jsonValue().put("value", 0).build())
                .add(JsonValueBuilder.jsonValue().put("value", new String[]{"OK", "NO", "CANCEL"}).build())
                .add(JsonValueBuilder.jsonValue().put("value", -1).build())
                .addLast(JsonValueBuilder.jsonValue().put("value", "0").build())
                .put("type", "ConfirmationCallback")
                .build();

        //When
        ConfirmationCallback convertedConfirmationCallback = restAuthConfirmationCallbackHandler.convertFromJson(
                confirmationCallback, jsonConfirmationCallback);

        //Then
        assertEquals(confirmationCallback, convertedConfirmationCallback);
        assertEquals("Select confirmation:", convertedConfirmationCallback.getPrompt());
        assertEquals(ConfirmationCallback.INFORMATION, convertedConfirmationCallback.getMessageType());
        assertEquals("OK", convertedConfirmationCallback.getOptions()[0]);
        assertEquals("NO", convertedConfirmationCallback.getOptions()[1]);
        assertEquals("CANCEL", convertedConfirmationCallback.getOptions()[2]);
        assertEquals(-1, convertedConfirmationCallback.getOptionType());
        assertEquals(0, convertedConfirmationCallback.getDefaultOption());
        assertEquals(2, convertedConfirmationCallback.getSelectedIndex());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JsonValue jsonConfirmationCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", 2).build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Select confirmation:").build())
                    .add(JsonValueBuilder.jsonValue().put("value", 0).build())
                    .add(JsonValueBuilder.jsonValue().put("value", new String[]{"OK", "NO", "CANCEL"}).build())
                    .add(JsonValueBuilder.jsonValue().put("value", -1).build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "PasswordCallback")
                .build();

        //When
        restAuthConfirmationCallbackHandler.convertFromJson(confirmationCallback, jsonConfirmationCallback);
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JsonValue jsonConfirmationCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", 2).build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Select confirmation:").build())
                    .add(JsonValueBuilder.jsonValue().put("value", 0).build())
                    .add(JsonValueBuilder.jsonValue().put("value", new String[]{"OK", "NO", "CANCEL"}).build())
                    .add(JsonValueBuilder.jsonValue().put("value", -1).build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "confirmationcallback")
                .build();

        //When
        ConfirmationCallback convertedConfirmationCallback = restAuthConfirmationCallbackHandler.convertFromJson(
                confirmationCallback, jsonConfirmationCallback);

        //Then
        assertEquals(confirmationCallback, convertedConfirmationCallback);
        assertEquals("Select confirmation:", convertedConfirmationCallback.getPrompt());
        assertEquals(ConfirmationCallback.INFORMATION, convertedConfirmationCallback.getMessageType());
        assertEquals("OK", convertedConfirmationCallback.getOptions()[0]);
        assertEquals("NO", convertedConfirmationCallback.getOptions()[1]);
        assertEquals("CANCEL", convertedConfirmationCallback.getOptions()[2]);
        assertEquals(-1, convertedConfirmationCallback.getOptionType());
        assertEquals(0, convertedConfirmationCallback.getDefaultOption());
        assertEquals(2, convertedConfirmationCallback.getSelectedIndex());
    }
}
