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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Defines methods to update a PasswordCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthPasswordCallbackHandler extends AbstractRestAuthCallbackHandler<PasswordCallback>
        implements RestAuthCallbackHandler<PasswordCallback> {

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "PasswordCallback";

    /**
     * Checks the request for the presence of a parameter name "password", if present and not an empty string then
     * sets this on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
                                        JsonValue postBody, PasswordCallback callback) throws RestAuthCallbackHandlerResponseException {

        String password = request.getParameter("password");

        if (password == null || "".equals(password)) {
            DEBUG.message("password not set in request.");
            return false;
        }

        callback.setPassword(password.toCharArray());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public PasswordCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
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
        char[] password = callback.getPassword();
        String passwordString;
        if (password == null) {
            passwordString = "";
        } else {
            passwordString = new String(password);
        }

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createOutputField("prompt", prompt))
                .array("input")
                .addLast(createInputField(index, passwordString))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public PasswordCallback convertFromJson(PasswordCallback callback, JsonValue jsonCallback) {

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
