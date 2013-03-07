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
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;

/**
 * Defines methods to update a PasswordCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthPasswordCallbackHandler implements RestAuthCallbackHandler<PasswordCallback> {

    private static final Debug logger = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "PasswordCallback";

    /**
     * Checks the request for the presence of a parameter name "password", if present and not an empty string then
     * sets this on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, PasswordCallback callback) {

        String password = request.getParameter("password");

        if (password == null || "".equals(password)) {
            logger.message("password not set in request.");
            return false;
        }

        callback.setPassword(password.toCharArray());
        return true;
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
    public JSONObject convertToJson(PasswordCallback callback) throws JSONException {

        String prompt = callback.getPrompt();
        char[] password = callback.getPassword();

        JSONObject jsonCallback = new JSONObject();
        jsonCallback.put("type", CALLBACK_NAME);

        JSONArray output = new JSONArray();

        JSONObject outputField = new JSONObject();
        outputField.put("name", "prompt");
        outputField.put("value", prompt == null ? "" : prompt);

        output.put(outputField);

        jsonCallback.put("output", output);

        JSONArray input = new JSONArray();

        JSONObject inputField = new JSONObject();
        inputField.put("name", "password");
        inputField.put("value", password == null ? "" : new String(password));

        input.put(inputField);

        jsonCallback.put("input", input);

        return jsonCallback;
    }

    /**
     * {@inheritDoc}
     */
    public PasswordCallback convertFromJson(PasswordCallback callback, JSONObject jsonCallback) throws JSONException {

        String type = jsonCallback.getString("type");
        if (!CALLBACK_NAME.equalsIgnoreCase(type)) {
            logger.message(MessageFormat.format("Method called with invalid callback, {0}.", type));
            throw new RestAuthException(Response.Status.BAD_REQUEST,
                    MessageFormat.format("Invalid Callback, {0}, for handler", type));
        }

        JSONArray input = jsonCallback.getJSONArray("input");

        for (int i = 0; i < input.length(); i++) {

            JSONObject inputField = input.getJSONObject(i);

            String name = inputField.getString("name");
            String value = inputField.getString("value");

            if ("password".equalsIgnoreCase(name)) {
                callback.setPassword(value.toCharArray());
            }
        }

        return callback;
    }
}
