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

import javax.security.auth.callback.ConfirmationCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);

        given(request.getParameter("selectedIndex")).willReturn("9");

        //When
        boolean updated = restAuthConfirmationCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, confirmationCallback, HttpMethod.POST);

        //Then
        verify(confirmationCallback).setSelectedIndex(9);
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenSelectedIndexIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);

        given(request.getParameter("selectedIndex")).willReturn(null);

        //When
        boolean updated = restAuthConfirmationCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, confirmationCallback, HttpMethod.POST);

        //Then
        verify(confirmationCallback, never()).setSelectedIndex(anyInt());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenSelectedIndexIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);

        given(request.getParameter("selectedIndex")).willReturn("");

        //When
        boolean updated = restAuthConfirmationCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, confirmationCallback, HttpMethod.POST);

        //Then
        verify(confirmationCallback, never()).setSelectedIndex(anyInt());
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ConfirmationCallback originalConfirmationCallback = mock(ConfirmationCallback.class);

        //When
        ConfirmationCallback confirmationCallback = restAuthConfirmationCallbackHandler.handle(headers, request,
                response, jsonPostBody, originalConfirmationCallback);

        //Then
        assertEquals(originalConfirmationCallback, confirmationCallback);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);

        //When
        JSONObject jsonObject = restAuthConfirmationCallbackHandler.convertToJson(confirmationCallback, 1);

        //Then
        assertEquals("ConfirmationCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(5, jsonObject.getJSONArray("output").length());
        assertEquals("Select confirmation:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals(ConfirmationCallback.INFORMATION,
                jsonObject.getJSONArray("output").getJSONObject(1).getInt("value"));
        assertEquals("OK", jsonObject.getJSONArray("output").getJSONObject(2).getJSONArray("value").getString(0));
        assertEquals("NO", jsonObject.getJSONArray("output").getJSONObject(2).getJSONArray("value").getString(1));
        assertEquals("CANCEL", jsonObject.getJSONArray("output").getJSONObject(2).getJSONArray("value").getString(2));
        assertEquals(-1, jsonObject.getJSONArray("output").getJSONObject(3).getInt("value"));
        assertEquals(0, jsonObject.getJSONArray("output").getJSONObject(4).getInt("value"));
        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(1, jsonObject.getJSONArray("input").length());
        assertEquals(0, jsonObject.getJSONArray("input").getJSONObject(0).getInt("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JSONObject jsonConfirmationCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 2)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select confirmation:"))
                        .put(new JSONObject()
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("OK").put("NO").put("CANCEL")))
                        .put(new JSONObject()
                                .put("value", -1))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "ConfirmationCallback");

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
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JSONObject jsonConfirmationCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 2)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select confirmation:"))
                        .put(new JSONObject()
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("OK").put("NO").put("CANCEL")))
                        .put(new JSONObject()
                                .put("value", -1))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "PasswordCallback");

        //When
        restAuthConfirmationCallbackHandler.convertFromJson(confirmationCallback, jsonConfirmationCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        ConfirmationCallback confirmationCallback = new ConfirmationCallback("Select confirmation:",
                ConfirmationCallback.INFORMATION, new String[]{"OK", "NO", "CANCEL"}, 0);
        JSONObject jsonConfirmationCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 2)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select confirmation:"))
                        .put(new JSONObject()
                                .put("value", 0))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("OK").put("NO").put("CANCEL")))
                        .put(new JSONObject()
                                .put("value", -1))
                        .put(new JSONObject()
                                .put("value", 0)))
                .put("type", "confirmationcallback");

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
