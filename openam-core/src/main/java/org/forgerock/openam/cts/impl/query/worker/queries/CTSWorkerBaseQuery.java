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

import java.io.Closeable;
import java.util.Collection;
import java.util.Iterator;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.LdapInitializationFailedException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Filter;

/**
 * Abstract class for the performing of queries related to the CTS Worker Framework.
 * <p>
 * Instances of sub-classes should always be used within try-with-resources statements. The implementations may throw
 * exceptions as part of their processing, at which point callers need to ensure they use the {@link #close()} method to
 * clean up used resources.
 *
 * @param <C> Connection type.
 */
public abstract class CTSWorkerBaseQuery<C> implements CTSWorkerQuery {

    private final ConnectionFactory<C> factory;
    private Iterator<Collection<PartialToken>> results;
    private C connection;

    public CTSWorkerBaseQuery(ConnectionFactory<C> factory) {
        this.factory = factory;
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
        // NB: this thread may be interrupted by shutdown
        if (Thread.currentThread().isInterrupted()) {
            close();
            return null;
        }
        try {
            if (connection == null) {
                connection = factory.create();
                results = getQuery().executeRawResults(connection, PartialToken.class);
            }
        } catch (DataLayerException e) {
            throw new LdapInitializationFailedException(e);
        }

        if (isQueryComplete()) {
            results = null;
            return null;
        }

        return results.next();
    }

    @Override
    public final void close() {
    	if (connection instanceof Closeable) {
    		IOUtils.closeIfNotNull((Closeable)connection);
    	}
        connection = null;
        results = null;
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
    protected abstract QueryBuilder<C, Filter> getQuery();

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
