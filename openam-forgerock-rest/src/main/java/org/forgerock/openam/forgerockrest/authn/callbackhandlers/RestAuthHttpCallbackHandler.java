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

import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;

/**
 * Defines methods to update a HttpCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthHttpCallbackHandler implements RestAuthCallbackHandler<HttpCallback> {

    private static final Debug logger = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "HttpCallback";

    /**
     * Checks the request for the presence of a header with the Authorization Header as define in the HttpCallBack,
     * if present and not an empty string then sets this on the Callback and returns true. Otherwise does nothing and
     * returns false.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response,  HttpCallback callback) {

        String httpAuthorization = request.getHeader(callback.getAuthorizationHeader());

        if (httpAuthorization == null || "".equals(httpAuthorization)) {
            logger.message("httpAuthorization not set in request.");
            return false;
        }

        callback.setAuthorization(httpAuthorization);
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
    public JSONObject convertToJson(HttpCallback callback) throws JSONException {

        String authorizationHeader = callback.getAuthorizationHeader();
        int negotiationCode = callback.getNegotiationCode();
        String negotiationHeaderName = callback.getNegotiationHeaderName();
        String negotiationHeaderValue = callback.getNegotiationHeaderValue();
        String authorization = callback.getAuthorization();

        JSONObject jsonCallback = new JSONObject();
        jsonCallback.put("type", CALLBACK_NAME);

        JSONArray output = new JSONArray();

        JSONObject outputField = new JSONObject();
        outputField.put("name", "authorizationHeader");
        outputField.put("value", authorizationHeader);

        output.put(outputField);

        outputField = new JSONObject();
        outputField.put("name", "negotiationCode");
        outputField.put("value", negotiationCode);

        output.put(outputField);

        outputField = new JSONObject();
        outputField.put("name", "negotiationHeaderName");
        outputField.put("value", negotiationHeaderName == null ? "" : negotiationHeaderName);

        output.put(outputField);

        outputField = new JSONObject();
        outputField.put("name", "negotiationHeaderValue");
        outputField.put("value", negotiationHeaderValue == null ? "" : negotiationHeaderValue);

        output.put(outputField);

        jsonCallback.put("output", output);

        JSONArray input = new JSONArray();

        JSONObject inputField = new JSONObject();
        inputField.put("name", "authorization");
        inputField.put("value", authorization == null ? "" : authorization);

        input.put(inputField);

        jsonCallback.put("input", input);

        return jsonCallback;

    }

    /**
     * {@inheritDoc}
     */
    public HttpCallback convertFromJson(HttpCallback callback, JSONObject jsonCallback) throws JSONException {

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

            if ("authorization".equalsIgnoreCase(name)) {
                callback.setAuthorization(value);
            }
        }

        return callback;
    }
}
