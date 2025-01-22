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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.rest.service;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Route;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

/**
 * A Restlet router which will route to service endpoints, dynamically handling realm URI parameters.
 *
 * @since 12.0.0
 * @deprecated Use {@code RealmRoutingFactory#createRouter(Restlet)} instead.
 */
@Deprecated
public class RestletRealmRouter extends Router {

    public static final String REALM = "realm";
    public static final String REALM_OBJECT = "realmObject";
    public static final String REALM_URL = "realmUrl";

    private final TemplateRoute delegateRoute;

    /**
     * Constructs a new RealmRouter instance.
     */
    public RestletRealmRouter() {
        Delegate delegate = new Delegate(this);
        delegateRoute = createRoute("/{subrealm}", delegate, Template.MODE_STARTS_WITH);
        super.setDefaultRoute(delegateRoute);
    }

    @Override
    public void setDefaultRoute(Route defaultRoute) {
        throw new UnsupportedOperationException("Default route is handled internally for realm routing");
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
        if (request.getAttributes().containsKey("realmId")) {
            super.doHandle(next, request, response);
            return;
        }

        try {
            Realm realm = getRealmFromURI(request);

            if (realm == null) {
                realm = getRealmFromServerName(request);
            }
            if (next != delegateRoute) {
                String overrideRealm = getRealmFromQueryString(request);
                if (overrideRealm != null) {
                    realm = Realm.of(overrideRealm);
                }
                request.getAttributes().put(REALM_URL, request.getResourceRef().getBaseRef().toString());
            }

            request.getAttributes().put(REALM, realm.asPath());
            request.getAttributes().put(REALM_OBJECT, realm);
            HttpServletRequest httpRequest = ServletUtils.getRequest(request);
            httpRequest.setAttribute(REALM, realm.asPath());
            httpRequest.setAttribute(REALM_OBJECT, realm);
            request.getAttributes().remove("subrealm");

            super.doHandle(next, request, response);
        } catch (RealmLookupException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Realm \"" + e.getRealm() + "\" not found", e);
        }
    }

    private Realm getRealmFromURI(Request request) throws RealmLookupException {
        Realm realm = (Realm) request.getAttributes().get(REALM_OBJECT);
        String subrealm = (String) request.getAttributes().get("subrealm");
        if (subrealm != null && !subrealm.isEmpty()) {
            if (realm == null) {
                throw new IllegalStateException("Realm is null! Has not been set from server name");
            } else {
                return Realm.of(realm, subrealm);
            }
        }
        return null;
    }

    private String getRealmFromQueryString(Request request) {
        String realm = request.getResourceRef().getQueryAsForm().getFirstValue(REALM);
        if (realm == null) {
            return null;
        }
        return realm;
    }

    private Realm getRealmFromServerName(Request request) {
        String serverName = request.getHostRef().getHostDomain();
        try {
            return Realm.of(serverName);
        } catch (RealmLookupException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    /**
     * Returns the realm from the given request.
     *
     * @param request Non null request to examine.
     * @return Null if no realm was found, otherwise the given Realm as a String.
     */
    public static String getRealmFromRequest(Request request) {
        String realm = (String) request.getAttributes().get(REALM);
        if (realm == null) {
            return null;
        }
        return realm;
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
