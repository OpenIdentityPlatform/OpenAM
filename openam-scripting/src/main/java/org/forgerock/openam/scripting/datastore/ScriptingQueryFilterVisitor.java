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
package org.forgerock.openam.scripting.datastore;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.openam.utils.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Query filter visitor for scripting resources. It will filter against data read from the data store,
 * which has the following structure: {@code Map<String, Map<String, Set<String>>>}
 * <p>
 * The outer map holds a resource UUID as the key. The inner map (value of the outer map) holds the names of
 * the attributes of the resource as keys and a set of values for that attribute as the value.
 * </p>
 * Each visit will return a set of UUIDs of resources that satisfied the filter criteria.
 *
 * @since 13.0.0
 */
public class ScriptingQueryFilterVisitor implements QueryFilterVisitor<Set<String>, Map<String, Map<String,
        Set<String>>>> {

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
        assertFieldDepth(field, 1);
        final Set<String> equalsResults = new HashSet<String>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
            if (valueAssertion.equals(getMapAttr(entry.getValue(), field.get(0)))) {
                equalsResults.add(entry.getKey());
            }
        }
        return equalsResults;
    }

    @Override
    public Set<String> visitContainsFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                           Object valueAssertion) {
        assertFieldDepth(field, 1);
        final Set<String> containsResults = new HashSet<String>();
        if (valueAssertion instanceof String) {
            for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
                final String value = getMapAttr(entry.getValue(), field.get(0));
                if (StringUtils.isNotEmpty(value) && value.contains((String) valueAssertion)) {
                    containsResults.add(entry.getKey());
                }
            }
        }
        return containsResults;
    }

    @Override
    public Set<String> visitStartsWithFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                             Object valueAssertion) {
        assertFieldDepth(field, 1);
        final Set<String> containsResults = new HashSet<String>();
        if (valueAssertion instanceof String) {
            for (Map.Entry<String, Map<String, Set<String>>> entry : resourceData.entrySet()) {
                String value = getMapAttr(entry.getValue(), field.get(0));
                if (StringUtils.isNotEmpty(value) && value.startsWith((String) valueAssertion)) {
                    containsResults.add(entry.getKey());
                }
            }
        }
        return containsResults;
    }

    private void assertFieldDepth(JsonPointer field, int depth) {
        if (field.size() > depth) {
            throw new UnsupportedOperationException(RESOURCE_FILTER_NOT_SUPPORTED.name());
        }
    }

    @Override
    public Set<String> visitBooleanLiteralFilter(Map<String, Map<String, Set<String>>> resourceData, boolean value) {
        if (value) {
            return resourceData.keySet();
        }
        throw new UnsupportedOperationException(FILTER_BOOLEAN_LITERAL_FALSE.name());
    }

    @Override
    public Set<String> visitExtendedMatchFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                JsonPointer field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException(FILTER_EXTENDED_MATCH.name());
    }

    @Override
    public Set<String> visitGreaterThanFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                              Object valueAssertion) {
        throw new UnsupportedOperationException(FILTER_GREATER_THAN.name());
    }

    @Override
    public Set<String> visitGreaterThanOrEqualToFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                       JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException(FILTER_GREATER_THAN_OR_EQUAL.name());
    }

    @Override
    public Set<String> visitLessThanFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field,
                                           Object valueAssertion) {
        throw new UnsupportedOperationException(FILTER_LESS_THAN.name());
    }

    @Override
    public Set<String> visitLessThanOrEqualToFilter(Map<String, Map<String, Set<String>>> resourceData,
                                                    JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException(FILTER_LESS_THAN_OR_EQUAL.name());
    }

    @Override
    public Set<String> visitNotFilter(Map<String, Map<String, Set<String>>> resourceData, QueryFilter subFilter) {
        throw new UnsupportedOperationException(FILTER_NOT.name());
    }

    @Override
    public Set<String> visitPresentFilter(Map<String, Map<String, Set<String>>> resourceData, JsonPointer field) {
        throw new UnsupportedOperationException(FILTER_PRESENT.name());
    }
}
