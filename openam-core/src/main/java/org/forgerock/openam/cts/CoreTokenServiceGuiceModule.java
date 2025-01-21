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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.core.guice.CTSObjectMapperProvider;
import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.DeletePreReadOptionFunction;
import org.forgerock.openam.cts.impl.ETagAssertionCTSOptionFunction;
import org.forgerock.openam.cts.impl.LdapOptionFunction;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerConstants;
import org.forgerock.openam.cts.impl.query.worker.queries.CTSWorkerPastExpiryDateQuery;
import org.forgerock.openam.cts.impl.query.worker.queries.MaxSessionTimeExpiredQuery;
import org.forgerock.openam.cts.impl.query.worker.queries.SessionIdleTimeExpiredQuery;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.CTSMonitoringStoreImpl;
import org.forgerock.openam.cts.monitoring.impl.queue.MonitoredResultHandlerFactory;
import org.forgerock.openam.cts.worker.CTSWorkerManager;
import org.forgerock.openam.cts.worker.CTSWorkerTask;
import org.forgerock.openam.cts.worker.CTSWorkerTaskProvider;
import org.forgerock.openam.cts.worker.filter.CTSWorkerSelectAllFilter;
import org.forgerock.openam.cts.worker.process.CTSWorkerDeleteProcess;
import org.forgerock.openam.cts.worker.process.MaxSessionTimeExpiredProcess;
import org.forgerock.openam.cts.worker.process.SessionIdleTimeExpiredProcess;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.util.Option;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;

/**
 * The Core Token Service is responsible for providing a key/value pair generic storage
 * mechanism for other components in the system.
 *
 * It has dependencies on other Guice modules:
 *
 * - Session: As it currently provides some session based functionality
 * - Shared: Thread Monitor
 * - Core: SSO Token and related (as a result of Session)
 * - Audit: as a result of Session
 */
