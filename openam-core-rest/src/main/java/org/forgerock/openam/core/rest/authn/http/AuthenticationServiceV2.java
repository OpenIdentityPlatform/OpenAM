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

package org.forgerock.openam.core.rest.authn.http;

import static org.forgerock.json.JsonValue.*;

import javax.inject.Inject;
import java.util.Map;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.authn.RestAuthenticationHandler;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.util.Reject;

/**
 *
 */
public class AuthenticationServiceV2 extends AuthenticationServiceV1 {

    /**
     * Constructs an instance of the AuthenticationRestService.
     *
     * @param restAuthenticationHandler An instance of the RestAuthenticationHandler.
     */
    @Inject
    public AuthenticationServiceV2(RestAuthenticationHandler restAuthenticationHandler) {
        super(restAuthenticationHandler);
    }

    @Override
    protected Response handleErrorResponse(Request request, Status status, Exception exception) {
        Reject.ifNull(status);
        Response response = new Response(status);
        if (exception instanceof RestAuthResponseException) {
            final RestAuthResponseException authResponseException = (RestAuthResponseException)exception;
            for (Map.Entry<String, String> entry : authResponseException.getResponseHeaders().entrySet()) {
                response.getHeaders().add(entry.getKey(), entry.getValue());
            }
            response.setEntity(authResponseException.getJsonResponse().asMap());
            return response;

        } else if (exception instanceof RestAuthException) {
            final RestAuthException rae = (RestAuthException)exception;
            ResourceException cause = ResourceException.getException(rae.getStatusCode(), getLocalizedMessage(request, rae));

            if (rae.getFailureUrl() != null) {
                cause.setDetail(json(object(field("failureUrl", rae.getFailureUrl()))));
            }

            return createExceptionResponse(response, cause);

        } else if (exception == null) {
            return createExceptionResponse(response, ResourceException.getException(status.getCode()));

        } else {
            return createExceptionResponse(response, ResourceException.getException(status.getCode(), exception.getMessage(), exception));
        }
    }

    private Response createExceptionResponse(Response response, ResourceException exception) {
        final JsonValue jsonResponse;

        if (exception.getMessage() != null) {
            jsonResponse = exception.toJsonValue();
        } else {
            jsonResponse = ResourceException.getException(response.getStatus().getCode(), response.getStatus().getReasonPhrase()).toJsonValue();
        }
        Map<String, Object> mapRepresentation = jsonResponse.asMap();
        response.setEntity(mapRepresentation);
        return response;
    }
}
