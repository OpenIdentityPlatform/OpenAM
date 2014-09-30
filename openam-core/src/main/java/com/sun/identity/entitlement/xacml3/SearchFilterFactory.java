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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.util.SearchFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for parsing and generating SearchFilters.
 */
public class SearchFilterFactory {

    public static final int INVALID_SEARCH_FILTER = EntitlementException.INVALID_SEARCH_FILTER;
    private final Map<String, SearchFilter.Operator> operatorMap;

    public SearchFilterFactory() {
        operatorMap = new HashMap<String, SearchFilter.Operator>();
        operatorMap.put("=", SearchFilter.Operator.EQUAL_OPERATOR);
        operatorMap.put("<", SearchFilter.Operator.LESSER_THAN_OPERATOR);
        operatorMap.put(">", SearchFilter.Operator.GREATER_THAN_OPERATOR);
    }

    /**
     * Returns a SearchFilter that matches the search string provided.
     *
     * Supports the following syntax:
     * [attribute name][operator][attribute value]
     *
     * Where operator can be one of the following:
     * Equals - =
     * Less Than - <
     * Greater Than - >
     *
     * @param filter Non null String representing a search filter.
     * @return Non null parsed SearchFilter.
     * @throws EntitlementException If there was an error pasing the filter.
     */
    public SearchFilter getFilter(String filter) throws EntitlementException {
        Object[] parts = parseFilter(filter);
        String name = (String) parts[0];
        SearchFilter.Operator operator = (SearchFilter.Operator) parts[1];
        String value = (String) parts[2];

        if (isAttributeNumeric(name)) { // The < and > operators only apply to numeric attributes
            try {
                return new SearchFilter(name, Long.parseLong(value), operator);
            } catch (NumberFormatException e) {
                throw new EntitlementException(
                        INVALID_SEARCH_FILTER,
                        new Object[]{"Invalid value for attribute", name, value});
            }
        } else { // Everything else is assumed to be equals.
            return new SearchFilter(name, value);
        }
    }

    /**
     * Parses the SearchFilter formatted text into a tuple of sorts.
     * @param filter Non null string.
     * @return Name, Operator, Value.
     * @throws EntitlementException If no operator was found.
     */
    private Object[] parseFilter(String filter) throws EntitlementException {
        for (String symbol : operatorMap.keySet()) {

            int index = filter.indexOf(symbol);

            if (index != -1) {
                return new Object[]{
                        filter.substring(0, index).trim(),
                        operatorMap.get(symbol),
                        filter.substring(index + symbol.length(), filter.length()).trim()};
            }
        }
        throw new EntitlementException(INVALID_SEARCH_FILTER, new Object[]{filter});
    }

    private boolean isAttributeNumeric(String name) {
        return name.equals(Privilege.LAST_MODIFIED_DATE_ATTRIBUTE) || name.equals(Privilege.CREATION_DATE_ATTRIBUTE);
    }
}
