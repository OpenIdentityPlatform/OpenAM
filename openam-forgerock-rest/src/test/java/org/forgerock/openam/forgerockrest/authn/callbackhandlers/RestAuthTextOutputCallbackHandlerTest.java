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

import javax.security.auth.callback.TextOutputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestAuthTextOutputCallbackHandlerTest {

    private RestAuthCallbackHandler<TextOutputCallback> testOutputRestAuthCallbackHandler;

    @BeforeClass
    public void setUp() {
        testOutputRestAuthCallbackHandler = new RestAuthTextOutputCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = testOutputRestAuthCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(TextOutputCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextOutputCallback textOutputCallback = mock(TextOutputCallback.class);

        //When
        boolean updated = testOutputRestAuthCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, textOutputCallback, HttpMethod.POST);

        //Then
        assertTrue(!updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextOutputCallback originalTextOutputCallback = mock(TextOutputCallback.class);

        //When
        TextOutputCallback textOutputCallback = testOutputRestAuthCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalTextOutputCallback);

        //Then
        assertEquals(originalTextOutputCallback, textOutputCallback);
    }

    @Test
    public void shouldConvertToJson() {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");

        //When
        JsonValue jsonObject = testOutputRestAuthCallbackHandler.convertToJson(textOutputCallback, 1);

        //Then
        assertEquals("TextOutputCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("output"));
        assertEquals(2, jsonObject.get("output").size());
        assertEquals("MESSAGE", jsonObject.get("output").get(0).get("value").asString());
        assertEquals(TextOutputCallback.INFORMATION, (int) jsonObject.get("output").get(1).get("value").asInteger());
        assertEquals(2, jsonObject.size());
    }

    @Test
    public void shouldConvertFromJson() {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JsonValue jsonTextOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "MESSAGE").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "TextOutputCallback")
                .build();

        //When
        TextOutputCallback convertedTextOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
                textOutputCallback, jsonTextOutputCallback);

        //Then
        assertEquals(textOutputCallback, convertedTextOutputCallback);
        assertEquals("MESSAGE", convertedTextOutputCallback.getMessage());
        assertEquals(TextOutputCallback.INFORMATION, convertedTextOutputCallback.getMessageType());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JsonValue jsonTextOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "MESSAGE").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "PasswordCallback")
                .build();


        //When
        testOutputRestAuthCallbackHandler.convertFromJson(textOutputCallback, jsonTextOutputCallback);

        //Then
        fail();
    }

    @Test
    public void shouldFailToConvertFromJsonWithTypeLowerCase() {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JsonValue jsonTextOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                    .add(JsonValueBuilder.jsonValue().put("value", "MESSAGE").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", 0).build())
                .put("type", "tExtoUtputcallback")
                .build();

        //When
        TextOutputCallback convertedTextOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
                textOutputCallback, jsonTextOutputCallback);

        //Then
        assertEquals(textOutputCallback, convertedTextOutputCallback);
        assertEquals("MESSAGE", convertedTextOutputCallback.getMessage());
        assertEquals(TextOutputCallback.INFORMATION, convertedTextOutputCallback.getMessageType());
    }
}
