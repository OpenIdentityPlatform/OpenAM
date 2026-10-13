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
 * Copyright 2013-2015 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest.authn;

import com.sun.identity.authentication.callbacks.NameValueOutputCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.HttpCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthCallbackHandler;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.LinkedHashMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
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
    public void shouldHandleCallbacksIntoJson() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
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
                    ArgumentMatchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(request, response, callback1))
                .willReturn(false);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(request, response, callback2))
                .willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1, 1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2, 2)).willReturn(jsonCallback2);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(request, response, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(request, response, callback1);
        verify(restAuthCallbackHandler2, never()).updateCallbackFromRequest(request, response, callback2);
        verify(restAuthCallbackHandler1).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 2);
        assertEquals(jsonCallbacks.get(0).get("KEY1").asString(), "VALUE1");
        assertEquals(jsonCallbacks.get(1).get("KEY2").asString(), "VALUE2");
    }

    @Test
    public void shouldHandleCallbacksInternally() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Callback callback1 = mock(Callback.class);
        Callback callback2 = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callback1, callback2};
        RestAuthCallbackHandler restAuthCallbackHandler1 = mock(RestAuthCallbackHandler.class);
        RestAuthCallbackHandler restAuthCallbackHandler2 = mock(RestAuthCallbackHandler.class);

        given(restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                ArgumentMatchers.<Class<? extends Callback>>anyObject())).willReturn(
                restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(request, response, callback1))
                .willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(request, response, callback2))
                .willReturn(true);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(request, response, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(request, response, callback1);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(request, response, callback2);
        verify(restAuthCallbackHandler1, never()).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2, never()).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 0);
    }

    @Test
    public void shouldHandleCallbacksIntoJsonIfAtLeastOneCannotBeDoneInternally()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
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
                    ArgumentMatchers.<Class<? extends Callback>>anyObject()))
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2)
                .willReturn(restAuthCallbackHandler1).willReturn(restAuthCallbackHandler2);
        given(restAuthCallbackHandler1.updateCallbackFromRequest(request, response, callback1))
                .willReturn(true);
        given(restAuthCallbackHandler2.updateCallbackFromRequest(request, response, callback2))
                .willReturn(false);
        given(restAuthCallbackHandler1.convertToJson(callback1, 1)).willReturn(jsonCallback1);
        given(restAuthCallbackHandler2.convertToJson(callback2, 2)).willReturn(jsonCallback2);

        //When
        JsonValue jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(request, response, callbacks);

        //Then
        verify(restAuthCallbackHandler1).updateCallbackFromRequest(request, response, callback1);
        verify(restAuthCallbackHandler2).updateCallbackFromRequest(request, response, callback2);
        verify(restAuthCallbackHandler1).convertToJson(callback1, 1);
        verify(restAuthCallbackHandler2).convertToJson(callback2, 2);
        assertEquals(jsonCallbacks.size(), 2);
        assertEquals(jsonCallbacks.get(0).get("KEY1").asString(), "VALUE1");
        assertEquals(jsonCallbacks.get(1).get("KEY2").asString(), "VALUE2");
    }

    @Test
    public void shouldHandleJsonCallbacks() throws RestAuthException {

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
                ArgumentMatchers.<Class<? extends Callback>>anyObject()))
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
    public void shouldHandleJsonCallbacksMixedUp() throws RestAuthException {

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
                ArgumentMatchers.<Class<? extends Callback>>anyObject()))
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
    public void shouldFailToHandleJsonCallbacksWithMissingJSONCallback() throws RestAuthException {

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
                ArgumentMatchers.<Class<? extends Callback>>anyObject()))
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

    @Test
    public void shouldSubmitCallbacksWhenHttpCallbackHeaderIsPresentAndScriptCannotBeFilled()
            throws RestAuthException {

        //Given
        RestAuthCallbackHandlerManager manager =
                new RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory.getInstance());
        HttpServletRequest request = postRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = new HttpCallback("Authorization", "WWW-Authenticate:Negotiate", "401");
        ScriptTextOutputCallback script = new ScriptTextOutputCallback("p.set('skipKerberos', 'true');");
        given(request.getHeader("Authorization")).willReturn("Negotiate YIIB");

        //When
        JsonValue jsonCallbacks = manager.handleCallbacks(request, response, new Callback[]{httpCallback, script});

        //Then
        assertEquals(jsonCallbacks.size(), 0);
        assertEquals(httpCallback.getAuthorization(), "Negotiate YIIB");
    }

    @Test
    public void shouldSubmitNameValueOutputCallbackWhenHttpCallbackHeaderIsPresent() throws RestAuthException {

        //Given
        RestAuthCallbackHandlerManager manager =
                new RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory.getInstance());
        HttpServletRequest request = postRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = new HttpCallback("Authorization", "WWW-Authenticate:Negotiate", "401");
        NameValueOutputCallback nameValue = new NameValueOutputCallback("realm", "/");
        given(request.getHeader("Authorization")).willReturn("Negotiate YIIB");

        //When
        JsonValue jsonCallbacks = manager.handleCallbacks(request, response,
                new Callback[]{httpCallback, nameValue});

        //Then
        assertEquals(jsonCallbacks.size(), 0);
    }

    @Test
    public void shouldChallengeWhenHttpCallbackFollowsOutputCallback() throws RestAuthException {

        //Given
        RestAuthCallbackHandlerManager manager =
                new RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory.getInstance());
        HttpServletRequest request = postRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = new HttpCallback("Authorization", "WWW-Authenticate:Negotiate", "401");
        ScriptTextOutputCallback script = new ScriptTextOutputCallback("p.set('skipKerberos', 'true');");

        //When
        try {
            manager.handleCallbacks(request, response, new Callback[]{script, httpCallback});
            fail("Expected a 401 challenge for the missing Authorization header");
        } catch (RestAuthResponseException e) {
            //Then
            assertEquals(e.getStatusCode(), 401);
            assertEquals(e.getResponseHeaders().get("WWW-Authenticate"), "Negotiate");
        }
    }

    @Test
    public void shouldFillOtherCallbacksFromRequestWhenHttpCallbackHeaderIsPresent() throws RestAuthException {

        //Given
        RestAuthCallbackHandlerManager manager =
                new RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory.getInstance());
        HttpServletRequest request = postRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = new HttpCallback("Authorization", "WWW-Authenticate:Negotiate", "401");
        NameCallback nameCallback = new NameCallback("User Name:");
        ScriptTextOutputCallback script = new ScriptTextOutputCallback("p.set('skipKerberos', 'true');");
        given(request.getHeader("Authorization")).willReturn("Negotiate YIIB");
        given(request.getHeader("X-OpenAM-Username")).willReturn("demo");

        //When
        JsonValue jsonCallbacks = manager.handleCallbacks(request, response,
                new Callback[]{httpCallback, nameCallback, script});

        //Then
        assertEquals(jsonCallbacks.size(), 0);
        assertEquals(nameCallback.getName(), "demo");
    }

    @Test
    public void shouldRejectHttpCallbackNextToInputCallbackThatCannotBeFilled() throws RestAuthException {

        //Given
        RestAuthCallbackHandlerManager manager =
                new RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory.getInstance());
        HttpServletRequest request = postRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpCallback httpCallback = new HttpCallback("Authorization", "WWW-Authenticate:Negotiate", "401");
        PasswordCallback passwordCallback = new PasswordCallback("Password:", false);
        given(request.getHeader("Authorization")).willReturn("Negotiate YIIB");

        //When
        try {
            manager.handleCallbacks(request, response, new Callback[]{httpCallback, passwordCallback});
            fail("Expected a 400 for the PasswordCallback that cannot be filled from the request");
        } catch (RestAuthException e) {
            //Then
            assertEquals(e.getStatusCode(), 400);
            assertNull(passwordCallback.getPassword());
        }
    }

    private HttpServletRequest postRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getMethod()).willReturn("POST");
        return request;
    }
}
