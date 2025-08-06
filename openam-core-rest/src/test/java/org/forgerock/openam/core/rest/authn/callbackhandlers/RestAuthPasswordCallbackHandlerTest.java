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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.security.auth.callback.PasswordCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class RestAuthPasswordCallbackHandlerTest {

    private RestAuthCallbackHandler<PasswordCallback> restAuthPasswordCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthPasswordCallbackHandler = new RestAuthPasswordCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthPasswordCallbackHandler.getCallbackClassName();

        //Then
        assertThat(PasswordCallback.class.getSimpleName()).isEqualTo(callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getHeader("X-OpenAM-Password")).willReturn("PASSWORD");

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(request, response,
                passwordCallback);

        //Then
        verify(passwordCallback).setPassword("PASSWORD".toCharArray());
        assertThat(updated).isTrue();
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsNull()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getParameter("password")).willReturn(null);

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(request, response,
                passwordCallback);

        //Then
        verify(passwordCallback, never()).setPassword(Matchers.<char[]>anyObject());
        assertThat(updated).isFalse();
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenPasswordIsEmptyString()
            throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);

        given(request.getParameter("password")).willReturn("");

        //When
        boolean updated = restAuthPasswordCallbackHandler.updateCallbackFromRequest(request, response,
                passwordCallback);

        //Then
        verify(passwordCallback, never()).setPassword(Matchers.<char[]>anyObject());
        assertThat(updated).isFalse();
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        PasswordCallback originalPasswordCallback = mock(PasswordCallback.class);

        //When
        PasswordCallback passwordCallback = restAuthPasswordCallbackHandler.handle(request, response, jsonPostBody,
                originalPasswordCallback);

        //Then
        assertThat(passwordCallback).isEqualTo(originalPasswordCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);

        //When
        JsonValue jsonObject = restAuthPasswordCallbackHandler.convertToJson(passwordCallback, 1);

        //Then
        assertThat(jsonObject).stringAt("type").isEqualTo("PasswordCallback");
        assertThat(jsonObject).hasArray("output").hasSize(1);
        assertThat(jsonObject.get("output").get(0)).stringAt("value").isEqualTo("Enter password:");
        assertThat(jsonObject).hasArray("input").hasSize(1);
        assertThat(jsonObject.get("input").get(0)).stringAt("value").isEqualTo("");
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JsonValue jsonPasswordCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "PASSWORD").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter password:").build())
                .put("type", "PasswordCallback")
                .build();

        //When
        PasswordCallback convertedPasswordCallback = restAuthPasswordCallbackHandler.convertFromJson(passwordCallback,
                jsonPasswordCallback);

        //Then
        assertThat(passwordCallback).isEqualTo(convertedPasswordCallback);
        assertThat(convertedPasswordCallback.getPrompt()).isEqualTo("Enter password:");
        assertThat(new String(convertedPasswordCallback.getPassword())).isEqualTo("PASSWORD");
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JsonValue jsonPasswordCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "PASSWORD").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter password:").build())
                .put("type", "NameCallback")
                .build();

        //When
        restAuthPasswordCallbackHandler.convertFromJson(passwordCallback, jsonPasswordCallback);
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

        //Given
        PasswordCallback passwordCallback = new PasswordCallback("Enter password:", false);
        JsonValue jsonPasswordCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "PASSWORD").build())
                .array("output")
                    .addLast(JsonValueBuilder.jsonValue().put("value", "Enter password:").build())
                .put("type", "passwordcallback")
                .build();

        //When
        PasswordCallback convertedPasswordCallback = restAuthPasswordCallbackHandler.convertFromJson(passwordCallback,
                jsonPasswordCallback);

        //Then
        assertThat(convertedPasswordCallback).isEqualTo(passwordCallback);
        assertThat(convertedPasswordCallback.getPrompt()).isEqualTo("Enter password:");
        assertThat(new String(convertedPasswordCallback.getPassword())).isEqualTo("PASSWORD");
    }
}
