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
package org.forgerock.openam.rest.query;

import static org.forgerock.openam.rest.query.QueryException.QueryErrorCode.FILTER_DEPTH_NOT_SUPPORTED;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use this {@code org.forgerock.json.resource.QueryFilterVisitor} implementation to convert a
 * {@code org.forgerock.json.resource.QueryFilter} to a {@code org.forgerock.util.query.QueryFilter<String>}.
 *
 * @since 13.0.0
 */
public class QueryByStringFilterConverter implements QueryFilterVisitor<QueryFilter<String>, Void> {

    private void assertFieldDepth(JsonPointer field, int depth) {
        if (field.size() > depth) {
            throw new QueryException(FILTER_DEPTH_NOT_SUPPORTED);
        }
    }

    @Override
    public QueryFilter<String> visitAndFilter(Void aVoid, List<org.forgerock.json.resource.QueryFilter> subFilters) {
        Set<QueryFilter<String>> andResults = new HashSet<>();
        for (org.forgerock.json.resource.QueryFilter filter : subFilters) {
            QueryFilter<String> result = filter.accept(this, aVoid);
            if (result != null) {
                andResults.add(result);
            }
        }
        return QueryFilter.and(andResults);
    }

    @Override
    public QueryFilter<String> visitOrFilter(Void aVoid, List<org.forgerock.json.resource.QueryFilter> subFilters) {
        Set<QueryFilter<String>> andResults = new HashSet<>();
        for (org.forgerock.json.resource.QueryFilter filter : subFilters) {
            QueryFilter<String> result = filter.accept(this, aVoid);
            if (result != null) {
                andResults.add(result);
            }
        }
        return QueryFilter.or(andResults);
    }

    @Override
    public QueryFilter<String> visitBooleanLiteralFilter(Void aVoid, boolean value) {
        return value ? QueryFilter.<String>alwaysTrue() : QueryFilter.<String>alwaysFalse();
    }

    @Override
    public QueryFilter<String> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.contains(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.equalTo(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator,
                                                        Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.extendedMatch(field.get(0), operator, valueAssertion);
    }

    @Override
    public QueryFilter<String> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.greaterThan(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.greaterThanOrEqualTo(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.lessThan(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.lessThanOrEqualTo(field.get(0), valueAssertion);
    }

    @Override
    public QueryFilter<String> visitNotFilter(Void aVoid, org.forgerock.json.resource.QueryFilter subFilter) {
        return QueryFilter.not(subFilter.accept(this, aVoid));
    }

    @Override
    public QueryFilter<String> visitPresentFilter(Void aVoid, JsonPointer field) {
        assertFieldDepth(field, 1);
        return QueryFilter.present(field.get(0));
    }

    @Override
    public QueryFilter<String> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        assertFieldDepth(field, 1);
        return QueryFilter.startsWith(field.get(0), valueAssertion);
    }
}
