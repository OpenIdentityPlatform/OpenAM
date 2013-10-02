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

package org.forgerock.openam.forgerockrest.authn;

import com.google.inject.Singleton;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandler;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandlerResponseException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the converting of Callbacks to and from JSON representation.
 */
@Singleton
public class RestAuthCallbackHandlerManager {

    private static final Debug logger = Debug.getInstance("amAuthREST");

    private final RestAuthCallbackHandlerFactory restAuthCallbackHandlerFactory;

    /**
     * Constructs an instance of the RestAuthCallbackHandlerManager.
     *
     * @param restAuthCallbackHandlerFactory An instance of the RestAuthCallbackHandlerFactory.
     */
    @Inject
    public RestAuthCallbackHandlerManager(RestAuthCallbackHandlerFactory restAuthCallbackHandlerFactory) {
        this.restAuthCallbackHandlerFactory = restAuthCallbackHandlerFactory;
    }

    /**
     * Handles Callbacks by either updating them with their required values from the headers and request or
     * converting them to JSON representations to be sent back to the client.
     *
     * @param headers The HttpHeaders from the request.
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse from the request.
     * @param callbacks The Callbacks to handle.
     * @return A JSONArray of Callbacks or empty if the Callbacks have been updated from the headers and request.
     * @throws RestAuthCallbackHandlerResponseException If one of the CallbackHandlers has its own response to be sent.
     */
    public JsonValue handleCallbacks(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, Callback[] callbacks)
            throws RestAuthCallbackHandlerResponseException {

        List<JsonValue> jsonCallbacks = new ArrayList<JsonValue>();
        int callbackIndex = 0;
        // check if can be completed by headers and/or request
        // if so then attempt it and response true if successful
        boolean handledInternally = handleCallbacksInternally(headers, request, response, callbacks);

        // else or on false convert callback into json
        if (!handledInternally) {
            logger.message("Cannot handle callbacks internally. Converting to JSON instead.");
            for (Callback callback : callbacks) {
                callbackIndex++;
                RestAuthCallbackHandler restAuthCallbackHandler =
                        restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(callback.getClass());

                JsonValue jsonCallback = restAuthCallbackHandler.convertToJson(callback, callbackIndex);
                jsonCallbacks.add(jsonCallback);
            }
        }

        return new JsonValue(jsonCallbacks);
    }

    /**
     * Attempts to update the Callbacks from the headers and request. If the Callback cannot be completed from the
     * headers and request or the headers and request do not contain the required information the method returns
     * false.
     *
     * @param headers The HttpHeaders from the request.
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse from the request.
     * @param callbacks The Callbacks to update with their required values from the headers and request.
     * @return Whether or not the Callbacks were successfully updated.
     * @throws RestAuthCallbackHandlerResponseException If one of the CallbackHandlers has its own response to be sent.
     */
    private boolean handleCallbacksInternally(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, Callback[] callbacks)
            throws RestAuthCallbackHandlerResponseException {

        for (Callback callback : callbacks) {

            RestAuthCallbackHandler restAuthCallbackHandler =
                    restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(callback.getClass());

            if (!restAuthCallbackHandler.updateCallbackFromRequest(headers, request, response, callback)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handles the JSON representations of Callbacks, converting them back to Callbacks by setting the values from
     * the JSONArray to the original Callbacks passed in.
     *
     * The method sets the appropriate values on the Callbacks parameter and returns the same Callbacks
     * parameter. This is required because of the way the AuthContext handles submitting requirements (Callbacks).
     *
     * The JSON callbacks array must be in the same order as it was sent it, so it matches the order of the Callback
     * object array.
     *
     * @param originalCallbacks The Callbacks to set values from the JSONArray onto.
     * @param jsonCallbacks The JSON representation of the Callbacks.
     * @return The same Callbacks as in the parameters with the required values set.
     */
    public Callback[] handleJsonCallbacks(Callback[] originalCallbacks, JsonValue jsonCallbacks) {

        for (int j = 0; j < originalCallbacks.length; j++) {

            RestAuthCallbackHandler restAuthCallbackHandler =
                    restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(originalCallbacks[j].getClass());

            boolean foundParser = false;

            for (int i = 0; i < jsonCallbacks.size(); i++) {

                JsonValue jsonCallback = jsonCallbacks.get(i);

                if (restAuthCallbackHandler.getCallbackClassName().equals(jsonCallback.get("type").asString())) {
                    foundParser = true;
                    restAuthCallbackHandler.convertFromJson(originalCallbacks[j], jsonCallback);
                    break;
                }
            }

            if (!foundParser) {
                logger.error("Required callback not found in JSON response");
                throw new RestAuthException(Response.Status.BAD_REQUEST,
                        "Required callback not found in JSON response");
            }
        }

        return originalCallbacks;
    }

    /**
     * Handles the processing of the JSON given in the request and updates the Callback objects from it.
     *
     * This is for special circumstances where the JSON from the request does not contain a "callback" attribute,
     * where the <code>handleJsonCallbacks()</code> method should be used.
     *
     * @param headers The HttpHeaders from the request.
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse from the request.
     * @param originalCallbacks The Callbacks to set values from the JSONArray onto.
     * @param jsonRequestObject The JSON object that was sent in the POST of the request.
     * @return The updated originalCallbacks.
     */
    public Callback[] handleResponseCallbacks(HttpHeaders headers, HttpServletRequest request,
            HttpServletResponse response, Callback[] originalCallbacks, JsonValue jsonRequestObject) {

        for (Callback originalCallback : originalCallbacks) {

            RestAuthCallbackHandler restAuthCallbackHandler =
                    restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(originalCallback.getClass());

            restAuthCallbackHandler.handle(headers, request, response, jsonRequestObject, originalCallback);
        }

        return originalCallbacks;
    }
}
