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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides validation for the OAuth2 token endpoint to ensure that the request is valid.
 *
 * @since 12.0.0
 */
public class TokenEndpointFilter extends OAuth2Filter {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    /**
     * Constructs a new TokenEndpointFilter.
     *
     * @param context The Restlet context.
     * @param resource The Restlet resource.
     */
    public TokenEndpointFilter(Context context, Restlet resource) {
        super(context, resource);
    }

    /**
     * Validates that the HTTP method on the request is POST.
     *
     * @param request {@inheritDoc}
     * @throws OAuth2RestletException {@inheritDoc}
     */
    @Override
    void validateMethod(Request request) throws OAuth2RestletException {
        if (!Method.POST.equals(request.getMethod())) {
            throw new OAuth2RestletException(405, "method_not_allowed", "Required Method: POST found: "
                    + request.getMethod().getName(), null);
        }
    }

    /**
     * Validates that the content type of the request is 'x-www-form-urlencoded'.
     *
     * @param request {@inheritDoc}
     * @throws InvalidRequestException {@inheritDoc}
     */
    @Override
    void validateContentType(Request request) throws InvalidRequestException {
        if (!(request.getEntity() == null || request.getEntity() instanceof EmptyRepresentation)
                && !MediaType.APPLICATION_WWW_FORM.equals(request.getEntity()
                .getMediaType())) {
            logger.error("Invalid Content Type for token endpoint");
            throw new InvalidRequestException("Invalid Content Type");
        }
    }
}
