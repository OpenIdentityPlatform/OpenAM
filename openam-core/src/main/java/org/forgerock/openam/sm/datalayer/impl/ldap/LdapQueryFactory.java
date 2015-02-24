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

import javax.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilterVisitor;

import com.google.inject.Injector;

/**
 * A query factory for LDAP connections.
 */
public class LdapQueryFactory implements QueryFactory<Connection, Filter> {

    private final Injector injector;

    @Inject
    public LdapQueryFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public QueryBuilder<Connection, Filter> createInstance() {
        return injector.getInstance(LdapQueryBuilder.class);
    }

    @Override
    public QueryFilterVisitor<Filter, Void, CoreTokenField> createFilterConverter() {
        return injector.getInstance(LdapQueryFilterVisitor.class);
    }

}
