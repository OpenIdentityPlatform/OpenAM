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
 * Copyright 2018 Open Identity Community.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.authentication.callbacks.NameValueOutputCallback;

public class RestAuthNameValueOutputCallbackHandlerTest {

    private RestAuthCallbackHandler<NameValueOutputCallback> testOutputRestAuthCallbackHandler;

    @BeforeClass
    public void setUp() {
        testOutputRestAuthCallbackHandler = new RestAuthNameValueOutputCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = testOutputRestAuthCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(NameValueOutputCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthResponseException,
            RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        NameValueOutputCallback nameValueOutputCallback = mock(NameValueOutputCallback.class);

        //When
        boolean updated = testOutputRestAuthCallbackHandler.updateCallbackFromRequest(request, response,
        		nameValueOutputCallback);

        //Then
        assertTrue(!updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameValueOutputCallback originalNameValueOutputCallback = mock(NameValueOutputCallback.class);

        //When
        NameValueOutputCallback nameValueOutputCallback = testOutputRestAuthCallbackHandler.handle(request, response,
                jsonPostBody, originalNameValueOutputCallback);

        //Then
        assertEquals(originalNameValueOutputCallback, nameValueOutputCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
    	NameValueOutputCallback nameValueOutputCallback = new NameValueOutputCallback("foo", "bar");

        //When
        JsonValue jsonObject = testOutputRestAuthCallbackHandler.convertToJson(nameValueOutputCallback, 1);

        //Then
        assertEquals("NameValueOutputCallback", jsonObject.get("type").asString());
        assertThat(jsonObject).hasArray("output").hasSize(1);
        assertEquals("foo", jsonObject.get("output").get(0).get("name").asString());
        assertEquals("bar", jsonObject.get("output").get(0).get("value").asString());
        assertEquals(2, jsonObject.size());
    }

    @Test
    public void shouldConvertToJsonAndEscapeCharacters() throws RestAuthException, JSONException {

        //Given
        final String script = "for (var i = 0; i < 10; i++) { alert(\"alert\"); }";
        NameValueOutputCallback nameValueOutputCallback = new NameValueOutputCallback("script",
                script);

        //When
        JsonValue jsonObject = testOutputRestAuthCallbackHandler.convertToJson(nameValueOutputCallback, 1);

        //Then
        assertEquals("NameValueOutputCallback", jsonObject.get("type").asString());
        assertThat(jsonObject).hasArray("output").hasSize(1);
        assertEquals("script",  jsonObject.get("output").get(0).get("name").asString());
        assertEquals(script, jsonObject.get("output").get(0).get("value").asString());
        assertEquals(2, jsonObject.size());

        // Round-trip via toString/parsing with JSONObject to verify correct escaping
        JSONObject parsed = new JSONObject(jsonObject.toString());
        assertEquals(script, parsed.getJSONArray("output").getJSONObject(0).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

        //Given
    	NameValueOutputCallback nameValueOutputCallback = new NameValueOutputCallback("foo", "bar");
        JsonValue jsonNameValueOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                .addLast(JsonValueBuilder.jsonValue().put("name", "foo").put("value", "bar").build())
                .put("type", "NameValueOutputCallback")
                .build();

        //When
        NameValueOutputCallback convertedNameValueOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
        		nameValueOutputCallback, jsonNameValueOutputCallback);

        //Then
        assertEquals(nameValueOutputCallback, convertedNameValueOutputCallback);
        assertEquals("foo", convertedNameValueOutputCallback.getName());
        assertEquals("bar", convertedNameValueOutputCallback.getValue());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

        //Given
    	NameValueOutputCallback nameValueOutputCallback = new NameValueOutputCallback("foo", "bar");
        JsonValue jsonNameValueOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                .addLast(JsonValueBuilder.jsonValue().put("name", "foo").put("value", "bar").build())
                .put("type", "PasswordCallback")
                .build();


        //When
        testOutputRestAuthCallbackHandler.convertFromJson(nameValueOutputCallback, jsonNameValueOutputCallback);
    }

    @Test
    public void shouldFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

        //Given
    	NameValueOutputCallback nameValueOutputCallback = new NameValueOutputCallback("foo", "bar");
        JsonValue jsonNameValueOutputCallback = JsonValueBuilder.jsonValue()
                .array("output")
                .addLast(JsonValueBuilder.jsonValue().put("name", "foo").put("value", "bar").build())
                .put("type", "nAmevAlueoUtputcallback")
                .build();


        //When
        NameValueOutputCallback convertedNameValueOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
        		nameValueOutputCallback, jsonNameValueOutputCallback);

        //Then
        assertEquals(nameValueOutputCallback, convertedNameValueOutputCallback);
        assertEquals("foo", convertedNameValueOutputCallback.getName());
        assertEquals("bar", convertedNameValueOutputCallback.getValue());
    }
}
