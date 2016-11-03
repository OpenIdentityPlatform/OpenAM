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
package org.forgerock.openam.cts;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.DeletePreReadOptionFunction;
import org.forgerock.openam.cts.impl.ETagAssertionCTSOptionFunction;
import org.forgerock.openam.cts.impl.LdapOptionFunction;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerConnection;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerConstants;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.impl.query.worker.queries.CTSWorkerPastExpiryDateQuery;
import org.forgerock.openam.cts.impl.query.worker.queries.MaxSessionTimeExpiredQuery;
import org.forgerock.openam.cts.impl.query.worker.queries.SessionIdleTimeExpiredQuery;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.CTSMonitoringStoreImpl;
import org.forgerock.openam.cts.monitoring.impl.queue.MonitoredResultHandlerFactory;
import org.forgerock.openam.cts.worker.CTSWorkerTask;
import org.forgerock.openam.cts.worker.CTSWorkerTaskProvider;
import org.forgerock.openam.cts.worker.filter.CTSWorkerSelectAllFilter;
import org.forgerock.openam.cts.worker.process.CTSWorkerDeleteProcess;
import org.forgerock.openam.cts.worker.process.MaxSessionTimeExpiredProcess;
import org.forgerock.openam.cts.worker.process.SessionIdleTimeExpiredProcess;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.util.Option;
import org.forgerock.util.thread.ExecutorServiceFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.SessionID;
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

        bind(CoreTokenConstants.class);
        bind(CoreTokenConfig.class);

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
        expose(new TypeLiteral<Map<Option<?>, LdapOptionFunction>>() {});
        expose(CoreTokenConfig.class);
        expose(CTSPersistentStore.class);
        expose(CTSConnectionMonitoringStore.class);
        expose(ExecutorService.class).annotatedWith(Names.named(CoreTokenConstants.CTS_WORKER_POOL));
        expose(ObjectMapper.class).annotatedWith(Names.named(CoreTokenConstants.OBJECT_MAPPER));
        expose(ResultHandlerFactory.class);
        expose(ScheduledExecutorService.class).annotatedWith(Names.named(CoreTokenConstants.CTS_SCHEDULED_SERVICE));
        expose(String.class).annotatedWith(Names.named(DataLayerConstants.ROOT_DN_SUFFIX));
    }

    /**
     * CTS Jackson Object Mapper.
     * <p>
     * Use a static singleton as per <a href="http://wiki.fasterxml.com/JacksonBestPracticesPerformance">performance
     * best practice.</a>
     */
    @Provides @Named(CoreTokenConstants.OBJECT_MAPPER) @Singleton
    ObjectMapper getCTSObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true);

        /**
         * @see http://stackoverflow.com/questions/7105745/how-to-specify-jackson-to-only-use-fields-preferably-globally
         */
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        SimpleModule customModule = new SimpleModule("openam", Version.unknownVersion());
        customModule.addKeyDeserializer(SessionID.class, new SessionIDKeyDeserialiser());
        mapper.registerModule(customModule);
        mapper.addHandler(new CompatibilityProblemHandler());
        return mapper;
    }

    /**
     * This extension allows us to ignore now unmapped fields within InternalSession and its sub-objects.
     *
     * Each field ignored is now calculated dynamically. See field JavaDoc for detail on why the field
     * is ignored and how it is generated.
     */
    private static class CompatibilityProblemHandler extends DeserializationProblemHandler {

        /**
         * InternalSession#restrictedTokensByRestriction, this legacy field is now calculated based on the
         * restrictedTokensBySid map.
         */
        private static final String RESTRICTED_TOKENS_BY_RESTRICTION = "restrictedTokensByRestriction";

        /**
         * SessionID#isParsed, is no longer persisted because of the dynamic nature of server/site configuration
         * it is now not safe to assume that a persisted SessionID has valid S1/SI values.
         */
        private static final String IS_PARSED = "isParsed";
        /**
         * SessionID#extensionPart, is not stored because it is extracted from the encryptedString.
         */
        private static final String EXTENSION_PART = "extensionPart";

        /**
         * SessionID#extensions, is not stored because it is calculated as part of parsing a SessionID.
         */
        private static final String EXTENSIONS = "extensions";

        /**
         * SessionID#tail, is not stored because it is calculated as part of parsing a SessionID.
         */
        private static final String TAIL = "tail";

        private static final Set<String> skipList = new HashSet<>(
                Arrays.asList(RESTRICTED_TOKENS_BY_RESTRICTION, IS_PARSED,
                        EXTENSION_PART, EXTENSIONS, TAIL));

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp,
                                             JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName)
                throws IOException, JsonProcessingException {
            if (skipList.contains(propertyName)) {
                ctxt.getParser().skipChildren();
                return true;
            }
            return false;
        }
    }

    /**
     * This simple {@link KeyDeserializer} implementation allows us to use the {@link SessionID#toString()} value as a
     * map key instead of a whole {@link SessionID} object. During deserialization this class will reconstruct the
     * original SessionID object from the session ID string.
     */
    private static class SessionIDKeyDeserialiser extends KeyDeserializer {

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return new SessionID(key);
        }
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
    ExecutorService getCTSWorkerExecutorService(ExecutorServiceFactory esf,
                                                @DataLayer(ConnectionType.CTS_ASYNC) QueueConfiguration queueConfiguration) {
        try {
            int size = queueConfiguration.getProcessors();
            return esf.createFixedThreadPool(size, CoreTokenConstants.CTS_WORKER_POOL);
        } catch (DataLayerException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides @Inject @Named(CTSMonitoringStoreImpl.EXECUTOR_BINDING_NAME)
    ExecutorService getCTSMonitoringExecutorService(ExecutorServiceFactory esf) {
        return esf.createFixedThreadPool(5);
    }

    @Provides @Inject @Named(CoreTokenConstants.CTS_SCHEDULED_SERVICE)
    ScheduledExecutorService getCTSScheduledService(ExecutorServiceFactory esf) {
        return esf.createScheduledService(1);
    }

    @Provides @Inject @Named(CTSWorkerConstants.PAST_EXPIRY_DATE)
    CTSWorkerQuery getPastExpiryDateQuery(
            CTSWorkerPastExpiryDateQuery query,
            @DataLayer(ConnectionType.CTS_WORKER) ConnectionFactory factory) {
        return new CTSWorkerConnection<>(factory, query);
    }

    @Provides @Inject @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED)
    CTSWorkerQuery getMaxSessionTimeExpiredQuery(
            MaxSessionTimeExpiredQuery query,
            @DataLayer(ConnectionType.CTS_WORKER) ConnectionFactory factory) {
        return new CTSWorkerConnection<>(factory, query);
    }

    @Provides @Inject @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED)
    CTSWorkerQuery getSessionIdleTimeExpiredQuery(
            SessionIdleTimeExpiredQuery query,
            @DataLayer(ConnectionType.CTS_WORKER) ConnectionFactory factory) {
        return new CTSWorkerConnection<>(factory, query);
    }

    @Provides @Inject @Named(CTSWorkerConstants.DELETE_ALL_MAX_EXPIRED)
    CTSWorkerTask getDeleteAllMaxExpiredReaperTask(
            @Named(CTSWorkerConstants.PAST_EXPIRY_DATE) CTSWorkerQuery query,
            CTSWorkerDeleteProcess deleteProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        return new CTSWorkerTask(query, deleteProcess, selectAllFilter);
    }

    @Provides @Inject @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED)
    CTSWorkerTask getMaxSessionTimeExpiredTask(
            @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED) CTSWorkerQuery query,
            MaxSessionTimeExpiredProcess maxSessionTimeExpiredProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        return new CTSWorkerTask(query, maxSessionTimeExpiredProcess, selectAllFilter);
    }

    @Provides @Inject @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED)
    CTSWorkerTask getSessionIdleTimeExpiredTask(
            @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED) CTSWorkerQuery query,
            SessionIdleTimeExpiredProcess sessionIdleTimeExpiredProcess,
            CTSWorkerSelectAllFilter selectAllFilter) {
        return new CTSWorkerTask(query, sessionIdleTimeExpiredProcess, selectAllFilter);
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
}
