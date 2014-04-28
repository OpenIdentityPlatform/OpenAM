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

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;

import java.util.Map;

/**
 * Sets up the OAuth2 provider endpoints and their handlers.
 *
 * @since 11.0.0
 */
public class OAuth2Application extends Application {

    private final Map<String, ServerResource> endpointClasses;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new OAuth2Application.
     * <br/>
     * Sets the default media type to {@link MediaType#APPLICATION_JSON} and sets the status service to
     * {@link OAuth2StatusService}.
     */
    public OAuth2Application() {
        endpointClasses =
                InjectorHolder.getInstance(Key.get(new TypeLiteral<Map<String, ServerResource>>() {}));
        requestFactory = InjectorHolder.getInstance(Key.get(new TypeLiteral<OAuth2RequestFactory<Request>>() {}));
        exceptionHandler = InjectorHolder.getInstance(ExceptionHandler.class);

        getMetadataService().setEnabled(true);
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new OAuth2StatusService());
    }

    /**
     * Creates the endpoint handler registrations for the OAuth2 endpoints.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Restlet createInboundRoot() {

        final Context context = getContext();

        final Router router = new Router(context);

        router.attach("/authorize", new AuthorizeEndpointFilter(context,
                new GuicedRestlet(context, AuthorizeResource.class)));

        final OAuth2FlowFinder finder = new OAuth2FlowFinder(context, requestFactory, exceptionHandler,
                endpointClasses);
        final TokenEndpointFilter tokenEndpointFilter = new TokenEndpointFilter(getContext(), finder);
        router.attach("/access_token", tokenEndpointFilter);

        //go to token info endpoint
        router.attach("/tokeninfo", new GuicedRestlet(getContext(), ValidationServerResource.class));

        return router;
    }
}
