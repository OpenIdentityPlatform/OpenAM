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

import org.forgerock.oauth2.restlet.GuicedRestlet;
import org.forgerock.oauth2.restlet.OAuth2Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Sets up the OpenId Connect provider endpoints and their handlers.
 *
 * @since 12.0.0
 */
public class OpenIdConnectApplication extends OAuth2Application {

    /**
     * Creates the endpoint handler registrations for the OpenId Connect endpoints.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Restlet createInboundRoot() {

        final Context context = getContext();

        final Router router = new Router(context);

        router.attachDefault(super.createInboundRoot());

        //connect client register
        router.attach("/connect/register", new GuicedRestlet(getContext(), ConnectClientRegistration.class));

        //connect userinfo
        router.attach("/userinfo", new GuicedRestlet(getContext(), UserInfo.class));

        //connect session management
        router.attach("/connect/endSession", new GuicedRestlet(getContext(), EndSession.class));

        //connect jwk_uri
        router.attach("/connect/jwk_uri", new GuicedRestlet(getContext(), OpenIDConnectJWKEndpoint.class));

        return router;
    }
}
