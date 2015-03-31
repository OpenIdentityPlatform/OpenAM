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

import java.util.Collections;

import org.forgerock.guava.common.base.Function;
import org.forgerock.json.resource.Router;

/**
 * Represents a leaf of {@code Router} tree. A leaf tree node should handle routes that a service
 * requires adding.
 *
 * @see SmsRouteTree
 * @since 13.0.0
 */
class SmsRouteTreeLeaf extends SmsRouteTree {

    private final Function<String, Boolean> handlesFunction;

    /**
     * Creates a {@code SmsRouteTreeLeaf} instance.
     *
     * @param router The {@code Router} instance.
     * @param handlesFunction A {@code Function} that determines whether this router should handle
     *                        the route.
     */
    SmsRouteTreeLeaf(Router router, Function<String, Boolean> handlesFunction) {
        super(false, router, Collections.<SmsRouteTree>emptySet());
        this.handlesFunction = handlesFunction;
    }

    /**
     * Returns this {@code SmsRouteTreeLeaf} if the {@code handlesFunction} returns {@code true},
     * otherwise returns {@code null}.
     *
     * @param serviceName {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    SmsRouteTree handles(String serviceName) {
        if (handlesFunction.apply(serviceName)) {
            return this;
        } else {
            return null;
        }
    }
}
