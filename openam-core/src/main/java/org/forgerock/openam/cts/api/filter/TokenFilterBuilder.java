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

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.fields.CoreTokenField;

/**
 * Allows the assembly of {@link TokenFilter} instances for use with the {@link CTSPersistentStore}.
 *
 * This builder will guide the caller through some of the detail around creating filter instances
 * and in particular improve code readability.
 */
public class TokenFilterBuilder {
    private TokenFilter tokenFilter = new TokenFilter();

    /**
     * @return Moves the builder into AND mode.
     */
    public FilterAttributeBuilder and() {
        return new FilterAttributeBuilder(tokenFilter, TokenFilter.Type.AND);
    }

    /**
     * @return Moves the builder into OR mode.
     */
    public FilterAttributeBuilder or() {
        return new FilterAttributeBuilder(tokenFilter, TokenFilter.Type.OR);
    }

    /**
     * @return Moves the builder into mode specified by type.
     */
    public FilterAttributeBuilder type(TokenFilter.Type type) {
        return new FilterAttributeBuilder(tokenFilter, type);
    }

    /**
     * Moves the TokenFilter into AND mode, and filters the query by the given attribute.
     *
     * @see TokenFilterBuilder.FilterAttributeBuilder#withAttribute(CoreTokenField, Object)
     *
     * @param field The {@link CoreTokenField} to filter against.
     * @param value The value for the field.
     * @return Moves the builder into AND mode, with the filter assigned.
     */
    public FilterAttributeBuilder withAttribute(CoreTokenField field, Object value) {
        return new FilterAttributeBuilder(tokenFilter, TokenFilter.Type.AND).withAttribute(field, value);
    }

    /**
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
        return tokenFilter;
    }

    /**
     * Once the TokenFilter has been assigned a mode, then further options can be applied to it.
     */
    public static class FilterAttributeBuilder {
        private final TokenFilter tokenFilter;

        public FilterAttributeBuilder(TokenFilter tokenFilter, TokenFilter.Type type) {
            this.tokenFilter = tokenFilter;
            tokenFilter.setType(type);
        }

        /**
         * Filters the query by the given attribute. This will apply based on the mode of the
         * TokenFilter.
         *
         * @see #and()
         * @see #or()
         *
         * @param field Non null field to filter Tokens on.
         * @param value Non null value to filter the field by.
         * @return This FilterAttributeBuilder.
         */
        public FilterAttributeBuilder withAttribute(CoreTokenField field, Object value) {
            tokenFilter.addFilter(field, value);
            return this;
        }

        /**
         * @return The assembled TokenFilter.
         */
        public TokenFilter build() {
            return tokenFilter;
        }
    }
}
