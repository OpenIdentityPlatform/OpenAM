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
package org.forgerock.openam.audit.configuration;

import static com.sun.identity.shared.Constants.AM_AUTH_COOKIE_NAME;
import static com.sun.identity.shared.Constants.AM_COOKIE_NAME;
import static com.sun.identity.shared.datastruct.CollectionHelper.*;
import static com.sun.identity.sm.SMSUtils.*;
import static java.util.Collections.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.SERVICE_NAME;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.filter.FilterPolicy;
import org.forgerock.openam.audit.AMAuditService;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.RealmUtils;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.HashMap;
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
    private final EventTopicsMetaData eventTopicsMetaData;

    private volatile boolean initialised = false;

    /**
     * Construct an instance of AuditServiceConfigurationProviderImpl.
     */
    public AuditServiceConfigurationProviderImpl() {
        this.eventTopicsMetaData = getEventTopicsMetaData();
    }

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

        // If setup has already completed, let the listener know of the current state
        if (initialised) {
            listener.globalConfigurationChanged();

            for (String realm : getRealmNames()) {
                ServiceConfig config = getAuditRealmConfiguration(realm);
                if (serviceExists(config)) {
                    listener.realmConfigurationChanged(realm);
                } else {
                    listener.realmConfigurationRemoved(realm);
                }
            }
        }
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
    public Set<AuditEventHandlerConfiguration> getDefaultEventHandlerConfigurations() {
        return getEventHandlerConfigurations(getAuditGlobalConfiguration());
    }

    @Override
    public Set<AuditEventHandlerConfiguration> getRealmEventHandlerConfigurations(String realm) {
        return getEventHandlerConfigurations(getAuditRealmConfiguration(realm));
    }

    @Override
    public EventTopicsMetaData getEventTopicsMetaData() {
        EventTopicsMetaDataBuilder builder = EventTopicsMetaDataBuilder.coreTopicSchemas();

        String path = "/org/forgerock/openam/audit/events-config.json";
        try {
            InputStream is = AMAuditService.class.getResourceAsStream(path);
            String contents = IOUtils.readStream(is);
            builder.withCoreTopicSchemaExtensions(JsonValueBuilder.toJsonValue(contents.replaceAll("\\s", "")));
        } catch (IOException e) {
            debug.error("Unable to read Audit event configuration file {}", path, e);
        }

        return builder.build();
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
        }
    }

    @SuppressWarnings("unchecked")
    private AMAuditServiceConfiguration getConfiguration(ServiceConfig config) {
        Map<String, Set<String>> attributes;
        if (config == null) {
            attributes = emptyMap();
        } else {
            attributes = config.getAttributes();
        }

        // We have a system property which says whether to audit AM_ACCESS_ATTEMPT.  If false (i.e. we do NOT
        // want to audit this event name, we blacklist the event name.  This is so in the future we will be able
        // to blacklist any or all event names to get better control over the amount of stuff that is audited.
        //
        Set<String> blacklistedEventNames = new HashSet<>();
        if (!SystemProperties.getAsBoolean(Constants.AUDIT_AM_ACCESS_ATTEMPT_ENABLED)) {
            blacklistedEventNames.add(EventName.AM_ACCESS_ATTEMPT.toString());
        }

        AMAuditServiceConfiguration configuration = new AMAuditServiceConfiguration(
                getBooleanMapAttr(attributes, "auditEnabled", false), blacklistedEventNames);

        Set<String> filterPolicies = new HashSet<>();
        for (String policy : attributes.get("fieldFilterPolicy")) {
            if (isNotEmpty(policy)) {
                policy = policy.replaceAll("%AM_COOKIE_NAME%", SystemProperties.get(AM_COOKIE_NAME));
                policy = policy.replaceAll("%AM_AUTH_COOKIE_NAME%", SystemProperties.get(AM_AUTH_COOKIE_NAME));
                filterPolicies.add(policy);
            }
        }
        Map<String, FilterPolicy> filterPolicyMap = new HashMap<>();
        FilterPolicy fieldFP = new FilterPolicy();
        fieldFP.setExcludeIf(filterPolicies);
        filterPolicyMap.put("field", fieldFP);
        configuration.setFilterPolicies(filterPolicyMap);

        return configuration;
    }

    private Set<AuditEventHandlerConfiguration> getEventHandlerConfigurations(ServiceConfig serviceConfig) {
        if (!serviceExists(serviceConfig)) {
            return emptySet();
        }

        Set<AuditEventHandlerConfiguration> eventHandlerConfigurations = new HashSet<>();
        try {
            Set<String> handlerNames = serviceConfig.getSubConfigNames();
            for (String handlerName : handlerNames) {
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> attributes = serviceConfig.getSubConfig(handlerName).getAttributes();
                AuditEventHandlerConfiguration config = AuditEventHandlerConfiguration.builder()
                        .withName(handlerName)
                        .withAttributes(attributes)
                        .withEventTopicsMetaData(eventTopicsMetaData).build();
                eventHandlerConfigurations.add(config);
            }
        } catch (SSOException | SMSException e) {
            debug.error("Error accessing service {}. No audit event handlers will be registered.", SERVICE_NAME, e);
        }

        return eventHandlerConfigurations;
    }

    private ServiceConfig getAuditGlobalConfiguration() {
        try {
            return new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getGlobalConfig("default");
        } catch (SMSException | SSOException e) {
            debug.error("Error accessing service {}", SERVICE_NAME, e);
        }
        return null;
    }

    private ServiceConfig getAuditRealmConfiguration(String realm) {
        try {
            return new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getOrganizationConfig(realm, null);
        } catch (SMSException | SSOException e) {
            debug.error("Error accessing service {}", SERVICE_NAME, e);
        }
        return null;
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    private Set<String> getRealmNames() {
        try {
            RealmUtils.getRealmNames(getAdminToken());
        } catch (SMSException e) {
            debug.error("An error occurred while trying to retrieve the list of realms", e);
        }
        return emptySet();
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // Ignore
    }
}
