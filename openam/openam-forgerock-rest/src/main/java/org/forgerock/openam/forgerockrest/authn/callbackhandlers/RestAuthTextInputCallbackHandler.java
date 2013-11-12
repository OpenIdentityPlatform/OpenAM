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

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.security.auth.callback.TextInputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Defines methods to update a TextInputCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthTextInputCallbackHandler extends AbstractRestAuthCallbackHandler<TextInputCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "TextInputCallback";

    /**
     * TextInputCallback not supported to be updated from request headers.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            TextInputCallback callback) throws RestAuthCallbackHandlerResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public TextInputCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, TextInputCallback originalCallback) {
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
    public JsonValue convertToJson(TextInputCallback callback, int index) {

        String prompt = callback.getPrompt();
        String defaultText = callback.getDefaultText();
        String text = callback.getText();

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .add(createOutputField("prompt", prompt))
                .addLast(createOutputField("defaultText", defaultText))
                .array("input")
                .addLast(createInputField(index, text))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public TextInputCallback convertFromJson(TextInputCallback callback, JsonValue jsonCallback) {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 1) {
            throw new JsonException("JSON Callback does not include a input field");
        }

        JsonValue inputField = input.get(0);
        String value = inputField.get("value").asString();
        callback.setText(value);

        return callback;
    }
}
