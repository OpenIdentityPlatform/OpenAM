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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.xacml;

import static org.forgerock.http.routing.RouteMatchers.requestResourceApiVersionMatcher;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.http.authz.HttpContextFilter;
import org.forgerock.openam.rest.RealmContextFilter;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.forgerock.openam.xacml.v3.rest.XacmlService;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Mounts the XACML policy import/export service at {@code /xacml} in the CHF servlet, with the same realm routing as
 * {@code /json} ({@code /xacml/policies} and {@code /xacml/realms/root/realms/sub/policies}).
 */
public class XacmlHttpRouteProvider implements HttpRouteProvider {

    private static final String POLICIES = "policies";

    private RealmRoutingFactory realmRoutingFactory;
    private RealmContextFilter realmContextFilter;
    private HttpContextFilter httpContextFilter;
    private Filter authenticationFilter;
    private Filter resourceApiVersionFilter;
    private Set<String> invalidRealmNames;
    private XacmlService xacmlService;

    @Inject
    public void setRealmRoutingFactory(RealmRoutingFactory realmRoutingFactory) {
        this.realmRoutingFactory = realmRoutingFactory;
    }

    @Inject
    public void setRealmContextFilter(RealmContextFilter realmContextFilter) {
        this.realmContextFilter = realmContextFilter;
    }

    @Inject
    public void setHttpContextFilter(HttpContextFilter httpContextFilter) {
        this.httpContextFilter = httpContextFilter;
    }

    @Inject
    public void setAuthenticationFilter(@Named("AuthenticationFilter") Filter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Inject
    public void setResourceApiVersionFilter(@Named("ResourceApiVersionFilter") Filter resourceApiVersionFilter) {
        this.resourceApiVersionFilter = resourceApiVersionFilter;
    }

    @Inject
    public void setInvalidRealmNames(@Named("InvalidRealmNames") Set<String> invalidRealmNames) {
        this.invalidRealmNames = invalidRealmNames;
    }

    @Inject
    public void setXacmlService(XacmlService xacmlService) {
        this.xacmlService = xacmlService;
    }

    @Override
    public Set<HttpRoute> get() {
        invalidRealmNames.add(POLICIES); // no realm may be called "policies"
        return Collections.singleton(newHttpRoute(STARTS_WITH, "xacml", createHandler(Endpoints.from(xacmlService))));
    }

    Handler createHandler(Handler policiesEndpoint) {
        Router versionRouter = new Router();
        versionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), policiesEndpoint);
        Router realmRouter = new Router();
        realmRouter.addRoute(requestUriMatcher(EQUALS, POLICIES), versionRouter);
        Router rootRouter = new Router();
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, RealmRoutingFactory.REALM_ROUTE),
                Handlers.chainOf(realmRoutingFactory.createRouter(rootRouter),
                        realmRoutingFactory.createHostnameFilter()));
        rootRouter.setDefaultRoute(Handlers.chainOf(realmRouter, realmContextFilter));
        // Outermost first: CAF authentication, SecurityContext from the CAF attributes (tokenId for XacmlService),
        // resource API version context, then protocol 1.0 as the former Restlet routing enforced it.
        return Handlers.chainOf(rootRouter, authenticationFilter, httpContextFilter, resourceApiVersionFilter,
                new EnforcedProtocolVersionFilter());
    }

    /** Content-API-Version carries "protocol=1.0", which the CHF filter writes only when the protocol version is set. */
    private static final class EnforcedProtocolVersionFilter implements Filter {
        @Override
        public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
            context.asContext(ApiVersionRouterContext.class).setProtocolVersion(version(1));
            return next.handle(context, request);
        }
    }
}
