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

package org.forgerock.openam.rest.sms;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.guava.common.base.Function;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

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
    static SmsRouteTree tree(SmsRouteTreeBuilder... subTreeBuilders) {
        Router router = new Router();
        return createRouteTree(true, router, Arrays.asList(subTreeBuilders));
    }

    private static SmsRouteTree createRouteTree(boolean isRoot, Router router, Collection<SmsRouteTreeBuilder> subTreeBuilders) {
        Set<SmsRouteTree> subTrees = new HashSet<SmsRouteTree>();
        for (SmsRouteTreeBuilder subTreeBuilder : subTreeBuilders) {
            subTrees.add(subTreeBuilder.build(router));
        }
        return new SmsRouteTree(isRoot, router, subTrees);
    }

    /**
     * Creates a builder which adds a branch to the route tree.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param subTreeBuilders The sub trees.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    static SmsRouteTreeBuilder branch(String uriTemplate, SmsRouteTreeBuilder... subTreeBuilders) {
        return new SmsRouteTreeBuilder(new Router(), uriTemplate, subTreeBuilders);
    }

    /**
     * Creates a builder which adds a leaf to the route tree.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param handlesFunction The function that determines whether this router should handle the
     *                        route being registered.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    static SmsRouteTreeBuilder leaf(String uriTemplate, Function<String, Boolean> handlesFunction) {
        return new SmsRouteTreeLeafBuilder(new Router(), uriTemplate, handlesFunction);
    }

    private final boolean isRoot;
    private final Router router;
    private final Set<SmsRouteTree> subTrees;

    /**
     * Creates a {@code SmsRouteTree} instance.
     *
     * @param isRoot {@code true} if this {@code SmsRouteTree} is the root of the tree.
     * @param router The {@code Router} instance.
     * @param subTrees Sub trees of this tree node.
     */
    SmsRouteTree(boolean isRoot, Router router, Set<SmsRouteTree> subTrees) {
        this.isRoot = isRoot;
        this.router = router;
        this.subTrees = subTrees;
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
    final Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        return router.addRoute(mode, uriTemplate, handler);
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while this router is
     * processing requests.
     *
     * @param routes The routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    final boolean removeRoute(Route... routes) {
        return router.removeRoute(routes);
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        router.handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        router.handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        router.handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        router.handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        router.handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        router.handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        router.handleUpdate(context, request, handler);
    }

    /**
     * Builder for creating {@code SmsRouteTree} instances.
     *
     * @since 13.0.0
     */
    static class SmsRouteTreeBuilder {

        private final Router router;
        private final String uriTemplate;
        private final Set<SmsRouteTreeBuilder> subTreeBuilders;

        private SmsRouteTreeBuilder(Router router, String uriTemplate, SmsRouteTreeBuilder... subTreeBuilders) {
            this.router = router;
            this.uriTemplate = uriTemplate;
            this.subTreeBuilders = new HashSet<SmsRouteTreeBuilder>(Arrays.asList(subTreeBuilders));
        }

        SmsRouteTree build(Router parent) {
            parent.addRoute(RoutingMode.STARTS_WITH, uriTemplate, router);
            return createRouteTree(false, router, subTreeBuilders);
        }
    }

    /**
     * Builder for creating {@code SmsRouteTreeLeaf} instances.
     *
     * @since 13.0.0
     */
    static final class SmsRouteTreeLeafBuilder extends SmsRouteTreeBuilder {

        private final Router router;
        private final String uriTemplate;
        private final Function<String, Boolean> handlesFunction;

        private SmsRouteTreeLeafBuilder(Router router, String uriTemplate, Function<String, Boolean> handlesFunction) {
            super(router, uriTemplate);
            this.router = router;
            this.uriTemplate = uriTemplate;
            this.handlesFunction = handlesFunction;
        }

        @Override
        SmsRouteTree build(Router parent) {
            parent.addRoute(RoutingMode.STARTS_WITH, uriTemplate, router);
            return new SmsRouteTreeLeaf(router, handlesFunction);
        }
    }
}
