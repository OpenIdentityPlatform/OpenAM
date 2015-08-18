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

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Set;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.rest.AuthenticationFilter;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;

public class OAuth2RestHttpRouteProvider implements HttpRouteProvider {

    private Provider<AuthenticationFilter> authenticationFilterProvider;

    @Inject
    public void setAuthenticationFilterProvider(
            @Named("RestAuthenticationFilter") Provider<AuthenticationFilter> authenticationFilterProvider) {
        this.authenticationFilterProvider = authenticationFilterProvider;
    }

    @Override
    public Set<HttpRoute> get() {
        Router router = new Router();

        AuthenticationFilter defaultAuthenticationFilter = authenticationFilterProvider.get();

        Router tokenVersionRouter = new Router();
        tokenVersionRouter.addRoute(version(1), InjectorHolder.getInstance(TokenResource.class));
        FilterChain tokenFilterChain = new FilterChain(tokenVersionRouter, defaultAuthenticationFilter);
        router.addRoute(requestUriMatcher(STARTS_WITH, "token"), tokenFilterChain);

        Router clientVersionRouter = new Router();
        clientVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ClientResource.class));
        FilterChain clientFilterChain = new FilterChain(clientAuthzFilterChain, defaultAuthenticationFilter);
        FilterChain clientAuthzFilterChain = createAuthorizationFilter(clientVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        router.addRoute(requestUriMatcher(STARTS_WITH, "client"), clientFilterChain);

        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "frrest/oauth2", CrestHttp.newHttpHandler(router)));
    }
}
