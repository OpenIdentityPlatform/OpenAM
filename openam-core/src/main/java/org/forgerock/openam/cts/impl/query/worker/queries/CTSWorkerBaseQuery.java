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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query.worker.queries;

import java.util.Collection;
import java.util.Iterator;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerConnection;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.util.Reject;

/**
 * Abstract class for the performing of queries related to the CTS Worker Framework.
 * <p>
 * Instances of sub-classes should always be decorated by {@link CTSWorkerConnection} for connection management.
 *
 * @param <C> Connection type.
 */
public abstract class CTSWorkerBaseQuery<C> implements CTSWorkerQuery {

    private Iterator<Collection<PartialToken>> results;
    private C connection;

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
     * @return Non null, non empty collection. Once the query is complete, null.
     *
     * @throws CoreTokenException {@inheritDoc}
     */
    @Override
    public Collection<PartialToken> nextPage() throws CoreTokenException {
        Reject.ifTrue(connection == null, "Connection must be assigned before use");

        if (results == null) {
            results = getQuery().executeRawResults(connection, PartialToken.class);
        }

        if (isQueryComplete()) {
            results = null;
            return null;
        }

        return results.next();
    }

    @Override
    public final void close() {
        // intentionally left blank as connection management is left to CTSWorkerConnection (decorator)
    }

    /**
     * Query state is tracked by the length of the paging cookie.
     *
     * This function also handles the initial case.
     *
     * Indicates if the query is now complete. Once complete no further calls to {@link #nextPage} are required.
     *
     * @return True if the paged query has reached the end. Otherwise false.
     */
    protected boolean isQueryComplete() {
        return !results.hasNext();
    }

    /**
     * Returns the QueryBuilder for this ReaperQuery.
     *
     * @return The QueryBuilder used to generate queries to perform.
     */
    protected abstract QueryBuilder<C, PartialToken> getQuery();

    /**
     * Enforces the overriding of toString for the purpose of logging.
     *
     * @return Readable name of the implemented query.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }
}
