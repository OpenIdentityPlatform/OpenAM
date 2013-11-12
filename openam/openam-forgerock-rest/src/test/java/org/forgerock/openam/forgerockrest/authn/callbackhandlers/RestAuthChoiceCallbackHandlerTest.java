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
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.ChoiceCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import java.util.Arrays;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RestAuthChoiceCallbackHandlerTest {

    private RestAuthCallbackHandler<ChoiceCallback> restAuthChoiceCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthChoiceCallbackHandler = new RestAuthChoiceCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthChoiceCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(ChoiceCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);

        //When
        boolean updated = restAuthChoiceCallbackHandler.updateCallbackFromRequest(headers, request, response,
                choiceCallback);

        //Then
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        ChoiceCallback originalChoiceCallback = mock(ChoiceCallback.class);

        //When
        ChoiceCallback choiceCallback = restAuthChoiceCallbackHandler.handle(headers, request, response, jsonPostBody,
                originalChoiceCallback);

        //Then
        assertEquals(originalChoiceCallback, choiceCallback);
    }

    @Test
    public void shouldConvertToJson() {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);

        //When
        JsonValue jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.get("type").asString());

        assertNotNull(jsonObject.get("output"));
        assertEquals(3, jsonObject.get("output").size());
        assertEquals("Select choice:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("1", jsonObject.get("output").get(1).get("value").get(0).asString());
        assertEquals("34", jsonObject.get("output").get(1).get("value").get(1).asString());
        assertEquals("66", jsonObject.get("output").get(1).get("value").get(2).asString());
        assertEquals("93", jsonObject.get("output").get(1).get("value").get(3).asString());
        assertEquals(0, (int) jsonObject.get("output").get(2).get("value").asInteger());

        assertNotNull(jsonObject.get("input"));
        assertEquals(1, jsonObject.get("input").size());
        assertEquals(0, (int) jsonObject.get("input").get(0).get("value").asInteger());
    }

    @Test
    public void shouldConvertToJsonWithPreSelectedIndexes() {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);
        choiceCallback.setSelectedIndex(1);

        //When
        JsonValue jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.get("type").asString());

        assertNotNull(jsonObject.get("output"));
        assertEquals(3, jsonObject.get("output").size());
        assertEquals("Select choice:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("1", jsonObject.get("output").get(1).get("value").get(0).asString());
        assertEquals("34", jsonObject.get("output").get(1).get("value").get(1).asString());
        assertEquals("66", jsonObject.get("output").get(1).get("value").get(2).asString());
        assertEquals("93", jsonObject.get("output").get(1).get("value").get(3).asString());
        assertEquals(0, (int) jsonObject.get("output").get(2).get("value").asInteger());

        assertNotNull(jsonObject.get("input"));
        assertEquals(1, jsonObject.get("input").size());
        assertEquals(1, (int) jsonObject.get("input").get(0).get("value").asInteger());
    }

    @Test
    public void shouldConvertFromJson() {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", 1).build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Select choice:").build())
                    .add(JsonValueBuilder.jsonValue().put("value", new String[]{"1", "34", "66", "93"}).build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "0").build())
                .put("type", "ChoiceCallback")
                .build();

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", 1).build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Select choice:").build())
                    .add(JsonValueBuilder.jsonValue().put("value", new String[]{"1", "34", "66", "93"}).build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "0").build())
                .put("type", "PasswordCallback")
                .build();

        //When
        restAuthChoiceCallbackHandler.convertFromJson(choiceCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", 1).build())
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "Select choice:").build())
                    .add(JsonValueBuilder.jsonValue().put("value", new String[]{"1", "34", "66", "93"}).build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "0").build())
                .put("type", "choicecallback")
                .build();

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }
}
