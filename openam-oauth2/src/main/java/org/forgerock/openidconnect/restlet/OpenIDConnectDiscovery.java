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

import java.util.Map;
import jakarta.inject.Inject;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openidconnect.OpenIDConnectProviderDiscovery;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Handles requests to the OpenId Connect discovery endpoint.
 *
 * @since 11.0.0
 */
public class OpenIDConnectDiscovery extends ServerResource {

    private final OAuth2RequestFactory requestFactory;
    private final OpenIDConnectProviderDiscovery providerDiscovery;
    private final ExceptionHandler exceptionHandler;
    private final BaseURLProviderFactory baseUrlProviderFactory;

    /**
     * Constructs a new OpenIDConnectDiscovery.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param providerDiscovery An instance of the OpenIDConnewctProviderDiscovery.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public OpenIDConnectDiscovery(OAuth2RequestFactory requestFactory,
            OpenIDConnectProviderDiscovery providerDiscovery, ExceptionHandler exceptionHandler,
            BaseURLProviderFactory baseUrlProviderFactory) {
        this.requestFactory = requestFactory;
        this.providerDiscovery = providerDiscovery;
        this.exceptionHandler = exceptionHandler;
        this.baseUrlProviderFactory = baseUrlProviderFactory;
    }

    /**
     * Handles GET requests to the OpenId Connect discovery endpoint.
     *
     * @return The representation of the OpenId Connect discovery.
     * @throws OAuth2RestletException If an error occurs whilst performing the discovery.
     */
    @Get
    public Representation discovery() throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        final String resource = request.getParameter("resource");
        final String rel = request.getParameter("rel");
        final String realm = request.getParameter("realm");

        try {
            final String deploymentUrl =
                    baseUrlProviderFactory.get(realm).getRootURL(ServletUtils.getRequest(getRequest()));

            final Map<String, Object> response = providerDiscovery.discover(resource, rel, deploymentUrl, request);

            return new JsonRepresentation(response);

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
