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

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.rest.query.QueryException.QueryErrorCode.*;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.openam.utils.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Raw data query filter visitor for resources. It will filter against data read from the data store,
 * which has the following structure: {@code Map<String, Map<String, Set<String>>>}
 * <p>
 * The outer map holds a resource ID as the key. The inner map (value of the outer map) holds the names of
 * the attributes of the resource as keys and a set of values for that attribute as the value.
 * </p>
 * Each visit will return a set of IDs of resources that satisfied the filter criteria.
 *
 * @since 13.0.0
 */
public class DataQueryFilterVisitor implements QueryFilterVisitor<Set<String>, Map<String, Map<String, Set<String>>>> {

    @Override
    public Set<String> visitAndFilter(Map<String, Map<String, Set<String>>> resourceData,
                                      List<QueryFilter> subFilters) {
        final Set<String> andResults = new HashSet<String>();
        boolean firstFilter = true;
        for (QueryFilter filter : subFilters) {
            final Set<String> result = filter.accept(this, resourceData);
            if (firstFilter) {
                andResults.addAll(result);
                firstFilter = false;
            } else {
                andResults.retainAll(result);
            }
        }
        return andResults;
    }

    @Override
    public Set<String> visitOrFilter(Map<String, Map<String, Set<String>>> resourceData, List<QueryFilter> subFilters) {
        final Set<String> orResults = new HashSet<String>();
        for (QueryFilter filter : subFilters) {
            orResults.addAll(filter.accept(this, resourceData));
        }
        return orResults;
    }

    @Override
    public Set<String> visitEqualsFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                         Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return Collections.emptySet();
        }
        assertFieldDepth(field, 1);
        final Set<String> equalsResults = new HashSet<String>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
            if (StringUtils.match(getMapAttr(entry.getValue(), field.get(0)), (String)valueAssertion)) {
                equalsResults.add(entry.getKey());
            }
        }
        return equalsResults;
    }

    @Override
    public Set<String> visitContainsFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                           Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return Collections.emptySet();
        }
        assertFieldDepth(field, 1);
        final Set<String> containsResults = new HashSet<String>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
            if (StringUtils.contains(getMapAttr(entry.getValue(), field.get(0)), (String)valueAssertion)) {
                containsResults.add(entry.getKey());
            }
        }
        return containsResults;
    }

    @Override
    public Set<String> visitStartsWithFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                             Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return Collections.emptySet();
        }
        assertFieldDepth(field, 1);
        final Set<String> containsResults = new HashSet<String>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
            if (StringUtils.startsWith(getMapAttr(entry.getValue(), field.get(0)), (String)valueAssertion)) {
                containsResults.add(entry.getKey());
            }
        }
        return containsResults;
    }

    private void assertFieldDepth(JsonPointer field, int depth) {
        if (field.size() > depth) {
            throw new QueryException(FILTER_DEPTH_SUPPORTED);
        }
    }

    @Override
    public Set<String> visitBooleanLiteralFilter(Map<String, Map<String, Set<String>>> resourceData, boolean value) {
        if (value) {
            return resourceData.keySet();
        }
        throw new QueryException(FILTER_BOOLEAN_LITERAL_FALSE);
    }

    @Override
    public Set<String> visitExtendedMatchFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                JsonPointer field, String operator, Object valueAssertion) {
        throw new QueryException(FILTER_EXTENDED_MATCH);
    }

    @Override
    public Set<String> visitGreaterThanFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                              Object valueAssertion) {
        throw new QueryException(FILTER_GREATER_THAN);
    }

    @Override
    public Set<String> visitGreaterThanOrEqualToFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                       JsonPointer field, Object valueAssertion) {
        throw new QueryException(FILTER_GREATER_THAN_OR_EQUAL);
    }

    @Override
    public Set<String> visitLessThanFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                           Object valueAssertion) {
        throw new QueryException(FILTER_LESS_THAN);
    }

    @Override
    public Set<String> visitLessThanOrEqualToFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                    JsonPointer field, Object valueAssertion) {
        throw new QueryException(FILTER_LESS_THAN_OR_EQUAL);
    }

    @Override
    public Set<String> visitNotFilter(Map<String, Map<String, Set<String>>> resourceData, QueryFilter subFilter) {
        throw new QueryException(FILTER_NOT);
    }

    @Override
    public Set<String> visitPresentFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field) {
        throw new QueryException(FILTER_PRESENT);
    }
}
