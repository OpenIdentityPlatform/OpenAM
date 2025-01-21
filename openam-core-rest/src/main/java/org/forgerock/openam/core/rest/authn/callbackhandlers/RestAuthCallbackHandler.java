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

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;

import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementations of this interface define how to update a Callback from the headers and request of a Rest call,
 * if possible, and the methods defined in <code>JsonCallbackParser</code>.
 *
 * @param <T> A class which implements the Callback interface.
 */
public interface RestAuthCallbackHandler<T extends Callback> extends JsonCallbackParser<T> {

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
     *          client.
     */
    boolean updateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            T callback) throws RestAuthException;

    /**
     * Handles the processing of the JSON given in the request and updates the Callback objects from it.
     *
     * This is for special circumstances where the JSON from the request does not contain a "callback" attribute,
     * where the <code>handleJsonCallbacks()</code> method should be used.
     *
     * @param request The HttpServletRequest from the request.
     * @param response The HttpServletResponse for the request.
     * @param postBody The POST body from the request.
     * @param originalCallback The original Callbacks to update.
     * @return The updated originalCallbacks.
     */
    T handle(HttpServletRequest request, HttpServletResponse response,
             JsonValue postBody, T originalCallback);
}
