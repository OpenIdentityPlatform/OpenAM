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
package org.forgerock.openam.cts.impl.query.reaper;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * This implementation will construct an appropriate filter to use for querying the persistence layer
 * and then perform the query.
 */
public class ReaperImpl implements ReaperQuery {
    // Represents the start and end state of the paged query.
    public static final ByteString EMPTY = QueryBuilder.getEmptyPagingCookie();

    private final QueryBuilder builder;
    private final int pageSize;

    private Connection connection;
    private ByteString cookie = null;

    @Inject
    public ReaperImpl(QueryFactory queryFactory, CoreTokenConfig config) {
        Reject.ifTrue(config.getCleanupPageSize() <= 0);
        pageSize = config.getCleanupPageSize();

        Calendar calendar = Calendar.getInstance();

        Filter expired = queryFactory.createFilter().and().beforeDate(calendar).build();
        builder = queryFactory.createInstance()
                .withFilter(expired)
                .returnTheseAttributes(CoreTokenField.TOKEN_ID);
    }

    /**
     * @param connection Non null connection to use for query.
     */
    public void setConnection(Connection connection) {
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

        if (isQueryComplete()) {
            return null;
        }

        builder.pageResultsBy(pageSize, cookie);

        Collection<String> results = new ArrayList<String>();
        for (Entry entry : builder.executeRawResults(connection)) {
            String tokenId = entry.getAttribute(CoreTokenField.TOKEN_ID.toString()).firstValueAsString();
            results.add(tokenId);
        }

        cookie = builder.getPagingCookie();
        return results;
    }

    /**
     * Query state is tracked by the length of the paging cookie.
     *
     * This function also handles the initial case.
     *
     * @return True if the paged query has reached the end. Otherwise false.
     */
    private boolean isQueryComplete() {
        if (cookie == null) {
            cookie = EMPTY;
            return false;
        }
        return cookie.length() == EMPTY.length();
    }
}
