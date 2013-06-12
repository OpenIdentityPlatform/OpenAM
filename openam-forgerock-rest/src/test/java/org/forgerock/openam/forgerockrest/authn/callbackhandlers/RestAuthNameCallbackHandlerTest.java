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

import junit.framework.Assert;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RestAuthNameCallbackHandlerTest {

    private RestAuthCallbackHandler<NameCallback> restAuthNameCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthNameCallbackHandler = new RestAuthNameCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthNameCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(NameCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn("USERNAME");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, nameCallback, HttpMethod.POST);

        //Then
        verify(nameCallback).setName("USERNAME");
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenUsernameIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn(null);

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, nameCallback, HttpMethod.POST);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn("");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, nameCallback, HttpMethod.POST);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameCallback originalNameCallback = mock(NameCallback.class);

        //When
        NameCallback nameCallback = restAuthNameCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalNameCallback);

        //Then
        assertEquals(originalNameCallback, nameCallback);
    }

    @Test
    public void shouldConvertToJson() {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");

        //When
        JsonValue jsonObject = restAuthNameCallbackHandler.convertToJson(nameCallback, 1);

        //Then
        assertEquals("NameCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("output"));
        Assert.assertEquals(1, jsonObject.get("output").size());
        Assert.assertEquals("Enter username:", jsonObject.get("output").get(0).get("value").asString());
        assertNotNull(jsonObject.get("input"));
        Assert.assertEquals(1, jsonObject.get("input").size());
        Assert.assertEquals("", jsonObject.get("input").get(0).get("value").asString());
    }

    @Test
    public void shouldConvertFromJson() {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "USERNAME").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter username:").build())
                .put("type", "NameCallback")
                .build();

        //When
        NameCallback convertedNameCallback = restAuthNameCallbackHandler.convertFromJson(nameCallback,
                jsonNameCallback);

        //Then
        Assert.assertEquals(nameCallback, convertedNameCallback);
        Assert.assertEquals("Enter username:", convertedNameCallback.getPrompt());
        Assert.assertEquals("USERNAME", convertedNameCallback.getName());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "USERNAME").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter username:").build())
                .put("type", "PasswordCallback")
                .build();

        //When
        restAuthNameCallbackHandler.convertFromJson(nameCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");
        JsonValue jsonNameCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "USERNAME").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter username:").build())
                .put("type", "namecallback")
                .build();

        //When
        NameCallback convertedNameCallback = restAuthNameCallbackHandler.convertFromJson(nameCallback,
                jsonNameCallback);

        //Then
        Assert.assertEquals(nameCallback, convertedNameCallback);
        Assert.assertEquals("Enter username:", convertedNameCallback.getPrompt());
        Assert.assertEquals("USERNAME", convertedNameCallback.getName());
    }
}
