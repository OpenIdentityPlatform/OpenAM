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

package org.openidentityplatform.openam.oauth2.webfinger;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.openidentityplatform.openam.oauth2.OAuth2HttpAccessAuditFilter;

/**
 * Mounts OpenID Connect issuer discovery at {@code /.well-known/webfinger} in the CHF servlet. Realms are addressed as
 * under the former Restlet application ({@code /.well-known/<realm>/webfinger}, {@code ?realm=}) and as under
 * {@code /json} ({@code /.well-known/realms/root/realms/<realm>/webfinger}).
 */
public class WebFingerHttpRouteProvider implements HttpRouteProvider {

    private static final String WELL_KNOWN = ".well-known";
    private static final String WEBFINGER = "webfinger";

    private RealmRoutingFactory realmRoutingFactory;
    private Filter auditFilter;
    private WebFingerService webFingerService;

    /**
     * Sets the factory of the {@code realms/...} routing.
     *
     * @param realmRoutingFactory The factory.
     */
    @Inject
    public void setRealmRoutingFactory(RealmRoutingFactory realmRoutingFactory) {
        this.realmRoutingFactory = realmRoutingFactory;
    }

    /**
     * Sets the access audit filter of the endpoint.
     *
     * @param auditFilter The OAuth2 access audit filter.
     */
    @Inject
    public void setAuditFilter(OAuth2HttpAccessAuditFilter auditFilter) {
        this.auditFilter = auditFilter;
    }

    /**
     * Sets the discovery endpoint.
     *
     * @param webFingerService The endpoint.
     */
    @Inject
    public void setWebFingerService(WebFingerService webFingerService) {
        this.webFingerService = webFingerService;
    }

    @Override
    public Set<HttpRoute> get() {
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, WELL_KNOWN, createHandler(Endpoints.from(webFingerService))));
    }

    Handler createHandler(Handler webFingerEndpoint) {
        // <sub>/.../webfinger: one sub-realm per segment, then the realm parameter, the audit and the endpoint.
        // EQUALS "webfinger" wins over the "{subrealm}" template for the last segment.
        Router realmPathRouter = new Router();
        realmPathRouter.addRoute(requestUriMatcher(EQUALS, WEBFINGER),
                Handlers.chainOf(webFingerEndpoint, WebFingerRealms.realmParameterFilter(), auditFilter));
        realmPathRouter.addRoute(requestUriMatcher(STARTS_WITH, "{" + WebFingerRealms.SUB_REALM + "}"),
                WebFingerRealms.subRealmHandler(realmPathRouter));
        // realms/root[/realms/<sub>]/...: the /json spelling, which continues with the default route.
        Router rootRouter = new Router();
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, RealmRoutingFactory.REALM_ROUTE),
                Handlers.chainOf(realmRoutingFactory.createRouter(rootRouter),
                        realmRoutingFactory.createHostnameFilter()));
        rootRouter.setDefaultRoute(Handlers.chainOf(realmPathRouter, WebFingerRealms.hostRealmFilter()));
        return rootRouter;
    }
}
