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
package org.forgerock.openam.audit.configuration;

import static com.iplanet.am.util.SystemProperties.CONFIG_PATH;
import static com.iplanet.am.util.SystemProperties.get;
import static com.sun.identity.shared.Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR;
import static org.forgerock.openam.audit.AuditConstants.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandlerConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

/**
 * Listens to Audit Logger configuration changes and notify the Audit Service.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditServiceConfiguratorImpl implements AuditServiceConfigurator, ServiceListener {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    private final AMAuditServiceConfiguration configuration = new AMAuditServiceConfiguration();
    private final AuditService auditService;
    private volatile boolean initialised = false;

    /**
     * Create a new instance of {@code AuditServiceConfiguratorImpl}.
     * @param auditService The audit service that needs to be configured.
     */
    @Inject
    public AuditServiceConfiguratorImpl(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void configureAuditService() {
        try {
            refreshConfiguration();
            registerServiceListener();
            registerEventHandlers();
            auditService.configure(configuration);
        } catch (ResourceException | AuditException e) {
            DEBUG.error("Unable to configure AuditService", e);
            throw new RuntimeException("Unable to configure AuditService.", e);
        }
    }

    @Override
    public AMAuditServiceConfiguration getAuditServiceConfiguration() {
        return configuration;
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String component, int type) {
        if (!SERVICE_NAME.equals(serviceName)) {
            return;
        }

        if (StringUtils.isEmpty(component)) {
            refreshConfiguration();

            if (configuration.isAuditEnabled()) {
                try {
                    registerEventHandlers();
                } catch (ResourceException | AuditException e) {
                    DEBUG.error("Unable to register audit event handlers.", e);
                }
            }
        } else {
            serviceComponentChanged(component);
        }
    }

    private void registerEventHandlers() throws ResourceException, AuditException {
        if (!configuration.isAuditEnabled()) {
            DEBUG.message("Audit logging is disabled. No event handlers will be registered.");
            return;
        }

        try {
            ServiceConfig parentConfig = getAuditGlobalConfiguration();
            Set<String> handlerNames = parentConfig.getSubConfigNames();
            for (String handler : handlerNames) {
                updateEventHandlerConfiguration(parentConfig.getSubConfig(handler), auditService);
            }
        } catch (SSOException | SMSException e) {
            DEBUG.error("Error accessing service {}", SERVICE_NAME, e);
        }
    }

    /**
     * Registers this configurator with the {@link com.sun.identity.sm.ServiceConfigManager} to receive updates
     * when the script configuration changes.
     *
     * @throws IllegalStateException if the configuration listener cannot be registered.
     */
    private void registerServiceListener() {
        if (initialised) {
            return;
        }
        try {
            String listenerId = new ServiceConfigManager(SERVICE_NAME, getAdminToken()).addListener(this);
            if (listenerId == null) {
                throw new SMSException("Unable to register service config listener");
            }
            initialised = true;
            DEBUG.message("Registered service config listener: {}", listenerId);
        } catch (SSOException | SMSException e) {
            DEBUG.error("Unable to create ServiceConfigManager", e);
            throw new IllegalStateException(e);
        }
    }

    private void refreshConfiguration() {
        ServiceConfig globalConfig = getAuditGlobalConfiguration();
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributes = globalConfig.getAttributes();
        configuration.setAuditEnabled(CollectionHelper.getBooleanMapAttr(attributes, "auditEnabled", false));
        configuration.setAuditFailureSuppressed(
                CollectionHelper.getBooleanMapAttr(attributes, "suppressAuditFailure", true));
        configuration.setResolveHostNameEnabled(CollectionHelper.getBooleanMapAttr(attributes,
                "resolveHostNameEnabled", false));
    }

    private void serviceComponentChanged(String serviceComponent) {
        serviceComponent = serviceComponent.startsWith("/") ? serviceComponent.substring(1).trim() : serviceComponent;
        String[] components = serviceComponent.split("/");
        if (components.length == 1) {
            ServiceConfig eventHandlerConfig = getEventHandlerConfiguration(components[0]);
            if (eventHandlerConfig == null) {
                DEBUG.error(
                        "No event handler configuration called {} found in service {}. No configuration changes made.",
                        components[0], SERVICE_NAME);
                return;
            }
            try {
                updateEventHandlerConfiguration(eventHandlerConfig, InjectorHolder.getInstance(AuditService.class));
            } catch (ResourceException | AuditException e) {
                DEBUG.error("Failed to configure the {} event handler", components[0], e);
            }
        }
    }

    private void updateEventHandlerConfiguration(ServiceConfig eventHandlerConfig, AuditService auditService)
            throws ResourceException, AuditException {

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributes = eventHandlerConfig.getAttributes();
        if (CSV.equalsIgnoreCase(eventHandlerConfig.getSchemaID())) {
            updateCsvEventHandlerConfiguration(attributes, auditService);
        }
    }

    private void updateCsvEventHandlerConfiguration(Map<String, Set<String>> attributes, AuditService auditService)
            throws AuditException, ResourceException {

        if (!CollectionHelper.getBooleanMapAttr(attributes, "enabled", false)) {
            // deregister the handler from the audit service here
            return;
        }

        AuditEventHandler csvAuditEventHandler = auditService.getRegisteredHandler(CSV);
        if (csvAuditEventHandler == null) {
            csvAuditEventHandler = new CSVAuditEventHandler();
            auditService.register(csvAuditEventHandler, CSV, attributes.get("topics"));
        }
        CSVAuditEventHandlerConfiguration csvHandlerConfiguration = new CSVAuditEventHandlerConfiguration();
        String location = CollectionHelper.getMapAttr(attributes, "location");
        csvHandlerConfiguration.setLogDirectory(location.replaceAll("%BASE_DIR%", get(CONFIG_PATH))
                .replaceAll("%SERVER_URI%", get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR)));
        csvAuditEventHandler.configure(csvHandlerConfiguration);
    }

    private ServiceConfig getEventHandlerConfiguration(String handler) {
        try {
            return getAuditGlobalConfiguration().getSubConfig(handler);
        } catch (SMSException | SSOException e) {
            DEBUG.error("Error accessing service {}", SERVICE_NAME, e);
        }
        return null;
    }

    private ServiceConfig getAuditGlobalConfiguration() {
        try {
            return new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getGlobalConfig("default");
        } catch (SMSException | SSOException e) {
            DEBUG.error("Error accessing service {}", SERVICE_NAME, e);
            throw new IllegalStateException(e);
        }
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // Ignore
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                          String serviceComponent, int type) {
        // Ignore
    }
}
