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

package org.forgerock.openam.uma;

import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;

/**
 *
 *
 * @since 13.0.0
 */
public final class UmaPolicyQueryFilterVisitor implements QueryFilterVisitor<PolicySearch, PolicySearch> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitAndFilter(PolicySearch policySearch, List<QueryFilter> subFilters) {
        PolicySearch andSearch = policySearch;
        for (QueryFilter filter : subFilters) {
            andSearch = filter.accept(this, andSearch);
        }
        return andSearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitBooleanLiteralFilter(PolicySearch policySearch, boolean value) {
        throw unsupportedFilterOperation("Boolean Literal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitContainsFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Contains");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitEqualsFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        return policySearch.equals(field, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitExtendedMatchFilter(PolicySearch policySearch, JsonPointer field, String operator, Object valueAssertion) {
        throw unsupportedFilterOperation("Extended match");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitGreaterThanFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitGreaterThanOrEqualToFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than or equal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitLessThanFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitLessThanOrEqualToFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than or equal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitNotFilter(PolicySearch policySearch, QueryFilter subFilter) {
        return policySearch.remove(subFilter.accept(this, policySearch));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitOrFilter(PolicySearch policySearch, List<QueryFilter> subFilters) {
        PolicySearch orSearch = new PolicySearch();
        for (QueryFilter filter : subFilters) {
            orSearch = orSearch.combine(filter.accept(this, policySearch));
        }
        return orSearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitPresentFilter(PolicySearch policySearch, JsonPointer field) {
        throw unsupportedFilterOperation("Presence");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicySearch visitStartsWithFilter(PolicySearch policySearch, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Starts with");
    }

    private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
        return new UnsupportedOperationException("'" + filterType + "' not supported");
    }
}
