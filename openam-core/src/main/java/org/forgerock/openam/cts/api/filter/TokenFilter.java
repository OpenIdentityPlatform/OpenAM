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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.api.filter;

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

import java.text.MessageFormat;
import java.util.*;

/**
 * Describes a collection of filters which can be applied to the CTS query function
 * as part of a complex query.
 *
 * This filter only currently supports single type And/Or filtering. This is not a
 * technical limitation and can be extended in the future.
 */
public class TokenFilter {
    private Set<CoreTokenField> returnFields;
    private QueryFilter<CoreTokenField> query;

    /**
     * Package private field to indicate that the {@link TokenFilterBuilder} is the recommended
     * way of assembling this object.
     */
    TokenFilter() {
        returnFields = new HashSet<CoreTokenField>();
    }

    /**
     * Inspect the CoreTokenField, Object pairs that have been assigned as filters.
     * @return An unmodifiable Map of filter components.
     */
    public QueryFilter<CoreTokenField> getQuery() {
        return query;
    }

    /**
     * Set the query component to the Filter.
     * @param query Non null query.
     */
    public void setQuery(QueryFilter<CoreTokenField> query) {
        Reject.ifNull(query);
        this.query = query;
    }

    /**
     * The current set of return fields. If this collection is empty, this
     * indicates that all fields are to be returned.
     *
     * @return A non null, possibly empty, non-modifiable Set of return attributes.
     */
    public Set<CoreTokenField> getReturnFields() {
        return Collections.unmodifiableSet(returnFields);
    }

    /**
     * Assigns an attribute to be part of the return attributes.
     *
     * When a return attribute is provided, the return results of the query will only
     * populate the indicated return attributes.
     *
     * The default is for all attributes to be returned, unless the query
     * constrains the results using this function.
     *
     * @param field The CoreTokenField to include in the return results.
     */
    public void addReturnAttribute(CoreTokenField field) {
        Reject.ifNull(field);
        returnFields.add(field);
    }

    /**
     * A multi-line representation of the filter.
     * @return Non null.
     */
    public String toString() {
        StringBuilder a = new StringBuilder();
        String separator = ",";
        for (CoreTokenField field : getReturnFields()) {
            a.append(field.toString()).append(separator);
        }
        return MessageFormat.format(
                "TokenFilter: Filter: [{0}] Attributes: {1}",
                query,
                a);
    }
}
