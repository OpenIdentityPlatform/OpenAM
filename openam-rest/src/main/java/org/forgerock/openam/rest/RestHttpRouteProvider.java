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

package org.forgerock.openam.rest;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.http.Handler;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.forgerockrest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.forgerockrest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.http.annotations.Endpoints;

public class RestHttpRouteProvider implements HttpRouteProvider {

    private Set<String> invalidRealms = new HashSet<>();
    private RestRouter rootRouter;
    private RestRouter realmRouter;
    private Router chfRealmRouter;
    private Injector injector;

    @Inject
    public void setInvalidRealms(@Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.invalidRealms = invalidRealms;
    }

    @Inject
    public void setRealmRouter(@Named("RestRealmRouter") Router realmRouter) {
        this.chfRealmRouter = realmRouter;
    }

    @Inject
    public void setRouters(@Named("RestRouter") DynamicRealmRestRouter router) {
        this.rootRouter = router;
        this.realmRouter = router.dynamically();
    }

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Set<HttpRoute> get() {
        addJsonRoutes(invalidRealms);
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, "json", Key.get(Handler.class, Names.named("RestHandler"))));
    }

    private Handler createAuthenticateHandler() {
        Router authenticateVersionRouter = new Router();
        Handler authenticateHandlerV1 = Endpoints.from(AuthenticationServiceV1.class);
        Handler authenticateHandlerV2 = Endpoints.from(AuthenticationServiceV2.class);
        // TODO need to do auditing
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1, 1)), authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2)), authenticateHandlerV2);
        //TODO authentication filter?
        return authenticateVersionRouter;
    }

    private void addJsonRoutes(final Set<String> invalidRealmNames) {
        chfRealmRouter.addRoute(RouteMatchers.requestUriMatcher(EQUALS, "authenticate"), createAuthenticateHandler());
        invalidRealmNames.add(firstPathSegment("authenticate"));

        for (RestRouteProvider routeProvider : ServiceLoader.load(RestRouteProvider.class)) {
            injector.injectMembers(routeProvider);
            routeProvider.addRoutes(rootRouter, realmRouter);
        }
    }

    /**
     * Returns the first path segment from a uri template. For example {@code /foo/bar} becomes {@code foo}.
     *
     * @param path the full uri template path.
     * @return the first non-empty path segment.
     * @throws IllegalArgumentException if the path contains no non-empty segments.
     */
    private static String firstPathSegment(final String path) {
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        throw new IllegalArgumentException("uriTemplate " + path + " is invalid");
    }
}
