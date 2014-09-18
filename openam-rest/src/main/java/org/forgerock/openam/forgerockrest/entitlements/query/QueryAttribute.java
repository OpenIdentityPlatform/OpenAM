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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements.query;

import com.sun.identity.entitlement.util.SearchFilter;
import org.forgerock.util.Reject;

/**
 * An attribute that can be used in a query filter for querying policies.
 *
 * @since 12.0.0
 */
public final class QueryAttribute {
    private final AttributeType type;
    private final String attributeName;

    public QueryAttribute(AttributeType type, String attributeName) {
        Reject.ifNull(type, attributeName);
        this.type = type;
        this.attributeName = attributeName;
    }

    /**
     * Gets a search filter that matches the given operator and value for this attribute.
     *
     * @param operator the operator to use.
     * @param value the value to compare against.
     * @return an appropriate search filter for this attribute.
     * @throws UnsupportedOperationException if the operator is not valid for this attribute.
     * @throws NullPointerException if either argument is null.
     */
    public SearchFilter getFilter(SearchFilter.Operator operator, Object value) {
        Reject.ifNull(operator, value);
        return type.getFilter(attributeName, operator, value);
    }
}
