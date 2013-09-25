/**
 * Copyright 2013 ForgeRock, AS.
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
package org.forgerock.openam.cts.impl.query;

import javax.inject.Inject;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.opendj.ldap.Filter;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;

/**
 * QueryFilter is responsible for restricting the number of results that can be returned by
 * the QueryBuilder.
 *
 * The filters currently supports the boolean operators OR and AND. It does not currently
 * support mixing operatins in the filter.
 *
 * This is not a technical limitation, and so could be added as required later.
 *
 * @author robert.wapshott@forgerock.com
 */
public class QueryFilter {
    private final LDAPDataConversion dataConversion;

    @Inject
    public QueryFilter(LDAPDataConversion dataConversion) {
        this.dataConversion = dataConversion;
    }

    public QueryFilterBuilder and() {
        return new QueryFilterBuilder(true);
    }

    public QueryFilterBuilder or() {
        return new QueryFilterBuilder(false);
    }

    public class QueryFilterBuilder {

        private boolean and;
        private Collection<Filter> filters = new LinkedList<Filter>();

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
        public QueryFilterBuilder beforeDate(Calendar timestamp) {
            String dateString = dataConversion.toLDAPDate(timestamp);
            filters.add(Filter.lessOrEqual(CoreTokenField.EXPIRY_DATE.toString(), dateString));
            return this;
        }

        /**
         * Equality ldapFilter in the format key=value.
         *
         * @param field CoreTokenField to ldapFilter against.
         * @param value Value that must match.
         * @return The QueryBuilder for subsequent calls.
         */
        public QueryFilterBuilder attribute(CoreTokenField field, Object value) {
            String s;
            if (CoreTokenFieldTypes.isCalendar(field)) {
                s = dataConversion.toLDAPDate((Calendar) value);
            } else {
                s = value.toString();
            }

            filters.add(Filter.equality(field.toString(), s));
            return this;
        }

        public Filter build() {
            if (and) {
                return Filter.and(filters);
            } else {
                return Filter.or(filters);
            }
        }
    }
}
