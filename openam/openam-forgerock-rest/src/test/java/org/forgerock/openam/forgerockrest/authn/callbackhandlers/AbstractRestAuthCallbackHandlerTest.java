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
import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AbstractRestAuthCallbackHandlerTest {

    private AbstractRestAuthCallbackHandler abstractRestAuthCallbackHandler;

    @BeforeClass
    public void setUp() {
        abstractRestAuthCallbackHandler = new AbstractRestAuthCallbackHandler<Callback>() {
            @Override
            boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
                    HttpServletResponse response, Callback callback)
                    throws RestAuthCallbackHandlerResponseException {
                return false;
            }

            public Callback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, JsonValue postBody, Callback originalCallback) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getCallbackClassName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public JsonValue convertToJson(Callback callback, int index) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Callback convertFromJson(Callback callback, JsonValue jsonObject) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Test
    public void shouldCreateJsonField() {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, value);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").asString(), "VALUE");
    }

    @Test
    public void shouldCreateJsonFieldWithNullValue() throws JSONException {

        //Given
        String name = "NAME";
        String value = null;

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, value);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").asString(), "");
    }

    @Test
    public void shouldCreateJsonFieldWithObjectArray() {

        //Given
        String name = "NAME";
        String[] values = new String[]{"VALUE1", "VALUE2", "VALUE3"};

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, values);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").get(0).asString(), "VALUE1");
        assertEquals(jsonObject.get("value").get(1).asString(), "VALUE2");
        assertEquals(jsonObject.get("value").get(2).asString(), "VALUE3");
    }

    @Test
    public void shouldCreateJsonFieldWithNullObjectArray() {

        //Given
        String name = "NAME";
        String[] values = null;

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, values);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").size(), 0);
    }

    @Test
    public void shouldCreateJsonInputField() throws JSONException {

        //Given
        String value = "VALUE";

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createInputField(0, value);

        //Then
        assertEquals(jsonObject.get("name").asString(), "IDToken0");
        assertEquals(jsonObject.get("value").asString(), "VALUE");
    }

    @Test
    public void shouldCreateJsonInputFieldWithObjectArray() throws JSONException {

        //Given
        String[] values = new String[]{"VALUE1", "VALUE2", "VALUE3"};

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createInputField(0, values);

        //Then
        assertEquals(jsonObject.get("name").asString(), "IDToken0");
        assertEquals(jsonObject.get("value").get(0).asString(), "VALUE1");
        assertEquals(jsonObject.get("value").get(1).asString(), "VALUE2");
        assertEquals(jsonObject.get("value").get(2).asString(), "VALUE3");
    }

    @Test
    public void shouldCreateJsonOutputField() throws JSONException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createOutputField(name, value);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").asString(), "VALUE");
    }

    @Test
    public void shouldCreateJsonOutputFieldWithObjectArray() throws JSONException {

        //Given
        String name = "NAME";
        String[] values = new String[]{"VALUE1", "VALUE2", "VALUE3"};

        //When
        JsonValue jsonObject = abstractRestAuthCallbackHandler.createOutputField(name, values);

        //Then
        assertEquals(jsonObject.get("name").asString(), "NAME");
        assertEquals(jsonObject.get("value").get(0).asString(), "VALUE1");
        assertEquals(jsonObject.get("value").get(1).asString(), "VALUE2");
        assertEquals(jsonObject.get("value").get(2).asString(), "VALUE3");
    }

    @Test
    public void shouldValidateCallbackTypeSuccessfully() throws JSONException {

        //Given
        String callbackName = "CALLBACK_NAME";
        JsonValue jsonCallback = mock(JsonValue.class);
        JsonValue typeJson = mock(JsonValue.class);

        given(jsonCallback.get("type")).willReturn(typeJson);
        given(typeJson.asString()).willReturn("CALLBACK_NAME");

        //When
        abstractRestAuthCallbackHandler.validateCallbackType(callbackName, jsonCallback);

        //Then
        // Success
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldValidateCallbackTypeUnsuccessfully() throws JSONException {

        //Given
        String callbackName = "CALLBACK_NAME";
        JsonValue jsonCallback = mock(JsonValue.class);
        JsonValue typeJson = mock(JsonValue.class);

        given(jsonCallback.get("type")).willReturn(typeJson);
        given(typeJson.asString()).willReturn("CALLBACK_NAME_NOT");

        //When
        abstractRestAuthCallbackHandler.validateCallbackType(callbackName, jsonCallback);

        //Then
        fail();
    }

    @Test
    public void shouldCheckIfJsonAttributePresentWithTrue() throws JSONException {

        //Given
        String attributeName = "ATTRIBUTE_NAME";
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("ATTRIBUTE_NAME", "VALUE");
        JsonValue jsonObject = new JsonValue(map);

        //When
        boolean isAttributePresent = abstractRestAuthCallbackHandler.isJsonAttributePresent(jsonObject, attributeName);

        //Then
        assertTrue(isAttributePresent);
    }

    @Test
    public void shouldCheckIfJsonAttributePresentWithFalse() throws JSONException {

        //Given
        String attributeName = "ATTRIBUTE_NAME";
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("OTHER_ATTRIBUTE_NAME", "VALUE");
        JsonValue jsonObject = new JsonValue(map);

        //When
        boolean isAttributePresent = abstractRestAuthCallbackHandler.isJsonAttributePresent(jsonObject, attributeName);

        //Then
        assertFalse(isAttributePresent);
    }
}
