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
package org.forgerock.openam.entitlement.configuration;

import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Query filter visitor that understands attributes associated with the SMS layer.
 * <p/>
 * Due to the current way attributes are stored in LDAP via the SMS layer,
 * only supported operator is equality. All logical operators are supported.
 *
 * @since 13.0.0
 */
public class SmsQueryFilterVisitor extends BaseQueryFilterVisitor<Filter, Void, SmsAttribute> {

    @Override
    public Filter visitAndFilter(Void aVoid, List<QueryFilter<SmsAttribute>> subFilters) {
        return Filter.and(getSubFilters(subFilters));
    }

    @Override
    public Filter visitBooleanLiteralFilter(Void aVoid, boolean value) {
        return value ? Filter.alwaysTrue() : Filter.alwaysFalse();
    }

    @Override
    public Filter visitEqualsFilter(Void aVoid, SmsAttribute field, Object valueAssertion) {
        return Filter.equality(field.toString(), valueAssertion);
    }

    @Override
    public Filter visitNotFilter(Void aVoid, QueryFilter<SmsAttribute> subFilter) {
        return Filter.not(subFilter.accept(this, null));
    }

    @Override
    public Filter visitOrFilter(Void aVoid, List<QueryFilter<SmsAttribute>> subFilters) {
        return Filter.or(getSubFilters(subFilters));
    }

    /**
     * Given the child query filters, returns a list of corresponding child filters.
     *
     * @param subQueryFilters
     *         child query filters
     *
     * @return corresponding child filters
     */
    private List<Filter> getSubFilters(List<QueryFilter<SmsAttribute>> subQueryFilters) {
        final List<Filter> subFilters = new ArrayList<Filter>(subQueryFilters.size());
        for (QueryFilter<SmsAttribute> subFilter : subQueryFilters) {
            subFilters.add(subFilter.accept(this, null));
        }
        return subFilters;
    }

}
