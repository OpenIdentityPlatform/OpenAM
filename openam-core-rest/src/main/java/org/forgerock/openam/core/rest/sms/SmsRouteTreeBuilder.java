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

import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.Set;

import javax.annotation.Nonnull;

import org.forgerock.guava.common.base.Function;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Router;

/**
 * A builder for creating {@link SmsRouteTree} trees.
 *
 * @since 13.0.0
 */
class SmsRouteTreeBuilder {

    private final String uriTemplate;
    private final Set<SmsRouteTreeBuilder> subTreeBuilders;
    private final Function<String, Boolean> handlesFunction;

    /**
     * Create a route tree with the specified sub trees.
     * @param uriTemplate The URI template for this node in the tree.
     * @param subTreeBuilders The builders of the subtrees.
     */
    SmsRouteTreeBuilder(String uriTemplate, SmsRouteTreeBuilder... subTreeBuilders) {
        this(uriTemplate, null, subTreeBuilders);
    }

    /**
     * Create a route tree with the specified sub trees, with a node that will handle requests itself.
     * @param uriTemplate The URI template for this node in the tree.
     * @param subTreeBuilders The builders of the subtrees.
     * @param handlesFunction The handle function for requests handled by this node.
     */
    SmsRouteTreeBuilder(String uriTemplate, Function<String, Boolean> handlesFunction,
            SmsRouteTreeBuilder... subTreeBuilders) {
        this.uriTemplate = uriTemplate;
        this.subTreeBuilders = asSet(subTreeBuilders);
        this.handlesFunction = handlesFunction;
    }

    /**
     * Build the tree from the provided parent.
     * @param parent The parent tree.
     * @return The built sub-tree.
     */
    SmsRouteTree build(@Nonnull SmsRouteTree parent) {
        Router router = new Router();

        ResourcePath path = SmsRouteTree.concat(parent.path, uriTemplate);
        SmsRouteTree tree = new SmsRouteTree(parent.authzModules, parent.defaultAuthzModule, false, router, null,
                path);
        for (SmsRouteTreeBuilder subTreeBuilder : subTreeBuilders) {
            tree.addSubTree(subTreeBuilder.build(tree));
        }
        parent.router.addRoute(RoutingMode.STARTS_WITH, uriTemplate(uriTemplate), router);
        if (handlesFunction != null) {
            tree.addSubTree(new SmsRouteTreeLeaf(parent.authzModules, parent.defaultAuthzModule, router, handlesFunction,
                    null, path));
        }
        return tree;
    }
}
