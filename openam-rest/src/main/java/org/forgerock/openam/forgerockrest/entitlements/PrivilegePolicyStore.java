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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryFilterVisitorAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores entitlements policies using the {@link PrivilegeManager}.
 *
 * @since 12.0.0
 */
final class PrivilegePolicyStore implements PolicyStore {

    /**
     * The default maximum time limit to apply to policy queries in seconds. 0 means no limit.
     * Note: this constant is here for completeness. The PrivilegeManager does not yet support time limits.
     */
    private static final int DEFAULT_QUERY_TIME_LIMIT_SECONDS = 0;

    private final Map<String, QueryAttribute> queryAttributes;
    private final PrivilegeManager privilegeManager;

    PrivilegePolicyStore(final PrivilegeManager privilegeManager, Map<String, QueryAttribute> queryAttributes) {
        this.privilegeManager = privilegeManager;
        this.queryAttributes = queryAttributes;
    }

    @Override
    public Privilege read(String policyName) throws EntitlementException {
        final Privilege policy = privilegeManager.findByName(policyName);
        if (policy == null) {
            throw new EntitlementException(EntitlementException.NO_SUCH_POLICY, new Object[] { policyName });
        }
        return policy;
    }

    @Override
    public Privilege create(Privilege policy) throws EntitlementException {
        privilegeManager.add(policy);
        return policy;
    }

    @Override
    public Privilege update(String existingName, Privilege policy) throws EntitlementException {
        privilegeManager.modify(existingName, policy);
        return policy;
    }

    @Override
    public void delete(String policyName) throws EntitlementException {
        privilegeManager.remove(policyName);
    }

    @Override
    public List<Privilege> query(QueryRequest request) throws EntitlementException {
        QueryFilter queryFilter = request.getQueryFilter();
        if (queryFilter == null) {
            // Return everything
            queryFilter = QueryFilter.alwaysTrue();
        }

        try {
            Set<SearchFilter> searchFilters = queryFilter.accept(new PrivilegeQueryBuilder(queryAttributes),
                                                                 new HashSet<SearchFilter>());

            return privilegeManager.search(searchFilters);
        } catch (UnsupportedOperationException ex) {
            throw new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER,
                    new Object[] { ex.getMessage() });
        } catch (IllegalArgumentException ex) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE,
                    new Object[] { ex.getMessage() });
        }
    }

    /**
     * Converts a set of CREST {@link QueryFilter} into a set of entitlement {@link SearchFilter}.
     */
    private static final class PrivilegeQueryBuilder extends QueryFilterVisitorAdapter {

        PrivilegeQueryBuilder(Map<String, QueryAttribute> queryAttributes) {
            super("policy", queryAttributes);
        }

        @Override
        public Set<SearchFilter> visitEqualsFilter(Set<SearchFilter> filters, JsonPointer field,
                                                   Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.EQUAL_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                        Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.GREATER_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                                 Object valueAssertion) {
            // Treat as greater-than (both are >= in the underlying implementation)
            return visitGreaterThanFilter(filters, field, valueAssertion);
        }

        @Override
        public Set<SearchFilter> visitLessThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                     Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESSER_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                              Object valueAssertion) {
            return visitLessThanFilter(filters, field, valueAssertion);
        }

    }
}
