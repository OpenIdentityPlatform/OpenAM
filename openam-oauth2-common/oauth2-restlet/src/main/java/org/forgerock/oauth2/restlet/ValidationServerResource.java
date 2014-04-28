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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenInfoService;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Handles requests to the OAuth2 tokeninfo endpoint for retrieving information about the provided token.
 *
 * @since 11.0.0
 */
public class ValidationServerResource extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenInfoService tokenInfoService;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new ValidationServerResource.
     *
     * @param requestFactory An instance of the ValidationServerResource.
     * @param tokenInfoService An instance of the TokenInfoService.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public ValidationServerResource(OAuth2RequestFactory<Request> requestFactory, TokenInfoService tokenInfoService,
            ExceptionHandler exceptionHandler) {
        this.requestFactory = requestFactory;
        this.tokenInfoService = tokenInfoService;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Handles GET requests to the OAuth2 tokeninfo endpoint for retrieving information about the provided token.
     *
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException
     */
    @Get
    public Representation validate() throws OAuth2RestletException {
        logger.trace("In Validator resource");

        final OAuth2Request request = requestFactory.create(getRequest());
        try {
            final JsonValue tokenInfo = tokenInfoService.getTokenInfo(request);

            // Sets the no-store Cache-Control header
            getResponse().getCacheDirectives().add(CacheDirective.noCache());
            getResponse().getCacheDirectives().add(CacheDirective.noStore());
            return new JacksonRepresentation<Map<String, Object>>(tokenInfo.asMap());

        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}
