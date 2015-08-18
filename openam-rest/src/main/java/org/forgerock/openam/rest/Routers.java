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

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RouteMatchers;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilterWrapper;

/**
 * Class containing methods for creating fluent route registration routers.
 *
 * @since 13.0.0
 */
public class Routers {

    static final class RestRouterImpl implements RestRouter {

        private final Router router;
        private final org.forgerock.json.resource.Router internalRouter;
        private final Set<String> invalidRealms;
        private final org.forgerock.http.Filter defaultAuthenticationFilter;
        private final AuditFilter auditFilter;
        private final Filter contextFilter;
        private final Filter loggingFilter;

        RestRouterImpl(Router router, org.forgerock.json.resource.Router internalRouter,
                Set<String> invalidRealms, org.forgerock.http.Filter defaultAuthenticationFilter,
                AuditFilter auditFilter, Filter contextFilter, Filter loggingFilter) {
            this.router = router;
            this.internalRouter = internalRouter;
            this.invalidRealms = invalidRealms;
            this.defaultAuthenticationFilter = defaultAuthenticationFilter;
            this.contextFilter = contextFilter;
            this.loggingFilter = loggingFilter;
            this.auditFilter = auditFilter;
        }

        @Override
        public Router getRouter() {
            return router;
        }

        @Override
        public Route route(String uriTemplate) {
            invalidRealms.add(firstPathSegment(uriTemplate));
            return new Route(router, internalRouter, defaultAuthenticationFilter, auditFilter, contextFilter, loggingFilter,
                    uriTemplate);
        }

        /**
         * Returns the first path segment from a uri template. For example {@code /foo/bar} becomes {@code foo}.
         *
         * @param path the full uri template path.
         * @return the first non-empty path segment.
         * @throws IllegalArgumentException if the path contains no non-empty segments.
         */
        private String firstPathSegment(final String path) {
            for (String part : path.split("/")) {
                if (!part.isEmpty()) {
                    return part;
                }
            }
            throw new IllegalArgumentException("uriTemplate " + path + " is invalid");
        }
    }

    /**
     * Creates a new authentication filter which allows for authentication
     * exceptions to be added for certain CREST operations.
     *
     * @return A new {@link ExceptionableAuthenticationFilter}.
     */
    public static ExceptionableAuthenticationFilter ssoToken() {
        return InjectorHolder.getInstance(Key.get(ExceptionableAuthenticationFilter.class,
                Names.named("RestAuthenticationFilter")));
    }

    /**
     * A route that is being registered fluently.
     *
     * @since 13.0.0
     */
    public static final class Route implements VersionableRoute {

        private final Router router;
        private final org.forgerock.json.resource.Router internalRouter;
        private final org.forgerock.http.Filter defaultAuthenticationFilter;
        private final AuditFilter auditFilter;
        private final Filter contextFilter;
        private final Filter loggingFilter;
        private final String uriTemplate;

        private org.forgerock.http.Filter authenticationFilter;
        private AuditFilterWrapper auditFilterWrapper;
        private List<CrestAuthorizationModule> authorizationModules;
        private List<Filter> filters;

        Route(Router router, org.forgerock.json.resource.Router internalRouter,
                org.forgerock.http.Filter defaultAuthenticationFilter, AuditFilter auditFilter, Filter contextFilter,
                Filter loggingFilter, String uriTemplate) {
            this.router = router;
            this.internalRouter = internalRouter;
            this.defaultAuthenticationFilter = defaultAuthenticationFilter;
            this.auditFilter = auditFilter;
            this.contextFilter = contextFilter;
            this.loggingFilter = loggingFilter;
            this.uriTemplate = uriTemplate;
        }

