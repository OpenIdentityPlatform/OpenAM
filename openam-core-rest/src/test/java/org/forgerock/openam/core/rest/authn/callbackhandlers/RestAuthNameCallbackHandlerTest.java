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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.security.auth.callback.NameCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
        assertThat(callbackClassName).isEqualTo(NameCallback.class.getSimpleName());
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getHeader("X-OpenAM-Username")).willReturn("USERNAME");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(request, response, nameCallback);

        //Then
        verify(nameCallback).setName("USERNAME");
        assertThat(updated).isTrue();
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenUsernameIsNull()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn(null);

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(request, response, nameCallback);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertThat(updated).isFalse();
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        NameCallback nameCallback = mock(NameCallback.class);

        given(request.getParameter("username")).willReturn("");

        //When
        boolean updated = restAuthNameCallbackHandler.updateCallbackFromRequest(request, response, nameCallback);

        //Then
        verify(nameCallback, never()).setName(anyString());
        assertThat(updated).isFalse();
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        NameCallback originalNameCallback = mock(NameCallback.class);

        //When
        NameCallback nameCallback = restAuthNameCallbackHandler.handle(request, response, jsonPostBody,
                originalNameCallback);

        //Then
        assertThat(nameCallback).isEqualTo(originalNameCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        NameCallback nameCallback = new NameCallback("Enter username:");

        //When
        JsonValue jsonObject = restAuthNameCallbackHandler.convertToJson(nameCallback, 1);

        //Then
        assertThat(jsonObject).stringAt("type").isEqualTo("NameCallback");
        assertThat(jsonObject).hasArray("output").hasSize(1);
        assertThat(jsonObject.get("output").get(0)).stringAt("value").isEqualTo("Enter username:");
        assertThat(jsonObject).hasArray("input").hasSize(1);
        assertThat(jsonObject.get("input").get(0)).stringAt("value").isEqualTo("");
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

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
        assertThat(convertedNameCallback).isEqualTo(nameCallback);
        assertThat(convertedNameCallback.getPrompt()).isEqualTo("Enter username:");
        assertThat(convertedNameCallback.getName()).isEqualTo("USERNAME");
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

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
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

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
        assertThat(convertedNameCallback).isEqualTo(nameCallback);
        assertThat(convertedNameCallback.getPrompt()).isEqualTo("Enter username:");
        assertThat(convertedNameCallback.getName()).isEqualTo("USERNAME");
    }
}
