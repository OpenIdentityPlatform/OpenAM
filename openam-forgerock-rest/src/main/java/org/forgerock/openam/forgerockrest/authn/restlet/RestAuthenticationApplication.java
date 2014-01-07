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

package org.forgerock.openam.forgerockrest.authn.restlet;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;

/**
 * Restlet Authenticate REST Endpoint Application.
 *
 * @since 12.0.0
 */
public final class RestAuthenticationApplication extends Application {

    /**
     * Constructs a new Rest Authentication Application.
     * <br/>
     * Sets the default media type to JSON and sets the Status Service to handle request failures.
     */
    public RestAuthenticationApplication() {
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new RestAuthenticationStatusService());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Restlet createInboundRoot() {
        Router router = new Router(getContext());
        router.attachDefault(AuthenticationService.class);
        return router;
    }
}
