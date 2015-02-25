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

package org.forgerock.openam.uma.audit;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * A visitor that returns a Map containing fieldname -> value representing fieldname==value style filters
 * @since 13.0.0
 */
public class UmaAuditQueryFilterVisitor implements QueryFilterVisitor<org.forgerock.util.query.QueryFilter<String>, Void> {

    public UmaAuditQueryFilterVisitor() {

    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitAndFilter(Void aVoid, List<QueryFilter> subFilters) {
        List<org.forgerock.util.query.QueryFilter<String>> childFilters =
                new ArrayList<org.forgerock.util.query.QueryFilter<String>>();
        for (QueryFilter filter : subFilters) {
            childFilters.add(filter.accept(this, null));
        }
        return org.forgerock.util.query.QueryFilter.and(childFilters);
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        final String fieldName = field.get(0);
        if (fieldName.equals("eventTime")) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis((Long) valueAssertion);
            return org.forgerock.util.query.QueryFilter.equalTo(field.get(0), cal);
        } else {
            return org.forgerock.util.query.QueryFilter.equalTo(field.get(0), valueAssertion);
        }
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return org.forgerock.util.query.QueryFilter.startsWith(field.get(0), valueAssertion);
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return org.forgerock.util.query.QueryFilter.contains(field.get(0), valueAssertion);
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitBooleanLiteralFilter(Void aVoid, boolean value) {
        throw unsupportedFilterOperation("Boolean Literal");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
        throw unsupportedFilterOperation("Extended match");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than or equal");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than or equal");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitNotFilter(Void aVoid, QueryFilter subFilter) {
        throw unsupportedFilterOperation("Not");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitOrFilter(Void aVoid, List<QueryFilter> subFilters) {
        throw unsupportedFilterOperation("Or");
    }

    @Override
    public org.forgerock.util.query.QueryFilter<String> visitPresentFilter(Void aVoid, JsonPointer field) {
        throw unsupportedFilterOperation("Present");
    }

    private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
        return new UnsupportedOperationException("'" + filterType + "' not supported");
    }
}
