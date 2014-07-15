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
package org.forgerock.openam.cts.api.filter;

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.util.Reject;

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

    public enum Type {
        AND,
        OR
    }

    private Type type;
    private Map<CoreTokenField, Object> filters;

    /**
     * Package private field to indicate that the TokenFilterBuilder is the recommended
     * way of assembling this object.
     */
    TokenFilter() {
        type = Type.AND;
        filters = new HashMap<CoreTokenField, Object>();
        returnFields = new HashSet<CoreTokenField>();
    }

    /**
     * The type of the Filter.
     *
     * @return AND by default, otherwise non null.
     */
    public Type getType() {
        return type;
    }

    /**
     * Assign the type to the Filter.
     * @param type Non null Type.
     */
    public void setType(Type type) {
        Reject.ifNull(type);
        this.type = type;
    }

    /**
     * Inspect the CoreTokenField, Object pairs that have been assigned as filters.
     * @return An unmodifiable Map of filter components.
     */
    public Map<CoreTokenField, Object> getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    /**
     * Append another filter component to the Filter.
     * @param field Non null CoreTokenField.
     * @param value Non null value.
     */
    public void addFilter(CoreTokenField field, Object value) {
        Reject.ifNull(field, value);
        filters.put(field, value);
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
        StringBuilder f = new StringBuilder();
        for (Map.Entry<CoreTokenField, Object> entry : getFilters().entrySet()) {
            f.append(entry.getKey().toString())
                    .append("=").append(entry.getValue())
                    .append("\n");
        }

        StringBuilder a = new StringBuilder();
        String separator = ",";
        for (CoreTokenField field : getReturnFields()) {
            a.append(field.toString()).append(separator);
        }
        return MessageFormat.format(
                "TokenFilter Type: {0} Filters: {1} Attributes: {2}",
                getType().toString(),
                f,
                a);
    }
}
