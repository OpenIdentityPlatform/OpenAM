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

import javax.annotation.Nonnull;

import org.forgerock.guava.common.base.Function;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.Router;
import org.forgerock.util.Reject;

/**
 * A builder for leaves of the {@link SmsRouteTree}.
 *
 * @since 13.0.0
 */
class SmsRouteTreeLeafBuilder extends SmsRouteTreeBuilder {

    private final String uriTemplate;
    private final Function<String, Boolean> handlesFunction;
    private final Filter filter;

    /**
     * Construct a leaf builder without a filter.
     * @param uriTemplate The URI pattern for this leaf.
     * @param handlesFunction A predicate function that returns true if the leaf handles the service name supplied.
     */
    SmsRouteTreeLeafBuilder(String uriTemplate, Function<String, Boolean> handlesFunction) {
        this(uriTemplate, handlesFunction, null);
    }

    /**
     * Construct a leaf builder with a filter.
     * @param uriTemplate The URI pattern for this leaf.
     * @param handlesFunction A predicate function that returns true if the leaf handles the service name supplied.
     * @param filter A filter.
     */
    SmsRouteTreeLeafBuilder(String uriTemplate, Function<String, Boolean> handlesFunction, Filter filter) {
        super(uriTemplate);
        Reject.ifNull(uriTemplate, handlesFunction);
        this.uriTemplate = uriTemplate;
        this.handlesFunction = handlesFunction;
        this.filter = filter;
    }

    @Override
    SmsRouteTree build(@Nonnull SmsRouteTree parent) {
        Router router = new Router();
        parent.addRoute(true, RoutingMode.STARTS_WITH, uriTemplate, router);
        return new SmsRouteTreeLeaf(parent.authzModules, parent.defaultAuthzModule, router, handlesFunction, filter,
                SmsRouteTree.concat(parent.path, uriTemplate));
    }
}
