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
 * Copyright 2019 Open Identity Platform Community.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.cassandra;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import jakarta.inject.Inject;

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.util.query.QueryFilter;

public class QueryFilterVisitor implements org.forgerock.util.query.QueryFilterVisitor<Filter, Void, CoreTokenField> {

    @Inject
    public QueryFilterVisitor() {
    }

    @Override
    public Filter visitAndFilter(Void aVoid, List<QueryFilter<CoreTokenField>> subQueryFilters) {
        return Filter.and(getSubFilters(subQueryFilters));
    }

    @Override
    public Filter visitOrFilter(Void aVoid, List<QueryFilter<CoreTokenField>> subQueryFilters) {
        //return Filter.or(getSubFilters(subQueryFilters));
   		throw new UnsupportedOperationException();
    }

    private List<Filter> getSubFilters(List<QueryFilter<CoreTokenField>> subQueryFilters) {
        List<Filter> subFilters = new ArrayList<Filter>(subQueryFilters.size());
        for (QueryFilter<CoreTokenField> subFilter : subQueryFilters) {
            subFilters.add(subFilter.accept(this, null));
        }
        return subFilters;
    }

    @Override
    public Filter visitNotFilter(Void aVoid, QueryFilter<CoreTokenField> queryFilter) {
        //return Filter.not(queryFilter.accept(this, null));
		Filter res=queryFilter.accept(this, null);
		return Filter.and(Arrays.asList( new Filter[] {
				Filter.greaterThan(res.field2value.keySet().iterator().next(), res.field2value.values().iterator().next()),
				Filter.lessThan(res.field2value.keySet().iterator().next(), res.field2value.values().iterator().next())
		}));
    }

    @Override
    public Filter visitBooleanLiteralFilter(Void aVoid, boolean value) {
        //return value ? Filter.alwaysTrue() : Filter.alwaysFalse();
   		throw new UnsupportedOperationException();
    }

    @Override
    public Filter visitContainsFilter(Void aVoid, CoreTokenField field, Object value) {
        //return Filter.substrings(field.toString(), null, Arrays.asList(value), null);
   		return visitEqualsFilter(aVoid, field, value);
    }

    @Override
    public Filter visitEqualsFilter(Void aVoid, CoreTokenField field, Object value) {
        return Filter.equality(field.toString(), convert(field, value));
    }

    @Override
    public Filter visitGreaterThanFilter(Void aVoid, CoreTokenField field, Object value) {
        return Filter.greaterThan(field.toString(), convert(field, value));
    }

    @Override
    public Filter visitGreaterThanOrEqualToFilter(Void aVoid, CoreTokenField field, Object value) {
        return Filter.greaterOrEqual(field.toString(), convert(field, value));
    }

    @Override
    public Filter visitLessThanFilter(Void aVoid, CoreTokenField field, Object value) {
        return Filter.lessThan(field.toString(), convert(field, value));
    }

    @Override
    public Filter visitLessThanOrEqualToFilter(Void aVoid, CoreTokenField field, Object value) {
        return Filter.lessOrEqual(field.toString(), convert(field, value));
    }

    @Override
    public Filter visitPresentFilter(Void aVoid, CoreTokenField field) {
        //return Filter.present(field.toString());
   		throw new UnsupportedOperationException();
    }

    @Override
    public Filter visitStartsWithFilter(Void aVoid, CoreTokenField field, Object value) {
        //return Filter.substrings(field.toString(), value, null, null);
   		throw new UnsupportedOperationException();
    }

    @Override
    public Filter visitExtendedMatchFilter(Void aVoid, CoreTokenField coreTokenField, String operator, Object value) {
        throw new UnsupportedOperationException();
    }

	private Object convert(CoreTokenField field, Object value) {
		if (value instanceof TokenType)
			value = value.toString();
		else if (value instanceof Calendar)
			value = ((Calendar) value).getTime().toInstant();
		else if (value instanceof byte[])
			value = ByteBuffer.wrap((byte[]) value);
		return value;
	}
}
