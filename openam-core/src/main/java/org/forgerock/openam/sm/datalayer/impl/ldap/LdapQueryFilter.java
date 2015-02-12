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

package org.forgerock.openam.sm.datalayer.impl.ldap;

import java.util.Calendar;

import javax.inject.Inject;

import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.sm.datalayer.api.query.QueryFilter;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Filter;

public class LdapQueryFilter extends QueryFilter<Filter> {
    private final LDAPDataConversion dataConversion;

    @Inject
    public LdapQueryFilter(LDAPDataConversion dataConversion) {
        this.dataConversion = dataConversion;
    }

    public QueryFilterBuilder<Filter> and() {
        return new LdapQueryFilterBuilder(true);
    }

    public QueryFilterBuilder<Filter> or() {
        return new LdapQueryFilterBuilder(false);
    }

    public class LdapQueryFilterBuilder extends QueryFilterBuilder<Filter> {

        public LdapQueryFilterBuilder(boolean and) {
            super(and);
        }

        /**
         * Match all Tokens that have an expiry date before or equal to the given date.
         * @param timestamp Non null date to ldapFilter against.
         * @return The QueryBuilder for subsequent calls.
         */
        public QueryFilterBuilder<Filter> beforeDate(Calendar timestamp) {
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
        public QueryFilterBuilder<Filter> attribute(CoreTokenField field, Object value) {
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
    }}
