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

package org.forgerock.oauth2.restlet.resources;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

/**
 * Filters the responses from the ResourceSetRegistrationEndpoint to ensure that all error responses are in the correct
 * format.
 *
 * @since 13.0.0
 */
public class ResourceSetRegistrationExceptionFilter extends Filter {

    static final Map<String, String> UNSUPPORTED_METHOD_TYPE = singletonMap("error", "unsupported_method_type");
    static final Map<String, String> PRECONDITION_FAILED = singletonMap("error", "precondition_failed");
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Constructs a new ResourceSetRegistrationExceptionFilter instance.
     *
     * @param next The Restlet instance to filter.
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    public ResourceSetRegistrationExceptionFilter(Restlet next,
            JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
        setNext(next);
    }

    /**
     * Checks if an error response is being returned and translates the error into the format described by the
     * specification, https://tools.ietf.org/html/draft-hardjono-oauth-resource-reg-04#section-3.
     *
     * @param request The request to handle.
     * @param response The response to update.
     */
    @Override
    protected void afterHandle(Request request, Response response) {
        if (response.getStatus().isError() && response.getEntity() == null) {
            if (405 == response.getStatus().getCode()) {
                response.setEntity(jacksonRepresentationFactory.create(UNSUPPORTED_METHOD_TYPE));
            } else if (412 == response.getStatus().getCode()) {
                response.setEntity(jacksonRepresentationFactory.create(PRECONDITION_FAILED));
            } else if (response.getStatus().getThrowable() instanceof OAuth2Exception) {
                OAuth2Exception exception = (OAuth2Exception) response.getStatus().getThrowable();
                setExceptionResponse(response, exception.getStatusCode(), exception.getError());
            } else {
                setExceptionResponse(response, 500, "server_error");
            }
        }
    }

    private void setExceptionResponse(Response response, int statusCode, String error) {
        Throwable throwable = response.getStatus().getThrowable();
        Map<String, String> responseBody = new HashMap<String, String>();
        responseBody.put("error", error);
        responseBody.put("error_description", throwable.getMessage());
        response.setEntity(jacksonRepresentationFactory.create(responseBody));
        response.setStatus(new Status(statusCode, response.getStatus().getThrowable()));
    }
}
