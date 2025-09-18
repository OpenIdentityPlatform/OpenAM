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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines methods to update a PollingWaitCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthPollingWaitCallbackHandler extends AbstractRestAuthCallbackHandler<PollingWaitCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "PollingWaitCallback";
    private static final String WAIT_TIME_JSON_FIELD_NAME = "waitTime";

    @Override
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
                                        PollingWaitCallback callback) throws RestAuthResponseException {
        return false;
    }

    @Override
    public PollingWaitCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, PollingWaitCallback originalCallback) {
        return originalCallback;
    }

    @Override
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    @Override
    public JsonValue convertToJson(PollingWaitCallback callback, int index) {

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createJsonField(WAIT_TIME_JSON_FIELD_NAME, callback.getWaitTime()))
                .build();

        return jsonValue;
    }


    @Override
    public PollingWaitCallback convertFromJson(PollingWaitCallback callback, JsonValue jsonCallback) throws RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        // Nothing to do here as PollingWaitCallback is only used to send information to the client.

        return callback;
    }
}
