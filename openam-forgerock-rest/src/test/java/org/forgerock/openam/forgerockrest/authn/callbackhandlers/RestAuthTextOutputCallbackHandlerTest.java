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

import org.forgerock.openam.forgerockrest.authn.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
        JSONObject jsonPostBody = mock(JSONObject.class);
        TextOutputCallback textOutputCallback = mock(TextOutputCallback.class);

        //When
        boolean updated = testOutputRestAuthCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, textOutputCallback, HttpMethod.POST);

        //Then
        assertTrue(!updated);
    }

    @Test
    public void shouldHandleCallback() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        TextOutputCallback originalTextOutputCallback = mock(TextOutputCallback.class);

        //When
        TextOutputCallback textOutputCallback = testOutputRestAuthCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalTextOutputCallback);

        //Then
        assertEquals(originalTextOutputCallback, textOutputCallback);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");

        //When
        JSONObject jsonObject = testOutputRestAuthCallbackHandler.convertToJson(textOutputCallback, 1);

        //Then
        assertEquals("TextOutputCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(2, jsonObject.getJSONArray("output").length());
        assertEquals("MESSAGE", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals(TextOutputCallback.INFORMATION,
                jsonObject.getJSONArray("output").getJSONObject(1).getInt("value"));
        assertEquals(2, jsonObject.length());
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JSONObject jsonTextOutputCallback = new JSONObject()
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "MESSAGE"))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "TextOutputCallback");

        //When
        TextOutputCallback convertedTextOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
                textOutputCallback, jsonTextOutputCallback);

        //Then
        assertEquals(textOutputCallback, convertedTextOutputCallback);
        assertEquals("MESSAGE", convertedTextOutputCallback.getMessage());
        assertEquals(TextOutputCallback.INFORMATION, convertedTextOutputCallback.getMessageType());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JSONObject jsonTextOutputCallback = new JSONObject()
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "MESSAGE"))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "PasswordCallback");


        //When
        testOutputRestAuthCallbackHandler.convertFromJson(textOutputCallback, jsonTextOutputCallback);

        //Then
        fail();
    }

    @Test
    public void shouldFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "MESSAGE");
        JSONObject jsonTextOutputCallback = new JSONObject()
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "MESSAGE"))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "tExtoUtputcallback");

        //When
        TextOutputCallback convertedTextOutputCallback = testOutputRestAuthCallbackHandler.convertFromJson(
                textOutputCallback, jsonTextOutputCallback);

        //Then
        assertEquals(textOutputCallback, convertedTextOutputCallback);
        assertEquals("MESSAGE", convertedTextOutputCallback.getMessage());
        assertEquals(TextOutputCallback.INFORMATION, convertedTextOutputCallback.getMessageType());
    }
}
