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

package org.forgerock.openam.forgerockrest.entitlements.query;

import com.sun.identity.entitlement.util.SearchFilter;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.util.Reject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for building converters that map CREST query filters to a set of entitlement search filters.
 *
 * All {@link QueryFilterVisitor} methods are implemented to throw UnsupportedOperationException. If a particular
 * query filter type is to be supported, the relevant method should be overridden to implement the required mapping.
 *
 * @since 12.0.0
 */
public abstract class QueryFilterVisitorAdapter implements QueryFilterVisitor<Set<SearchFilter>, Set<SearchFilter>> {

    private final String type;
    private final Map<String, QueryAttribute> queryAttributes;

    /**
     * Constructor.
     *
     * @param type Non null, The name of the CREST resource type that is being queried. Used in exception messages.
     * @param queryAttributes Non null, defines queryable attributes of the queried CREST resource.
     */
    public QueryFilterVisitorAdapter(String type, Map<String, QueryAttribute> queryAttributes) {
        this.type = type;
        this.queryAttributes = queryAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitAndFilter(
            Set<SearchFilter> filters, List<QueryFilter> subFilters) {
        for (QueryFilter queryFilter : subFilters) {
            queryFilter.accept(this, filters);
        }
        return filters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitBooleanLiteralFilter(
            Set<SearchFilter> filters, boolean value) {
        if (!value) {
            throw unsupportedFilterOperation("false");
        }
        // Nothing to do for 'true' case as we only support AND expressions
        // and 'anything AND true' is just the original expression ('anything').
        return filters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitContainsFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Contains");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitEqualsFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Equals");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitExtendedMatchFilter(
            Set<SearchFilter> filters, JsonPointer field, String operator, Object valueAssertion) {
        throw unsupportedFilterOperation("Extended match");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitGreaterThanFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitGreaterThanOrEqualToFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than or equal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitLessThanFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitLessThanOrEqualToFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than or equal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitNotFilter(
            Set<SearchFilter> filters, QueryFilter subFilter) {
        throw unsupportedFilterOperation("Negation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitOrFilter(
            Set<SearchFilter> filters, List<QueryFilter> subFilters) {
        throw unsupportedFilterOperation("Or");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitPresentFilter(
            Set<SearchFilter> filters, JsonPointer field) {
        throw unsupportedFilterOperation("Presence");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SearchFilter> visitStartsWithFilter(
            Set<SearchFilter> filters, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Starts with");
    }

    /**
     * Attempts to convert the given field name, operator and value into an appropriate SearchFilter instance.
     * The conversion used depends on the {@link QueryAttribute} configured for this field in the queryAttributes
     * map.
     *
     * @param field Non null, The name of a queryable Application attribute.
     * @param operator Non null, The query operator to use when comparing the field value to the valueAssertion.
     * @param valueAssertion Non null, The value with which the Application field's value should be compared.
     */
    protected SearchFilter comparison(String field, SearchFilter.Operator operator, Object valueAssertion) {
        Reject.ifNull(field, operator, valueAssertion);

        QueryAttribute attribute = queryAttributes.get(field);
        if (attribute == null) {
            throw new UnsupportedOperationException("Unknown query field '" + field + "'");
        }

        return attribute.getFilter(operator, valueAssertion);
    }

    /**
     * Factory method for UnsupportedOperationException to indicate no support for a CREST filter type.
     *
     * @param filterType The type of filter that is not supported
     * @return UnsupportedOperationException
     */
    protected UnsupportedOperationException unsupportedFilterOperation(String filterType) {
        return new UnsupportedOperationException("'" + filterType + "' not supported in " + type + " queries");
    }
}
