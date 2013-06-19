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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandler;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandlerResponseException;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import java.util.LinkedHashMap;

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
    public void shouldHandleCallbacksIntoJson() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = new JsonValue(new LinkedHashMap<String, String>());
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JsonValue jsonCallback1 = new JsonValue(new LinkedHashMap<String, String>());
        jsonCallback1.put("KEY1", "VALUE1");
        JsonValue jsonCallback2 = new JsonValue(new LinkedHashMap<String, String>());
        jsonCallback2.put("KEY2", "VALUE2");

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                    Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST)).willReturn(false);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST)).willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1, 1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2, 2)).willReturn(jsonCallback2);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, response,
                jsonPostBody, callbacks, HttpMethod.POST);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST);
        verify(restAuthCallbackHandler2, never()).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST);
        verify(restAuthCallbackHandler1).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 2);
        assertEquals(jsonCallbacks.get(0).get("KEY1").asString(), "VALUE1");
        assertEquals(jsonCallbacks.get(1).get("KEY2").asString(), "VALUE2");
    }

    @Test
    public void shouldHandleCallbacksInternally() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject())).willReturn(
                restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST)).willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST)).willReturn(true);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, response,
                jsonPostBody, callbacks, HttpMethod.POST);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST);
        verify(restAuthCallbackHandler1, never()).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2, never()).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 0);
    }

    @Test
    public void shouldHandleCallbacksIntoJsonIfAtLeastOneCannotBeDoneInternally()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = new JsonValue(new LinkedHashMap<String, String>());
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JsonValue jsonCallback1 = new JsonValue(new LinkedHashMap<String, String>());
        jsonCallback1.put("KEY1", "VALUE1");
        JsonValue jsonCallback2 = new JsonValue(new LinkedHashMap<String, String>());
        jsonCallback2.put("KEY2", "VALUE2");

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                    Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST)).willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST)).willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1, 1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2, 2)).willReturn(jsonCallback2);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(headers, request, response,
                jsonPostBody, callbacks, HttpMethod.POST);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback1, HttpMethod.POST);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(headers, request, response, jsonPostBody,
                callback2, HttpMethod.POST);
        verify(restAuthCallbackHandler1).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 2);
        assertEquals(jsonCallbacks.get(0).get("KEY1").asString(), "VALUE1");
        assertEquals(jsonCallbacks.get(1).get("KEY2").asString(), "VALUE2");
    }

    @Test
    public void shouldHandleJsonCallbacks() {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JsonValue jsonCallback1 = mock(JsonValue.class);
        JsonValue jsonCallback2 = mock(JsonValue.class);
        JsonValue jsonCallbacks = mock(JsonValue.class);
        JsonValue jsonCallback1Type = mock(JsonValue.class);
        JsonValue jsonCallback2Type = mock(JsonValue.class);


        given(jsonCallbacks.size()).willReturn(2);
        given(jsonCallbacks.get(0)).willReturn(jsonCallback1);
        given(jsonCallbacks.get(1)).willReturn(jsonCallback2);
        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(jsonCallback1.get("type")).willReturn(jsonCallback1Type);
        given(jsonCallback2.get("type")).willReturn(jsonCallback2Type);
        given(jsonCallback1Type.asString()).willReturn("CALLBACK1");
        given(jsonCallback2Type.asString()).willReturn("CALLBACK2");
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
    public void shouldHandleJsonCallbacksMixedUp() {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        JsonValue jsonCallback1 = mock(JsonValue.class);
        JsonValue jsonCallback2 = mock(JsonValue.class);
        JsonValue jsonCallbacks = mock(JsonValue.class);
        JsonValue jsonCallback1Type = mock(JsonValue.class);
        JsonValue jsonCallback2Type = mock(JsonValue.class);


        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(jsonCallback1.get("type")).willReturn(jsonCallback1Type);
        given(jsonCallback2.get("type")).willReturn(jsonCallback2Type);
        given(jsonCallback1Type.asString()).willReturn("CALLBACK1");
        given(jsonCallback2Type.asString()).willReturn("CALLBACK2");
        given(restAuthCallbackHandler1.convertFromJson(callback1, jsonCallback1)).willReturn(callback1);
        given(restAuthCallbackHandler2.convertFromJson(callback2, jsonCallback2)).willReturn(callback2);
        given(jsonCallbacks.size()).willReturn(2);
        given(jsonCallbacks.get(0)).willReturn(jsonCallback1);
        given(jsonCallbacks.get(1)).willReturn(jsonCallback2);

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
    public void shouldFailToHandleJsonCallbacksWithMissingJSONCallback() {

        //Given
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback callback3 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2, callback3};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler3 = mock(RestAuthCallbackHandler.class);
        JsonValue jsonCallback1 = mock(JsonValue.class);
        JsonValue jsonCallback2 = mock(JsonValue.class);
        JsonValue jsonCallbacks = mock(JsonValue.class);
        JsonValue jsonCallback1Type = mock(JsonValue.class);
        JsonValue jsonCallback2Type = mock(JsonValue.class);


        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                Matchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler2)
                .willReturn(restAuthCallbackHandler3);
        given(restAuthCallbackHandler1.getCallbackClassName()).willReturn("CALLBACK1");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK2");
        given(restAuthCallbackHandler2.getCallbackClassName()).willReturn("CALLBACK3");
        given(jsonCallback1.get("type")).willReturn(jsonCallback1Type);
        given(jsonCallback2.get("type")).willReturn(jsonCallback2Type);
        given(jsonCallback1Type.asString()).willReturn("CALLBACK1");
        given(jsonCallback2Type.asString()).willReturn("CALLBACK2");
        given(restAuthCallbackHandler1.convertFromJson(callback1, jsonCallback1)).willReturn(callback1);
        given(restAuthCallbackHandler2.convertFromJson(callback2, jsonCallback2)).willReturn(callback2);

        //When
        restAuthCallbackHandlerManager.handleJsonCallbacks(callbacks, jsonCallbacks);

        //Then
        fail();
    }
}
