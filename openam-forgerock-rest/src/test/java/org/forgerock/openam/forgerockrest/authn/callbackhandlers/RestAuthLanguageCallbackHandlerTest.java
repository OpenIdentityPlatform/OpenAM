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

import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.LanguageCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
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
    public void shouldUpdateCallbackFromRequest() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        given(request.getParameter("localeLanguage")).willReturn("LANGUAGE");
        given(request.getParameter("localeCountry")).willReturn("COUNTRY");

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        verify(languageCallback).setLocale(eq(new Locale("LANGUAGE", "COUNTRY")));
        assertTrue(updated);
    }

    @Test
    public void shouldUpdateCallbackFromRequestWhenLocaleCountryIsNull() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        given(request.getParameter("localeLanguage")).willReturn("LANGUAGE");
        given(request.getParameter("localeCountry")).willReturn(null);

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        verify(languageCallback).setLocale(eq(new Locale("LANGUAGE")));
        assertTrue(updated);
    }

    @Test
    public void shouldUpdateCallbackFromRequestWhenLocaleCountryIsEmptyString() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        given(request.getParameter("localeLanguage")).willReturn("LANGUAGE");
        given(request.getParameter("localeCountry")).willReturn("");

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        verify(languageCallback).setLocale(eq(new Locale("LANGUAGE")));
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenLocaleLanguageIsNull() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        given(request.getParameter("localeLanguage")).willReturn(null);

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        verify(languageCallback, never()).setLocale(Matchers.<Locale>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenLocaleLanguageIsEmptyString() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        LanguageCallback languageCallback = mock(LanguageCallback.class);

        given(request.getParameter("localeLanguage")).willReturn("");

        //When
        boolean updated = restAuthLanguageCallbackHandler.updateCallbackFromRequest(headers, request, response,
                languageCallback);

        //Then
        verify(languageCallback, never()).setLocale(Matchers.<Locale>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldConvertToJsonWhenLocaleNotSet() throws JSONException {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();

        //When
        JSONObject jsonObject = restAuthLanguageCallbackHandler.convertToJson(languageCallback);

        //Then
        assertEquals(1, jsonObject.length());
        assertEquals("LanguageCallback", jsonObject.getString("type"));
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        languageCallback.setLocale(new Locale("LANGUAGE", "COUNTRY"));

        //When
        JSONObject jsonObject = restAuthLanguageCallbackHandler.convertToJson(languageCallback);

        //Then
        assertEquals(2, jsonObject.length());
        assertEquals("LanguageCallback", jsonObject.getString("type"));
        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(2, jsonObject.getJSONArray("input").length());
        assertEquals("localeLanguage", jsonObject.getJSONArray("input").getJSONObject(0).getString("name"));
        assertEquals("language", jsonObject.getJSONArray("input").getJSONObject(0).getString("value"));
        assertEquals("localeCountry", jsonObject.getJSONArray("input").getJSONObject(1).getString("name"));
        assertEquals("COUNTRY", jsonObject.getJSONArray("input").getJSONObject(1).getString("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JSONObject jsonLanguageCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "localeLanguage")
                                .put("value", "language"))
                        .put(new JSONObject()
                                .put("name", "localeCountry")
                                .put("value", "COUNTRY")))
                .put("type", "LanguageCallback");

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
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JSONObject jsonLanguageCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "localeLanguage")
                                .put("value", "language"))
                        .put(new JSONObject()
                                .put("name", "localeCountry")
                                .put("value", "COUNTRY")))
                .put("type", "PasswordCallback");

        //When
        restAuthLanguageCallbackHandler.convertFromJson(languageCallback, jsonLanguageCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        LanguageCallback languageCallback = new LanguageCallback();
        JSONObject jsonLanguageCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("name", "localeLanguage")
                                .put("value", "lAngUage"))
                        .put(new JSONObject()
                                .put("name", "localeCountry")
                                .put("value", "COuntRY")))
                .put("type", "lanGuagecalLback");

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
