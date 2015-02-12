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
package org.forgerock.openam.core.guice;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.connections.MonitoredCTSConnectionFactory;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.SMSConfigurationFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.FilterConversion;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutorThreadFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryPartialTokenConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryStringConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.EntryTokenConverter;
import org.forgerock.openam.sm.datalayer.impl.ldap.ExternalLdapConfig;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryBuilder;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilter;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapSearchHandler;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.openam.sm.datalayer.providers.ConnectionFactoryProvider;
import org.forgerock.openam.sm.datalayer.providers.DataLayerConnectionFactoryCache;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

/**
 * Guice Module to capture the details of the Data Layer specific bindings. These are bound in a private binder so that
 * each {@link ConnectionType} can have different bindings, allowing different backend databases.
 * <p>
 * ConnectionFactory and TaskExecutor instances will be available outside the private binder using the
 * @{@link DataLayer} annotation with the desired {@link ConnectionType}.
 */
@GuiceModule
public class DataLayerGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<ConnectionType, DataLayerConfiguration> configurationMapBinder = MapBinder.newMapBinder(binder(),
                ConnectionType.class, DataLayerConfiguration.class);

        for (ConnectionType connectionType : ConnectionType.values()) {
            PrivateBinder binder = binder().newPrivateBinder().withSource(connectionType);

            DataLayer typed = DataLayer.Types.typed(connectionType);

            binder.bind(ConnectionType.class).toInstance(connectionType);
            binder.bind(DataLayerConfiguration.class)
                    .toProvider(DataLayerConfigurationProvider.class).in(Singleton.class);
            configurationMapBinder.addBinding(connectionType).toProvider(binder.getProvider(DataLayerConfiguration.class));

            Class<? extends javax.inject.Provider<ConnectionFactory>> providerType;
            if (connectionType == ConnectionType.CTS_ASYNC) {
                providerType = CTSConnectionFactoryProvider.class;
            } else {
                providerType = DataLayerConnectionFactoryCache.class;
            }

            binder.bind(ConnectionConfig.class).annotatedWith(Names.named(DataLayerConstants.EXTERNAL_CONFIG))
                    .toProvider(ExternalConnectionConfigProvider.class);

            binder.bind(ConnectionFactoryProvider.class)
                    .toProvider(ConnectionFactoryProviderProvider.class);

            Key<ConnectionFactory> externalConnectionFactoryKey = Key.get(ConnectionFactory.class, typed);
            binder.bind(ConnectionFactory.class).toProvider(providerType);
            binder.bind(externalConnectionFactoryKey).toProvider(providerType);
            binder.expose(externalConnectionFactoryKey);

            Key<QueueConfiguration> externalQueueConfigurationKey = Key.get(QueueConfiguration.class, typed);
            binder.bind(QueueConfiguration.class)
                    .toProvider(InternalQueueConfigurationProvider.class).in(Singleton.class);
            binder.bind(externalQueueConfigurationKey)
                    .toProvider(binder.getProvider(QueueConfiguration.class));
            binder.expose(externalQueueConfigurationKey);

            Key<QueryFactory> externalQueryFactoryKey = Key.get(QueryFactory.class, typed);
            binder.bind(QueryFactory.class).toProvider(InternalQueryFactoryProvider.class);
            binder.bind(externalQueryFactoryKey).toProvider(InternalQueryFactoryProvider.class);
            binder.expose(externalQueryFactoryKey);

            binder.bind(TaskFactory.class);
            binder.bind(Key.get(TaskFactory.class, typed))
                    .toProvider(binder.getProvider(TaskFactory.class));
            binder.expose(Key.get(TaskFactory.class, typed));

            binder.bind(FilterConversion.class).toProvider(InternalFilterConversionProvider.class);

            binder.bind(TokenStorageAdapter.class).toProvider(TokenStorageAdapterProvider.class);

            binder.bind(ExecutorService.class).toProvider(ExecutorServiceProvider.class);

            binder.bind(LdapTokenAttributeConversion.class);
            binder.bind(LdapSearchHandler.class);
            binder.bind(SeriesTaskExecutorThreadFactory.class);
            binder.bind(EntryPartialTokenConverter.class);
            binder.bind(EntryTokenConverter.class);
            binder.bind(LdapQueryBuilder.class);
            binder.bind(LdapQueryFactory.class);
            binder.bind(LdapQueryFilter.class);

            MapBinder<Class, EntryConverter> entryConverterBinder = MapBinder.newMapBinder(binder, Class.class,
                    EntryConverter.class);
            entryConverterBinder.addBinding(String.class).to(EntryStringConverter.class);
            entryConverterBinder.addBinding(PartialToken.class).to(EntryPartialTokenConverter.class);
            entryConverterBinder.addBinding(Token.class).to(EntryTokenConverter.class);

            Key<TaskExecutor> executorKey = Key.get(TaskExecutor.class, typed);
            binder.bind(executorKey)
                    .toProvider(InternalTaskExecutorProvider.class);
            binder.expose(executorKey);
        }

    }

    @Provides @Inject @Named(DataLayerConstants.SERVICE_MANAGER_CONFIG)
    ConnectionConfig getDataLayerConfig(SMSConfigurationFactory smsConfigurationFactory) {
        return smsConfigurationFactory.getSMSConfiguration();
    }

    /**
     * This provider provides ConnectionConfig instances for external LDAP data stores. Configuration values are
     * updated at the point of providing the ConnectionConfig instance.
     */
    private static final class ExternalConnectionConfigProvider implements Provider<ConnectionConfig> {
        private final DataLayerConfiguration configuration;
        private final ExternalLdapConfig externalConfig;

        @Inject
        public ExternalConnectionConfigProvider (ExternalLdapConfig externalConfig, DataLayerConfiguration configuration) {
            this.externalConfig = externalConfig;
            this.configuration = configuration;
        }

        public ConnectionConfig get() {
            externalConfig.update(configuration);
            return externalConfig;
        }
    }

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

    private static class InternalQueryFactoryProvider implements Provider<QueryFactory> {
        private final Provider<DataLayerConfiguration> configuration;
        private final Injector injector;

        @Inject
        private InternalQueryFactoryProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
            this.injector = injector;
        }

        @Override
        public QueryFactory get() {
            return (QueryFactory) injector.getInstance(configuration.get().getQueryFactoryType());
        }
    }

    private static class ExecutorServiceProvider implements Provider<ExecutorService> {
        private final Provider<DataLayerConfiguration> configuration;

        @Inject
        private ExecutorServiceProvider(Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
        }

        @Override
        public ExecutorService get() {
            return configuration.get().createExecutorService();
        }
    }

    private static class DataLayerConfigurationProvider implements Provider<DataLayerConfiguration> {
        private final ConnectionType connectionType;
        private final Injector injector;

        @Inject
        private DataLayerConfigurationProvider(Injector injector, ConnectionType connectionType) {
            this.injector = injector;
            this.connectionType = connectionType;
        }

        @Override
        public DataLayerConfiguration get() {
            return injector.getInstance(connectionType.getConfigurationClass());
        }
    }

    private static class InternalTaskExecutorProvider implements Provider<TaskExecutor> {
        private final Provider<DataLayerConfiguration> configuration;
        private final Injector injector;

        @Inject
        private InternalTaskExecutorProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
            this.injector = injector;
        }

        @Override
        public TaskExecutor get() {
            return (TaskExecutor) injector.getInstance(configuration.get().getTaskExecutorType());
        }
    }

    private static class TokenStorageAdapterProvider implements Provider<TokenStorageAdapter> {
        private final Provider<DataLayerConfiguration> configuration;
        private final Injector injector;

        @Inject
        private TokenStorageAdapterProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
            this.injector = injector;
        }

        @Override
        public TokenStorageAdapter get() {
            return (TokenStorageAdapter) injector.getInstance(configuration.get().getTokenStorageAdapterType());
        }
    }

    private static class InternalQueueConfigurationProvider implements Provider<QueueConfiguration> {
        private final Injector injector;
        private final Provider<DataLayerConfiguration> configuration;

        @Inject
        private InternalQueueConfigurationProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.injector = injector;
            this.configuration = configuration;
        }

        @Override
        public QueueConfiguration get() {
            return (QueueConfiguration) injector.getInstance(configuration.get().getQueueConfigurationType());
        }
    }

    private static class InternalFilterConversionProvider implements Provider<FilterConversion> {
        private final Provider<DataLayerConfiguration> configuration;
        private final Injector injector;

        @Inject
        private InternalFilterConversionProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
            this.injector = injector;
        }

        @Override
        public FilterConversion get() {
            return (FilterConversion) injector.getInstance(configuration.get().getFilterConversionType());
        }
    }

    private static class ConnectionFactoryProviderProvider implements Provider<ConnectionFactoryProvider> {
        private final Provider<DataLayerConfiguration> configuration;
        private final Injector injector;

        @Inject
        private ConnectionFactoryProviderProvider(Injector injector, Provider<DataLayerConfiguration> configuration) {
            this.configuration = configuration;
            this.injector = injector;
        }

        @Override
        public ConnectionFactoryProvider get() {
            return (ConnectionFactoryProvider) injector.getInstance(configuration.get().getConnectionFactoryProviderType());
        }
    }

}
