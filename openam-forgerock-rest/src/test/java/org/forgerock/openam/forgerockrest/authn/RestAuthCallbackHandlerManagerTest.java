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

package org.forgerock.openam.forgerockrest.authn;

import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class RestAuthCallbackHandlerManagerTest {

    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;

    private RestAuthCallbackHandlerFactory restAuthCallbackHandlerFactory;

    @BeforeClass
    public void setUp() {

        restAuthCallbackHandlerFactory = mock(RestAuthCallbackHandlerFactory.class);

        restAuthCallbackHandlerManager = new RestAuthCallbackHandlerManager(restAuthCallbackHandlerFactory);
    }

    @Test
    public void shouldHandleCallbacksIntoJson() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JSONObject jsonCallback1 = mock(JSONObject.class);
        JSONObject jsonCallback2 = mock(JSONObject.class);

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                    Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, callback1)).willReturn(false);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, callback2)).willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2)).willReturn(jsonCallback2);

        //When
        JSONArray jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, callback1);
        verify(restAuthCallbackHandler2, never()).updateCallbackFromRequest(headers, request, callback2);
        verify(restAuthCallbackHandler1).convertToJson(callback1);
        verify(restAuthCallbackHandler2).convertToJson(callback2);
        assertEquals(jsonCallbacks.length(), 2);
        assertEquals(jsonCallbacks.get(0), jsonCallback1);
        assertEquals(jsonCallbacks.get(1), jsonCallback2);
    }

    @Test
    public void shouldHandleCallbacksInternally() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(Matchers.<Class<? extends Callback>>anyObject())).willReturn(
                restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, callback1)).willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, callback2)).willReturn(true);

        //When
        JSONArray jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, callback1);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(headers, request, callback2);
        verify(restAuthCallbackHandler1, never()).convertToJson(callback1);
        verify(restAuthCallbackHandler2, never()).convertToJson(callback2);
        assertEquals(jsonCallbacks.length(), 0);
    }

    @Test
    public void shouldHandleCallbacksIntoJsonIfAtLeastOneCannotBeDoneInternally() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JSONObject jsonCallback1 = mock(JSONObject.class);
        JSONObject jsonCallback2 = mock(JSONObject.class);

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                    Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, callback1)).willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, callback2)).willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2)).willReturn(jsonCallback2);

        //When
        JSONArray jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, callback1);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(headers, request, callback2);
        verify(restAuthCallbackHandler1).convertToJson(callback1);
        verify(restAuthCallbackHandler2).convertToJson(callback2);
        assertEquals(jsonCallbacks.length(), 2);
        assertEquals(jsonCallbacks.get(0), jsonCallback1);
        assertEquals(jsonCallbacks.get(1), jsonCallback2);
    }

    @Test
    public void shouldHandleJsonCallbacks() throws JSONException {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JSONObject jsonCallback1 = mock(JSONObject.class);
        JSONObject jsonCallback2 = mock(JSONObject.class);
        JSONArray jsonCallbacks = new JSONArray();
        jsonCallbacks.put(jsonCallback1);
        jsonCallbacks.put(jsonCallback2);


        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(jsonCallback1.getString("type")).willReturn("CALLBACK1");
        given(jsonCallback2.getString("type")).willReturn("CALLBACK2");
        given(restAuthCallbackHandler1.convertFromJson(callback1, jsonCallback1)).willReturn(callback1);
        given(restAuthCallbackHandler2.convertFromJson(callback2, jsonCallback2)).willReturn(callback2);

        //When
        Callback[] originalCallbacks = restAuthCallbackHandlerManager.handleJsonCallbacks(callbacks, jsonCallbacks);

        //Then
        verify(restAuthCallbackHandler1).convertFromJson(callback1, jsonCallback1);
        verify(restAuthCallbackHandler2).convertFromJson(callback2, jsonCallback2);
        assertEquals(originalCallbacks.length, 2);
        assertEquals(originalCallbacks[0], callback1);
        assertEquals(originalCallbacks[1], callback2);
    }

    @Test
    public void shouldHandleJsonCallbacksMixedUp() throws JSONException {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JSONObject jsonCallback1 = mock(JSONObject.class);
        JSONObject jsonCallback2 = mock(JSONObject.class);
        JSONArray jsonCallbacks = new JSONArray();
        jsonCallbacks.put(jsonCallback2);
        jsonCallbacks.put(jsonCallback1);


        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(jsonCallback1.getString("type")).willReturn("CALLBACK1");
        given(jsonCallback2.getString("type")).willReturn("CALLBACK2");
        given(restAuthCallbackHandler1.convertFromJson(callback1, jsonCallback1)).willReturn(callback1);
        given(restAuthCallbackHandler2.convertFromJson(callback2, jsonCallback2)).willReturn(callback2);

        //When
        Callback[] originalCallbacks = restAuthCallbackHandlerManager.handleJsonCallbacks(callbacks, jsonCallbacks);

        //Then
        verify(restAuthCallbackHandler1).convertFromJson(callback1, jsonCallback1);
        verify(restAuthCallbackHandler2).convertFromJson(callback2, jsonCallback2);
        assertEquals(originalCallbacks.length, 2);
        assertEquals(originalCallbacks[0], callback1);
        assertEquals(originalCallbacks[1], callback2);
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToHandleJsonCallbacksWithMissinJSONCallback() throws JSONException {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback callback3 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2, callback3};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler3 = mock(RestAuthCallbackHandler.class);
        JSONObject jsonCallback1 = mock(JSONObject.class);
        JSONObject jsonCallback2 = mock(JSONObject.class);
        JSONArray jsonCallbacks = new JSONArray();
        jsonCallbacks.put(jsonCallback2);
        jsonCallbacks.put(jsonCallback1);


        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2)
                .willReturn(restAuthCallbackHandler3);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK3");
        given(jsonCallback1.getString("type")).willReturn("CALLBACK1");
        given(jsonCallback2.getString("type")).willReturn("CALLBACK2");
        given(restAuthCallbackHandler1.convertFromJson(callback1, jsonCallback1)).willReturn(callback1);
        given(restAuthCallbackHandler2.convertFromJson(callback2, jsonCallback2)).willReturn(callback2);

        //When
            restAuthCallbackHandlerManager.handleJsonCallbacks(callbacks, jsonCallbacks);

        //Then
        fail();
    }
}
