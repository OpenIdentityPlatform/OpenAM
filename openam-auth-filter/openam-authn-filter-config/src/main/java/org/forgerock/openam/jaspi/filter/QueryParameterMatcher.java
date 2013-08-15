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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.filter;

import org.apache.commons.lang.StringUtils;

/**
 * A class that matches the query parameter contained by the QueryParameterMatcher against a given request query
 * string.
 */
public class QueryParameterMatcher {

    private final String queryParam;
    private final String value;

    /**
     * Constructs an instance of a QueryParamMatcher.
     *
     * @param queryParam The query parameter name,
     * @param value The value of the query paramter.
     */
    public QueryParameterMatcher(String queryParam, String value) {
        this.queryParam = queryParam;
        this.value = value;
    }

    /**
     * Splits the given request query string into the separate query parameter key/value pairs and returns true
     * if the query string contains the query parameter name and value that this matcher represents. Otherwise
     * returns false.
     * <p>
     * The query string MUST not be null or empty string, but a real query string from a http request.
     *
     * @param queryString The request query string.
     * @return <code>true</code> if the query string contains this query parameter.
     */
    public boolean match(String queryString) {

        if (StringUtils.isEmpty(queryString)) {
            return false;
        }

        String[] queryParams = queryString.split("&");

        for (String param : queryParams) {

            String[] params = param.split("=");
            if (params.length != 2) {
                throw new IllegalArgumentException("Query Param string does not contain valid query parameters");
            }

            String key = params[0];
            String val = params[1];

            if (queryParam.equalsIgnoreCase(key) && value.equalsIgnoreCase(val)) {
                return true;
            }
        }

        return false;
    }
}