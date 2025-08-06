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
 * Copyright 2013-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.MessageFormat;

/**
 * This class contains common functionality for all of the RestAuthCallbackHandlers.
 *
 * @param <T> Callback type.
 */
public abstract class AbstractRestAuthCallbackHandler<T extends Callback> implements RestAuthCallbackHandler<T> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    /**
     * Implemented here to provide the default behaviour of returning false if the request was made using GET.
     * RestAuthCallbackHandlers can override if required but should not do so unless absolutely necessary.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response, T callback)
            throws RestAuthException {

        // If Http Method is GET then by default callbacks should not be handled internally/
        if ("GET".equals(request.getMethod())) {
            return false;
        }

        return doUpdateCallbackFromRequest(request, response, callback);
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
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse for the request.
     * @param callback The Callback to update with its required values from the headers and request.
     * @return Whether or not the Callback was successfully updated.
     * @throws RestAuthResponseException If one of the CallbackHandlers has its own response to be sent.
     */
    abstract boolean doUpdateCallbackFromRequest(HttpServletRequest request,
            HttpServletResponse response, T callback) throws RestAuthResponseException;

    /**
     * Creates a JSON input field for a callback.
     *
     * @param index The index of the input field.
     * @param value The value of the field.
     * @return The JSON field object.
     */
    final Object createInputField(int index, Object value) {
        return createJsonField("IDToken" + index, value).getObject();
    }

    /**
     * Creates a JSON input field for a callback.
     *
     * @param index The index of the input field.
     * @param values The array value of the field.
     * @return The JSON field object.
     */
    final Object createInputField(int index, Object[] values) {
        return createJsonField("IDToken" + index, values).getObject();
    }

    /**
     * Creates a JSON input field for a callback.
     *
     * @param index The index of the input field.
     * @param postString A String to append to the name of the field.
     * @param value The value of the field.
     * @return The JSON field object.
     */
    final Object createInputField(int index, String postString, Object value) {
        return createJsonField("IDToken" + index + postString, value).getObject();
    }

    /**
     * Creates a JSON output field for a callback.
     *
     * @param name The name of the output field.
     * @param value The value of the field.
     * @return The JSON field object.
     */
    final Object createOutputField(String name, Object value) {
        return createJsonField(name, value).getObject();
    }

    /**
     * Creates a JSON output field for a callback.
     *
     * @param name The name of the output field.
     * @param values The array value of the field.
     * @return The JSON field object.
     */
    final Object createOutputField(String name, Object[] values) {
        return createJsonField(name, values).getObject();
    }

    /**
     * Creates a JSON field for a callback.
     *
     * @param name The name of the field.
     * @param value The value of the field.
     * @return The JSON field object.
     */
    final JsonValue createJsonField(String name, Object value) {
        return JsonValueBuilder.jsonValue()
                .put("name", name == null ? "" : name)
                .put("value", value == null ? "" : value)
                .build();
    }

    /**
     * Creates a JSON field for a callback.
     *
     * @param name The name of the field.
     * @param values The array value of the field.
     * @return The JSON field object.
     */
    final JsonValue createJsonField(String name, Object[] values) {
        JsonArray jsonArray = JsonValueBuilder.jsonValue()
                .put("name", name == null ? "" : name)
                .array("value");

        if (values != null) {
            for (Object value : values) {
                jsonArray.add(value);
            }
        }
        JsonObject jsonObject = jsonArray.build();

        return jsonObject.build();
    }

    /**
     * Checks that the JSON callback being converted is of the same type as the CallbackHandler.
     *
     * @param callbackName The required name of the callback.
     * @param jsonCallback The JSON callback object.
     */
    final void validateCallbackType(String callbackName, JsonValue jsonCallback) throws RestAuthException {
        String type = jsonCallback.get("type").asString();
        if (!callbackName.equalsIgnoreCase(type)) {
            DEBUG.message(MessageFormat.format("Method called with invalid callback, {0}.", type));
            throw new RestAuthException(ResourceException.BAD_REQUEST,
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
    boolean isJsonAttributePresent(JsonValue jsonObject, String attributeName) {
        if (jsonObject.get(attributeName).isNull()) {
            return false;
        }
        return true;
    }
}
