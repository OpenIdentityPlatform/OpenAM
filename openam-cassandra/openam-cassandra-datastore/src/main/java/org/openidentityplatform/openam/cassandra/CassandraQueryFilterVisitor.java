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
 * Copyright 2022 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.cassandra;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CassandraQueryFilterVisitor implements QueryFilterVisitor<Map<String, Set<String>>, Void, JsonPointer> {

    final Map<String, Set<String>> filter = new HashMap<>();

    @Override
    public Map<String, Set<String>> visitAndFilter(Void unused, List<QueryFilter<JsonPointer>> list) {
        for (QueryFilter<JsonPointer> filter: list) {
            filter.accept(this, null);
        }
        return this.filter;
    }

    @Override
    public Map<String, Set<String>> visitBooleanLiteralFilter(Void unused, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitContainsFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitEqualsFilter(Void unused, JsonPointer strings, Object o) {
        for (String key : strings) {
            Set<String> values = this.filter.getOrDefault(key, new HashSet<>());
            values.add(o.toString());
            this.filter.put(key, values);
        }
        return this.filter;
    }

    @Override
    public Map<String, Set<String>> visitExtendedMatchFilter(Void unused, JsonPointer strings, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitGreaterThanFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitGreaterThanOrEqualToFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitLessThanFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitLessThanOrEqualToFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitNotFilter(Void unused, QueryFilter<JsonPointer> queryFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitOrFilter(Void unused, List<QueryFilter<JsonPointer>> list) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitPresentFilter(Void unused, JsonPointer strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<String>> visitStartsWithFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }
}
