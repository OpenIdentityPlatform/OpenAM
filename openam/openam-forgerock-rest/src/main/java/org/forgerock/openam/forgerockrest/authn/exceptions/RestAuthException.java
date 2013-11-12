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

package org.forgerock.openam.forgerockrest.authn.exceptions;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This exception is designed to be thrown from RESTful authentication calls when an error occurs.
 */
public class RestAuthException extends RuntimeException {

    private Response.Status responseStatus;
    private int statusCode;

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param errorMessage The error message relating to the exception.
     */
    public RestAuthException(Response.Status responseStatus, String errorMessage) {
        super(errorMessage);
        this.responseStatus = responseStatus;
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param errorMessage The error message relating to the exception.
     * @param throwable The cause of the exception.
     */
    public RestAuthException(Response.Status responseStatus, String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
        this.responseStatus = responseStatus;
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param throwable The cause of the exception.
     */
    public RestAuthException(Response.Status responseStatus, Throwable throwable) {
        this(responseStatus, throwable.getLocalizedMessage(), throwable);
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param errorMessage The error message relating to the exception.
     */
    public RestAuthException(int responseStatus, String errorMessage) {
        super(errorMessage);
        statusCode = responseStatus;
    }

    /**
     * Constructs a RestAuthException.
     *
     * @param responseStatus The HTTP response to code to send back to the client.
     * @param throwable The cause of the exception.
     */
    public RestAuthException(int responseStatus, Throwable throwable) {
        super(throwable);
        statusCode = responseStatus;
    }

    /**
     * Creates a JAX-RS Response object with the HTTP response code the exception was created with and the error
     * message as a JSON string in the body.
     *
     * @return A JAX-RS Response object.
     */
    public Response getResponse() {
        return getResponse(null);
    }

    /**
     * Creates a JAX-RS Response object with the HTTP response code the exception was created with and the error
     * message as a JSON string in the body, including the given failureUrl, if not null.
     *
     * @param failureUrl The failureUrl for the request.
     * @return A JAX-RS Response object.
     */
    public Response getResponse(String failureUrl) {


        if (responseStatus != null) {
            statusCode = responseStatus.getStatusCode();
        }

        Response.ResponseBuilder responseBuilder = Response.status(statusCode);

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("errorMessage", getLocalizedMessage())
                .build();
        if (failureUrl != null) {
            jsonValue.put("failureUrl", failureUrl);
        }
        responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
        responseBuilder.entity(jsonValue.toString());

        return responseBuilder.build();
    }
}
