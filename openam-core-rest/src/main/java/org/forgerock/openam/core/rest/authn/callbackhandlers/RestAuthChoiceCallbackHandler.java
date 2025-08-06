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
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.ChoiceCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines methods to update a ChoiceCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthChoiceCallbackHandler extends AbstractRestAuthCallbackHandler<ChoiceCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "ChoiceCallback";

    /**
     * ChoiceCallback not supported to be updated from request headers.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            ChoiceCallback callback) throws RestAuthResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public ChoiceCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, ChoiceCallback originalCallback) {
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
    public JsonValue convertToJson(ChoiceCallback callback, int index) {

        String prompt = callback.getPrompt();
        String[] choices = callback.getChoices();
        int defaultChoice = callback.getDefaultChoice();
        int[] selectedIndexes = callback.getSelectedIndexes();
        int selectedIndex = 0;
        if (selectedIndexes != null) {
            selectedIndex = selectedIndexes[0];
        }

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .add(createOutputField("prompt", prompt))
                .add(createOutputField("choices", choices))
                .addLast(createOutputField("defaultChoice", defaultChoice))
                .array("input")
                .addLast(createInputField(index, selectedIndex))
                .build();

        return jsonValue;

    }

    /**
     * {@inheritDoc}
     */
    public ChoiceCallback convertFromJson(ChoiceCallback callback, JsonValue jsonCallback) throws RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 1) {
            throw new JsonException("JSON Callback does not include a input field");
        }

        JsonValue inputField = input.get(0);
        int selectedIndex = toInteger(inputField.get("value"));

        callback.setSelectedIndex(selectedIndex);

        return callback;
    }

    /**
     * Try to get or parse JsonValue as int.
     *
     * @param value JsonValue that should contain a number or String that can be converted to int
     * @throws org.forgerock.json.JsonValueException if value doesn't represent an int
     * @return the int value
     */
    private int toInteger(JsonValue value) {

        if (value.isString()) {
            try {
                return Integer.parseInt(value.asString());
            } catch (NumberFormatException ex) {
                // ignore error and let call to value.asInteger throw a more informative exception
            }
        }

        return value.asInteger();
    }
}
