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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedGrantTypeException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Finds the correct OAuth2 endpoint handler based on the specified grant type in the request.
 *
 * @since 11.0.0
 */
public class OAuth2FlowFinder extends Finder {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2RequestFactory<Request> requestFactory;
    private final Map<String, Finder> endpointClasses;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new OAuth2FlowFinder.
     *
     * @param context The Restlet context.
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param endpointClasses The endpoint handlers for the OAuth2 token endpoints.
     */
    public OAuth2FlowFinder(Context context, OAuth2RequestFactory<Request> requestFactory,
            ExceptionHandler exceptionHandler, Map<String, Finder> endpointClasses) {
        super(context);
        this.requestFactory = requestFactory;
        this.exceptionHandler = exceptionHandler;
        this.endpointClasses = new ConcurrentHashMap<String, Finder>(endpointClasses);
    }

    /**
     * Creates a new instance of the handler for the correct OAuth2 endpoint based from the grant type specified in
     * the requests query parameters.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @return {@inheritDoc}
     */
    public ServerResource create(Request request, Response response) {

        final OAuth2Request oAuth2Request = requestFactory.create(request);

        final String grantType = oAuth2Request.getParameter("grant_type");
        if (isEmpty(grantType)) {
            logger.error("Type is not set");
            return new ErrorResource(exceptionHandler, new InvalidRequestException("Grant type is not set"));
        }

        Finder finder = endpointClasses.get(grantType);
        if (finder == null) {
            logger.error("Unsupported grant type: Type is not supported: " + grantType);
            return new ErrorResource(exceptionHandler,
                    new UnsupportedGrantTypeException("Grant type is not supported: " + grantType));
        }

        try {
            return finder.create(request, response);
        } catch (Exception e) {
            logger.warn("Exception while instantiating the target server resource.", e);
            return new ErrorResource(exceptionHandler, new ServerException(e.getMessage()));
        }
    }
}
