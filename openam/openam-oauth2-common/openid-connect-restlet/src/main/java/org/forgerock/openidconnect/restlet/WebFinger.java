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

package org.forgerock.openidconnect.restlet;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.restlet.GuicedRestlet;
import org.forgerock.oauth2.restlet.OAuth2StatusService;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;

/**
 * Sets up the OpenId Connect web finger endpoints and their handlers.
 *
 * @since 11.0.0
 */
public class WebFinger extends Application {

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;

    /**
     * Constructs a new WebFinger.
     * <br/>
     * Sets the default media type to {@link MediaType#APPLICATION_JSON} and sets the status service to
     * {@link OAuth2StatusService}.
     */
    public WebFinger() {
        realmValidator = InjectorHolder.getInstance(RestRealmValidator.class);
        coreWrapper = InjectorHolder.getInstance(CoreWrapper.class);

        getMetadataService().setEnabled(true);
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new OAuth2StatusService());
    }

    /**
     * Creates the endpoint handler registrations for the OpenId Connect web finger endpoints.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Restlet createInboundRoot() {
        final Router root = new RestletRealmRouter(realmValidator, coreWrapper);

        /**
         * For now we only use webfinger for OpenID Connect. Once the standard is finalized
         * or we decide to use it for other tasks we dont need a full blown handler
         */
        root.attach("/webfinger", new GuicedRestlet(getContext(), OpenIDConnectDiscovery.class));

        return root;
    }
}
