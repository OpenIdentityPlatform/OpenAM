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

package org.forgerock.openam.forgerockrest.authn.restlet;

import org.forgerock.openam.forgerockrest.authn.RestAuthenticationHandler;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.util.Reject;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import javax.inject.Inject;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Base Restlet class for server-side resources. It acts as a wrapper to a given call,
 * including the incoming {@link org.restlet.Request} and the outgoing {@link org.restlet.Response}.
 *
 * @since 12.0.0
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
    protected Representation handleErrorResponse(Status status, Exception exception) {
        Reject.ifNull(status);

        if (exception instanceof RestAuthResponseException) {
            final RestAuthResponseException authResponseException = (RestAuthResponseException)exception;
            for (final String key : authResponseException.getResponseHeaders().keySet()) {
                addResponseHeader(key, authResponseException.getResponseHeaders().get(key));
            }
            getResponse().setStatus(status);
            return new JacksonRepresentation<Map>(authResponseException.getJsonResponse().asMap());

        } else if (exception instanceof RestAuthException) {
            final RestAuthException rae = (RestAuthException)exception;
            org.forgerock.json.resource.ResourceException cause =
                    org.forgerock.json.resource.ResourceException.getException(rae.getStatusCode(), rae.getMessage());

            if (rae.getFailureUrl() != null) {
                cause.setDetail(json(object(field("failureUrl", rae.getFailureUrl()))));
            }

            throw new ResourceException(rae.getStatusCode(), cause);

        } else if (exception == null) {
            throw new ResourceException(status);

        } else {
            throw new ResourceException(status, exception);
        }
    }
}
