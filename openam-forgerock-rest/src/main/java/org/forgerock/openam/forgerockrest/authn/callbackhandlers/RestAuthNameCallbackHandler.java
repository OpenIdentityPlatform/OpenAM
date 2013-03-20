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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Defines methods to update a NameCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthNameCallbackHandler extends AbstractRestAuthCallbackHandler<NameCallback>
        implements RestAuthCallbackHandler<NameCallback> {

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "NameCallback";

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Checks the request for the presence of a parameter name "username", if present and not an empty string then
     * sets this on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, NameCallback callback) throws RestAuthCallbackHandlerResponseException {

        String username = request.getParameter("username");

        if (username == null || "".equals(username)) {
            DEBUG.message("username not set in request.");
            return false;
        }

        callback.setName(username);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public NameCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, NameCallback originalCallback) throws JSONException {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public JSONObject convertToJson(NameCallback callback, int index) throws JSONException {

        String prompt = callback.getPrompt();
        String name = callback.getName();

        JSONObject jsonCallback = new JSONObject();
        jsonCallback.put("type", CALLBACK_NAME);

        JSONArray output = new JSONArray();
        output.put(createOutputField("prompt", prompt));
        jsonCallback.put("output", output);

        JSONArray input = new JSONArray();
        input.put(createInputField("IDToken" + index, name));
        jsonCallback.put("input", input);

        return jsonCallback;
    }

    /**
     * {@inheritDoc}
     */
    public NameCallback convertFromJson(NameCallback callback, JSONObject jsonCallback) throws JSONException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JSONArray input = jsonCallback.getJSONArray("input");

        if (input.length() != 1) {
            throw new JSONException("JSON Callback does not include a input field");
        }

        JSONObject inputField = input.getJSONObject(0);
        String value = inputField.getString("value");
        callback.setName(value);

        return callback;
    }
}
