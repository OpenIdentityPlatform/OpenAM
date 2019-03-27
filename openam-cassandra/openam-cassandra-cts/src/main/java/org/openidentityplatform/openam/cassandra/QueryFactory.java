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
 * Copyright 2019 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.cassandra;

import javax.inject.Inject;

import org.forgerock.openam.tokens.CoreTokenField;
import com.datastax.driver.core.Session;
import com.google.inject.Injector;

/**
 * A query factory for LDAP connections.
 */
public class QueryFactory implements org.forgerock.openam.sm.datalayer.api.query.QueryFactory<Session, Filter> {

    private final Injector injector;

    @Inject
    public QueryFactory(Injector injector) {
        this.injector = injector;
    }

	@Override
    public org.forgerock.openam.sm.datalayer.api.query.QueryBuilder<Session, Filter> createInstance() {
        return injector.getInstance(QueryBuilder.class);
    }

    @Override
    public org.forgerock.util.query.QueryFilterVisitor<Filter, Void, CoreTokenField> createFilterConverter() {
        return injector.getInstance(QueryFilterVisitor.class);
    }

}
