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
 * Copyright 2018 Open Identity Community.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.callbackhandlers;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import com.sun.identity.authentication.callbacks.NameValueOutputCallback;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
<<<<<<< HEAD
 * Defines methods to convert a NameValueOutputCallback to a JSON representation.
=======
 * Defines methods to convert a TextOutputCallback to a JSON representation.
>>>>>>> upstream/master
 */
public class RestAuthNameValueOutputCallbackHandler extends AbstractRestAuthCallbackHandler<NameValueOutputCallback>  {

    private static final String CALLBACK_NAME = "NameValueOutputCallback";

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public NameValueOutputCallback handle(HttpServletRequest request, HttpServletResponse response, JsonValue postBody,
			NameValueOutputCallback originalCallback) {
		return originalCallback;
	}
    /**
     * {@inheritDoc}
     */
	@Override
	public JsonValue convertToJson(NameValueOutputCallback callback, int index) throws RestAuthException {
        final String name = callback.getName();
        final String value = callback.getValue();

        final JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(JsonValueBuilder.jsonValue().put("name", name).put("value", value).build())
                .build();

        return jsonValue;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public NameValueOutputCallback convertFromJson(NameValueOutputCallback callback, JsonValue jsonCallback)
			throws RestAuthException {
		validateCallbackType(CALLBACK_NAME, jsonCallback);
		// Nothing to do here as NameValueOutputCallback is purely used to send information to the client.
	    return callback;
	}

	 /**
     * NameValueOutputCallback is used to send information to the client so there is nothing to update the callback with
     * so always returns false.
     *
     * {@inheritDoc}
     */
	@Override
	boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
			NameValueOutputCallback callback) throws RestAuthResponseException {
		return false;
	}
}
