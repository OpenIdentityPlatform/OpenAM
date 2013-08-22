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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.LanguageCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.util.Locale;

/**
 * Defines methods to update a LanguageCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthLanguageCallbackHandler extends AbstractRestAuthCallbackHandler<LanguageCallback>
        implements RestAuthCallbackHandler<LanguageCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "LanguageCallback";

    /**
     * Checks the request for the presence of a parameter name "localeLanguage" and "localeCountry", and uses them to
     * create a Locale instance and sets it on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, LanguageCallback callback) throws RestAuthCallbackHandlerResponseException {

        String localeLanguage = request.getParameter("localeLanguage");
        String localeCountry = request.getParameter("localeCountry");

        if (localeLanguage == null || "".equals(localeLanguage)) {
            DEBUG.message("localeLanguage not set in request.");
            return false;
        }

        callback.setLocale(createLocale(localeLanguage, localeCountry));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public LanguageCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, LanguageCallback originalCallback) {
        return originalCallback;
    }

    /**
     * Creates a Locale object.
     *
     * @param language The language.
     * @param country The country.
     * @return The Locale.
     */
    private Locale createLocale(String language, String country) {
        Locale locale;
        if (country != null) {
            locale = new Locale(language, country);
        } else {
            DEBUG.message("country not set. Only using localeLanguage");
            locale = new Locale(language);
        }

        return locale;
    }

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue convertToJson(LanguageCallback callback, int index) {

        Locale locale = callback.getLocale();

        JsonObject jsonObject = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME);

        if (locale != null) {
            jsonObject.array("input")
                    .add(createInputField(index, "Language", locale.getLanguage()))
                    .addLast(createInputField(index, "Country", locale.getCountry()));
        }

        return jsonObject.build();
    }

    /**
     * {@inheritDoc}
     */
    public LanguageCallback convertFromJson(LanguageCallback callback, JsonValue jsonCallback) {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 2) {
            throw new JsonException("JSON Callback does not include the required input fields");
        }

        String language = null;
        String country = null;

        for (int i = 0; i < input.size(); i++) {

            JsonValue inputField = input.get(i);

            String value = inputField.get("value").asString();

            if (i == 0) {
                language = value;
            } else {
                country = value;
            }
        }

        callback.setLocale(createLocale(language, country));

        return callback;
    }
}
