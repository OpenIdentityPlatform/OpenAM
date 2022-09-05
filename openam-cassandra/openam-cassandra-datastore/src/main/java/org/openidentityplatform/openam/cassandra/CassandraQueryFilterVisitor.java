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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CassandraQueryFilterVisitor implements QueryFilterVisitor<CassandraFilter, Void, JsonPointer> {

    CassandraFilter cassandraFilter = new CassandraFilter();

    @Override
    public CassandraFilter visitAndFilter(Void unused, List<QueryFilter<JsonPointer>> list) {
        for (QueryFilter<JsonPointer> filter: list) {
            filter.accept(this, null);
        }
        return this.cassandraFilter;
    }

    @Override
    public CassandraFilter visitBooleanLiteralFilter(Void unused, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitContainsFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitEqualsFilter(Void unused, JsonPointer strings, Object o) {
        for (String key : strings) {
            Set<String> values = cassandraFilter.getFilter().getOrDefault(key, new HashSet<>());
            values.add(o.toString());
            cassandraFilter.getFilter().put(key, values);
        }
        return cassandraFilter;
    }

    @Override
    public CassandraFilter visitExtendedMatchFilter(Void unused, JsonPointer strings, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitGreaterThanFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitGreaterThanOrEqualToFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitLessThanFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitLessThanOrEqualToFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitNotFilter(Void unused, QueryFilter<JsonPointer> queryFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitOrFilter(Void unused, List<QueryFilter<JsonPointer>> list) {
        for (QueryFilter<JsonPointer> filter: list) {
            filter.accept(this, null);
        }
        this.cassandraFilter.setFilterOp(Repo.OR_MOD);
        return this.cassandraFilter;
    }

    @Override
    public CassandraFilter visitPresentFilter(Void unused, JsonPointer strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CassandraFilter visitStartsWithFilter(Void unused, JsonPointer strings, Object o) {
        throw new UnsupportedOperationException();
    }
}
