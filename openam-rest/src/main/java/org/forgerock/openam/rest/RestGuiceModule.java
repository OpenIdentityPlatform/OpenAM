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

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.caf.authentication.framework.CrestAuthenticationFilter;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;

@GuiceModule
public class RestGuiceModule extends PrivateModule {

    private static final String SSO_TOKEN_COOKIE_NAME_PROPERTY = "com.iplanet.am.cookie.name";

    @Override
    protected void configure() {
        bind(ResourceApiVersionBehaviourManager.class).to(VersionBehaviourConfigListener.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("ContextFilter"))).to(ContextFilter.class).in(Singleton.class);
        bind(Key.get(Filter.class, Names.named("LoggingFilter"))).to(CrestLoggingFilter.class).in(Singleton.class);

        bind(Key.get(AsyncServerAuthModule.class, Names.named("SsoTokenSession")))
                .to(LocalSSOTokenSessionModule.class).in(Singleton.class);
        bind(Key.get(AsyncServerAuthModule.class, Names.named("OptionalSsoTokenSession")))
                .to(OptionalSSOTokenSessionModule.class).in(Singleton.class);

        expose(Key.get(Router.class, Names.named("RootRouter")));
        expose(Key.get(Router.class, Names.named("RealmRouter")));

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
        bind(new TypeLiteral<Config<TokenStore>>() {}).toInstance(new Config<TokenStore>() {
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
    RequestHandler getRestHandler(@Named("ResourceApiVersionFilter") Filter resourceApiVersionFilter,
            @Named("ContextFilter") Filter contextFilter, @Named("LoggingFilter") Filter loggingFilter,
            @Named("RootRouter") Router rootRouter) {
        return new FilterChain(rootRouter, resourceApiVersionFilter, contextFilter, loggingFilter);
    }

    @Provides
    @Named("ResourceApiVersionFilter")
    @Singleton
    Filter getResourceApiVersionFilter(ResourceApiVersionBehaviourManager behaviourManager) {
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

    @Provides
    @Named("RestAuthenticationFilter")
    @Singleton
    Filter getAuthenticationFilter(@Named("RestAuthentication") Logger logger,
            AuditApi auditApi, @Named("RestAuthentication") AuthenticationModule sessionModule) {
        return CrestAuthenticationFilter.builder()
                .logger(logger)
                .auditApi(auditApi)
                .sessionModule(configureModule(sessionModule))
                .build();
    }

    @Provides
    @Named("RestAuthentication")
    AuthenticationFilter getRestAuthenticationFilter(
            @Named("RestAuthentication") CrestAuthenticationFilter authenticationFilter,
            @Named("RestAuthentication") AuthenticationModule authenticationModule) {
        return new AuthenticationFilter(authenticationFilter, authenticationModule);
    }

    @Provides
    @Named("RestAuthentication")
    AuthenticationModule getAuthenticationModule(@Named("SsoTokenSession") AsyncServerAuthModule ssoTokenSessionModule,
            @Named("OptionalSsoTokenSession") AsyncServerAuthModule optionalSsoTokenSessionModule) {
        return new AuthenticationModule(ssoTokenSessionModule, optionalSsoTokenSessionModule);
    }
}
