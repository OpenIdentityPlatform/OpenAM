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
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines methods to update a NameCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthNameCallbackHandler extends AbstractRestAuthCallbackHandler<NameCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "NameCallback";
    private static final String USERNAME_HEADER_KEY = "X-OpenAM-Username";

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Checks the request for the presence of a header "X-OpenAM-Username", if set and not an empty string then
     * sets the header value on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            NameCallback callback) throws RestAuthResponseException {

        String username = RestUtils.getMimeHeaderValue(request, USERNAME_HEADER_KEY);

        if (StringUtils.isEmpty(username)) {
            DEBUG.message("username not set in request.");
            return false;
        }

        callback.setName(username);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public NameCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, NameCallback originalCallback) {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue convertToJson(NameCallback callback, int index) {

        String prompt = callback.getPrompt();
        
        //if no value provided, setting default
        String name = callback.getName() == null ? callback.getDefaultName() : callback.getName();

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createOutputField("prompt", prompt))
                .array("input")
                .addLast(createInputField(index, name))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public NameCallback convertFromJson(NameCallback callback, JsonValue jsonCallback) throws RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 1) {
            throw new JsonException("JSON Callback does not include a input field");
        }

        JsonValue inputField = input.get(0);
        String value = inputField.get("value").asString();
        callback.setName(value);

        return callback;
    }
}
