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

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.sm.InvalidRealmNameManager;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.caf.authentication.framework.CrestAuthenticationFilter;
import org.forgerock.caf.authentication.framework.HttpAuthenticationFilter;
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
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GuiceModule
public class RestGuiceModule extends PrivateModule {

    private static final String SSO_TOKEN_COOKIE_NAME_PROPERTY = "com.iplanet.am.cookie.name";

    @Override
    protected void configure() {
        bind(ResourceApiVersionBehaviourManager.class).to(VersionBehaviourConfigListener.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("ContextFilter"))).to(ContextFilter.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("LoggingFilter"))).to(CrestLoggingFilter.class).in(Singleton.class);
        bind(Key.get(Logger.class, Names.named("RestAuthentication")))
                .toInstance(LoggerFactory.getLogger("restAuthenticationFilter"));
        bind(AuditApi.class).to(NoopAuditApi.class);
        bind(Key.get(new TypeLiteral<Set<String>>(){}, Names.named("InvalidRealmNames")))
                .toInstance(InvalidRealmNameManager.getInvalidRealmNames());

        expose(Key.get(Handler.class, Names.named("RestHandler")));
        expose(Key.get(org.forgerock.http.routing.Router.class, Names.named("RestRootRouter")));
        expose(Key.get(org.forgerock.http.routing.Router.class, Names.named("RestRealmRouter")));
        expose(Key.get(Router.class, Names.named("CrestRootRouter")));
        expose(Key.get(Router.class, Names.named("CrestRealmRouter")));
        expose(Key.get(org.forgerock.http.Filter.class, Names.named("ResourceApiVersionFilter")));
        expose(Key.get(Filter.class, Names.named("LoggingFilter")));
        expose(Key.get(Filter.class, Names.named("ContextFilter")));
        expose(ResourceApiVersionBehaviourManager.class);
        expose(Key.get(AuthenticationFilter.class, Names.named("RestAuthenticationFilter")));
        expose(Key.get(new TypeLiteral<Set<String>>(){}, Names.named("InvalidRealmNames")));



        //From AuthFilterGuiceModule
        bind(Key.get(AsyncServerAuthModule.class, Names.named("SsoTokenSession")))
                .to(LocalSSOTokenSessionModule.class).in(Singleton.class);
        bind(Key.get(AsyncServerAuthModule.class, Names.named("OptionalSsoTokenSession")))
                .to(OptionalSSOTokenSessionModule.class).in(Singleton.class);

        bind(String.class)
                .annotatedWith(Names.named(AuthnRequestUtils.SSOTOKEN_COOKIE_NAME))
                .toProvider(new Provider<String>() {
                    public String get() {
                        return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY);
                    }
                });
        bind(new TypeLiteral<Config<String>>() {})
                .annotatedWith(Names.named(AuthnRequestUtils.ASYNC_SSOTOKEN_COOKIE_NAME))
                .toInstance(new Config<String>() {

                    public boolean isReady() {
                        return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY) != null;
                    }

                    public String get() {
                        return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY);
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

        expose(Key.get(new TypeLiteral<Config<String>>() {
        }, Names.named(AuthnRequestUtils.ASYNC_SSOTOKEN_COOKIE_NAME)));
    }

    @Provides
    @Named("RestHandler")
    @Singleton
    Handler getRestHandler(@Named("ResourceApiVersionFilter") org.forgerock.http.Filter resourceApiVersionFilter,
            @Named("RestRootRouter") org.forgerock.http.routing.Router rootRouter) {
        return Handlers.chainOf(rootRouter, resourceApiVersionFilter);
    }

    @Provides
    @Named("ResourceApiVersionFilter")
    @Singleton //TODO this should be in openam-http
    org.forgerock.http.Filter getChfResourceApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        return RouteMatchers.resourceApiVersionContextFilter(behaviourManager);
    }

    @Provides
    @Named("RestRootRouter")
    @Singleton
    org.forgerock.http.routing.Router getRestRootRouter(@Named("RestRealmRouter") org.forgerock.http.routing.Router realmRouter, RealmContextFilter realmContextFilter) {
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
    @Named("RestletAuthenticationFilter")
    @Singleton
    org.forgerock.http.Filter getRestletAuthenticationFilter(@Named("RestAuthentication") Logger logger,
            AuditApi auditApi, @Named("RestAuthentication") AuthenticationModule sessionModule) {
        return HttpAuthenticationFilter.builder()
                .logger(logger)
                .auditApi(auditApi)
                .sessionModule(configureModule(sessionModule))
                .build();
    }

    @Provides
    @Named("RestAuthenticationFilter")
    AuthenticationFilter getAuthenticationFilter(@Named("RestAuthentication") Logger logger,
            AuditApi auditApi, @Named("RestAuthentication") AuthenticationModule sessionModule) {
        return new AuthenticationFilter(CrestAuthenticationFilter.builder()
                .logger(logger)
                .auditApi(auditApi)
                .sessionModule(configureModule(sessionModule))
                .build(), sessionModule);
    }

    @Provides
    @Named("RestAuthentication")
    AuthenticationModule getAuthenticationModule(@Named("SsoTokenSession") AsyncServerAuthModule ssoTokenSessionModule,
            @Named("OptionalSsoTokenSession") AsyncServerAuthModule optionalSsoTokenSessionModule) {
        return new AuthenticationModule(ssoTokenSessionModule, optionalSsoTokenSessionModule);
    }
}
