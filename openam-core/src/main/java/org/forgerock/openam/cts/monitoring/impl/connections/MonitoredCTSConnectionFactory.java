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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring.impl.connections;

import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.util.promise.FailureHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.SuccessHandler;

/**
 * A wrapper for the CTSConnectionFactory which tracks the success/failure
 * of connection attempts requested through this factory. The tracked values are
 * stored in a data structure which is shared by the monitoring system.
 */
public class MonitoredCTSConnectionFactory<C> implements ConnectionFactory<C> {

    private final CTSConnectionMonitoringStore monitorStore;
    private final ConnectionFactory<C> connectionFactory;
    private final FailureHandler<DataLayerException> creationFailureHandler = new FailureHandler<DataLayerException>() {
        @Override
        public void handleError(DataLayerException error) {
            monitorStore.addConnection(false);
        }
    };
    private final SuccessHandler<C> creationSuccessHandler = new SuccessHandler<C>() {
        @Override
        public void handleResult(C result) {
            monitorStore.addConnection(true);
        }
    };

    /**
     * The connection factory registers as a config listener in case these settings change.
     *
     * @param connectionFactory The ConnectionFactory to use to generate connections
     * @param monitorStore The data structure in which to count connection success/failures
     */
    public MonitoredCTSConnectionFactory(ConnectionFactory<C> connectionFactory,
                                         CTSConnectionMonitoringStore monitorStore) {
        this.connectionFactory = connectionFactory;
        this.monitorStore = monitorStore;
    }

    /**
     * Provides a connection to the applicable token store, whether embedded or external.
     * Additionally tracks the number of successful/failed attempts to get connections
     * and stores in the provided structure.
     *
     * @return a connection to the token store.
     * @throws DataLayerException unable to provide a connection.
     */
    public C create() throws DataLayerException {
        boolean success = false;
        try {
            C response = connectionFactory.create();
            success = true;
            return response;
        } finally {
            monitorStore.addConnection(success);
        }
    }

    @Override
    public Promise<C, DataLayerException> createAsync() {
        return connectionFactory.createAsync().onFailure(creationFailureHandler).onSuccess(creationSuccessHandler);
    }

    @Override
    public void close() {
        connectionFactory.close();
    }

    @Override
    public boolean isValid(C connection) {
        return connectionFactory.isValid(connection);
    }
}
