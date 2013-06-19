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

import org.forgerock.json.fluent.JsonValue;

import javax.security.auth.callback.Callback;

/**
 * Implementations of this interface define how to convert a Callback into and from a JSON representation.
 *
 * See the javadoc for the <code>convertToJson</code> method for details on the required JSON representation.
 *
 * @param <T> A class which implements the Callback interface.
 */
public interface JsonCallbackParser<T extends Callback> {

    /**
     * Returns the class name of the Callback the implementation of this interface is for.
     *
     * Should only return the simple name of the class not the FQDN.
     *
     * @return The name of the class this parser is for.
     */
    String getCallbackClassName();

    /**
     * Converts the given Callback into JSON format.
     *
     * The JSON representation of the Callback MUST be in the following form:
     * <p></p>
     * <code>
     * {
     *      type : "NameCallback",
     *      output : [
     *          {
     *              name : "prompt",
     *              value : "Enter User Name:"
     *          }
     *      ],
     *      input : [
     *          {
     *              key : "name",
     *              value : ""
     *          }
     *      ]
     * }
     * </code>
     * <p></p>
     *
     * The type field MUST be the name of the Callback class.
     * Output fields detail information that the client can use to display to the user.
     * Input fields detail information that need to be complete before the callbacks is submitted.
     *
     * @param callback The Callback to convert to JSON.
     * @param index the position of this callback in the returned structure.
     * @return The JSON representation of the Callback.
     */
    JsonValue convertToJson(T callback, int index);

    /**
     * Converts the JSONObject into a Callback, setting the values set in the JSONObject onto the given Callback.
     *
     * Method must set the appropriate values on the Callback parameter and return the same Callback parameter.
     * This is required because of the way the AuthContext handles submitting requirements (Callbacks).
     *
     * @param callback The Callback to set values from the JSONObject onto.
     * @param jsonObject The JSON representation of the Callback.
     * @return The same Callback as in the parameters with the required values set.
     */
    T convertFromJson(T callback, JsonValue jsonObject);
}
