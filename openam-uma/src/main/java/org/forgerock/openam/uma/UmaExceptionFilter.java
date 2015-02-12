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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.routing.Filter;

/**
 * Filters the responses from the PermissionRequestEndpoint to ensure that all error responses are in the correct
 * format.
 *
 * @since 13.0.0
 */
public class UmaExceptionFilter extends Filter {

    /**
     * Constructs a new UmaExceptionFilter instance.
     *
     * @param next The Restlet instance to filter.
     */
    public UmaExceptionFilter(Restlet next) {
        setNext(next);
    }

    /**
     * Checks if an error response is being returned and translates the error into the format described by the
     * specification, https://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-error-response.
     *
     * @param request The request to handle.
     * @param response The response to update.
     */
    @Override
    protected void afterHandle(Request request, Response response) {
        if (response.getStatus().isError() && response.getEntity() == null) {
            Throwable throwable = response.getStatus().getThrowable();
            if (throwable instanceof UmaException) {
                UmaException exception = (UmaException) throwable;
                setExceptionResponse(response, exception.getStatusCode(), exception.getError());
            } else if (throwable instanceof OAuth2Exception) {
                OAuth2Exception exception = (OAuth2Exception) throwable;
                setExceptionResponse(response, exception.getStatusCode(), exception.getError());
            } else {
                setExceptionResponse(response, response.getStatus().getCode(), "server_error");
            }
        }
    }

    private void setExceptionResponse(Response response, int statusCode, String error) {
        Throwable throwable = response.getStatus().getThrowable();
        Map<String, String> responseBody = new HashMap<String, String>();
        responseBody.put("error", error);
        responseBody.put("error_description", throwable.getMessage());
        response.setEntity(new JacksonRepresentation<Map<String, String>>(responseBody));
        response.setStatus(new Status(statusCode, response.getStatus().getThrowable()));
    }
}
