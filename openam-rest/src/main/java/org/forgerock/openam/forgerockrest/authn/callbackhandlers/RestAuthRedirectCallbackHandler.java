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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.RedirectCallback;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.forgerock.json.fluent.JsonValue.*;

/**
 * Defines methods to handle a RedirectCallback.
 */
public class RestAuthRedirectCallbackHandler extends AbstractRestAuthCallbackHandler<RedirectCallback> {

    private static final String CALLBACK_NAME = "RedirectCallback";

    /**
     * This method will never be called as the <code>updateCallbackFromRequest</code> method from
     * <code>AbstractRestAuthCallbackHandler</code> has been overridden.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            RedirectCallback callback) throws RestAuthResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public RedirectCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, RedirectCallback originalCallback) {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Converts the {@code RedirectCallback} into a JSON representation.
     *
     * {@inheritDoc}
     */
    public JsonValue convertToJson(RedirectCallback callback, int index) throws RestAuthException {

        JsonValue callbacksJson = json(array(
                createOutputField("redirectUrl", callback.getRedirectUrl()),
                createOutputField("redirectMethod", callback.getMethod())));

        JsonValue jsonValue = json(object(
                field("type", CALLBACK_NAME),
                field("output", callbacksJson)
        ));

        if (callback.getRedirectData() != null) {
            callbacksJson.add(createOutputField("redirectData", callback.getRedirectData()));
        }

        return jsonValue;
    }

    /**
     * Validates that the JSON is valid for the callback type but does nothing more.
     *
     * {@inheritDoc}
     */
    public RedirectCallback convertFromJson(RedirectCallback callback, JsonValue jsonCallback) throws RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        // Nothing to do here as RedirectCallback is purely used to send redirect information to the client.

        return callback;
    }
}
