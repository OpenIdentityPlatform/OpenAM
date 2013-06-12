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

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.share.RedirectCallbackHandler;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Defines methods to handle a RedirectCallback.
 */
public class RestAuthRedirectCallbackHandler extends AbstractRestAuthCallbackHandler<RedirectCallback>
        implements RestAuthCallbackHandler<RedirectCallback> {

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    private static final String CALLBACK_NAME = "RedirectCallback";

    private final RedirectCallbackHandler redirectCallbackHandler;

    public RestAuthRedirectCallbackHandler(RedirectCallbackHandler redirectCallbackHandler) {
        this.redirectCallbackHandler = redirectCallbackHandler;
    }

    /**
     * Redirects the client to the URL defined in the RedirectCallback.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request,
                                             HttpServletResponse response, JsonValue postBody, RedirectCallback callback, HttpMethod httpMethod) {

        try {
            String contextPath = request.getContextPath();
            String requestURI = request.getRequestURI();
            int contextPathEnd = requestURI.indexOf(contextPath) + contextPath.length();
            String returnUrl = requestURI.substring(contextPathEnd, requestURI.length() - 1);

            redirectCallbackHandler.handleRedirectCallback(request, response, callback,
                    AuthClientUtils.getServiceURI() + returnUrl);
        } catch (IOException e) {
            DEBUG.error("Failed to redirect to " + callback.getRedirectUrl());
            throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to redirect to "
                    + callback.getRedirectUrl());
        }

        return true;
    }

    /**
     * This method will never be called as the <code>updateCallbackFromRequest</code> method from
     * <code>AbstractRestAuthCallbackHandler</code> has been overridden.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
                                        JsonValue postBody, RedirectCallback callback) throws RestAuthCallbackHandlerResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public RedirectCallback handle(HttpHeaders headers, HttpServletRequest request, HttpServletResponse response,
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
     * Will throw a 501 RestAuthException with an UnsupportedOperationException as RedirectCallbacks cannot be
     * converted to JSON as they will immediately redirect the client to the URL defined in the Callback.
     *
     * {@inheritDoc}
     */
    public JsonValue convertToJson(RedirectCallback callback, int index) {
        throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR, new UnsupportedOperationException(
                "RedirectCallbacks cannot be converted to JSON"));
    }

    /**
     * Will throw a 501 RestAuthException withan UnsupportedOperationException as RedirectCallbacks cannot be converted
     * from JSON as they will immediately redirect the client to the URL defined in the Callback.
     *
     * {@inheritDoc}
     */
    public RedirectCallback convertFromJson(RedirectCallback callback, JsonValue jsonCallback) {
        throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR, new UnsupportedOperationException(
                "RedirectCallbacks cannot be converted from JSON"));
    }
}
