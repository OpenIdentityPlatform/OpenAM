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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import javax.security.auth.callback.TextOutputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthResponseException,
            RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TextOutputCallback textOutputCallback = mock(TextOutputCallback.class);

        //When
        boolean updated = testOutputRestAuthCallbackHandler.updateCallbackFromRequest(request, response,
                textOutputCallback);

        //Then
        assertTrue(!updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        TextOutputCallback originalTextOutputCallback = mock(TextOutputCallback.class);

        //When
        TextOutputCallback textOutputCallback = testOutputRestAuthCallbackHandler.handle(request, response,
                jsonPostBody, originalTextOutputCallback);

        //Then
        assertEquals(originalTextOutputCallback, textOutputCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");

        //When
        JsonValue jsonObject = testOutputRestAuthCallbackHandler.convertToJson(textOutputCallback, 1);

        //Then
        assertEquals("TextOutputCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("output"));
        assertEquals(2, jsonObject.get("output").size());
        assertEquals("MESSAGE", jsonObject.get("output").get(0).get("value").asString());
        assertEquals(TextOutputCallback.INFORMATION,
                Integer.parseInt(jsonObject.get("output").get(1).get("value").asString()));
        assertEquals(2, jsonObject.size());
    }

    @Test
    public void shouldConvertToJsonAndEscapeCharacters() throws RestAuthException, JSONException {

        //Given
        final String script = "for (var i = 0; i < 10; i++) { alert(\"alert\"); }";
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION,
                script);

        //When
        JsonValue jsonObject = testOutputRestAuthCallbackHandler.convertToJson(textOutputCallback, 1);

        //Then
        assertEquals("TextOutputCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("output"));
        assertEquals(2, jsonObject.get("output").size());
        assertEquals(script,
                jsonObject.get("output").get(0).get("value").asString());
        assertEquals(TextOutputCallback.INFORMATION,
                Integer.parseInt(jsonObject.get("output").get(1).get("value").asString()));
        assertEquals(2, jsonObject.size());

        // Round-trip via toString/parsing with JSONObject to verify correct escaping
        JSONObject parsed = new JSONObject(jsonObject.toString());
        assertEquals(script, parsed.getJSONArray("output").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

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
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

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
    public void shouldFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

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
