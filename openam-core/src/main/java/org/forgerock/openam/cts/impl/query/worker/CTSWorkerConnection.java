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
package org.forgerock.openam.cts.impl.query.worker;

import java.io.Closeable;
import java.util.Collection;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.queries.CTSWorkerBaseQuery;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.utils.IOUtils;

/**
 * This implementation delegates to the actual implementation and manages the details
 * of the connection that will be used for the paged query against the LDAP server.
 *
 * This implementation is not thread safe.
 *
 * Threading Policy: This class will detect that the current thread has been interrupted
 * and close the established connection.
 */
public class CTSWorkerConnection<C extends Closeable> implements CTSWorkerQuery {

    private final ConnectionFactory<C> factory;
    private final CTSWorkerBaseQuery query;
    private C connection;
    private boolean failed = false;

    /**
     * Creates a new CTSWorkerConnection with appropriate {@link ConnectionFactory} to produce
     * connections and a {@link CTSWorkerBaseQuery} to execute on the connection.
     *
     * @param factory Required for establishing a connection to the persistence layer.
     * @param query The specific implementation that will be delegated to.
     */
    public CTSWorkerConnection(ConnectionFactory factory, CTSWorkerBaseQuery query) {
        this.factory = factory;
        this.query = query;
    }

    /**
     * If this first call, then establishes a connection to the persistence layer and
     * assigns this to the delegate implementation. If the query has been exhausted then
     * the connection will be closed automatically.
     *
     * @return Null if the current thread was interrupted. Otherwise, delegates to
     * provided implementation.
     *
     * @throws CoreTokenException {@inheritDoc}
     */
    @Override
    public Collection<String> nextPage() throws CoreTokenException {
        // Detect interruption.
        if (Thread.currentThread().isInterrupted()) {
            close();
            return null;
        }

        if (failed) {
            throw new IllegalStateException();
        }

        try {
            initConnection();
            Collection<String> results = query.nextPage();
            endProcessing(results);
            return results;
        } catch (CoreTokenException e) {
            failed = true;
            close();
            throw e;
        }
    }

    /**
     * If this is the first call, then initialise the connection.
     *
     * @throws CoreTokenException If there was an error getting the connection.
     */
    private void initConnection() throws CoreTokenException {
        if (connection == null || !factory.isValid(connection)) {
            try {
                connection = factory.create();
                query.setConnection(connection);
            } catch (DataLayerException e) {
                throw new CoreTokenException("Failed to init connection to data layer", e);
            }
        }
    }

    /**
     * If processing has ended then the connection must be closed.
     *
     * @param results If null, then this signals end of processing.
     */
    private void endProcessing(Collection<String> results) {
        if (results == null) {
            close();
        }
    }

    /**
     * Close and null the connection.
     */
    private void close() {
        IOUtils.closeIfNotNull(connection);
    }
}
