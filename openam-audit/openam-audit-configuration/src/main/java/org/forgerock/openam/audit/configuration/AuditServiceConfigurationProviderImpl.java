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

import static com.iplanet.am.util.SystemProperties.*;
import static com.sun.identity.shared.Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR;
import static com.sun.identity.shared.datastruct.CollectionHelper.*;
import static java.util.Collections.emptySet;
import static org.forgerock.openam.audit.AuditConstants.EventHandlerType.CSV;
import static org.forgerock.openam.audit.AuditConstants.SERVICE_NAME;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandlerConfiguration;

import javax.inject.Singleton;
import java.security.AccessController;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listens to Audit Logger configuration changes and notify the Audit Service.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditServiceConfigurationProviderImpl implements AuditServiceConfigurationProvider, ServiceListener {

    private final Debug debug = Debug.getInstance("amAudit");
    private final List<AuditServiceConfigurationListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean initialised = false;

    @Override
    public void setupComplete() {
        if (initialised) {
            return;
        }

        notifyDefaultConfigurationListeners();

        for (String realm : getRealmNames()) {
            notifyRealmConfigurationListeners(realm);
        }

        registerServiceListener();
        initialised = true;
    }

    @Override
    public void addConfigurationListener(AuditServiceConfigurationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConfigurationListener(AuditServiceConfigurationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String component, int type) {
        if (SERVICE_NAME.equals(serviceName)) {
            notifyDefaultConfigurationListeners();
        }
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                          String component, int type) {

        if (SERVICE_NAME.equals(serviceName)) {
            notifyRealmConfigurationListeners(DNMapper.orgNameToRealmName(orgName));
        }

    }

    @Override
    public AMAuditServiceConfiguration getDefaultConfiguration() {
        return getConfiguration(getAuditGlobalConfiguration());
    }

    @Override
    public AMAuditServiceConfiguration getRealmConfiguration(String realm) {
        return getConfiguration(getAuditRealmConfiguration(realm));
    }

    @Override
    public Set<AuditEventHandlerConfigurationWrapper> getDefaultEventHandlerConfigurations() {
        return getEventHandlerConfigurations(getAuditGlobalConfiguration());
    }

    @Override
    public Set<AuditEventHandlerConfigurationWrapper> getRealmEventHandlerConfigurations(String realm) {
        return getEventHandlerConfigurations(getAuditRealmConfiguration(realm));
    }

    private void notifyDefaultConfigurationListeners() {
        for (AuditServiceConfigurationListener listener : listeners) {
            listener.globalConfigurationChanged();
        }
    }

    private void notifyRealmConfigurationListeners(String realm) {
        ServiceConfig config = getAuditRealmConfiguration(realm);
        if (serviceExists(config)) {
            for (AuditServiceConfigurationListener listener : listeners) {
                listener.realmConfigurationChanged(realm);
            }
        } else {
            for (AuditServiceConfigurationListener listener : listeners) {
                listener.realmConfigurationRemoved(realm);
            }
        }
    }

    /**
     * Registers this configurator with the {@link com.sun.identity.sm.ServiceConfigManager} to receive updates
     * when the script configuration changes.
     *
     * @throws IllegalStateException if the configuration listener cannot be registered.
     */
    private void registerServiceListener() {
        try {
            String listenerId = new ServiceConfigManager(SERVICE_NAME, getAdminToken()).addListener(this);
            if (listenerId == null) {
                throw new SMSException("Unable to register service config listener");
            }
            debug.message("Registered service config listener: {}", listenerId);
        } catch (SSOException | SMSException e) {
            debug.error("Unable to create ServiceConfigManager", e);
            throw new IllegalStateException(e);
        }
    }

    private AMAuditServiceConfiguration getConfiguration(ServiceConfig config) {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributes = config.getAttributes();
        return new AMAuditServiceConfiguration(
                getBooleanMapAttr(attributes, "auditEnabled", false),
                getBooleanMapAttr(attributes, "suppressAuditFailure", true),
                getBooleanMapAttr(attributes, "resolveHostNameEnabled", false));
    }

    private Set<AuditEventHandlerConfigurationWrapper> getEventHandlerConfigurations(ServiceConfig serviceConfig) {
        if (!serviceExists(serviceConfig)) {
            return emptySet();
        }

        Set<AuditEventHandlerConfigurationWrapper> eventHandlerConfigurations = new HashSet<>();
        try {
            Set<String> handlerNames = serviceConfig.getSubConfigNames();
            for (String handlerName : handlerNames) {
                AuditEventHandlerConfigurationWrapper eventHandlerConfiguration =
                        getEventHandlerConfiguration(serviceConfig.getSubConfig(handlerName));
                if (eventHandlerConfiguration != null) {
                    eventHandlerConfigurations.add(eventHandlerConfiguration);
                }
            }
        } catch (SSOException | SMSException e) {
            debug.error("Error accessing service {}. No audit event handlers will be registered.", SERVICE_NAME, e);
        }

        return eventHandlerConfigurations;
    }

    private AuditEventHandlerConfigurationWrapper getEventHandlerConfiguration(ServiceConfig eventHandlerConfig) {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributes = eventHandlerConfig.getAttributes();
        boolean handlerEnabled = getBooleanMapAttr(attributes, "enabled", false);

        if (handlerEnabled && CSV.name().equalsIgnoreCase(eventHandlerConfig.getSchemaID())) {
            return getCsvEventHandlerConfiguration(eventHandlerConfig.getName(), attributes);
        }

        return null;
    }

    private AuditEventHandlerConfigurationWrapper getCsvEventHandlerConfiguration(
            String name, Map<String, Set<String>> attributes) {

        CSVAuditEventHandlerConfiguration csvHandlerConfiguration = new CSVAuditEventHandlerConfiguration();
        String location = getMapAttr(attributes, "location");
        csvHandlerConfiguration.setLogDirectory(location.replaceAll("%BASE_DIR%", get(CONFIG_PATH))
                .replaceAll("%SERVER_URI%", get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR)));

        return new AuditEventHandlerConfigurationWrapper(csvHandlerConfiguration, CSV, name, attributes.get("topics"));
    }

    private ServiceConfig getAuditGlobalConfiguration() {
        try {
            return new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getGlobalConfig("default");
        } catch (SMSException | SSOException e) {
            debug.error("Error accessing service {}", SERVICE_NAME, e);
            throw new IllegalStateException(e);
        }
    }

    private ServiceConfig getAuditRealmConfiguration(String realm) {
        try {
            return new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getOrganizationConfig(realm, null);
        } catch (SMSException | SSOException e) {
            debug.error("Error accessing service {}", SERVICE_NAME, e);
            throw new IllegalStateException(e);
        }
    }

    private boolean serviceExists(ServiceConfig auditServiceConfig) {
        return auditServiceConfig != null && auditServiceConfig.exists();
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRealmNames() {
        try {
            Set<String> rootSubRealms = new OrganizationConfigManager(getAdminToken(), "/")
                    .getSubOrganizationNames("*", true);
            Set<String> qualifiedRealmNames = new HashSet<>();
            qualifiedRealmNames.add("/");
            for (String subRealm : rootSubRealms) {
                qualifiedRealmNames.add("/" + subRealm);
            }
            return qualifiedRealmNames;
        } catch (SMSException e) {
            debug.error("An error occurred while trying to retrieve the list of realms", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // Ignore
    }
}
