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
package org.forgerock.openam.core.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.iplanet.am.util.SystemProperties;
import org.apache.commons.lang.StringUtils;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.connections.MonitoredCTSConnectionFactory;
import org.forgerock.openam.cts.monitoring.impl.connections.WrappedHandlerFactory;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ExternalCTSConfig;
import org.forgerock.openam.sm.SMSConfigurationFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider;
import org.forgerock.openam.sm.datalayer.providers.DataLayerConnectionFactoryCache;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Guice Module to capture the details of the Data Layer specific bindings.
 */
@GuiceModule
public class DataLayerGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConnectionFactoryProvider.class).to(DataLayerConnectionFactoryCache.class);
    }

    @Provides
    StoreMode getStoreMode() {
        String mode = SystemProperties.get(CoreTokenConstants.CTS_STORE_LOCATION);
        if (StringUtils.isNotEmpty(mode)) {
            return StoreMode.valueOf(mode.toUpperCase());
        } else {
            return StoreMode.DEFAULT;
        }
    }

    @Provides @Inject @Named(DataLayerConstants.SERVICE_MANAGER_CONFIG)
    ConnectionConfig getDataLayerConfig(SMSConfigurationFactory smsConfigurationFactory) {
        return smsConfigurationFactory.getSMSConfiguration();
    }

    @Provides @Inject @Named(DataLayerConstants.EXTERNAL_CTS_CONFIG)
    ConnectionConfig getExternalCTSConfig(ExternalCTSConfig externalCTSConfig) {
        externalCTSConfig.update();
        return externalCTSConfig;
    }

    @Provides @Inject @Named(DataLayerConstants.DATA_LAYER_CTS_ASYNC_BINDING)
    ConnectionFactory getCTSAsyncConnectionFactory(DataLayerConnectionFactoryCache factoryCache,
                                                   CTSConnectionMonitoringStore monitorStore,
                                                   WrappedHandlerFactory handlerFactory) {
        return new MonitoredCTSConnectionFactory(
                getConnectionFactory(ConnectionType.CTS_ASYNC, factoryCache),
                monitorStore,
                handlerFactory);
    }

    @Provides @Inject @Named(DataLayerConstants.DATA_LAYER_CTS_REAPER_BINDING)
    ConnectionFactory getCTSReaperConnectionFactory(DataLayerConnectionFactoryCache factoryCache) {
        return getConnectionFactory(ConnectionType.CTS_REAPER, factoryCache);
    }

    @Provides @Inject @Named(DataLayerConstants.DATA_LAYER_BINDING)
    ConnectionFactory getDataLayerConnectionFactory(DataLayerConnectionFactoryCache factoryCache) {
        return getConnectionFactory(ConnectionType.DATA_LAYER, factoryCache);
    }

    private ConnectionFactory getConnectionFactory(ConnectionType type,
                                                   DataLayerConnectionFactoryCache factoryCache) {
        try {
            return factoryCache.createFactory(type);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
