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

package org.forgerock.openam.http;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

import com.google.inject.Injector;
import org.forgerock.http.routing.Router;

/**
 * Guice {@link Provider} that provider the root HTTP {@link Router}.
 *
 * @since 13.0.0
 */
final class HttpRouterProvider implements Provider<Router> {

    private final Injector injector;
    private final Iterable<HttpRouteProvider> routeProviders;

    @Inject
    HttpRouterProvider(Injector injector, Iterable<HttpRouteProvider> routeProviders) {
        this.injector = injector;
        this.routeProviders = routeProviders;
    }

    @Override
    public Router get() {
        Router router = new Router();
        for (HttpRouteProvider routeProvider : routeProviders) {
            injector.injectMembers(routeProvider);
            Set<HttpRoute> routes = routeProvider.get();
            for (HttpRoute route : routes) {
                router.addRoute(requestUriMatcher(route.getMode(), route.getUriTemplate()), route.getHandler());
            }
        }
        return router;
    }
}
