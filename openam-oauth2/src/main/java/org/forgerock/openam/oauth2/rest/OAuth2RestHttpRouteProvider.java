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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.openam.audit.AuditConstants.Component.OAUTH;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Set;

import org.forgerock.http.Filter;
import org.forgerock.http.handler.Handlers;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;

/**
 * OAuth2 HTTP REST route provider which registers the OAuth2 REST endpoints.
 *
 * @since 13.0.0
 */
public class OAuth2RestHttpRouteProvider implements HttpRouteProvider {

    private ResourceRouter rootRouter;
    private Filter authenticationFilter;

    @Inject
    public void setRouters(ResourceRouter router) {
        this.rootRouter = router;
    }

    @Inject
    public void setAuthenticationFilter(@Named("AuthenticationFilter") Filter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Override
    public Set<HttpRoute> get() {

        rootRouter.route("token")
                .auditAs(OAUTH)
                .toCollection(TokenResource.class);

        rootRouter.route("client")
                .auditAs(OAUTH)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(ClientResource.class);

        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "frrest/oauth2",
                Handlers.chainOf(newHttpHandler(rootRouter.getRouter()), authenticationFilter)));
    }
}