        /**
         * Specifies the authentication filter to be used to protected the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * <p>If no authentication filter is set then the default
         * authentication filter will be used, which protects all operations on the endpoint.</p>
         *
         * @param authenticationFilter The authentication filter.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        public Route authenticateWith(org.forgerock.http.Filter authenticationFilter) {
            if (this.authenticationFilter != null) {
                throw new IllegalStateException("Authentication Filter has already been set!");
            }
            this.authenticationFilter = authenticationFilter;
            return this;
        }

        /**
         * Specifies the audit component to use when auditing requests to the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param component The audit component.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        public Route auditAs(AuditConstants.Component component) {
            if (this.auditFilterWrapper != null) {
                throw new IllegalStateException("Audit component has already been set!");
            }
            this.auditFilterWrapper = new AuditFilterWrapper(auditFilter, component);
            return this;
        }

        /**
         * Specifies the authorization modules to use when authorizing requests
         * to the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param authorizationModules The authorization modules.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        @SafeVarargs
        public final Route authorizeWith(Class<? extends CrestAuthorizationModule>... authorizationModules) {
            if (this.authorizationModules != null) {
                throw new IllegalStateException("Authorization Filters have already been set!");
            } else {
                this.authorizationModules = new ArrayList<>();
            }
            for (Class<? extends CrestAuthorizationModule> authorizationModuleClass : authorizationModules) {
                CrestAuthorizationModule module = InjectorHolder.getInstance(authorizationModuleClass);
                this.authorizationModules.add(new LoggingAuthzModule(module, module.getName()));
            }
            return this;
        }

        /**
         * Specifies filters that the request must pass through before reaching
         * the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param filters The filters.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        @SafeVarargs
        public final Route through(Class<? extends Filter>... filters) {
            if (this.filters != null) {
                throw new IllegalStateException("Filters have already been set!");
            } else {
                this.filters = new ArrayList<>();
            }
            for (Class<? extends Filter> filterClass : filters) {
                this.filters.add(InjectorHolder.getInstance(filterClass));
            }
            return this;
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceClass The resource endpoint class.
         */
        public void toCollection(Class<? extends CollectionResourceProvider> resourceClass) {
            toCollection(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceKey The resource endpoint key.
         */
        public void toCollection(Key<? extends CollectionResourceProvider> resourceKey) {
            forVersion(1).toCollection(resourceKey);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resource The resource endpoint instance.
         */
        public void toCollection(CollectionResourceProvider resource) {
            forVersion(1).toCollection(resource);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceClass The resource endpoint class.
         */
        public void toSingleton(Class<? extends SingletonResourceProvider> resourceClass) {
            toSingleton(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceKey The resource endpoint key.
         */
        public void toSingleton(Key<? extends SingletonResourceProvider> resourceKey) {
            forVersion(1).toSingleton(resourceKey);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resource The resource endpoint instance.
         */
        public void toSingleton(SingletonResourceProvider resource) {
            forVersion(1).toSingleton(resource);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param mode The routing mode.
         * @param handlerClass The resource endpoint handler class.
         */
        public void toRequestHandler(RoutingMode mode, Class<? extends RequestHandler> handlerClass) {
            toRequestHandler(mode, Key.get(handlerClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param mode The routing mode.
         * @param handlerKey The resource endpoint handler key.
         */
        public void toRequestHandler(RoutingMode mode, Key<? extends RequestHandler> handlerKey) {
            forVersion(1).toRequestHandler(mode, handlerKey);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param mode The routing mode.
         * @param handler The resource endpoint handler instance.
         */
        public void toRequestHandler(RoutingMode mode, RequestHandler handler) {
            forVersion(1).toRequestHandler(mode, handler);
        }

        void addRoute(RoutingMode mode, RequestHandler resource) {
            if (authorizationModules != null) {
                resource = createAuthorizationFilter(resource, authorizationModules);
            }
            if (filters != null) {
                resource = new FilterChain(resource, filters);
            }
            resource = new FilterChain(resource, getFilters());
            Handler handler = Handlers.chainOf(newHttpHandler(resource), getAuthenticationFilter());
            router.addRoute(requestUriMatcher(STARTS_WITH, uriTemplate), handler);
            internalRouter.addRoute(RouteMatchers.requestUriMatcher(mode, uriTemplate), resource);
        }

        private List<Filter> getFilters() {
            List<Filter> filters = new ArrayList<>();
            /*TODO authenticationCheckFilter*/
            filters.addAll(Arrays.asList(contextFilter, loggingFilter));
            if (auditFilterWrapper != null) {
                filters.add(auditFilterWrapper);
            }
            return filters;
        }

        private org.forgerock.http.Filter getAuthenticationFilter() {
            if (authenticationFilter == null) {
                authenticationFilter = defaultAuthenticationFilter;
            }
            return authenticationFilter;
        }

        @Override
        public VersionedRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedRoute forVersion(Version version) {
            return new VersionedRoute(this, version, getAuthenticationFilter());
        }
    }

    /**
     * A route that is being registered fluently, which requires versioning.
     *
     * @since 13.0.0
     */
    public static final class VersionedRoute implements VersionableRoute {

        private final Route route;
        private final Version version;
        private final org.forgerock.http.Filter authenticationFilter;

        private List<CrestAuthorizationModule> authorizationModules;
        private List<Filter> filters;

        VersionedRoute(Route route, Version version, org.forgerock.http.Filter authenticationFilter) {
            this.route = route;
            this.version = version;
            this.authenticationFilter = authenticationFilter;
        }

        /**
         * Specifies the authorization modules to use when authorizing requests
         * to this version of the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param authorizationModules The authorization modules.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        @SafeVarargs
        public final VersionedRoute authorizeWith(Class<? extends CrestAuthorizationModule>... authorizationModules) {
            if (this.authorizationModules != null) {
                throw new IllegalStateException("Authorization Filters have already been set!");
            } else {
                this.authorizationModules = new ArrayList<>();
            }
            for (Class<? extends CrestAuthorizationModule> authorizationModuleClass : authorizationModules) {
                this.authorizationModules.add(InjectorHolder.getInstance(authorizationModuleClass));
            }
            return this;
        }

        /**
         * Specifies filters that the request must pass through before reaching
         * this version of the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param filters The filters.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        @SafeVarargs
        public final VersionedRoute through(Class<? extends Filter>... filters) {
            if (this.filters != null) {
                throw new IllegalStateException("Filters have already been set!");
            } else {
                this.filters = new ArrayList<>();
            }
            for (Class<? extends Filter> filterClass : filters) {
                this.filters.add(InjectorHolder.getInstance(filterClass));
            }
            return this;
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceClass The resource endpoint class.
         */
        public VersionableRoute toCollection(Class<? extends CollectionResourceProvider> resourceClass) {
            return toCollection(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceKey The resource endpoint key.
         */
        public VersionableRoute toCollection(Key<? extends CollectionResourceProvider> resourceKey) {
            return addRoute(STARTS_WITH, Resources.newCollection(InjectorHolder.getInstance(resourceKey)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resource The resource endpoint instance.
         */
        public VersionableRoute toCollection(CollectionResourceProvider resource) {
            return addRoute(STARTS_WITH, Resources.newCollection(resource));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceClass The resource endpoint class.
         */
        public VersionableRoute toSingleton(Class<? extends SingletonResourceProvider> resourceClass) {
            return toSingleton(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceKey The resource endpoint key.
         */
        public VersionableRoute toSingleton(Key<? extends SingletonResourceProvider> resourceKey) {
            return addRoute(EQUALS, Resources.newSingleton(InjectorHolder.getInstance(resourceKey)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resource The resource endpoint instance.
         */
        public VersionableRoute toSingleton(SingletonResourceProvider resource) {
            return addRoute(EQUALS, Resources.newSingleton(resource));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handlerClass The resource endpoint handler class.
         */
        public VersionableRoute toRequestHandler(RoutingMode mode, Class<? extends RequestHandler> handlerClass) {
            return toRequestHandler(mode, Key.get(handlerClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handlerKey The resource endpoint handler key.
         */
        public VersionableRoute toRequestHandler(RoutingMode mode, Key<? extends RequestHandler> handlerKey) {
            return addRoute(mode, InjectorHolder.getInstance(handlerKey));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handler The resource endpoint handler instance.
         */
        public VersionableRoute toRequestHandler(RoutingMode mode, RequestHandler handler) {
            return addRoute(mode, handler);
        }

        private VersionableRoute addRoute(RoutingMode mode, RequestHandler resource) {

            if (filters != null) {
                resource = new FilterChain(resource, filters);
            }
            if (authorizationModules != null) {
                resource = createAuthorizationFilter(resource, authorizationModules);
            }

            org.forgerock.json.resource.Router versionRouter = new org.forgerock.json.resource.Router();
            versionRouter.addRoute(version, resource);

            route.addRoute(mode, versionRouter);

            return this;
        }

        @Override
        public VersionedRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedRoute forVersion(Version version) {
            return new VersionedRoute(route, version, authenticationFilter);
        }
    }

    /**
     * A route that is being registered fluently, which can be versioned.
     *
     * @since 13.0.0
     */
    public interface VersionableRoute {

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @return A version route.
         */
        VersionedRoute forVersion(int major);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @param minor The minor version.
         * @return A version route.
         */
        VersionedRoute forVersion(int major, int minor);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param version The version.
         * @return A version route.
         */
        VersionedRoute forVersion(Version version);
    }
}
