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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.RedirectCallback;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class RestAuthRedirectCallbackHandlerTest {

    private RestAuthCallbackHandler<RedirectCallback> restAuthRedirectCallbackHandler;

    @BeforeClass
    public void setUp() {

        restAuthRedirectCallbackHandler = new RestAuthRedirectCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthRedirectCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(RedirectCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        RedirectCallback originalRedirectCallback = mock(RedirectCallback.class);

        //When
        RedirectCallback redirectCallback = restAuthRedirectCallbackHandler.handle(request, response, jsonPostBody,
                originalRedirectCallback);

        //Then
        Assert.assertEquals(originalRedirectCallback, redirectCallback);
    }

    @Test
    public void shouldFailConvertToJson() throws RestAuthException {

        //Given
        RedirectCallback redirectCallback = mock(RedirectCallback.class);

        given(redirectCallback.getRedirectUrl()).willReturn("REDIRECT_URL");
        given(redirectCallback.getMethod()).willReturn("REDIRECT_METHOD");
        given(redirectCallback.getRedirectData()).willReturn(Collections.emptyMap());

        //When
        JsonValue json = restAuthRedirectCallbackHandler.convertToJson(redirectCallback, 1);

        //Then
        assertThat(json.asMap()).hasSize(2);
        assertThat(json.get("type").asString()).isEqualTo("RedirectCallback");
        assertThat(json.get("output").asList()).hasSize(3);
        assertThat(json.get("output").get(0).get("name").asString()).isEqualTo("redirectUrl");
        assertThat(json.get("output").get(0).get("value").asString()).isEqualTo("REDIRECT_URL");
        assertThat(json.get("output").get(1).get("name").asString()).isEqualTo("redirectMethod");
        assertThat(json.get("output").get(1).get("value").asString()).isEqualTo("REDIRECT_METHOD");
        assertThat(json.get("output").get(2).get("name").asString()).isEqualTo("redirectData");
        assertThat(json.get("output").get(2).get("value").asMap()).hasSize(0);
    }

    @Test
    public void shouldFailToConvertFromJson() throws RestAuthException {

        //Given
        RedirectCallback redirectCallback = mock(RedirectCallback.class);
        JsonValue jsonValue = json(object(field("type", "RedirectCallback")));

        //When
        RedirectCallback redirectCb = restAuthRedirectCallbackHandler.convertFromJson(redirectCallback, jsonValue);

        //Then
        assertThat(redirectCb).isEqualTo(redirectCallback);
    }
}
