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
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

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
                    HttpServletResponse response, JSONObject postBody, Callback callback)
                    throws RestAuthCallbackHandlerResponseException {
                return false;
            }
        };
    }

    @Test
    public void shouldCreateJsonField() throws JSONException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        JSONObject jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, value);

        //Then
        assertEquals(jsonObject.getString("value"), "VALUE");
    }

    @Test
    public void shouldCreateJsonFieldWithNullValue() throws JSONException {

        //Given
        String name = "NAME";
        String value = null;

        //When
        JSONObject jsonObject = abstractRestAuthCallbackHandler.createJsonField(name, value);

        //Then
        assertEquals(jsonObject.getString("value"), "");
    }

    @Test
    public void shouldCreateJsonInputField() throws JSONException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        JSONObject jsonObject = abstractRestAuthCallbackHandler.createInputField(name, value);

        //Then
        assertEquals(jsonObject.getString("value"), "VALUE");
    }

    @Test
    public void shouldCreateJsonOutputField() throws JSONException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        JSONObject jsonObject = abstractRestAuthCallbackHandler.createOutputField(name, value);

        //Then
        assertEquals(jsonObject.getString("value"), "VALUE");
    }

    @Test
    public void shouldValidateCallbackTypeSuccessfully() throws JSONException {

        //Given
        String callbackName = "CALLBACK_NAME";
        JSONObject jsonCallback = mock(JSONObject.class);

        given(jsonCallback.getString("type")).willReturn("CALLBACK_NAME");

        //When
        abstractRestAuthCallbackHandler.validateCallbackType(callbackName, jsonCallback);

        //Then
        // Success
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldValidateCallbackTypeUnsuccessfully() throws JSONException {

        //Given
        String callbackName = "CALLBACK_NAME";
        JSONObject jsonCallback = mock(JSONObject.class);

        given(jsonCallback.getString("type")).willReturn("CALLBACK_NAME_NOT");

        //When
        abstractRestAuthCallbackHandler.validateCallbackType(callbackName, jsonCallback);

        //Then
        fail();
    }

    @Test
    public void shouldCheckIfJsonAttributePresentWithTrue() throws JSONException {

        //Given
        String attributeName = "ATTRIBUTE_NAME";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ATTRIBUTE_NAME", "VALUE");

        //When
        boolean isAttributePresent = abstractRestAuthCallbackHandler.isJsonAttributePresent(jsonObject, attributeName);

        //Then
        assertTrue(isAttributePresent);
    }

    @Test
    public void shouldCheckIfJsonAttributePresentWithFalse() throws JSONException {

        //Given
        String attributeName = "ATTRIBUTE_NAME";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("OTHER_ATTRIBUTE_NAME", "VALUE");

        //When
        boolean isAttributePresent = abstractRestAuthCallbackHandler.isJsonAttributePresent(jsonObject, attributeName);

        //Then
        assertFalse(isAttributePresent);
    }
}
