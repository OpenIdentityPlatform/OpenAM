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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements.query;

import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.DateUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * Supported attribute types for queries over policies.
 *
 * @since 12.0.0
 */
public enum AttributeType {
    STRING {
        @Override
        public SearchFilter getFilter(String field, SearchFilter.Operator operator, Object value) {
            if (operator != SearchFilter.Operator.EQUAL_OPERATOR) {
                throw new UnsupportedOperationException("Only equality supported for string attributes");
            }
            return new SearchFilter(field, value.toString());
        }
    },
    NUMBER {
        @Override
        public SearchFilter getFilter(String field, SearchFilter.Operator operator, Object value) {
            try {
                return new SearchFilter(field, ((Number) value).longValue(), operator);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Expected number but got '" + value + "'");
            }
        }
    },
    TIMESTAMP {
        @Override
        public SearchFilter getFilter(String field, SearchFilter.Operator operator, Object value) {
            try {
                Date timestamp = DateUtils.stringToDate(value.toString());
                return new SearchFilter(field, timestamp.getTime(), operator);
            } catch (ParseException ex) {
                // Attempt to parse as a numeric value
                return NUMBER.getFilter(field, operator, value);
            }
        }
    };

    /**
     * Gets an appropriate search filter for this attribute type.
     *
     * @param field the name of the field to compare against.
     * @param operator the operator to use in the comparison.
     * @param value the value to compare the field to.
     * @return an appropriate search filter for this attribute type.
     * @throws UnsupportedOperationException if the operator is invalid for this attribute type.
     * @throws IllegalArgumentException if the value is not of a type that this attribute can handle.
     */
    public abstract SearchFilter getFilter(String field, SearchFilter.Operator operator, Object value);
}
