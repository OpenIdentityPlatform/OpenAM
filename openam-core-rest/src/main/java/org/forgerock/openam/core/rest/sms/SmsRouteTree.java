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

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.Router.*;
import static org.forgerock.openam.forgerockrest.utils.MatchingResourcePath.match;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guava.common.base.Function;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.util.promise.Promise;

/**
 * Represents a {@code Router} tree. Allows the structuring of Routers in a tree like manner whilst
 * offering a flat entry point to add and remove routes and services.
 *
 * @since 13.0.0
 */
class SmsRouteTree implements RequestHandler {

    /**
     * Creates a {@code SmsRouteTree} structure with the provided sub trees.
     *
     * @param subTreeBuilders The sub trees.
     * @return A {@code SmsRouteTree}.
     */
    static SmsRouteTree tree(Map<MatchingResourcePath, CrestAuthorizationModule> authModules,
            CrestAuthorizationModule defaultAuthModule,
            SmsRouteTreeBuilder... subTreeBuilders) {
        Router router = new Router();
        SmsRouteTree tree = new SmsRouteTree(authModules, defaultAuthModule, true, router, null, ResourcePath.empty());
        for (SmsRouteTreeBuilder subTreeBuilder : subTreeBuilders) {
            tree.addSubTree(subTreeBuilder.build(tree));
        }
        return tree;
    }

    /**
     * Creates a builder which adds a branch to the route tree.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param subTreeBuilders The sub trees.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    static SmsRouteTreeBuilder branch(String uriTemplate, SmsRouteTreeBuilder... subTreeBuilders) {
        return new SmsRouteTreeBuilder(uriTemplate, subTreeBuilders);
    }

    /**
     * Creates a builder which adds a branch to the route tree, and handles services at this path.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param handlesFunction The function that determines whether this router should handle the
     *                        service being registered.
     * @param subTreeBuilders The sub trees.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    static SmsRouteTreeBuilder branch(String uriTemplate, Function<String, Boolean> handlesFunction,
            SmsRouteTreeBuilder... subTreeBuilders) {
        return new SmsRouteTreeBuilder(uriTemplate, handlesFunction, subTreeBuilders);
    }

    /**
     * Creates a builder which adds a leaf to the route tree.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param handlesFunction The function that determines whether this router should handle the
     *                        service being registered.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    static SmsRouteTreeBuilder leaf(String uriTemplate, Function<String, Boolean> handlesFunction) {
        return new SmsRouteTreeLeafBuilder(uriTemplate, handlesFunction);
    }

    static SmsRouteTreeBuilder filter(String uriTemplate, Function<String, Boolean> handlesFunction, Filter filter) {
        return new SmsRouteTreeLeafBuilder(uriTemplate, handlesFunction, filter);
    }

    final Map<MatchingResourcePath, CrestAuthorizationModule> authzModules;
    final CrestAuthorizationModule defaultAuthzModule;
    final Router router;
    final ResourcePath path;
    private final boolean isRoot;
    private final Set<SmsRouteTree> subTrees;
    private final Filter filter;

    /**
     * Creates a {@code SmsRouteTree} instance.
     * @param authzModules Authz modules to use for specific matching resource paths.
     * @param defaultAuthzModule Auth module to use if no matching resouce path exists in {@code authzModules}.
     * @param isRoot {@code true} if this {@code SmsRouteTree} is the root of the tree.
     * @param router The {@code Router} instance.
     * @param filter The filter to wrap around all routes.
     * @param path The path of this tree element.
     */
    SmsRouteTree(Map<MatchingResourcePath, CrestAuthorizationModule> authzModules,
            CrestAuthorizationModule defaultAuthzModule, boolean isRoot, Router router, Filter filter,
            ResourcePath path) {
        this.authzModules = authzModules;
        this.defaultAuthzModule = defaultAuthzModule;
        this.isRoot = isRoot;
        this.router = router;
        this.path = path;
        this.subTrees = new HashSet<>();
        this.filter = filter;
    }

    final void addSubTree(SmsRouteTree subTree) {
        this.subTrees.add(subTree);
    }

    /**
     * Returns the {@code SmsRouteTree} instance that the route should be added to.
     *
     * @param serviceName The name of the service that the route is being added for.
     * @return The {@code SmsRouteTree} to handle the route.
     */
    SmsRouteTree handles(String serviceName) {
        for (SmsRouteTree subTree : subTrees) {
            SmsRouteTree tree = subTree.handles(serviceName);
            if (tree != null) {
                return tree;
            }
        }
        if (isRoot) {
            return this;
        } else {
            return null;
        }
    }

    /**
     * Adds a new route to this router for the provided request handler. New routes may be added
     * while this router is processing requests.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The request handler to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    final RouteMatcher<Request> addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        return addRoute(false, mode, uriTemplate, handler);
    }

    final RouteMatcher<Request> addRoute(boolean internal, RoutingMode mode, String uriTemplate,
            RequestHandler handler) {
        CrestAuthorizationModule authzModule = authzModules.get(match(concat(path, uriTemplate)));
        if (authzModule != null || !internal) {
            handler = createAuthorizationFilter(handler, authzModule == null ? defaultAuthzModule : authzModule);
        }

        if (filter != null) {
            handler = new FilterChain(handler, filter);
        }

        return router.addRoute(mode, uriTemplate(uriTemplate), handler);
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while this router is
     * processing requests.
     *
     * @param routes The routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    final boolean removeRoute(RouteMatcher<Request>... routes) {
        return router.removeRoute(routes);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return router.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return router.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return router.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return router.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return router.handleQuery(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return router.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return router.handleUpdate(context, request);
    }

    static ResourcePath concat(ResourcePath parent, String child) {
        if (StringUtils.isEmpty(child)) {
            return parent;
        }
        ResourcePath childPath = resourcePath(child);
        return parent.concat(childPath);
    }

}
