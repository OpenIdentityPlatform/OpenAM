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

import org.forgerock.openam.rest.router.RestRealmValidator;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Router;

import javax.servlet.http.HttpServletRequest;

/**
 * A Restlet router which will route to service endpoints, dynamically handling realm URI parameters.
 *
 * @since 12.0.0
 */
public class RestletRealmRouter extends Router {

    private final RestRealmValidator realmValidator;
    private final Restlet delegate;

    /**
     * Constructs a new RealmRouter instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     */
    public RestletRealmRouter(RestRealmValidator realmValidator) {
        this.realmValidator = realmValidator;
        this.delegate = new Delegate(this);
    }

    /**
     * Detaches and reattaches the dynamic realm route to ensure that it is the last route on the router.
     */
    private void reattachRealmRoute() {
        detach(delegate);
        attach("/{subrealm}", delegate, Router.MODE_BEST_MATCH);
    }

    /**
     * Reattaches the dynamic realm route to ensure that it is the last route on the router and then calls
     * {@code super}.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     */
    @Override
    public void handle(Request request, Response response) {
        reattachRealmRoute();
        super.handle(request, response);
    }

    /**
     * <p>Takes the last realm URI parameter from the request and appends to the growing full realm value.</p>
     *
     * <p>i.e. last realm URI parameter: realm2, current full realm value: /realm1, after appending: /realm1/realm2.</p>
     *
     * @param next {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     */
    @Override
    protected void doHandle(Restlet next, Request request, Response response) {
        String realm = (String) request.getAttributes().get("realm");
        String subrealm = (String) request.getAttributes().get("subrealm");
        if (realm == null || realm.isEmpty()) {
            realm = "/";
        } else if (subrealm != null && !subrealm.isEmpty()) {
            realm = realm.equals("/") ? realm + subrealm : realm + "/" + subrealm;
        }
        request.getAttributes().put("realm", realm);
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        httpRequest.setAttribute("realm", realm);
        request.getAttributes().remove("subrealm");

        // Check that the path references an existing realm
        if (!realmValidator.isRealm(realm)) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid realm, " + realm);
        }

        super.doHandle(next, request, response);
    }

    /**
     * Restlet eagerly starts/loads its routes so cannot have a direct route back to itself as causes a stack overflow.
     * To get round this adding in a delegate to lazy start/load the dynamic realm route back to itself.
     *
     * @since 12.0.0
     */
    private static final class Delegate extends Restlet {

        private final Router router;

        private Delegate(Router router) {
            this.router = router;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(Request request, Response response) {
            router.handle(request, response);
        }
    }
}
