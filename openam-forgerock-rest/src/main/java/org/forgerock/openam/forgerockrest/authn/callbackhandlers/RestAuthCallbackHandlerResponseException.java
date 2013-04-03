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

import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * This exception is thrown by any of the RestAuthCallbackHandlers for it to return its own Http Response.
 */
public class RestAuthCallbackHandlerResponseException extends Exception {

    private Response.StatusType responseStatus;
    private Map<String, String> responseHeaders;
    private JsonValue jsonResponse;

    /**
     * Constructs a RestAuthCallbackHandlerResponseException.
     *
     * @param responseStatus The Http Status of the response.
     * @param responseHeaders A Map of the header key value pairs for the response.
     * @param jsonResponse The JSON object for the response body.
     */
    public RestAuthCallbackHandlerResponseException(Response.StatusType responseStatus,
            Map<String, String> responseHeaders, JsonValue jsonResponse) {
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
        this.jsonResponse = jsonResponse;
    }

    /**
     * Gets the Http Status of the response.
     *
     * @return The Http response status.
     */
    public Response.StatusType getResponseStatus() {
        return responseStatus;
    }

    /**
     * Gets the Map of header key value pairs.
     *
     * @return response headers.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Gets the JSON object for the response body.
     *
     * @return The response JSON body.
     */
    public JsonValue getJsonResponse() {
        return jsonResponse;
    }
}
