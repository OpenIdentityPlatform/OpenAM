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

package org.forgerock.openam.utils;

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

    /**
     * Constructs a new CrestQuery instance with the specified query id.
     *
     * @param queryIdPattern The query id.
     */
    public CrestQuery(String queryIdPattern) {
        this.queryId = queryIdPattern;
        this.queryFilter = null;
    }

    /**
     * Constructs a new CrestQuery instance with the specified query filter.
     *
     * @param queryFilter The query filter.
     */
    public CrestQuery(QueryFilter<JsonPointer> queryFilter) {
        this.queryFilter = queryFilter;
        this.queryId = null;
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
     * This is mainly for debugging purposes so you can say "this is a rough idea of the CrestQuery object I've
     * been handed".
     *
     * @return a pretty representation of this object
     */
    @Override
    public String toString() {
        if (hasQueryFilter()) {
            return "[_queryFilter: " + queryFilter.toString() + "]";
        }
        return "[_queryId: " + queryId + "]";
    }
}
