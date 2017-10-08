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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

import java.util.List;

/**
 * QueryFilterVisitor that only implements 'equals _id' functionality.
 */
public final class IdQueryFilterVisitor implements QueryFilterVisitor<String, Void, JsonPointer> {

    @Override
    public String visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
        throw new UnsupportedOperationException("And is not supported");
    }

    @Override
    public String visitBooleanLiteralFilter(Void aVoid, boolean value) {
        if (value) {
            return null;
        } else {
            throw new UnsupportedOperationException("Boolean literal 'false' is not supported");
        }
    }

    @Override
    public String visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Contains is not supported");
    }

    @Override
    public String visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        if ("_id".equalsIgnoreCase(field.leaf())) {
            if (!(valueAssertion instanceof String)) {
                throw new IllegalArgumentException("Invalid value assertion type: "
                        + valueAssertion.getClass().getSimpleName());
            }
            return (String) valueAssertion;
        }
        throw new UnsupportedOperationException("Equals is not supported");
    }

    @Override
    public String visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException("Extended match is not supported");
    }

    @Override
    public String visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Greater than is not supported");
    }

    @Override
    public String visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Greater than or equal to is not supported");
    }

    @Override
    public String visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Less than is not supported");
    }

    @Override
    public String visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Less than or equal to is not supported");
    }

    @Override
    public String visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
        throw new UnsupportedOperationException("Not is not supported");
    }

    @Override
    public String visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
        throw new UnsupportedOperationException("Or is not supported");
    }

    @Override
    public String visitPresentFilter(Void aVoid, JsonPointer field) {
        throw new UnsupportedOperationException("Present is not supported");
    }

    @Override
    public String visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        throw new UnsupportedOperationException("Starts with is not supported");
    }
}