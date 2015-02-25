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

package org.forgerock.openam.cts.monitoring.impl.connections;

import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.ResultHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A wrapper for the CTSConnectionFactory which tracks the success/failure
 * of connection attempts requested through this factory. The tracked values are
 * stored in a data structure which is shared by the monitoring system.
 */
public class MonitoredCTSConnectionFactory implements ConnectionFactory {

    private final CTSConnectionMonitoringStore monitorStore;
    private final ConnectionFactory connectionFactory;
    private final WrappedHandlerFactory handlerFactory;

    /**
     * The connection factory registers as a config listener in case these settings change.
     *
     * @param connectionFactory The ConnectionFactory to use to generate connections
     * @param monitorStore The data structure in which to count connection success/failures
     */
    @Inject
    public MonitoredCTSConnectionFactory(@Named(DataLayerConstants.DATA_LAYER_CTS_ASYNC_BINDING)
                                         ConnectionFactory connectionFactory,
                                         CTSConnectionMonitoringStore monitorStore,
                                         WrappedHandlerFactory handlerFactory) {
        this.connectionFactory = connectionFactory;
        this.monitorStore = monitorStore;
        this.handlerFactory = handlerFactory;
    }

    /**
     * Provides a connection to the applicable token store, whether embedded or external.
     * Additionally tracks the number of successful/failed attempts to get connections
     * and stores in the provided structure.
     *
     * @return a connection to the token store.
     * @throws org.forgerock.opendj.ldap.ErrorResultException unable to provide a connection.
     */
    public Connection getConnection() throws ErrorResultException {
        boolean success = false;
        try {
            Connection response = connectionFactory.getConnection();
            success = true;
            return response;
        } finally {
            monitorStore.addConnection(success);
        }
    }

    /**
     * Provides an asynchronous connection to the token store.
     * @param resultHandler the result handler.
     * @return an asynchronous connection.
     */
    public FutureResult<Connection> getConnectionAsync(final ResultHandler<? super Connection> resultHandler) {

        ResultHandler<? super Connection> wrappingHandler = handlerFactory.build(resultHandler);

        return connectionFactory.getConnectionAsync(wrappingHandler);
    }

    @Override
    public void close() {
        connectionFactory.close();
    }
}
