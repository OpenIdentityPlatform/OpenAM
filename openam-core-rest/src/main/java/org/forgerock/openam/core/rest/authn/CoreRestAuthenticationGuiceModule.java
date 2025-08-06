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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static org.forgerock.http.handler.Handlers.chainOf;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.audit.HttpAccessAuditFilterFactory;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.services.routing.RouteMatcher;

/**
 * Guice module for binding authentication REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestAuthenticationGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<RouteMatcher<Request>, Handler> chfEndpointHandlers = newMapBinder(binder(),
                new TypeLiteral<RouteMatcher<Request>>() {
                }, new TypeLiteral<Handler>() {
                });
        chfEndpointHandlers.addBinding(requestUriMatcher(EQUALS, "authenticate"))
                .to(Key.get(Handler.class, Names.named("AuthenticateHandler")));

        MapBinder<Component, AbstractHttpAccessAuditFilter> httpAccessAuditFilterMapBinder
                = newMapBinder(binder(), Component.class, AbstractHttpAccessAuditFilter.class);
        httpAccessAuditFilterMapBinder.addBinding(AUTHENTICATION).to(AuthenticationAccessAuditFilter.class);
    }

    @Provides
    @Named("AuthenticateHandler")
    @Inject
    Handler getAuthenticateHandler(@Named("InvalidRealmNames") Set<String> invalidRealms,
            HttpAccessAuditFilterFactory httpAuditFactory) {
        invalidRealms.add(firstPathSegment("authenticate"));
        org.forgerock.http.routing.Router authenticateVersionRouter = new org.forgerock.http.routing.Router();
        Handler authenticateHandlerV1 = Endpoints.from(AuthenticationServiceV1.class);
        Handler authenticateHandlerV2 = Endpoints.from(AuthenticationServiceV2.class);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1, 2)),
                authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2, 1)),
                authenticateHandlerV2);
        return chainOf(authenticateVersionRouter, httpAuditFactory.createFilter(AUTHENTICATION));
    }

    /**
     * Returns the first path segment from a uri template. For example {@code /foo/bar} becomes {@code foo}.
     *
     * @param path
     *         the full uri template path.
     *
     * @return the first non-empty path segment.
     *
     * @throws IllegalArgumentException
     *         if the path contains no non-empty segments.
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