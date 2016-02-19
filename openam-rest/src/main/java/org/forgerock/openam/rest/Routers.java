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

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RouteMatchers.requestResourceApiVersionMatcher;
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
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RouteMatchers;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.HttpAccessAuditFilterFactory;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilterWrapper;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Class containing methods for creating fluent route registration routers.
 *
 * @since 13.0.0
 */
public class Routers {

    static class ResourceRouterImpl implements ResourceRouter { //TODO need to try and make these classes smaller

        private final Router router;
        private final Set<String> invalidRealms;
        private final Filter defaultAuthenticationEnforcer;
        private final AuditFilter auditFilter;
        private final Filter contextFilter;
        private final Filter loggingFilter;

        ResourceRouterImpl(Router router,
                Set<String> invalidRealms, Filter defaultAuthenticationEnforcer,
                AuditFilter auditFilter, Filter contextFilter, Filter loggingFilter) {
            this.router = router;
            this.invalidRealms = invalidRealms;
            this.defaultAuthenticationEnforcer = defaultAuthenticationEnforcer;
            this.contextFilter = contextFilter;
            this.loggingFilter = loggingFilter;
            this.auditFilter = auditFilter;
        }

        @Override
        public final Router getRouter() {
            return router;
        }

        @Override
        public final ResourceRoute route(String uriTemplate) {
            if (!StringUtils.isEmpty(uriTemplate)) {
                invalidRealms.add(firstPathSegment(uriTemplate));
            }
            return createRoute(uriTemplate);
        }