@GuiceModule
public class CoreTokenServiceGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(CTSPersistentStore.class).to(CTSPersistentStoreImpl.class);

        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_DEBUG))
                .toInstance(Debug.getInstance(CoreTokenConstants.CTS_DEBUG));

        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_REAPER_DEBUG))
                .toInstance(Debug.getInstance(CoreTokenConstants.CTS_REAPER_DEBUG));

        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_ASYNC_DEBUG))
                .toInstance(Debug.getInstance(CoreTokenConstants.CTS_ASYNC_DEBUG));

        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_MONITOR_DEBUG))
                .toInstance(Debug.getInstance(CoreTokenConstants.CTS_MONITOR_DEBUG));

        bind(ObjectMapper.class)
                .annotatedWith(Names.named(CoreTokenConstants.OBJECT_MAPPER))
                .toProvider(CTSObjectMapperProvider.class)
                .in(Singleton.class);

        // CTS Connection Management
        bind(String.class).annotatedWith(Names.named(DataLayerConstants.ROOT_DN_SUFFIX)).toProvider(new Provider<String>() {
            public String get() {
                return SMSEntry.getRootSuffix();
            }
        }).in(Singleton.class);

        bind(ConfigurationObserver.class).toProvider(new Provider<ConfigurationObserver>() {
            public ConfigurationObserver get() {
                return ConfigurationObserver.getInstance();
            }
        }).in(Singleton.class);

        // CTS Monitoring
        bind(CTSOperationsMonitoringStore.class).to(CTSMonitoringStoreImpl.class);
        bind(CTSReaperMonitoringStore.class).to(CTSMonitoringStoreImpl.class);
        bind(CTSConnectionMonitoringStore.class).to(CTSMonitoringStoreImpl.class);

        // Enable monitoring of all CTS operations
        bind(ResultHandlerFactory.class).to(MonitoredResultHandlerFactory.class);

        /**
         * Core Token Service bindings are divided into a number of logical groups.
         */
        MapBinder<Option<?>, LdapOptionFunction> optionFunctionMapBinder = MapBinder.newMapBinder(binder(),
                new TypeLiteral<Option<?>>() {}, new TypeLiteral<LdapOptionFunction>() {});
        optionFunctionMapBinder.addBinding(CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION)
                .to(ETagAssertionCTSOptionFunction.class);
        optionFunctionMapBinder.addBinding(CTSOptions.PRE_DELETE_READ_OPTION).to(DeletePreReadOptionFunction.class);

        expose(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_DEBUG));
        expose(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_ASYNC_DEBUG));
        expose(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_MONITOR_DEBUG));
        
        expose(new TypeLiteral<Map<Option<?>, LdapOptionFunction>>() {});
        expose(CoreTokenConfig.class);
        expose(CTSPersistentStore.class);
        expose(CTSConnectionMonitoringStore.class);
        expose(CTSOperationsMonitoringStore.class);
        expose(CTSReaperMonitoringStore.class);
        
        expose(ExecutorService.class).annotatedWith(Names.named(CoreTokenConstants.CTS_WORKER_POOL));
        expose(ObjectMapper.class).annotatedWith(Names.named(CoreTokenConstants.OBJECT_MAPPER));
        expose(ResultHandlerFactory.class);
        expose(String.class).annotatedWith(Names.named(DataLayerConstants.ROOT_DN_SUFFIX));
    }

    @Provides @Singleton
    CoreTokenConfig getCoreTokenConfig() {
        return CoreTokenConfig.newCoreTokenConfig();
    }

    /**
     * The CTS Worker Pool provides a thread pool specifically for CTS usage.
     *
     * This is only utilised by the CTS asynchronous queue implementation, therefore
     * we can size the pool based on the configuration for that.
     *
     * @param esf Factory for generating an appropriate ExecutorService.
     * @param queueConfiguration Required to resolve how many threads are required.
     * @return A configured ExecutorService, appropriate for the CTS usage.
     *
     * @throws java.lang.RuntimeException If there was an error resolving the configuration.
     */
    @Provides
    @Inject
    @Named(CoreTokenConstants.CTS_WORKER_POOL)
    ExecutorService getCTSWorkerExecutorService(
            AMExecutorServiceFactory esf,
            @DataLayer(ConnectionType.CTS_ASYNC) QueueConfiguration queueConfiguration) {
        try {
            int size = queueConfiguration.getProcessors();
            return esf.createFixedThreadPool(size, CoreTokenConstants.CTS_WORKER_POOL);
        } catch (DataLayerException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides @Inject @Named(CTSMonitoringStoreImpl.EXECUTOR_BINDING_NAME)
    ExecutorService getCTSMonitoringExecutorService(AMExecutorServiceFactory esf) {
        return esf.createFixedThreadPool(5, "CTSMonitoring");
    }

    @Provides @Inject @Named(CTSWorkerConstants.DELETE_ALL_MAX_EXPIRED)
    CTSWorkerTask getDeleteAllMaxExpiredReaperTask(
            CTSWorkerPastExpiryDateQuery query,
            CTSWorkerDeleteProcess deleteProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        String taskName = CTSWorkerConstants.DELETE_ALL_MAX_EXPIRED + "Task";
        return new CTSWorkerTask(query, deleteProcess, selectAllFilter, taskName);
    }

    @Provides @Inject @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED)
    CTSWorkerTask getMaxSessionTimeExpiredTask(
            MaxSessionTimeExpiredQuery query,
            MaxSessionTimeExpiredProcess maxSessionTimeExpiredProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        String taskName = CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED + "Task";
        return new CTSWorkerTask(query, maxSessionTimeExpiredProcess, selectAllFilter, taskName);
    }

    @Provides @Inject @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED)
    CTSWorkerTask getSessionIdleTimeExpiredTask(
            SessionIdleTimeExpiredQuery query,
            SessionIdleTimeExpiredProcess sessionIdleTimeExpiredProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        String taskName = CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED + "Task";
        return new CTSWorkerTask(query, sessionIdleTimeExpiredProcess, selectAllFilter, taskName);
    }

    @Provides @Inject
    CTSWorkerTaskProvider getWorkerTaskProvider(
            @Named(CTSWorkerConstants.DELETE_ALL_MAX_EXPIRED) CTSWorkerTask deleteExpiredTokensTask,
            @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED) CTSWorkerTask maxSessionTimeExpiredTask,
            @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED) CTSWorkerTask sessionIdleTimeExpiredTask) {
        return new CTSWorkerTaskProvider(Arrays.asList(
                deleteExpiredTokensTask,
                maxSessionTimeExpiredTask,
                sessionIdleTimeExpiredTask));
    }

    @Provides @Inject @Singleton
    CTSWorkerManager getCTSWorkerManager(CTSWorkerTaskProvider workerTaskProvider, ThreadMonitor monitor,
            CoreTokenConfig config, AMExecutorServiceFactory executorServiceFactory,
            @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        return CTSWorkerManager.newCTSWorkerInit(workerTaskProvider, monitor, config, executorServiceFactory, debug);
    }

}
