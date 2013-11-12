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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.LanguageCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RestAuthLanguageCallbackHandlerTest {

    private RestAuthCallbackHandler<LanguageCallback> restAuthLanguageCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthLanguageCallbackHandler = new RestAuthLanguageCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthLanguageCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(LanguageCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        assertFalse(updated);
    }

    @Test
    public void shouldConvertToJsonWhenLocaleNotSet() {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();

        //When
        JsonValue jsonObject = restAuthLanguageCallbackHandler.convertToJson(languageCallback, 1);

        //Then
        assertEquals(1, jsonObject.size());
        assertEquals("LanguageCallback", jsonObject.get("type").asString());
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        LanguageCallback originalLanguageCallback = mock(LanguageCallback.class);

        //When
        LanguageCallback languageCallback = restAuthLanguageCallbackHandler.handle(headers, request,
                response, jsonPostBody, originalLanguageCallback);

        //Then
        assertEquals(originalLanguageCallback, languageCallback);
    }

    @Test
    public void shouldConvertToJson() {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        languageCallback.setLocale(new Locale("LANGUAGE", "COUNTRY"));

        //When
        JsonValue jsonObject = restAuthLanguageCallbackHandler.convertToJson(languageCallback, 1);

        //Then
        assertEquals(2, jsonObject.size());
        assertEquals("LanguageCallback", jsonObject.get("type").asString());
        assertNotNull(jsonObject.get("input"));
        assertEquals(2, jsonObject.get("input").size());
        assertEquals("language", jsonObject.get("input").get(0).get("value").asString());
        assertEquals("COUNTRY", jsonObject.get("input").get(1).get("value").asString());
    }

    @Test
    public void shouldConvertFromJson() {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JsonValue jsonLanguageCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .add(JsonValueBuilder.jsonValue().put("value", "language").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "COUNTRY").build())
                .put("type", "LanguageCallback")
                .build();

        //When
        LanguageCallback convertedLanguageCallback = restAuthLanguageCallbackHandler.convertFromJson(
                languageCallback, jsonLanguageCallback);

        //Then
        assertEquals(languageCallback, convertedLanguageCallback);
        Assert.assertNotNull(convertedLanguageCallback.getLocale());
        assertEquals("language", convertedLanguageCallback.getLocale().getLanguage());
        assertEquals("COUNTRY", convertedLanguageCallback.getLocale().getCountry());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JsonValue jsonLanguageCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .add(JsonValueBuilder.jsonValue().put("value", "language").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "COUNTRY").build())
                .put("type", "PasswordCallback")
                .build();

        //When
        restAuthLanguageCallbackHandler.convertFromJson(languageCallback, jsonLanguageCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JsonValue jsonLanguageCallback = JsonValueBuilder.jsonValue()
                .array("input")
                    .add(JsonValueBuilder.jsonValue().put("value", "lAngUage").build())
                    .addLast(JsonValueBuilder.jsonValue().put("value", "COuntRY").build())
                .put("type", "lanGuagecalLback")
                .build();

        //When
        LanguageCallback convertedLanguageCallback = restAuthLanguageCallbackHandler.convertFromJson(
                languageCallback, jsonLanguageCallback);

        //Then
        assertEquals(languageCallback, convertedLanguageCallback);
        Assert.assertNotNull(convertedLanguageCallback.getLocale());
        assertEquals("language", convertedLanguageCallback.getLocale().getLanguage());
        assertEquals("COUNTRY", convertedLanguageCallback.getLocale().getCountry());
    }
}
