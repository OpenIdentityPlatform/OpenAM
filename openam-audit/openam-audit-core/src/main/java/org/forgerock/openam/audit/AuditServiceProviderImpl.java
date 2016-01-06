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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.audit;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for creating the AuditService on configuration change.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditServiceProviderImpl implements AuditServiceProvider {

    private final Debug debug = Debug.getInstance("amAudit");

    private final AuditServiceConfigurationProvider configProvider;
    private final AMAuditService defaultAuditService;
    private final Map<String, AMAuditService> auditServices = new ConcurrentHashMap<>();
    private final ShutdownManager shutdownManager;
    private final EventTopicsMetaData eventTopicsMetaData;

    /**
     * Create an instance of AuditServiceProviderImpl.
     *
     * @param configProvider The configProvider responsible for providing audit service configuration.
     * @param shutdownManager The shutdown manager to register the shutdown listener to.
     */
    @Inject
    public AuditServiceProviderImpl(AuditServiceConfigurationProvider configProvider, ShutdownManager shutdownManager) {
        this.configProvider = configProvider;
        this.shutdownManager = shutdownManager;
        this.eventTopicsMetaData = configProvider.getEventTopicsMetaData();
        this.defaultAuditService = createDefaultAuditService();
        registerListeners();
    }

    private DefaultAuditServiceProxy createDefaultAuditService() {
        AMAuditServiceConfiguration configuration = new AMAuditServiceConfiguration(false);
        configuration.setAvailableAuditEventHandlers(Collections.<String>emptyList());
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withEventTopicsMetaData(eventTopicsMetaData)
                .withConfiguration(configuration);

        DefaultAuditServiceProxy auditServiceProxy =  new DefaultAuditServiceProxy(builder.build(), configuration);
        try {
            auditServiceProxy.startup();
        } catch (ServiceUnavailableException e) {
            debug.error("Default Audit Service configuration failed.", e);
        }
        return auditServiceProxy;
    }

    private void registerListeners() {

        configProvider.addConfigurationListener(new AuditServiceConfigurationListener() {
            @Override
            public void globalConfigurationChanged() {
                refreshDefaultAuditService();
            }

            @Override
            public void realmConfigurationChanged(String realm) {
                refreshRealmAuditService(realm);
            }

            @Override
            public void realmConfigurationRemoved(String realm) {
                removeRealmAuditService(realm);
            }
        });


        shutdownManager.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdown() {
                closeAuditServices();
            }
        });
    }

    @Override
    public AMAuditService getAuditService(String realm) {
        AMAuditService auditService = auditServices.get(realm);
        if (auditService == null) {
            return defaultAuditService;
        } else {
            return auditService;
        }
    }

    @Override
    public AMAuditService getDefaultAuditService() {
        return defaultAuditService;
    }

    private void refreshDefaultAuditService() {
        AMAuditServiceConfiguration configuration = configProvider.getDefaultConfiguration();
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withEventTopicsMetaData(eventTopicsMetaData)
                .withConfiguration(configuration);
        if (configuration.isAuditEnabled()) {
            configureEventHandlers(builder, configProvider.getDefaultEventHandlerConfigurations());
        }

        try {
            defaultAuditService.setDelegate(builder.build(), configuration);
        } catch (ServiceUnavailableException e) {
            debug.error("Default Audit Service configuration failed.", e);
        }
    }

    private void refreshRealmAuditService(String realm) {
        AMAuditServiceConfiguration configuration = configProvider.getRealmConfiguration(realm);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withEventTopicsMetaData(eventTopicsMetaData)
                .withConfiguration(configuration);
        if (configuration.isAuditEnabled()) {
            configureEventHandlers(builder, configProvider.getRealmEventHandlerConfigurations(realm));
        }

        AMAuditService auditService = auditServices.get(realm);
        try {
            if (auditService == null) {
                auditService = new RealmAuditServiceProxy(builder.build(), defaultAuditService, configuration);
                auditService.startup();
                auditServices.put(realm, auditService);
            } else {
                auditService.setDelegate(builder.build(), configuration);
            }
        } catch (ServiceUnavailableException e) {
            debug.error("New Audit Service configuration for realm {} failed.", e, realm);
            auditServices.remove(realm); // remove it so that we can fall back to the default service
        }
    }

    private void removeRealmAuditService(String realm) {
        AMAuditService auditService = auditServices.remove(realm);
        if (auditService != null) {
            auditService.shutdown();
        }
    }

    private void closeAuditServices() {
        for (final AMAuditService auditService : auditServices.values()) {
            auditService.shutdown();
        }
        defaultAuditService.shutdown();
    }

    private void configureEventHandlers(AuditServiceBuilder builder, Set<AuditEventHandlerConfiguration> configs) {
        for (AuditEventHandlerConfiguration config : configs) {
            AuditEventHandler eventHandler = createEventHandler(config);
            if (eventHandler != null) {
                try {
                    builder.withAuditEventHandler(eventHandler);
                } catch (AuditException e) {
                    debug.error("Unable to configure audit event handler called {}", config.getHandlerName(), e);
                }
            }
        }
    }

    private AuditEventHandler createEventHandler(AuditEventHandlerConfiguration config) {
        String className = getMapAttr(config.getAttributes(), "handlerFactory");
        try {
            Class<? extends AuditEventHandlerFactory> handlerFactoryClass =
                    Class.forName(className).asSubclass(AuditEventHandlerFactory.class);
            return InjectorHolder.getInstance(handlerFactoryClass).create(config);
        } catch (AuditException | ClassNotFoundException | RuntimeException e) {
            debug.error("Unable to create audit event handler called {}", config.getHandlerName(), e);
        }

        return null;
    }

}
