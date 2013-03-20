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

import javax.security.auth.callback.TextInputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Defines methods to update a TextInputCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthTextInputCallbackHandler extends AbstractRestAuthCallbackHandler<TextInputCallback>
        implements RestAuthCallbackHandler<TextInputCallback> {

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "TextInputCallback";

    /**
     * Checks the request for the presence of a parameter name "text", if present and not an empty string then
     * sets this on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, TextInputCallback callback) throws RestAuthCallbackHandlerResponseException {

        String text = request.getParameter("text");

        if (text == null || "".equals(text)) {
            DEBUG.message("text not set in request.");
            return false;
        }

        callback.setText(text);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public TextInputCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, TextInputCallback originalCallback) throws JSONException {
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
    public JSONObject convertToJson(TextInputCallback callback, int index) throws JSONException {

        String prompt = callback.getPrompt();
        String defaultText = callback.getDefaultText();
        String text = callback.getText();

        JSONObject jsonCallback = new JSONObject();
        jsonCallback.put("type", CALLBACK_NAME);

        JSONArray output = new JSONArray();
        output.put(createOutputField("prompt", prompt));
        output.put(createOutputField("defaultText", defaultText));
        jsonCallback.put("output", output);

        JSONArray input = new JSONArray();
        input.put(createInputField("IDToken" + index, text));
        jsonCallback.put("input", input);

        return jsonCallback;
    }

    /**
     * {@inheritDoc}
     */
    public TextInputCallback convertFromJson(TextInputCallback callback, JSONObject jsonCallback)
            throws JSONException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JSONArray input = jsonCallback.getJSONArray("input");

        if (input.length() != 1) {
            throw new JSONException("JSON Callback does not include a input field");
        }

        JSONObject inputField = input.getJSONObject(0);
        String value = inputField.getString("value");
        callback.setText(value);

        return callback;
    }
}
