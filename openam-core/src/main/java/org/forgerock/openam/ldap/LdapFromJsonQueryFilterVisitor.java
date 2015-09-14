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
package org.forgerock.openam.ldap;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is primarily intended to convert Json (from an incoming query filter) into an LDAP
 * compatible query.  This is based on James's {@link org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilterVisitor}.
 */
public class LdapFromJsonQueryFilterVisitor implements QueryFilterVisitor<Filter, Void, JsonPointer> {

    @Override
    public Filter visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subQueryFilters) {
        return Filter.and(getSubFilters(subQueryFilters));
    }

    @Override
    public Filter visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subQueryFilters) {
        return Filter.or(getSubFilters(subQueryFilters));
    }

    private List<Filter> getSubFilters(List<QueryFilter<JsonPointer>> subQueryFilters) {
        List<Filter> subFilters = new ArrayList<Filter>(subQueryFilters.size());
        for (QueryFilter<JsonPointer> subFilter : subQueryFilters) {
            subFilters.add(subFilter.accept(this, null));
        }
        return subFilters;
    }

    @Override
    public Filter visitNotFilter(Void aVoid, QueryFilter<JsonPointer> queryFilter) {
        return Filter.not(queryFilter.accept(this, null));
    }

    @Override
    public Filter visitBooleanLiteralFilter(Void aVoid, boolean value) {
        return value ? Filter.alwaysTrue() : Filter.alwaysFalse();
    }

    @Override
    public Filter visitContainsFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.substrings(fieldName(field), null, Arrays.asList(value), null);
    }

    @Override
    public Filter visitEqualsFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.equality(fieldName(field), value);
    }

    @Override
    public Filter visitGreaterThanFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.greaterThan(fieldName(field), value);
    }

    @Override
    public Filter visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.greaterOrEqual(fieldName(field), value);
    }

    @Override
    public Filter visitLessThanFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.lessThan(fieldName(field), value);
    }

    @Override
    public Filter visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.lessOrEqual(fieldName(field), value);
    }

    @Override
    public Filter visitPresentFilter(Void aVoid, JsonPointer field) {
        return Filter.present(fieldName(field));
    }

    @Override
    public Filter visitStartsWithFilter(Void aVoid, JsonPointer field, Object value) {
        return Filter.substrings(fieldName(field), value, null, null);
    }

    @Override
    public Filter visitExtendedMatchFilter(Void aVoid, JsonPointer coreTokenField, String operator, Object value) {
        throw new UnsupportedOperationException();
    }

    private String fieldName(JsonPointer field) {
        if (field == null || StringUtils.isBlank(field.toString())) {
            throw new IllegalArgumentException("Cannot determine field name from JsonPointer object");
        }
        String name = field.toString();
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        return name;
    }
}
