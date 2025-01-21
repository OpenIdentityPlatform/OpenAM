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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openidconnect.OpenIDConnectProviderConfiguration;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import jakarta.inject.Inject;

/**
 * Handles requests to the OpenId Connect .well-known endpoint for retrieving OpenId Connect provider configuration.
 *
 * @since 11.0.0
 */
public class OpenIDConnectConfiguration extends ServerResource {

    private final OAuth2RequestFactory requestFactory;
    private final OpenIDConnectProviderConfiguration providerConfiguration;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new OpenIDConnectConfiguration.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param providerConfiguration An instance of the OpenIDConnectProviderConfiguration.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public OpenIDConnectConfiguration(OAuth2RequestFactory requestFactory,
            OpenIDConnectProviderConfiguration providerConfiguration, ExceptionHandler exceptionHandler) {
        this.requestFactory = requestFactory;
        this.providerConfiguration = providerConfiguration;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Handles GET requests to the OpenId Connect .well-known endpoint for retrieving the OpenId Connect provider
     * configuration.
     *
     * @return The representation of the OpenId Connect provider configuration.
     * @throws OAuth2RestletException If an error occurs whilst retrieving the OpenId Connect provider configuration.
     */
    @Get
    public Representation getConfiguration() throws OAuth2RestletException {
        try {
            final OAuth2Request request = requestFactory.create(getRequest());
            final JsonValue configuration = providerConfiguration.getConfiguration(request);
            return new JsonRepresentation(configuration.asMap());
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
