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

package org.forgerock.openam.cts.impl;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.connections.MonitoredCTSConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThreadFactory;
import org.forgerock.openam.sm.datalayer.providers.DataLayerConnectionFactoryCache;

import com.google.inject.Key;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class CTSAsyncConnectionModule extends CTSConnectionModule {

    public CTSAsyncConnectionModule() {
        super(SeriesTaskExecutor.class, true);
    }

    @Override
    protected void configureTaskExecutor(PrivateBinder binder) {
        binder().bind(ExecutorService.class).toProvider(
                binder().getProvider(Key.get(ExecutorService.class, Names.named(CoreTokenConstants.CTS_WORKER_POOL))));
        binder().bind(QueueConfiguration.class).to(CTSQueueConfiguration.class);
        binder().bind(SeriesTaskExecutorThreadFactory.class);
        super.configureTaskExecutor(binder);
    }

    @Override
    protected Class<? extends javax.inject.Provider<ConnectionFactory>> getConnectionFactoryProviderType() {
        return CTSConnectionFactoryProvider.class;
    }

    /**
     * This provider provides ConnectionFactory instances that are wrapped in a monitoring factory.
     */
    private static class CTSConnectionFactoryProvider implements Provider<ConnectionFactory> {
        private final CTSConnectionMonitoringStore monitorStore;
        private final DataLayerConnectionFactoryCache factoryCache;

        @Inject
        public CTSConnectionFactoryProvider(DataLayerConnectionFactoryCache factoryCache,
                CTSConnectionMonitoringStore monitorStore) {
            this.factoryCache = factoryCache;
            this.monitorStore = monitorStore;
        }

        public ConnectionFactory get() {
            return new MonitoredCTSConnectionFactory(
                    factoryCache.get(),
                    monitorStore);
        }
    }

}
