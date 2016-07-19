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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.forgerock.http.routing.RouteMatchers.resourceApiVersionContextFilter;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.service.SessionConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.sm.InvalidRealmNameManager;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.audit.HttpAccessAuditFilterFactory;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Guice module for bindings for the REST routers and route registration.
 *
 * @since 13.0.0
 */
@GuiceModule
public class RestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("frRest")).toInstance(Debug.getInstance("frRest"));
        bind(ResourceApiVersionBehaviourManager.class).to(VersionBehaviourConfigListener.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("LoggingFilter"))).to(CrestLoggingFilter.class).in(Singleton.class);
        bind(Key.get(Logger.class, Names.named("RestAuthentication")))
                .toInstance(LoggerFactory.getLogger("restAuthenticationFilter"));
        bind(AuditApi.class).to(NoopAuditApi.class);
        bind(Key.get(new TypeLiteral<Set<String>>() {}, Names.named("InvalidRealmNames")))
                .toInstance(InvalidRealmNameManager.getInvalidRealmNames());

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
        bind(new TypeLiteral<Config<String>>() {})
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

        MapBinder.newMapBinder(binder(), String.class, Handler.class);

        bind(Key.get(RequestHandler.class, Names.named("CrestRealmHandler")))
                .to(Key.get(Router.class, Names.named("CrestRealmRouter")));

        install(new FactoryModuleBuilder()
                .implement(SSOTokenContext.class, SSOTokenContext.class)
                .build(SSOTokenContext.Factory.class));
    }

    @Provides
    @Named("ResourceApiVersionFilter")
    @Singleton
    org.forgerock.http.Filter getChfResourceApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return resourceApiVersionContextFilter(behaviourManager);
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
    @Named("RestHandler")
    @Singleton
    Handler getRestHandler(@Named("ChfRootRouter") org.forgerock.http.routing.Router chfRootRouter,
            @Named("AuthenticationFilter") org.forgerock.http.Filter authenticationFilter,
            @Named("ResourceApiVersionFilter") org.forgerock.http.Filter resourceApiVersionFilter) {
        return Handlers.chainOf(chfRootRouter, authenticationFilter,
                resourceApiVersionFilter);
    }

    @Provides
    @Named("RestHandler")
    @Singleton
    RequestHandler getInternalRestHandler(@Named("CrestRootRouter") Router crestRootRouter,
            ContextFilter contextFilter) {
        return new FilterChain(crestRootRouter, contextFilter);
    }

    @Provides
    @Named("ChfRootRouter")
    @Singleton
    org.forgerock.http.routing.Router getChfRootRouter(
            @Named("ChfRealmRouter") org.forgerock.http.routing.Router chfRealmRouter,
            RealmContextFilter realmContextFilter) {
        org.forgerock.http.routing.Router chfRootRouter = new org.forgerock.http.routing.Router();
        chfRootRouter.setDefaultRoute(Handlers.chainOf(chfRealmRouter, realmContextFilter));
        return chfRootRouter;
    }

    @Provides
    @Named("ChfRealmRouter")
    @Singleton
    org.forgerock.http.routing.Router getChfRealmRouter(@Named("CrestRealmHandler") RequestHandler crestRealmHandler,
            ContextFilter contextFilter, CrestProtocolEnforcementFilter crestProtocolEnforcementFilter) {
        org.forgerock.http.routing.Router chfRealmRouter = new org.forgerock.http.routing.Router();
        chfRealmRouter.setDefaultRoute(Handlers.chainOf(
                newHttpHandler(new FilterChain(crestRealmHandler, contextFilter)),
                crestProtocolEnforcementFilter));
        return chfRealmRouter;
    }

    @Provides
    @Named("CrestRootRouter")
    @Singleton
    Router getCrestRootRouter(@Named("CrestRealmRouter") Router crestRealmRouter) {
        Router crestRootRouter = new Router();
        crestRootRouter.setDefaultRoute(crestRealmRouter);
        return crestRealmRouter;
    }

    @Provides
    @Named("InternalCrestRouter")
    @Singleton
    Router getInternalCrestRealmRouter(@Named("CrestRealmRouter") Router crestRealmRouter) {
        Router internalCrestRouter = new Router();
        internalCrestRouter.setDefaultRoute(crestRealmRouter);
        return internalCrestRouter;
    }

    @Provides
    @Named("CrestRealmRouter")
    @Singleton
    Router getCrestRealmRouter() {
        return new Router();
    }

    @Provides
    @Named("RootServiceRouter")
    ServiceRouter getRootServiceRouter(@Named("ChfRootRouter")org.forgerock.http.routing.Router chfRootRouter,
            @Named("InvalidRealmNames") Set<String> invalidRealms, HttpAccessAuditFilterFactory httpAuditFactory) {
        return new Routers.ServiceRouterImpl(chfRootRouter, invalidRealms, httpAuditFactory);
    }

    @Provides
    @Named("RealmServiceRouter")
    ServiceRouter getRealmServiceRouter(@Named("ChfRealmRouter")org.forgerock.http.routing.Router chfRealmRouter,
            @Named("InvalidRealmNames") Set<String> invalidRealms,
            AuthenticationEnforcer defaultAuthenticationEnforcer, HttpAccessAuditFilterFactory httpAuditFactory) {
        return new Routers.ServiceRouterImpl(chfRealmRouter, invalidRealms, httpAuditFactory);
    }

    @Provides
    @Named("RootResourceRouter")
    ResourceRouter getRootResourceRouter(@Named("CrestRootRouter") Router crestRootRouter,
            @Named("ChfRootRouter")org.forgerock.http.routing.Router chfRootRouter,
            CrestProtocolEnforcementFilter crestProtocolEnforcementFilter,
            @Named("InvalidRealmNames") Set<String> invalidRealms,
            AuthenticationEnforcer defaultAuthenticationEnforcer, AuditFilter auditFilter,
            ContextFilter contextFilter, @Named("LoggingFilter") Filter loggingFilter) {
        return new Routers.RootResourceRouterImpl(crestRootRouter, chfRootRouter, crestProtocolEnforcementFilter,
                invalidRealms, defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
    }

    @Provides
    @Named("RealmResourceRouter")
    ResourceRouter getRealmResourceRouter(@Named("CrestRealmRouter") Router crestRealmRouter,
            @Named("InvalidRealmNames") Set<String> invalidRealms,
            AuthenticationEnforcer defaultAuthenticationEnforcer, AuditFilter auditFilter,
            ContextFilter contextFilter, @Named("LoggingFilter") Filter loggingFilter) {
        return new Routers.ResourceRouterImpl(crestRealmRouter, invalidRealms, defaultAuthenticationEnforcer,
                auditFilter, contextFilter, loggingFilter);
    }

    @Provides
    @Named("InternalResourceRouter")
    ResourceRouter getInternalRealmResourceRouter(@Named("InternalCrestRouter") Router internalCrestRouter,
            @Named("InvalidRealmNames") Set<String> invalidRealms, AuthenticationEnforcer defaultAuthenticationEnforcer,
            AuditFilter auditFilter, ContextFilter contextFilter, @Named("LoggingFilter") Filter loggingFilter) {
        return new Routers.ResourceRouterImpl(internalCrestRouter, invalidRealms,
                defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
    }

    @Provides
    ResourceRouter getNewRestRouter(Router rootRouter, AuthenticationEnforcer defaultAuthenticationEnforcer,
            AuditFilter auditFilter, ContextFilter contextFilter, @Named("LoggingFilter") Filter loggingFilter) {
        return new Routers.ResourceRouterImpl(rootRouter, new HashSet<String>(),
                defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
    }
}
