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


import java.util.HashSet;
import java.util.Set;

/**
 * Models an Endpoint.
 */
public class Endpoint {

    private final String uri;
    private final String httpMethod;
    private final String queryParam;
    private final String[] queryParamValues;

    /**
     * Constructs an instance of an Endpoint.
     *
     * @param uri The URI of the endpoint.
     * @param httpMethod The Http method allowed for the endpoint.
     */
    public Endpoint(String uri, String httpMethod) {
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.queryParam = null;
        this.queryParamValues = null;
    }

    /**
     * Constructs an instance of an Endpoint.
     *
     * @param uri The URI of the endpoint.
     * @param httpMethod The Http method allowed for the endpoint.
     * @param queryParam The query parameter for the endpoint.
     * @param queryParamValues The possible allowed values for the endpoint query paramter.
     */
    public Endpoint(String uri, String httpMethod, String queryParam, String... queryParamValues) {
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.queryParam = queryParam;
        this.queryParamValues = queryParamValues;
    }

    /**
     * Returns this endpoints query parameter matchers that will be used to match the
     * query parameters of the request.
     *
     * @return The endpoints query parameter matchers.
     */
    public Set<QueryParameterMatcher> getQueryParamMatchers() {

        Set<QueryParameterMatcher> queryParamMatchers = new HashSet<QueryParameterMatcher>();

        if (queryParam != null) {
            for (String queryParamValue : queryParamValues) {
                queryParamMatchers.add(new QueryParameterMatcher(queryParam, queryParamValue));
            }
        }

        return queryParamMatchers;
    }

    /**
     * Determines if two endpoints are equal.
     *
     * @param o {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint that = (Endpoint) o;

        if (!httpMethod.equals(that.httpMethod)) return false;
        if (!uri.startsWith(that.uri)) return false;

        return true;
    }

    /**
     * Determines the hash code for this endpoint.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + httpMethod.hashCode();
        return result;
    }
}
