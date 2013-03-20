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

import javax.security.auth.callback.TextOutputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Defines methods to convert a TextOutputCallback to a JSON representation.
 */
public class RestAuthTextOutputCallbackHandler extends AbstractRestAuthCallbackHandler<TextOutputCallback>
        implements RestAuthCallbackHandler<TextOutputCallback> {

    private static final String CALLBACK_NAME = "TextOutputCallback";

    /**
     * TextOutputCallback is used to send information to the client so there is nothing to update the callback with
     * so always returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, TextOutputCallback callback) throws RestAuthCallbackHandlerResponseException {

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public TextOutputCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
            JSONObject postBody, TextOutputCallback originalCallback) throws JSONException {
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
    public JSONObject convertToJson(TextOutputCallback callback, int index) throws JSONException {

        String message = callback.getMessage();
        int messageType = callback.getMessageType();

        JSONObject jsonCallback = new JSONObject();
        jsonCallback.put("type", CALLBACK_NAME);

        JSONArray output = new JSONArray();
        output.put(createOutputField("message", message));
        output.put(createOutputField("messageType", messageType));
        jsonCallback.put("output", output);

        return jsonCallback;
    }

    /**
     * {@inheritDoc}
     */
    public TextOutputCallback convertFromJson(TextOutputCallback callback, JSONObject jsonCallback)
            throws JSONException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        // Nothing to do here as TextOutputCallback is purely used to send information to the client.

        return callback;
    }
}
