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
package org.forgerock.openam.audit;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfigurationWrapper;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.util.thread.listener.ShutdownListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
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

    private final AuditServiceConfigurator configurator;
    private final AuditEventHandlerFactory handlerFactory;
    private final AMAuditService defaultAuditService;
    private final Map<String, AMAuditService> auditServices = new ConcurrentHashMap<>();
    private final ShutdownManager shutdownManager;

    @Inject
    public AuditServiceProviderImpl(AuditServiceConfigurator configurator, AuditEventHandlerFactory handlerFactory,
                                    ShutdownManager shutdownManager) {
        this.configurator = configurator;
        this.handlerFactory = handlerFactory;
        this.shutdownManager = shutdownManager;
        this.defaultAuditService = createDefaultAuditService();
        registerListeners();
    }

    private AMAuditServiceProxy createDefaultAuditService() {
        AMAuditServiceConfiguration configuration = new AMAuditServiceConfiguration(false, true, false);
        configuration.setAvailableAuditEventHandlers(Collections.<String>emptyList());
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withCoreTopicSchemaExtensions(getCoreTopicSchemaExtensions())
                .withConfiguration(configuration);

        AMAuditServiceProxy auditServiceProxy =  new AMAuditServiceProxy(builder.build(), configuration);
        try {
            auditServiceProxy.startup();
        } catch (ServiceUnavailableException e) {
            debug.error("Default Audit Service configuration failed.", e);
        }
        return auditServiceProxy;
    }

    private void registerListeners() {

        configurator.addConfigurationListener(new AuditServiceConfigurationListener() {
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
        AMAuditServiceConfiguration configuration = configurator.getDefaultConfiguration();
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withCoreTopicSchemaExtensions(getCoreTopicSchemaExtensions())
                .withConfiguration(configuration);
        configureEventHandlers(builder, configurator.getDefaultEventHandlerConfigurations());

        try {
            defaultAuditService.setDelegate(builder.build(), configuration);
        } catch (ServiceUnavailableException e) {
            debug.error("Default Audit Service configuration failed.", e);
        }
    }

    private void refreshRealmAuditService(String realm) {
        AMAuditServiceConfiguration configuration = configurator.getRealmConfiguration(realm);
        AuditServiceBuilder builder = AuditServiceBuilder.newAuditService()
                .withCoreTopicSchemaExtensions(getCoreTopicSchemaExtensions())
                .withConfiguration(configuration);
        configureEventHandlers(builder, configurator.getRealmEventHandlerConfigurations(realm));

        AMAuditService auditService = auditServices.get(realm);
        try {
            if (auditService == null) {
                auditService = new AMAuditServiceProxy(builder.build(), configuration);
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
            new Thread() {
                @Override
                public void run() {
                    auditService.shutdown();
                }
            }.start();
        }
    }

    private void configureEventHandlers(AuditServiceBuilder builder,
                                        Set<AuditEventHandlerConfigurationWrapper> eventHandlerConfigurations) {

        for (AuditEventHandlerConfigurationWrapper config : eventHandlerConfigurations) {
            try {
                AuditEventHandler eventHandler = handlerFactory.create(config);
                builder.withAuditEventHandler(eventHandler, config.getName(), config.getEventTopics());
            } catch (AuditException e) {
                debug.error("Unable to configure audit event handler called {}", e, config.getName());
            }
        }
    }

    private JsonValue getCoreTopicSchemaExtensions() {
        String path = "/org/forgerock/openam/audit/events-config.json";
        try {
            InputStream is = AMAuditService.class.getResourceAsStream(path);
            String contents = IOUtils.readStream(is);
            return JsonValueBuilder.toJsonValue(contents.replaceAll("\\s", ""));
        } catch (IOException e) {
            debug.error("Unable to read Audit event configuration file {}", path, e);
        }

        return json(object());
    }

}