        ResourceRoute createRoute(String uriTemplate) {
            return new ResourceRoute(router, defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter,
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

    static class RootResourceRouterImpl extends ResourceRouterImpl {

        private final Router router;
        private final org.forgerock.http.routing.Router chfRouter;
        private final CrestProtocolEnforcementFilter crestProtocolEnforcementFilter;
        private final Filter defaultAuthenticationEnforcer;
        private final AuditFilter auditFilter;
        private final Filter contextFilter;
        private final Filter loggingFilter;

        RootResourceRouterImpl(Router router, org.forgerock.http.routing.Router chfRouter,
                CrestProtocolEnforcementFilter crestProtocolEnforcementFilter, Set<String> invalidRealms,
                Filter defaultAuthenticationEnforcer, AuditFilter auditFilter, Filter contextFilter,
                Filter loggingFilter) {
            super(router, invalidRealms, defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter);
            this.router = router;
            this.chfRouter = chfRouter;
            this.crestProtocolEnforcementFilter = crestProtocolEnforcementFilter;
            this.defaultAuthenticationEnforcer = defaultAuthenticationEnforcer;
            this.contextFilter = contextFilter;
            this.loggingFilter = loggingFilter;
            this.auditFilter = auditFilter;
        }

        @Override
        ResourceRoute createRoute(String uriTemplate) {
            return new RootResourceRoute(router, chfRouter, crestProtocolEnforcementFilter,
                    defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter, uriTemplate);
        }
    }

    static final class ServiceRouterImpl implements ServiceRouter {

        private final org.forgerock.http.routing.Router router;
        private final Set<String> invalidRealms;
        private final HttpAccessAuditFilterFactory httpAuditFactory;

        ServiceRouterImpl(org.forgerock.http.routing.Router router,
                Set<String> invalidRealms, HttpAccessAuditFilterFactory httpAuditFactory) {
            this.router = router;
            this.invalidRealms = invalidRealms;
            this.httpAuditFactory = httpAuditFactory;
        }

        @Override
        public org.forgerock.http.routing.Router getRouter() {
            return router;
        }

        @Override
        public ServiceRoute route(String uriTemplate) {
            if (!StringUtils.isEmpty(uriTemplate)) {
                invalidRealms.add(firstPathSegment(uriTemplate));
            }
            return new ServiceRoute(router, httpAuditFactory, uriTemplate);
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
     * @return A new {@link AuthenticationEnforcer}.
     */
    public static AuthenticationEnforcer ssoToken() {
        return InjectorHolder.getInstance(AuthenticationEnforcer.class);
    }

    static class RootResourceRoute extends ResourceRoute {

        private final org.forgerock.http.routing.Router chfRouter;
        private final CrestProtocolEnforcementFilter crestProtocolEnforcementFilter;

        RootResourceRoute(Router router, org.forgerock.http.routing.Router chfRouter,
                CrestProtocolEnforcementFilter crestProtocolEnforcementFilter, Filter defaultAuthenticationEnforcer,
                AuditFilter auditFilter, Filter contextFilter, Filter loggingFilter, String uriTemplate) {
            super(router, defaultAuthenticationEnforcer, auditFilter, contextFilter, loggingFilter, uriTemplate);
            this.chfRouter = chfRouter;
            this.crestProtocolEnforcementFilter = crestProtocolEnforcementFilter;
        }

        @Override
        void addRoute(RoutingMode mode, String uriTemplate, RequestHandler resource) {
            super.addRoute(mode, uriTemplate, resource);
            chfRouter.addRoute(requestUriMatcher(mode, uriTemplate), Handlers.chainOf(newHttpHandler(resource), crestProtocolEnforcementFilter));
        }
    }

    /**
     * A route that is being registered fluently.
     *
     * @since 13.0.0
     */
    public static class ResourceRoute implements VersionableResourceRoute {

        private final Router router;
        private final Filter defaultAuthenticationEnforcer;
        private final AuditFilter auditFilter;
        private final Filter contextFilter;
        private final Filter loggingFilter;
        private final String uriTemplate;

        private Filter authenticationEnforcer;
        private AuditFilterWrapper auditFilterWrapper;
        private List<CrestAuthorizationModule> authorizationModules;
        private List<Filter> filters;

        ResourceRoute(Router router,
                Filter defaultAuthenticationEnforcer, AuditFilter auditFilter, Filter contextFilter,
                Filter loggingFilter, String uriTemplate) {
            this.router = router;
            this.defaultAuthenticationEnforcer = defaultAuthenticationEnforcer;
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
        public ResourceRoute authenticateWith(Filter authenticationFilter) {
            Reject.ifNull(authenticationFilter);
            if (this.authenticationEnforcer != null) {
                throw new IllegalStateException("Authentication Filter has already been set!");
            }
            this.authenticationEnforcer = authenticationFilter;
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
        public ResourceRoute auditAs(AuditConstants.Component component) {
            Reject.ifNull(component);
            if (this.auditFilterWrapper != null) {
                throw new IllegalStateException("Audit component has already been set!");
            }
            this.auditFilterWrapper = new AuditFilterWrapper(auditFilter, component);
            return this;
        }

        /**
         * Specifies the audit component to use when auditing requests to the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param component The audit component.
         * @param filterClass The audit filter class.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        public ResourceRoute auditAs(AuditConstants.Component component, Class<? extends Filter> filterClass) {
            Reject.ifNull(component, filterClass);
            if (this.auditFilterWrapper != null) {
                throw new IllegalStateException("Audit component has already been set!");
            }
            this.auditFilterWrapper = new AuditFilterWrapper(InjectorHolder.getInstance(filterClass), component);
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
        public final ResourceRoute authorizeWith(Class<? extends CrestAuthorizationModule>... authorizationModules) {
            Reject.ifNull(authorizationModules);
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
        public final ResourceRoute through(Class<? extends Filter>... filters) {
            Reject.ifNull(filters);
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
            Reject.ifNull(resourceClass);
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
            Reject.ifNull(resourceKey);
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
            Reject.ifNull(resource);
            forVersion(1).toCollection(resource);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceClass The annotated resource endpoint class. Should be annotated with
         *         {@link org.forgerock.json.resource.annotations.RequestHandler}.
         */
        public void toAnnotatedCollection(Class<? extends Object> resourceClass) {
            Reject.ifNull(resourceClass);
            forVersion(1).toAnnotatedCollection(resourceClass);
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * <p>Using this method to complete the route registration will add a
         * version of {@literal 1} into the route.</p>
         *
         * @param resourceClass The annotated resource endpoint class. Should be annotated with
         *         {@link org.forgerock.json.resource.annotations.RequestHandler}.
         */
        public void toAnnotatedSingleton(Class<? extends Object> resourceClass) {
            Reject.ifNull(resourceClass);
            forVersion(1).toAnnotatedSingleton(resourceClass);
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
            Reject.ifNull(resourceClass);
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
            Reject.ifNull(resourceKey);
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
            Reject.ifNull(resource);
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
            Reject.ifNull(handlerClass);
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
            Reject.ifNull(handlerKey);
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
            Reject.ifNull(handler);
            forVersion(1).toRequestHandler(mode, handler);
        }

        private void addRoute(RoutingMode mode, RequestHandler resource) {
            if (authorizationModules != null && !authorizationModules.isEmpty()) {
                resource = createAuthorizationFilter(resource, authorizationModules);
            }
            if (filters != null) {
                resource = new FilterChain(resource, filters);
            }
            addRoute(mode, uriTemplate, new FilterChain(resource, getFilters()));
        }

        void addRoute(RoutingMode mode, String uriTemplate, RequestHandler resource) {
            router.addRoute(RouteMatchers.requestUriMatcher(mode, uriTemplate), resource);
        }

        private List<Filter> getFilters() {
            List<Filter> filters = new ArrayList<>();
            filters.addAll(Arrays.asList(getAuthenticationEnforcer(), contextFilter, loggingFilter));
            if (auditFilterWrapper != null) {
                filters.add(auditFilterWrapper);
            }
            return filters;
        }

        private Filter getAuthenticationEnforcer() {
            if (authenticationEnforcer == null) {
                authenticationEnforcer = defaultAuthenticationEnforcer;
            }
            return authenticationEnforcer;
        }

        @Override
        public VersionedResourceRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedResourceRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedResourceRoute forVersion(Version version) {
            Reject.ifNull(version);
            return new VersionedResourceRoute(this, version);
        }
    }

    public static final class ServiceRoute implements VersionableServiceRoute {

        private final org.forgerock.http.routing.Router router;
        private final HttpAccessAuditFilterFactory httpAuditFactory;
        private final String uriTemplate;

        private org.forgerock.http.Filter authenticationEnforcer;
        private org.forgerock.http.Filter auditFilter;
        private List<org.forgerock.http.Filter> filters;

        ServiceRoute(org.forgerock.http.routing.Router router, HttpAccessAuditFilterFactory httpAuditFactory,
                String uriTemplate) {
            this.router = router;
            this.httpAuditFactory = httpAuditFactory;
            this.uriTemplate = uriTemplate;
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
        public ServiceRoute auditAs(AuditConstants.Component component) {
            Reject.ifNull(component);
            if (this.auditFilter != null) {
                throw new IllegalStateException("Audit component has already been set!");
            }
            this.auditFilter = httpAuditFactory.createFilter(component);
            return this;
        }

        /**
         * Specifies the audit component to use when auditing requests to the endpoint.
         *
         * <p>Can only be set <strong>once</strong>, attempt to do so twice
         * will result in an {@code IllegalStateException}.</p>
         *
         * @param component The audit component.
         * @param filterClass The audit filter class.
         * @return This route.
         * @throws IllegalStateException If attempted to be set twice.
         */
        public ServiceRoute auditAs(AuditConstants.Component component, Class<? extends Filter> filterClass) {
            Reject.ifNull(component, filterClass);
            if (this.auditFilter != null) {
                throw new IllegalStateException("Audit component has already been set!");
            }
            this.auditFilter = httpAuditFactory.createFilter(component);
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
        public final ServiceRoute through(Class<? extends org.forgerock.http.Filter>... filters) {
            Reject.ifNull(filters);
            if (this.filters != null) {
                throw new IllegalStateException("Filters have already been set!");
            } else {
                this.filters = new ArrayList<>();
            }
            for (Class<? extends org.forgerock.http.Filter> filterClass : filters) {
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
        public void toService(RoutingMode mode, Class<? extends Handler> resourceClass) {
            Reject.ifNull(resourceClass);
            toService(mode, Key.get(resourceClass));
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
        public void toService(RoutingMode mode, Key<?> resourceKey) {
            Reject.ifNull(resourceKey);
            forVersion(1).toService(mode, resourceKey);
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
        public void toService(RoutingMode mode, Object resource) {
            Reject.ifNull(resource);
            forVersion(1).toService(mode, resource);
        }

        private void addRoute(RoutingMode mode, Handler resource) {
            if (filters != null) {
                resource = Handlers.chainOf(resource, filters);
            }
            resource = Handlers.chainOf(resource, getFilters());
            router.addRoute(requestUriMatcher(mode, uriTemplate), resource);
        }

        private List<org.forgerock.http.Filter> getFilters() {
            List<org.forgerock.http.Filter> filters = new ArrayList<>();
            if (auditFilter != null) {
                filters.add(auditFilter);
            }
            return filters;
        }

        @Override
        public VersionedServiceRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedServiceRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedServiceRoute forVersion(Version version) {
            Reject.ifNull(version);
            return new VersionedServiceRoute(this, version);
        }
    }

    /**
     * A route that is being registered fluently, which requires versioning.
     *
     * @since 13.0.0
     */
    public static final class VersionedResourceRoute implements VersionableResourceRoute {

        private final ResourceRoute route;
        private final Version version;
        private final Router versionRouter;

        private List<CrestAuthorizationModule> authorizationModules;
        private List<Filter> filters;

        VersionedResourceRoute(ResourceRoute route, Version version) {
            this(route, version, new Router());
        }

        private VersionedResourceRoute(ResourceRoute route, Version version, Router versionRouter) {
            this.route = route;
            this.version = version;
            this.versionRouter = versionRouter;
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
        public final VersionedResourceRoute authorizeWith(Class<? extends CrestAuthorizationModule>... authorizationModules) {
            Reject.ifNull(authorizationModules);
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
        public final VersionedResourceRoute through(Class<? extends Filter>... filters) {
            Reject.ifNull(filters);
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
        public VersionableResourceRoute toCollection(Class<? extends CollectionResourceProvider> resourceClass) {
            Reject.ifNull(resourceClass);
            return toCollection(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceKey The resource endpoint key.
         */
        public VersionableResourceRoute toCollection(Key<? extends CollectionResourceProvider> resourceKey) {
            Reject.ifNull(resourceKey);
            return addRoute(STARTS_WITH, Resources.newCollection(InjectorHolder.getInstance(resourceKey)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resource The resource endpoint instance.
         */
        public VersionableResourceRoute toCollection(CollectionResourceProvider resource) {
            Reject.ifNull(resource);
            return addRoute(STARTS_WITH, Resources.newCollection(resource));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceClass The annotated resource endpoint class. Should be annotated with
         *         {@link org.forgerock.json.resource.annotations.RequestHandler}.
         */
        public VersionableResourceRoute toAnnotatedCollection(Class<? extends Object> resourceClass) {
            Reject.ifNull(resourceClass);
            return addRoute(STARTS_WITH, Resources.newCollection(InjectorHolder.getInstance(resourceClass)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceClass The annotated resource endpoint class. Should be annotated with
         *         {@link org.forgerock.json.resource.annotations.RequestHandler}.
         */
        public VersionableResourceRoute toAnnotatedSingleton(Class<? extends Object> resourceClass) {
            Reject.ifNull(resourceClass);
            return addRoute(EQUALS, Resources.newSingleton(InjectorHolder.getInstance(resourceClass)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceClass The resource endpoint class.
         */
        public VersionableResourceRoute toSingleton(Class<? extends SingletonResourceProvider> resourceClass) {
            Reject.ifNull(resourceClass);
            return toSingleton(Key.get(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceKey The resource endpoint key.
         */
        public VersionableResourceRoute toSingleton(Key<? extends SingletonResourceProvider> resourceKey) {
            Reject.ifNull(resourceKey);
            return addRoute(EQUALS, Resources.newSingleton(InjectorHolder.getInstance(resourceKey)));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resource The resource endpoint instance.
         */
        public VersionableResourceRoute toSingleton(SingletonResourceProvider resource) {
            Reject.ifNull(resource);
            return addRoute(EQUALS, Resources.newSingleton(resource));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handlerClass The resource endpoint handler class.
         */
        public VersionableResourceRoute toRequestHandler(RoutingMode mode, Class<? extends RequestHandler> handlerClass) {
            Reject.ifNull(handlerClass);
            return toRequestHandler(mode, Key.get(handlerClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handlerKey The resource endpoint handler key.
         */
        public VersionableResourceRoute toRequestHandler(RoutingMode mode, Key<? extends RequestHandler> handlerKey) {
            Reject.ifNull(handlerKey);
            return addRoute(mode, InjectorHolder.getInstance(handlerKey));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param mode The routing mode.
         * @param handler The resource endpoint handler instance.
         */
        public VersionableResourceRoute toRequestHandler(RoutingMode mode, RequestHandler handler) {
            Reject.ifNull(handler);
            return addRoute(mode, handler);
        }

        private VersionableResourceRoute addRoute(RoutingMode mode, RequestHandler resource) {

            if (filters != null) {
                resource = new FilterChain(resource, filters);
            }
            if (authorizationModules != null && !authorizationModules.isEmpty()) {
                resource = createAuthorizationFilter(resource, authorizationModules);
            }

            versionRouter.addRoute(version, resource);

            route.addRoute(mode, versionRouter);

            return this;
        }

        @Override
        public VersionedResourceRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedResourceRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedResourceRoute forVersion(Version version) {
            Reject.ifNull(version);
            return new VersionedResourceRoute(route, version, versionRouter);
        }
    }

    public static final class VersionedServiceRoute implements VersionableServiceRoute {

        private final ServiceRoute route;
        private final Version version;
        private final org.forgerock.http.routing.Router versionRouter;

        private List<org.forgerock.http.Filter> filters;

        VersionedServiceRoute(ServiceRoute route, Version version) {
            this(route, version, new org.forgerock.http.routing.Router());
        }

        private VersionedServiceRoute(ServiceRoute route, Version version, org.forgerock.http.routing.Router versionRouter) {
            this.route = route;
            this.version = version;
            this.versionRouter = versionRouter;
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
        public final VersionedServiceRoute through(Class<? extends org.forgerock.http.Filter>... filters) {
            Reject.ifNull(filters);
            if (this.filters != null) {
                throw new IllegalStateException("Filters have already been set!");
            } else {
                this.filters = new ArrayList<>();
            }
            for (Class<? extends org.forgerock.http.Filter> filterClass : filters) {
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
        public VersionableServiceRoute toService(RoutingMode mode, Class<?> resourceClass) {
            Reject.ifNull(resourceClass);
            return addRoute(mode, Endpoints.from(resourceClass));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resourceKey The resource endpoint key.
         */
        public VersionableServiceRoute toService(RoutingMode mode, Key<?> resourceKey) {
            Reject.ifNull(resourceKey);
            return addRoute(mode, Endpoints.from(resourceKey));
        }

        /**
         * Completes the route registration with the resource that requests
         * matching the route should be routed to.
         *
         * @param resource The resource endpoint instance.
         */
        public VersionableServiceRoute toService(RoutingMode mode, Object resource) { //TODO rename to service
            Reject.ifNull(resource);
            return addRoute(mode, Endpoints.from(resource));
        }

        private VersionableServiceRoute addRoute(RoutingMode mode, Handler resource) {

            if (filters != null) {
                resource = Handlers.chainOf(resource, filters);
            }

            versionRouter.addRoute(requestResourceApiVersionMatcher(version), resource);

            route.addRoute(mode, versionRouter);

            return this;
        }

        @Override
        public VersionedServiceRoute forVersion(int major) {
            return forVersion(version(major));
        }

        @Override
        public VersionedServiceRoute forVersion(int major, int minor) {
            return forVersion(version(major, minor));
        }

        @Override
        public VersionedServiceRoute forVersion(Version version) {
            Reject.ifNull(version);
            return new VersionedServiceRoute(route, version, versionRouter);
        }
    }

    /**
     * A route that is being registered fluently, which can be versioned.
     *
     * @since 13.0.0
     */
    public interface VersionableResourceRoute {

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @return A version route.
         */
        VersionedResourceRoute forVersion(int major);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @param minor The minor version.
         * @return A version route.
         */
        VersionedResourceRoute forVersion(int major, int minor);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param version The version.
         * @return A version route.
         */
        VersionedResourceRoute forVersion(Version version);
    }


    public interface VersionableServiceRoute {

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @return A version route.
         */
        VersionedServiceRoute forVersion(int major);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param major The major version.
         * @param minor The minor version.
         * @return A version route.
         */
        VersionedServiceRoute forVersion(int major, int minor);

        /**
         * Returns a versioned route which can fluently register the route to
         * the specified version of a resource.
         *
         * @param version The version.
         * @return A version route.
         */
        VersionedServiceRoute forVersion(Version version);
    }
}
