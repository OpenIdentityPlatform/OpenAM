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

package org.forgerock.openam.core.rest.sms.tree;

import static java.util.Collections.emptySet;
import static com.google.common.base.Predicates.alwaysFalse;
import static org.forgerock.json.resource.ResourcePath.empty;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.RouteMatcher;

/**
 * A builder for creating {@link SmsRouteTree} trees.
 *
 * @since 13.0.0
 */
public class SmsRouteTreeBuilder {

    private final String uriTemplate;
    private boolean supportGeneralActions = false;
    private Set<SmsRouteTreeBuilder> subTreeBuilders = emptySet();
    private Predicate<String> handlesFunction = alwaysFalse();
    private Filter filter;

    /**
     * Create a route tree.
     * @param uriTemplate The URI template for this node in the tree.
     */
    SmsRouteTreeBuilder(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    /**
     * Creates a {@code SmsRouteTree} structure with the provided sub trees.
     *
     * @param subTreeBuilders The sub trees.
     * @return A {@code SmsRouteTree}.
     */
    public static SmsRouteTree tree(Map<MatchingResourcePath, CrestAuthorizationModule> authModules,
            CrestAuthorizationModule defaultAuthModule,
            SmsRouteTreeBuilder... subTreeBuilders) {
        SmsRouter router = new SmsRouter();
        SmsRouteTree tree = new SmsRouteTree(authModules, defaultAuthModule, true, router, null, empty(),
                Predicates.<String>alwaysFalse(), null, false);
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
    public static SmsRouteTreeBuilder branch(String uriTemplate, SmsRouteTreeBuilder... subTreeBuilders) {
        return new SmsRouteTreeBuilder(uriTemplate).subTrees(subTreeBuilders);
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
    public static SmsRouteTreeBuilder branch(String uriTemplate, Predicate<String> handlesFunction,
            SmsRouteTreeBuilder... subTreeBuilders) {
        return new SmsRouteTreeBuilder(uriTemplate).handles(handlesFunction).subTrees(subTreeBuilders);
    }

    /**
     * Creates a builder which adds a leaf to the route tree.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param handlesFunction The function that determines whether this router should handle the
     *                        service being registered.
     * @param generalActions Whether the tree leaf (and nodes attached to it) will support the general actions in
     *                       {@link SmsRouteTree#handleAction(Context, ActionRequest)}.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    public static SmsRouteTreeBuilder leaf(String uriTemplate, Predicate<String> handlesFunction,
            boolean generalActions) {
        return new SmsRouteTreeBuilder(uriTemplate).handles(handlesFunction).supportGeneralActions(generalActions);
    }

    /**
     * Creates a builder which adds a leaf to the route tree, through the provided filter.
     *
     * @param uriTemplate The uri template that matches roots from the parent router.
     * @param handlesFunction The function that determines whether this router should handle the
     *                        service being registered.
     * @param filter The filter to route through.
     * @return A {@code SmsRouteTreeBuilder}.
     */
    public static SmsRouteTreeBuilder filter(String uriTemplate, Predicate<String> handlesFunction, Filter filter) {
        return new SmsRouteTreeBuilder(uriTemplate).handles(handlesFunction).filtered(filter);
    }


    /**
     * Add subtrees to this tree node.
     * @param subTreeBuilders The builders of the subtrees.
     * @return This builder.
     */
    SmsRouteTreeBuilder subTrees(SmsRouteTreeBuilder... subTreeBuilders) {
        this.subTreeBuilders = asSet(subTreeBuilders);
        return this;
    }

    /**
     * Specify that this tree node handles a particular service.
     * @param handlesFunction The handle function for requests handled by this node.
     * @return This buidler.
     */
    SmsRouteTreeBuilder handles(Predicate<String> handlesFunction) {
        this.handlesFunction = handlesFunction;
        return this;
    }

    /**
     * Specify that this tree node needs filtering.
     * @param filter The filter to apply before this tree node.
     * @return This buidler.
     */
    SmsRouteTreeBuilder filtered(Filter filter) {
        this.filter = filter;
        return this;
    }

    SmsRouteTreeBuilder supportGeneralActions(boolean supportGeneralActions) {
        this.supportGeneralActions = supportGeneralActions;
        return this;
    }

    /**
     * Build the tree from the provided parent.
     * @param parent The parent tree.
     * @return The built sub-tree.
     */
    SmsRouteTree build(@Nonnull SmsRouteTree parent) {
        SmsRouter router = new SmsRouter();

        ResourcePath path = SmsRouteTree.concat(parent.path, uriTemplate);
        SmsRouteTree tree = new SmsRouteTree(parent.authzModules, parent.defaultAuthzModule, false, router, filter,
                path, handlesFunction, uriTemplate, supportGeneralActions);
        for (SmsRouteTreeBuilder subTreeBuilder : subTreeBuilders) {
            tree.addSubTree(subTreeBuilder.build(tree));
        }
        return tree;
    }

    /**
     * A router for the SmsRouteTree that allows access to the internal routes.
     */
    static class SmsRouter extends Router {
        /**
         * Get the routes map.
         * @return The map.
         */
        public Map<RouteMatcher<Request>, RequestHandler> getAllRoutes() {
            return getRoutes();
        }
    }
}
