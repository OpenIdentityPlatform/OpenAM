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

package org.forgerock.openam.rest.service;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Router;
import org.restlet.util.RouteList;

import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Restlet Application for REST Service Endpoints.
 *
 * @since 12.0.0
 */
public final class ServiceEndpointApplication extends Application {

    public static final String ROUTER_NAME = "ServiceEndpointRouter";

    private final RestRealmValidator realmValidator;
    private final Router endpointRouter;

    /**
     * Constructs a new ServiceEndpointApplication.
     * <br/>
     * Sets the default media type as "application/json" and sets the StatusService to {@link RestStatusService}.
     */
    public ServiceEndpointApplication() {
        this.realmValidator = InjectorHolder.getInstance(RestRealmValidator.class);
        this.endpointRouter = InjectorHolder.getInstance(Key.get(Router.class, Names.named(ROUTER_NAME)));
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new RestStatusService());
    }

    /**
     * Crests an inbound Restlet root for all registered REST Service endpoints.
     *
     * @return A Restlet for routing incoming REST Service endpoint requests.
     */
    @Override
    public Restlet createInboundRoot() {
        return realm("");
    }

    /**
     * Creates a dynamic Router for the given realm.
     *
     * @param realm The realm.
     * @return A Router.
     */
    private Router realm(final String realm) {
        Router router = new Router(getContext());
        router.setRoutes(new RouteList(endpointRouter.getRoutes()));
        router.attach("/{realm}", subrealm(realm));
        return router;
    }

    /**
     * Creates a Restlet for the sub-realms of the given parent realm.
     *
     * @param parentRealm The parent realm.
     * @return A Restlet instance.
     */
    private Restlet subrealm(final String parentRealm) {
        return new Router() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final Request request, final Response response) {
                subrealm(parentRealm, request).handle(request, response);
            }

            /**
             * Gets the realm part that was matched whilst routing and concatenates in to the current realm, verifies
             * that the realm exists and is valid and then creates a Router for the realm.
             *
             * @param parentRealm The parent realm.
             * @param request The Request.
             * @return A Router instance.
             */
            private Router subrealm(final String parentRealm, final Request request) {
                final String matchedRealm = request.getAttributes().get("realm").toString();
                final String realm = parentRealm + "/" + matchedRealm;

                // Check that the path references an existing realm
                if (!realmValidator.isRealm(realm)) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid realm, " + realm);
                }

                HttpServletRequestWrapper httpRequest = (HttpServletRequestWrapper) ServletUtils.getRequest(request);
                request.getAttributes().put("realm", realm);
                httpRequest.setAttribute("realm", realm);

                return realm(realm);
            }
        };
    }
}
