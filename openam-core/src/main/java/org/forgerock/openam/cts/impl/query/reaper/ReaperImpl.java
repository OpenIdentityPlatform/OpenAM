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
package org.forgerock.openam.cts.impl.query.reaper;

import static org.forgerock.openam.utils.Time.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

/**
 * This implementation will construct an appropriate filter to use for querying the persistence layer
 * and then perform the query.
 */
public class ReaperImpl<C, F> implements ReaperQuery {

    private Iterator<Collection<String>> results;
    private final QueryBuilder<C, F> query;
    private C connection;

    @Inject
    public ReaperImpl(@DataLayer(ConnectionType.CTS_REAPER) QueryFactory queryFactory, CoreTokenConfig config) {
        QueryFactory<C, F> queryFactoryProvided = queryFactory;
        Reject.ifTrue(config.getCleanupPageSize() <= 0);
        int pageSize = config.getCleanupPageSize();

        Calendar calendar = getCalendarInstance();

        QueryFilter<CoreTokenField> filter = QueryFilter.lessThan(CoreTokenField.EXPIRY_DATE, calendar);
        query = queryFactoryProvided.createInstance()
                .withFilter(filter.accept(queryFactoryProvided.createFilterConverter(), null))
                .pageResultsBy(pageSize)
                .returnTheseAttributes(CoreTokenField.TOKEN_ID);
    }

    /**
     * @param connection Non null connection to use for query.
     */
    public void setConnection(C connection) {
        Reject.ifNull(connection);
        this.connection = connection;
    }

    /**
     * Performs the query against the persistence layer.
     *
     * The paging cookie must be provided on each subsequent call to the query.
     *
     * Note: The connection provided must not change during the course of the query.
     * Internally the LDAP server associated the query with the connection.
     *
     * @return Non null, non empty collection. Once the query is complete, null.
     *
     * @throws CoreTokenException {@inheritDoc}
     */
    @Override
    public Collection<String> nextPage() throws CoreTokenException {
        Reject.ifTrue(connection == null, "Connection must be assigned before use");

        if (results == null) {
            results = query.executeRawResults(connection, String.class);
        }

        if (isQueryComplete()) {
            return null;
        }

        return results.next();
    }

    /**
     * Query state is tracked by the length of the paging cookie.
     *
     * This function also handles the initial case.
     *
     * @return True if the paged query has reached the end. Otherwise false.
     */
    private boolean isQueryComplete() {
        return !results.hasNext();
    }
}
