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

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.http.Handler;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;

/**
 * HTTP route provider for the REST ({@literal /json}) endpoints.
 *
 * To add new REST endpoints the {@link RestRouteProvider} interface must be
 * implemented and an entry into the Service loader file created.
 *
 * @since 13.0.0
 */
public class RestHttpRouteProvider implements HttpRouteProvider {

    private RestRouter rootRouter;
    private RestRouter realmRouter;
    private Injector injector;

    @Override
    public Set<HttpRoute> get() {
        for (RestRouteProvider routeProvider : ServiceLoader.load(RestRouteProvider.class)) {
            injector.injectMembers(routeProvider);
            routeProvider.addRoutes(rootRouter, realmRouter);
        }
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, "json", Key.get(Handler.class, Names.named("RestHandler"))));
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
}
