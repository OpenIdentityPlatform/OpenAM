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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;

/**
 * Handles exceptions from the UMA spec endpoints to ensure that all error responses are in the correct
 * format.
 *
 * @since 13.0.0
 */
public class UmaExceptionHandler {

    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    @Inject
    public UmaExceptionHandler(JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Checks if an error response is being returned and translates the error into the format described by the
     * specification, https://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-error-response.
     *
     * @param response The response to update.
     * @param throwable The throwable
     */
    protected void handleException(Response response, Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause instanceof UmaException) {
            UmaException exception = (UmaException) cause;
            setExceptionResponse(response, cause, exception.getStatusCode(), exception.getError(),
                    exception.getDetail());
        } else if (cause instanceof OAuth2Exception) {
            OAuth2Exception exception = (OAuth2Exception) cause;
            setExceptionResponse(response, cause, exception.getStatusCode(), exception.getError(), null);
        } else {
            setExceptionResponse(response, throwable, response.getStatus().getCode(), "server_error", null);
        }
    }

    private void setExceptionResponse(Response response, Throwable throwable, int statusCode, String error,
            JsonValue detail) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", error);
        responseBody.put("error_description", throwable.getMessage());
        if (detail != null) {
            responseBody.putAll(detail.asMap());
        }
        response.setEntity(jacksonRepresentationFactory.create(responseBody));
        response.setStatus(new Status(statusCode, response.getStatus().getThrowable()));
    }
}
