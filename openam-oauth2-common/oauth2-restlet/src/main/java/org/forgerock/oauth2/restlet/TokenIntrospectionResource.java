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

package org.forgerock.oauth2.restlet;

import javax.inject.Inject;

import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenIntrospectionService;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * A restlet resource for serving token introspection information.
 */
public class TokenIntrospectionResource extends ServerResource {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final ExceptionHandler exceptionHandler;
    private final TokenIntrospectionService tokenIntrospectionService;

    @Inject
    public TokenIntrospectionResource(OAuth2ProviderSettingsFactory providerSettingsFactory,
            OAuth2RequestFactory<Request> requestFactory, ExceptionHandler exceptionHandler,
            TokenIntrospectionService tokenIntrospectionService) {
        this.requestFactory = requestFactory;
        this.providerSettingsFactory = providerSettingsFactory;
        this.exceptionHandler = exceptionHandler;
        this.tokenIntrospectionService = tokenIntrospectionService;
    }

    /**
     * Wraps the introspection service in a Restlet API.
     * @param body The body - this is ignored but needs to be present to be made available in the request.
     * @return A JSON representation of the introspection result.
     * @throws org.forgerock.oauth2.restlet.OAuth2RestletException
     */
    @Post("form")
    @Get
    public Representation introspect(Representation body) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());

        try {
            return new JsonRepresentation(tokenIntrospectionService.introspect(request).asMap());
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 introspection request.
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }

}
