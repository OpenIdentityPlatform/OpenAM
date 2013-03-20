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
import org.forgerock.openam.forgerockrest.authn.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;

/**
 * This class contains common functionality for all of the RestAuthCallbackHandlers.
 */
public abstract class AbstractRestAuthCallbackHandler<T extends Callback> {

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    /**
     * Implemented here to provide the default behaviour of returning false if the request was made using GET.
     * RestAuthCallbackHandlers can override if required but should not do so unless absolutely necessary.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, JSONObject postBody, T callback, HttpMethod httpMethod)
            throws RestAuthCallbackHandlerResponseException {

        // If HttMethod is GET then by default callbacks should not be handled internally/
        if (HttpMethod.GET.equals(httpMethod)) {
            return false;
        }

        return doUpdateCallbackFromRequest(headers, request, response, postBody, callback);
    }

    /**
     * Uses the headers and request contents to update the Callback. If the Callback cannot be completed from the
     * headers and request or the headers and request do not contain the required information the method MUST return
     * false.
     *
     * This is for "zero page login" where the request contains all the required information to authenticate
     * successfully. In this case no callbacks will be sent back to the client, only the success or failure of the
     * authentication.
     *
     * @param headers The HttpHeaders from the request.
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse for the request.
     * @param postBody The POST body from the request.
     * @param callback The Callback to update with its required values from the headers and request.
     * @return Whether or not the Callback was successfully updated.
     * @throws RestAuthCallbackHandlerResponseException If one of the CallbackHandlers has its own response to be sent.
     */
    abstract boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, JSONObject postBody, T callback)
            throws RestAuthCallbackHandlerResponseException;

    /**
     * Creates a JSON input field for a callback.
     *
     * @param value The value of the field.
     * @return The JSON field object.
     * @throws JSONException If there is a problem creating the JSON object.
     */
    final JSONObject createInputField(String name, Object value) throws JSONException {
        return createJsonField(name, value);
    }

    /**
     * Creates a JSON output field for a callback.
     *
     * @param value The value of the field.
     * @return The JSON field object.
     * @throws JSONException If there is a problem creating the JSON object.
     */
    final JSONObject createOutputField(String name, Object value) throws JSONException {
        return createJsonField(name, value);
    }

    /**
     * Creates a JSON field for a callback.
     *
     * @param value The value of the field.
     * @return The JSON field object.
     * @throws JSONException If there is a problem creating the JSON object.
     */
    final JSONObject createJsonField(String name, Object value) throws JSONException {
        JSONObject field = new JSONObject();
        field.put("name", name == null ? "" : name);
        field.put("value", value == null ? "" : value);
        return field;
    }

    /**
     * Checks that the JSON callback being converted is of the same type as the CallbackHandler.
     *
     * @param callbackName The required name of the callback.
     * @param jsonCallback The JSON callback object.
     * @throws JSONException If there is a problem getting the type of the JSON callback.
     */
    final void validateCallbackType(String callbackName, JSONObject jsonCallback) throws JSONException {
        String type = jsonCallback.getString("type");
        if (!callbackName.equalsIgnoreCase(type)) {
            DEBUG.message(MessageFormat.format("Method called with invalid callback, {0}.", type));
            throw new RestAuthException(Response.Status.BAD_REQUEST,
                    MessageFormat.format("Invalid Callback, {0}, for handler", type));
        }
    }

    /**
     * Checks to see if the given JSON object has the specified attribute name.
     *
     * @param jsonObject The JSON object.
     * @param attributeName The attribute name to check the presence of.
     * @return If the JSON object contains the attribute name.
     */
    boolean isJsonAttributePresent(JSONObject jsonObject, String attributeName) {
        try {
            jsonObject.get(attributeName);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }
}
