/**
 * Copyright 2013-2015 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.sm.datalayer.api.query;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Filter;

/**
 * QueryFilter is responsible for restricting the number of results that can be returned by
 * the QueryBuilder.
 *
 * The filters currently supports the boolean operators OR and AND. It does not currently
 * support mixing operatins in the filter.
 *
 * This is not a technical limitation, and so could be added as required later.
 *
 * @param <F> The type of filter representation that is exposed.
 */
public abstract class QueryFilter<F> {

    public abstract QueryFilterBuilder<F> and();

    public abstract QueryFilterBuilder<F> or();

    public abstract class QueryFilterBuilder<FF> {

        protected boolean and;
        protected Collection<Filter> filters = new LinkedList<Filter>();

        public QueryFilterBuilder(boolean and) {
            this.and = and;
        }

        /**
         * Query by the Users ID.
         *
         * @param userId Users ID which must be match for the Token to be returned.
         * @return The QueryBuilder instance.
         */
        public QueryFilterBuilder userId(String userId) {
            attribute(CoreTokenField.USER_ID, userId);
            return this;
        }

        /**
         * Match all Tokens that have an expiry date before or equal to the given date.
         * @param timestamp Non null date to ldapFilter against.
         * @return The QueryBuilder for subsequent calls.
         */
        public abstract QueryFilterBuilder<FF> beforeDate(Calendar timestamp);

        /**
         * Equality ldapFilter in the format key=value.
         *
         * @param field CoreTokenField to ldapFilter against.
         * @param value Value that must match.
         * @return The QueryBuilder for subsequent calls.
         */
        public abstract QueryFilterBuilder<FF> attribute(CoreTokenField field, Object value);

        public abstract FF build();
    }
}
