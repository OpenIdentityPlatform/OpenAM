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
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CacheDirective;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.routing.Filter;
import org.restlet.util.Series;

import java.util.Map;

/**
 * Provides validation for OAuth2 endpoints to ensure that the request is valid for the endpoint being requested.
 *
 * @since 12.0.0
 */
public abstract class OAuth2Filter extends Filter {

    /**
     * Constructs a new OAuth2Filter.
     *
     * @param context The Restlet context.
     * @param restlet The Restlet resource.
     */
    public OAuth2Filter(Context context, Restlet restlet) {
        super(context);
        setNext(restlet);
    }

    /**
     * Validates that the request is valid for the OAuth2 endpoint being requested.
     * <br/>
     * Will also add no cache headers to the response.
     *
     * @param request The Restlet request.
     * @param response The Restlet response.
     * @return {@link Filter#CONTINUE} if the request is valid and processing should continue.
     */
    @Override
    protected int beforeHandle(Request request, Response response) {
        try {
            validateMethod(request);
            validateContentType(request);
        } catch (OAuth2RestletException e) {
            response.setStatus(e.getStatus());
            response.setEntity(new JacksonRepresentation<Map>(e.asMap()));
        } catch (InvalidRequestException e) {
            final OAuth2RestletException ex = new OAuth2RestletException(e.getStatusCode(), e.getError(),
                    e.getMessage(), null);
            response.setStatus(ex.getStatus());
            response.setEntity(new JacksonRepresentation<Map>(ex.asMap()));
        }
        // -------------------------------------
        // Add Cache-Control: no-store
        // Pragma: no-cache
        // -------------------------------------
        response.getCacheDirectives().add(CacheDirective.noStore());
        Series<Header> additionalHeaders =
                (Series<Header>) response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders == null) {
            additionalHeaders = new Series<Header>(Header.class);
            response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
        }
        additionalHeaders.add(HeaderConstants.HEADER_PRAGMA, HeaderConstants.CACHE_NO_CACHE);

        return super.beforeHandle(request, response);
    }

    /**
     * Validates that the HTTP method on the request is valid for the OAuth2 endpoint being requested.
     *
     * @param request The Restlet request.
     * @throws OAuth2RestletException If the HTTP method is not valid for the endpoint.
     */
    abstract void validateMethod(Request request) throws OAuth2RestletException;

    /**
     * Validates that the content type on the request is valid for the OAuth2 endpoint being requested.
     *
     * @param request The Restlet request.
     * @throws OAuth2RestletException If the content type is not valid for the endpoint.
     */
    abstract void validateContentType(Request request) throws InvalidRequestException;
}
