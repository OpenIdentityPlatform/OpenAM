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

package org.forgerock.openidconnect.restlet;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;

/**
 * Exposes the JWK Set of configured signing and encryption keys for the OAuth2 Authorization Server/OpenID Provider.
 *
 * @since 12.0.0
 */
public class OpenIDConnectJWKEndpoint extends ServerResource {

    private final OAuth2RequestFactory<Request> requestFactory;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new OpenIDConnectJWKEndpoint.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public OpenIDConnectJWKEndpoint(OAuth2RequestFactory<Request> requestFactory,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ExceptionHandler exceptionHandler) {
        this.requestFactory = requestFactory;
        this.providerSettingsFactory = providerSettingsFactory;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Gets the JWK Set of signing and encryption keys.
     *
     * @return The JWK Set.
     * @throws OAuth2RestletException If a problem occurs.
     */
    @Get
    public Representation getJWKSet() throws OAuth2RestletException {
        OAuth2Request request = requestFactory.create(getRequest());
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        try {
            return new JsonRepresentation(providerSettings.getJWKSet().asMap());
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Handles any exception that is thrown when processing a request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}
