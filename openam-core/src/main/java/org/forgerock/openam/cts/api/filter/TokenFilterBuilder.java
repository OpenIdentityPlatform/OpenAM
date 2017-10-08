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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.api.filter;

import java.util.LinkedList;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.time.Duration;

/**
 * Allows the assembly of {@link TokenFilter} instances for use with the {@link CTSPersistentStore}
 * and other uses of the generic data layer.
 * <p>
 * The role of a TokenFilter is to restrict the results of a CTS query to both
 * reduce load on the CTS and make the return results more specific to the
 * callers query.
 * <p>
 * Each TokenFilter can include {@link CoreTokenField} attribute filters which ensure that only CTS
 * tokens that have the matching attribute are returned.
 * <p>
 * In addition the TokenFilter can define the return attributes from matched CTS Tokens. Rather than
 * returning complete CTS tokens, when a return attribute is defined the CTS Tokens will only contain
 * the defined subset of {@link CoreTokenField} attributes.
 */
public class TokenFilterBuilder {
    private TokenFilter tokenFilter = new TokenFilter();

    private enum Type {
        AND, OR
    }

    /**
     * Creates an AND query where the results must match all provided filters.
     * @return Non null FilterAttributeBuilder.
     */
    public FilterAttributeBuilder and() {
        return new FilterAttributeBuilder(tokenFilter, Type.AND);
    }

    /**
     * Creates an OR query where the results will match any of the provided filters.
     * @return Non null FilterAttributeBuilder.
     */
    public FilterAttributeBuilder or() {
        return new FilterAttributeBuilder(tokenFilter, Type.OR);
    }

    /**
     * Filters the result CTS Tokens to only include those that have the matching
     * attribute. Will also move this Builder into AND mode as if {@link #and()} had
     * been called.
     *
     * @see TokenFilterBuilder.FilterAttributeBuilder#withAttribute(CoreTokenField, Object)
     *
     * @param field The {@link CoreTokenField} to filter against.
     * @param value The value for the field.
     * @return Moves the builder into AND mode, with the filter assigned.
     */
    public FilterAttributeBuilder withAttribute(CoreTokenField field, Object value) {
        return new FilterAttributeBuilder(tokenFilter, Type.AND).withAttribute(field, value);
    }

    /**
     * Rather than building up the query using these builder methods, callers can define
     * the entire query themselves. This option allows for the full range of expressions
     * provided in the {@link QueryFilter} api.
     * <p>
     * Note: Using this option will disable other options in this Builder. E.g. cannot
     * be followed by {@link #or()}, {@link #and()}, or
     * {@link #withAttribute(org.forgerock.openam.tokens.CoreTokenField, Object)}.
     *
     * @param query The complex token query.
     * @return This builder.
     */
    public TokenFilterBuilder withQuery(QueryFilter<CoreTokenField> query) {
        tokenFilter.setQuery(query);
        return this;
    }

    /**
     * Sets the size limit for the query request. Defaults to 0. The size limit will be enforced by the underlying
     * backend technology.
     *
     * @param sizeLimit The non-negative amount of entries that should be returned at most via the query.
     * @return This builder.
     */
    public TokenFilterBuilder withSizeLimit(int sizeLimit) {
        tokenFilter.setSizeLimit(sizeLimit);
        return this;
    }

    /**
     * Sets the time limit for the query request. Defaults to 0 seconds - no time limit. The time limit will be
     * enforced by the underlying backend technology.
     *
     * @param timeLimit The non-negative time duration under which the query must finish.
     * @return This builder.
     */
    public TokenFilterBuilder withTimeLimit(Duration timeLimit) {
        tokenFilter.setTimeLimit(timeLimit);
        return this;
    }

    /**
     * If you only require the returned CTS Tokens to contains a subset of the standard
     * {@link CoreTokenField#values()} then this method allows the caller to specify the
     * fields they would like in the returned CTS Tokens.
     *
     * @param field The required attribute to return from the query.
     * @return This query builder.
     */
    public TokenFilterBuilder returnAttribute(CoreTokenField field) {
        tokenFilter.addReturnAttribute(field);
        return this;
    }

    /**
     * @return The assembled TokenFilter.
     */
    public TokenFilter build() {
        if (tokenFilter.getQuery() == null) {
            tokenFilter.setQuery(QueryFilter.<CoreTokenField>alwaysTrue());
        }
        return tokenFilter;
    }

    /**
     * Used when the {@link TokenFilterBuilder} is in either {@link #and()} or {@link #or()} mode.
     */
    public class FilterAttributeBuilder {
        private final Type type;
        private LinkedList<QueryFilter<CoreTokenField>> criteria = new LinkedList<>();

        private FilterAttributeBuilder(TokenFilter tokenFilter, Type type) {
            Reject.ifTrue(tokenFilter.getQuery() != null, "TokenFilter already has query assigned, invalid state.");
            this.type = type;
        }

        /**
         * Filters the result CTS Tokens to only include those that have the matching
         * attribute. This filter will be combined depending on the mode of the Builder
         * and so will depend on the preceding call to either {@link #and()} or {@link #or}.
         *
         * @param field Non null {@link CoreTokenField} to filter on.
         * @param value Non null value that matching CTS Tokens must contain.
         * @return This FilterAttributeBuilder.
         */
        public FilterAttributeBuilder withAttribute(CoreTokenField field, Object value) {
            criteria.add(QueryFilter.equalTo(field, value));
            return this;
        }

        /**
         * A variant of {@link #withAttribute(CoreTokenField, Object)} which changes the order of the filter arguments
         * to allow the underlying backend to efficiently run the query.
         *
         * @param field Non null {@link CoreTokenField} to filter on.
         * @param value Non null value that matching CTS Tokens must contain.
         * @return This FilterAttributeBuilder.
         * @see #withAttribute(CoreTokenField, Object)
         */
        public FilterAttributeBuilder withPriorityAttribute(CoreTokenField field, Object value) {
            criteria.addFirst(QueryFilter.equalTo(field, value));
            return this;
        }

        /**
         * If you only require the returned CTS Tokens to contains a subset of the standard
         * {@link CoreTokenField#values()} then this method allows the caller to specify the
         * fields they would like in the returned CTS Tokens.
         *
         * @param field The required attribute to return from the query.
         * @return This query builder.
         */
        public FilterAttributeBuilder returnAttribute(CoreTokenField field) {
            tokenFilter.addReturnAttribute(field);
            return this;
        }

        /**
         * @return The assembled TokenFilter.
         */
        public TokenFilter build() {
            tokenFilter.setQuery(type == Type.AND ? QueryFilter.and(criteria) : QueryFilter.or(criteria));
            return tokenFilter;
        }
    }
}
