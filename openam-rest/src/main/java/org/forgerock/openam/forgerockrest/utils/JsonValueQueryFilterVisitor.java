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

package org.forgerock.openam.forgerockrest.utils;

import java.util.List;

import org.forgerock.guava.common.base.Predicate;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A query filter visitor that can be used to test whether a JsonValue matches a given
 * {@code QueryFilter}. String operations are compared case-insensitively.
 */
public class JsonValueQueryFilterVisitor implements QueryFilterVisitor<Boolean, JsonValue, JsonPointer> {

    @Override
    public Boolean visitAndFilter(JsonValue jsonValue, List<QueryFilter<JsonPointer>> list) {
        for (QueryFilter<JsonPointer> subfilter : list) {
            if (!subfilter.accept(this, jsonValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visitOrFilter(JsonValue jsonValue, List<QueryFilter<JsonPointer>> list) {
        for (QueryFilter<JsonPointer> subfilter : list) {
            if (subfilter.accept(this, jsonValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNotFilter(JsonValue jsonValue, QueryFilter<JsonPointer> queryFilter) {
        return !queryFilter.accept(this, jsonValue);
    }

    @Override
    public Boolean visitBooleanLiteralFilter(JsonValue jsonValue, boolean value) {
        return value;
    }

    @Override
    public Boolean visitContainsFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new Contains(match), jsonValue, jsonPointer);
    }

    private static class Contains implements Predicate<Object> {
        private final Object match;

        public Contains(Object match) {
            this.match = match;
        }

        @Override
        public boolean apply(Object o) {
            return o.toString().toLowerCase().contains(match.toString().toLowerCase());
        }
    }

    @Override
    public Boolean visitEqualsFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new Equals(match), jsonValue, jsonPointer);
    }

    private static class Equals implements Predicate<Object> {
        private final Object match;

        public Equals(Object match) {
            this.match = match;
        }

        @Override
        public boolean apply(Object o) {
            return o instanceof String ?
                    ((String) o).equalsIgnoreCase(match.toString()) :
                    o.equals(match);
        }
    }

    @Override
    public Boolean visitStartsWithFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new StartsWith(match), jsonValue, jsonPointer);
    }

    private static class StartsWith implements Predicate<Object> {
        private final Object match;

        public StartsWith(Object match) {
            this.match = match;
        }

        @Override
        public boolean apply(Object o) {
            return o.toString().toLowerCase().startsWith(match.toString().toLowerCase());
        }
    }

    @Override
    public Boolean visitExtendedMatchFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            String s, Object o) {
        throw new UnsupportedOperationException("Operation " + s + " not supported");
    }

    @Override
    public Boolean visitGreaterThanFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new GreaterThan(match), jsonValue, jsonPointer);
    }

    private static class GreaterThan extends Comparison {

        public GreaterThan(Object match) {
            super(match);
        }

        @Override
        boolean test(int comparison) {
            return comparison > 0;
        }

    }

    @Override
    public Boolean visitLessThanFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new LessThan(match), jsonValue, jsonPointer);
    }

    private static class LessThan extends Comparison {

        public LessThan(Object match) {
            super(match);
        }

        @Override
        boolean test(int comparison) {
            return comparison < 0;
        }

    }

    @Override
    public Boolean visitGreaterThanOrEqualToFilter(JsonValue jsonValue,
            JsonPointer jsonPointer, Object match) {
        return filter(new GreaterThanEqualTo(match), jsonValue, jsonPointer);
    }

    private static class GreaterThanEqualTo extends Comparison {

        public GreaterThanEqualTo(Object match) {
            super(match);
        }

        @Override
        boolean test(int comparison) {
            return comparison >= 0;
        }

    }

    @Override
    public Boolean visitLessThanOrEqualToFilter(JsonValue jsonValue, JsonPointer jsonPointer,
            Object match) {
        return filter(new LessThanEqualTo(match), jsonValue, jsonPointer);
    }

    private static class LessThanEqualTo extends Comparison {

        public LessThanEqualTo(Object match) {
            super(match);
        }

        @Override
        boolean test(int comparison) {
            return comparison <= 0;
        }

    }

    private boolean filter(Predicate<Object> predicate, JsonValue value, JsonPointer pointer) {
        if (value.get(pointer).isCollection()) {
            for (Object arrayItem : value.get(pointer).asCollection()) {
                if (predicate.apply(arrayItem)) {
                    return true;
                }
            }
            return false;
        }
        return predicate.apply(value.get(pointer).getObject());
    }

    @Override
    public Boolean visitPresentFilter(JsonValue jsonValue, JsonPointer jsonPointer) {
        return jsonValue.get(jsonPointer) != null;
    }

    private abstract static class Comparison implements Predicate<Object> {
        private final Object match;

        public Comparison(Object match) {
            this.match = match;
        }

        @Override
        public boolean apply(Object o) {
            return test(((Comparable) o).compareTo(match));
        }

        abstract boolean test(int comparison);
    }

}
