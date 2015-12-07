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
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import com.sun.identity.authentication.spi.RedirectCallback;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
        given(redirectCallback.getRedirectData()).willReturn(Collections.<String, String>emptyMap());

        //When
        JsonValue json = restAuthRedirectCallbackHandler.convertToJson(redirectCallback, 1);

        //Then
        assertThat(json.asMap()).hasSize(2);
        assertThat(json.get("type").asString()).isEqualTo("RedirectCallback");
        assertThat(json.get("output").asList()).hasSize(4);
        assertThat(json.get("output").get(0).get("name").asString()).isEqualTo("redirectUrl");
        assertThat(json.get("output").get(0).get("value").asString()).isEqualTo("REDIRECT_URL");
        assertThat(json.get("output").get(1).get("name").asString()).isEqualTo("redirectMethod");
        assertThat(json.get("output").get(1).get("value").asString()).isEqualTo("REDIRECT_METHOD");
        assertThat(json.get("output").get(2).get("name").asString()).isEqualTo("trackingCookie");
        assertThat(json.get("output").get(2).get("value").asBoolean()).isEqualTo(false);
        assertThat(json.get("output").get(3).get("name").asString()).isEqualTo("redirectData");
        assertThat(json.get("output").get(3).get("value").asMap()).hasSize(0);
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

    @Test
    public void shouldSerialiseToJsonCorrectly() throws Exception {
        // Given
        RedirectCallback redirectCallback = mock(RedirectCallback.class);
        final Map<String, String> redirectData = Collections.singletonMap("foo", "bar");
        given(redirectCallback.getRedirectUrl()).willReturn("REDIRECT_URL");
        given(redirectCallback.getMethod()).willReturn("REDIRECT_METHOD");
        given(redirectCallback.getRedirectData()).willReturn(redirectData);

        // When
        // Round-trip via Jackson to ensure actual JSON produced by CREST would be correct. OPENAM-7143.
        String json = JsonValueBuilder.getObjectMapper().writeValueAsString(
                restAuthRedirectCallbackHandler.convertToJson(redirectCallback, 1).getObject());
        JsonValue parsed = JsonValueBuilder.toJsonValue(json);

        // Then
        assertThat(parsed).stringAt("/type").isEqualTo("RedirectCallback");
        assertThat(parsed).hasArray("/output").hasSize(4);
        assertThat(parsed).hasObject("/output/0").containsExactly(entry("name", "redirectUrl"),
                entry("value", "REDIRECT_URL"));
        assertThat(parsed).hasObject("/output/1").containsExactly(entry("name", "redirectMethod"),
                entry("value", "REDIRECT_METHOD"));
        assertThat(parsed).hasObject("/output/2").containsExactly(entry("name", "trackingCookie"),
                entry("value", false));
        assertThat(parsed).hasObject("/output/3").containsExactly(entry("name", "redirectData"),
                entry("value", redirectData));
    }
}
