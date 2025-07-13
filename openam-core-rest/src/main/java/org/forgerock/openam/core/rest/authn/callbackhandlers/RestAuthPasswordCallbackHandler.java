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
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines methods to update a PasswordCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthPasswordCallbackHandler extends AbstractRestAuthCallbackHandler<PasswordCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "PasswordCallback";
    private static final String PASSWORD_HEADER_KEY = "X-OpenAM-Password";

    /**
     * Checks the request for the presence of a header "X-OpenAM-Password", if set and not an empty string then
     * sets the header value on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            PasswordCallback callback) throws RestAuthResponseException {

        String password = RestUtils.getMimeHeaderValue(request, PASSWORD_HEADER_KEY);

        if (StringUtils.isEmpty(password)) {
            DEBUG.message("password not set in request.");
            return false;
        }

        callback.setPassword(password.toCharArray());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public PasswordCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, PasswordCallback originalCallback) {
        return originalCallback;
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
    public JsonValue convertToJson(PasswordCallback callback, int index) {

        String prompt = callback.getPrompt();

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createOutputField("prompt", prompt))
                .array("input")
                .addLast(createInputField(index, ""))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public PasswordCallback convertFromJson(PasswordCallback callback, JsonValue jsonCallback) throws RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 1) {
            throw new JsonException("JSON Callback does not include a input field");
        }

        JsonValue inputField = input.get(0);
        String value = inputField.get("value").asString();
        callback.setPassword(value.toCharArray());

        return callback;
    }
}
