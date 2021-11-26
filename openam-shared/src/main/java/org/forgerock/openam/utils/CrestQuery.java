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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;

/**
 * This class was created to handle queries made via CREST that can provide either a _queryID (String) or a
 * _queryFilter (QueryFilter&lt;JsonPointer&gt;) to search for.  Obviously the query filter cannot be converted
 * into a string here because we support LDAP, SQL, etc. at a lower level and so we probably don't know at the time
 * this object is created what the target is.
 */
public class CrestQuery {

    private final String queryId;
    private final QueryFilter<JsonPointer> queryFilter;
    private final List<JsonPointer> fields;
    private boolean escapeQueryId = true;

    /**
     * Constructs a new CrestQuery instance with the specified query id.
     *
     * @param queryIdPattern The query id.
     */
    public CrestQuery(String queryIdPattern) {
        this(queryIdPattern, null, null);
    }

    /**
     * Constructs a new CrestQuery instance with the specified query filter.
     *
     * @param queryFilter The query filter.
     */
    public CrestQuery(QueryFilter<JsonPointer> queryFilter) {
        this(null, queryFilter, null);
    }

    /**
     * Constructs a new CrestQuery instance with the specified query filter.
     *
     * @param queryId The query ID.
     * @param queryFilter The query filter.
     * @param fields The fields to return in the query results.
     */
    public CrestQuery(String queryId, QueryFilter<JsonPointer> queryFilter, List<JsonPointer> fields) {
        this.queryId = queryId;
        this.queryFilter = queryFilter;
        this.fields = fields;
    }

    /**
     * Constructs a new CrestQuery instance with the specified query filter.
     *
     * @param queryId The query ID.
     * @param queryFilter The query filter.
     * @param fields The fields to return in the query results.
     * @param escapeQueryId escapes queryId to prevent LDAP injection
     */
    public CrestQuery(String queryId, QueryFilter<JsonPointer> queryFilter, List<JsonPointer> fields, boolean escapeQueryId) {
        this.queryId = queryId;
        this.queryFilter = queryFilter;
        this.fields = fields;
        this.escapeQueryId = escapeQueryId;
    }

    /**
     * Gets the CREST query id.
     *
     * @return the trimmed query id string.
     */
    public String getQueryId() {
        if (queryId == null) {
            return null;
        }
        return queryId.trim();
    }

    /**
     * Gets the CREST query filter.
     *
     * @return the query filter
     */
    public QueryFilter<JsonPointer> getQueryFilter() {
        return queryFilter;
    }

    /**
     * Gets the list of CREST fields that should be returned in the query result.
     *
     * @return The fields to return.
     */
    public List<JsonPointer> getFields() {
        return fields;
    }

    /**
     * Determines if the CREST query is based on a query id.
     *
     * @return true if the object contains a query id.
     */
    public boolean hasQueryId() {
        return queryFilter == null;
    }

    /**
     * Determines if the CREST query is based on a query filter.
     *
     * @return true if the object contains a query filter.
     */
    public boolean hasQueryFilter() {
        return queryFilter != null;
    }

    /**
     * Determines if queryId for the CREST should be escaped
     *
     * @return true queryId for the CREST should be escaped
     */
    public boolean isEscapeQueryId() {
        return escapeQueryId;
    }
    /**
     * This is mainly for debugging purposes so you can say "this is a rough idea of the CrestQuery object I've
     * been handed".
     *
     * @return a pretty representation of this object
     */
    @Override
    public String toString() {
        return "CrestQuery{"
                + "queryId='" + queryId + '\''
                + ", queryFilter=" + queryFilter
                + ", fields=" + fields
                + ", escapeQueryId=" + escapeQueryId
                + '}';
    }
}
