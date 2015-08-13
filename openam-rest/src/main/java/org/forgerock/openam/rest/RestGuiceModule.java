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
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.RouteMatchers.resourceApiVersionContextFilter;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;

@GuiceModule
public class RestGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(ResourceApiVersionBehaviourManager.class).to(VersionBehaviourConfigListener.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("ContextFilter"))).to(ContextFilter.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("LoggingFilter"))).to(CrestLoggingFilter.class).in(Singleton.class);

        expose(Key.get(Router.class, Names.named("RootRouter")));
        expose(Key.get(Router.class, Names.named("RealmRouter")));
    }

    @Provides
    @Named("RestHandler")
    @Singleton
    RequestHandler getRestHandler(@Named("ApiVersionFilter") Filter apiVersionFilter,
            @Named("ContextFilter") Filter contextFilter, @Named("LoggingFilter") Filter loggingFilter,
            @Named("RootRouter") Router rootRouter) {
        return new FilterChain(rootRouter, apiVersionFilter, contextFilter, loggingFilter);
    }

    @Provides
    @Named("ApiVersionFilter")
    @Singleton
    Filter getApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return resourceApiVersionContextFilter(behaviourManager);
    }

    @Provides
    @Named("RootRouter")
    @Singleton
    Router getRootRouter(@Named("RealmRouter") Router realmRouter) {
        Router rootRouter = new Router();
        rootRouter.setDefaultRoute(realmRouter);
        return rootRouter;
    }

    @Provides
    @Named("RealmRouter")
    @Singleton
    Router getRealmRouter(RealmContextFilter realmContextFilter) {
        Router realmRouter = new Router();
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "{realm}"),
                new FilterChain(realmRouter, realmContextFilter));
        return realmRouter;
    }
}
