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

import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.RouteMatchers.resourceApiVersionContextFilter;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.sm.InvalidRealmNameManager;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.Router;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.rest.Routers.Route;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GuiceModule
public class RestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ResourceApiVersionBehaviourManager.class).to(VersionBehaviourConfigListener.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("ContextFilter"))).to(ContextFilter.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("LoggingFilter"))).to(CrestLoggingFilter.class).in(Singleton.class);
        bind(Key.get(Logger.class, Names.named("RestAuthentication")))
                .toInstance(LoggerFactory.getLogger("restAuthenticationFilter"));
        bind(AuditApi.class).to(NoopAuditApi.class);
        bind(Key.get(new TypeLiteral<Set<String>>() {
        }, Names.named("InvalidRealmNames")))
                .toInstance(InvalidRealmNameManager.getInvalidRealmNames());
        bind(AuthenticationEnforcer.class);

        bind(Key.get(AsyncServerAuthModule.class, Names.named("OptionalSsoTokenSession")))
                .to(OptionalSSOTokenSessionModule.class).in(Singleton.class);

        bind(String.class)
                .annotatedWith(Names.named(AuthnRequestUtils.SSOTOKEN_COOKIE_NAME))
                .toProvider(new Provider<String>() {
                    @Override
                    public String get() {
                        return CookieUtils.getAmCookieName();
                    }
                });
        bind(new TypeLiteral<Config<String>>() {
        })
                .annotatedWith(Names.named(AuthnRequestUtils.ASYNC_SSOTOKEN_COOKIE_NAME))
                .toInstance(new Config<String>() {
                    @Override
                    public boolean isReady() {
                        return CookieUtils.getAmCookieName() != null;
                    }

                    @Override
                    public String get() {
                        return CookieUtils.getAmCookieName();
                    }
                });
        bind(new TypeLiteral<Config<TokenStore>>() {
        }).toInstance(new Config<TokenStore>() {
            public boolean isReady() {
                return true;
            }

            public TokenStore get() {
                return InjectorHolder.getInstance(OpenAMTokenStore.class);
            }
        });
    }

    @Provides
    @Named("RestHandler")
    @Singleton
    Handler getRestHandler(@Named("ResourceApiVersionFilter") org.forgerock.http.Filter chfResourceApiVersionFilter,
            @Named("ResourceApiVersionFilter") Filter resourceApiVersionFilter,
            @Named("RestRootRouter") org.forgerock.http.routing.Router chfRootRouter,
            CrestProtocolEnforcementFilter crestProtocolEnforcementFilter,
            @Named("CrestRootRouter") Router crestRootRouter,
            @Named("AuthenticationFilter") org.forgerock.http.Filter authenticationFilter) {
        return new RestHandler(Handlers.chainOf(chfRootRouter, chfResourceApiVersionFilter),
                Handlers.chainOf(newHttpHandler(new FilterChain(crestRootRouter, resourceApiVersionFilter)),
                        crestProtocolEnforcementFilter, authenticationFilter), Collections.singleton("authenticate"));
    }

    @Provides
    @Named("ResourceApiVersionFilter")
    @Singleton
    org.forgerock.http.Filter getChfResourceApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return RouteMatchers.resourceApiVersionContextFilter(behaviourManager);
    }

    @Provides
    @Named("ResourceApiVersionFilter")
    @Singleton
    Filter getCrestResourceApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return resourceApiVersionContextFilter(behaviourManager);
    }

    @Provides
    @Named("RestRootRouter")
    @Singleton
    org.forgerock.http.routing.Router getRestRootRouter(
            @Named("RestRealmRouter") org.forgerock.http.routing.Router realmRouter,
            RealmContextFilter realmContextFilter) {
        org.forgerock.http.routing.Router rootRouter = new org.forgerock.http.routing.Router();
        rootRouter.setDefaultRoute(Handlers.chainOf(realmRouter, realmContextFilter));
        return rootRouter;
    }

    @Provides
    @Named("RestRealmRouter")
    @Singleton
    org.forgerock.http.routing.Router getRestRealmRouter(RealmContextFilter realmContextFilter) {
        org.forgerock.http.routing.Router realmRouter = new org.forgerock.http.routing.Router();
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "{realm}"),
                Handlers.chainOf(realmRouter, realmContextFilter));
        return realmRouter;
    }

    @Provides
    @Named("CrestRootRouter")
    @Singleton
    Router getCrestRootRouter(@Named("CrestRealmRouter") Router realmRouter, RealmContextFilter realmContextFilter) {
        Router rootRouter = new Router();
        rootRouter.setDefaultRoute(new FilterChain(realmRouter, realmContextFilter));
        return rootRouter;
    }

    @Provides
    @Named("CrestRealmRouter")
    @Singleton
    Router getCrestRealmRouter(RealmContextFilter realmContextFilter) {
        Router realmRouter = new Router();
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "{realm}"),
                new FilterChain(realmRouter, realmContextFilter));
        return realmRouter;
    }

    @Provides
    @Named("AuthenticationFilter")
    @Singleton
    org.forgerock.http.Filter getAuthenticationFilter(@Named("RestAuthentication") Logger logger,
            AuditApi auditApi, @Named("OptionalSsoTokenSession") AsyncServerAuthModule ssoTokenSessionModule) {
        return AuthenticationFilter.builder()
                .logger(logger)
                .auditApi(auditApi)
                .sessionModule(configureModule(ssoTokenSessionModule))
                .build();
    }

    @Provides
    @Named("RestRouter")
    @Singleton
    DynamicRealmRestRouter getRestRouter(
            @Named("CrestRootRouter") final Router rootRouter,
            @Named("CrestRealmRouter") final Router realmRouter,
            @Named("InvalidRealmNames") final Set<String> invalidRealms,
            final AuthenticationEnforcer defaultAuthenticationEnforcer,
            final AuditFilter auditFilter, @Named("ContextFilter") final Filter contextFilter,
            @Named("LoggingFilter") final Filter loggingFilter) {
        return new DynamicRealmRestRouter() {
            @Override
            public RestRouter dynamically() {
                return new Routers.RestRouterImpl(realmRouter, invalidRealms,
                        defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
            }

            @Override
            public Route route(String uriTemplate) {
                return new Routers.RestRouterImpl(rootRouter, invalidRealms,
                        defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter).route(uriTemplate);
            }

            @Override
            public Router getRouter() {
                return rootRouter;
            }
        };
    }

    @Provides
    @Singleton
    RestRouter getNewRestRouter(
            final Router rootRouter,
            final Router realmRouter,
            final AuthenticationEnforcer defaultAuthenticationEnforcer,
            final AuditFilter auditFilter, @Named("ContextFilter") final Filter contextFilter,
            @Named("LoggingFilter") final Filter loggingFilter) {
        return new DynamicRealmRestRouter() {
            @Override
            public RestRouter dynamically() {
                return new Routers.RestRouterImpl(realmRouter, new HashSet<String>(),
                        defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
            }

            @Override
            public Route route(String uriTemplate) {
                return new Routers.RestRouterImpl(rootRouter, new HashSet<String>(),
                        defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter).route(uriTemplate);
            }

            @Override
            public Router getRouter() {
                return rootRouter;
            }
        };
    }
}
