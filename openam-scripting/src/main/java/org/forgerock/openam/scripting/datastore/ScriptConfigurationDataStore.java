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
package org.forgerock.openam.scripting.datastore;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.forgerock.openam.scripting.ScriptException.createAndLogError;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.sm.ServiceConfigQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.util.Reject;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The data store responsible for persisting {@code ScriptConfiguration} instances.
 *
 * @since 13.0.0
 */
public class ScriptConfigurationDataStore implements ScriptingDataStore<ScriptConfiguration> {

    private ServiceConfigManager configManager;
    private final Subject subject;
    private final String realm;
    private final Logger logger;

    /**
     * Construct a new instance of {@code ScriptConfigurationDataStore}.
     * @param logger The logger log any error and debug messages to.
     * @param subject The subject requesting modification to the {@code ScriptConfiguration}.
     * @param realm The realm in which the {@code ScriptConfiguration} resides in.
     */
    @Inject
    public ScriptConfigurationDataStore(@Named("ScriptLogger") Logger logger,
                                        @Assisted Subject subject, @Assisted String realm) {
        Reject.ifNull(subject, realm);
        this.subject = subject;
        this.realm = realm;
        this.logger = logger;
    }

    @Override
    public void save(ScriptConfiguration config) throws ScriptException {
        try {
            createCollectionConfig();
            final Map<String, Set<String>> data = getScriptConfigurationData(config);
            if (containsGlobalUuid(config.getId())) {
                getSubGlobalConfig().getSubConfig(config.getId()).setAttributes(data);
            }

            if (containsOrgUuid(config.getId())) {
                getSubOrgConfig().getSubConfig(config.getId()).setAttributes(data);
            } else {
                getSubOrgConfig().addSubConfig(config.getId(), SCRIPT_CONFIGURATION, 0, data);
            }
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, SAVE_FAILED, e, config.getId(), realm);
        }
    }

    @Override
    public void delete(String uuid) throws ScriptException {
        try {
            getSubOrgConfig().removeSubConfig(uuid);
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, DELETE_FAILED, e, uuid, realm);
        }
    }

    @Override
    public Set<ScriptConfiguration> getAll() throws ScriptException {
        final Set<ScriptConfiguration> scriptConfigurations = new LinkedHashSet<>();
        try {
            ServiceConfig config = getSubOrgConfig();
            Set<String> uuids = config.getSubConfigNames();
            for (String uuid : uuids) {
                scriptConfigurations.add(
                        scriptConfigurationFromMap(uuid, config.getSubConfig(uuid).getAttributesForRead()));
            }

            config = getSubGlobalConfig();
            uuids = config.getSubConfigNames();
            for (String uuid : uuids) {
                scriptConfigurations.add(
                        scriptConfigurationFromMap(uuid, config.getSubConfig(uuid).getAttributesForRead()));
            }
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, RETRIEVE_ALL_FAILED, e, realm);
        }
        return scriptConfigurations;
    }

    @Override
    public ScriptConfiguration get(String uuid) throws ScriptException {
        try {
            ServiceConfig config = getSubOrgConfig();
            if (config.getSubConfigNames().contains(uuid)) {
                return scriptConfigurationFromMap(uuid, config.getSubConfig(uuid).getAttributesForRead());
            }
            config = getSubGlobalConfig();
            if (config.getSubConfigNames().contains(uuid)) {
                return scriptConfigurationFromMap(uuid, config.getSubConfig(uuid).getAttributesForRead());
            }
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, RETRIEVE_FAILED, e, uuid, realm);
        }

        throw createAndLogError(logger, SCRIPT_UUID_NOT_FOUND, uuid, realm);
    }

    @Override
    public boolean containsUuid(String uuid) throws ScriptException {
        return containsOrgUuid(uuid) || containsGlobalUuid(uuid);
    }

    private boolean containsOrgUuid(String uuid) throws ScriptException {
        final ServiceConfig orgConfig;
        try {
            orgConfig = getSubOrgConfig().getSubConfig(uuid);
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, FIND_BY_UUID_FAILED, e, uuid, realm);
        }
        return orgConfig != null && orgConfig.exists();
    }

    private boolean containsGlobalUuid(String uuid) throws ScriptException {
        final ServiceConfig globalConfig;
        try {
            globalConfig = getSubGlobalConfig().getSubConfig(uuid);
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, FIND_BY_UUID_FAILED, e, uuid, realm);
        }
        return globalConfig != null && globalConfig.exists();
    }

    @Override
    public boolean containsName(String name) throws ScriptException {
        try {
            ServiceConfig config = getSubOrgConfig();
            Set<String> configNames = config.getSubConfigNames();
            for (String configName : configNames) {
                if (name.equals(getMapAttr(config.getSubConfig(configName).getAttributes(), SCRIPT_NAME))) {
                    return true;
                }
            }
            config = getSubGlobalConfig();
            configNames = config.getSubConfigNames();
            for (String configName : configNames) {
                if (name.equals(getMapAttr(config.getSubConfig(configName).getAttributes(), SCRIPT_NAME))) {
                    return true;
                }
            }
        } catch (SSOException | SMSException e) {
            throw createAndLogError(logger, FIND_BY_NAME_FAILED, e, name, realm);
        }
        return false;
    }

    @Override
    public Set<ScriptConfiguration> get(QueryFilter<String> queryFilter) throws ScriptException {
        final Set<ScriptConfiguration> scriptConfigurations = new LinkedHashSet<>();
        try {
            ServiceConfig config = getSubOrgConfig();
            Set<String> uuids = config.getSubConfigNames();
            for (String uuid : uuids) {
                if (queryFilter.accept(new ServiceConfigQueryFilterVisitor(), config.getSubConfig(uuid))) {
                    scriptConfigurations.add(get(uuid));
                }
            }

            config = getSubGlobalConfig();
            uuids = config.getSubConfigNames();
            for (String uuid : uuids) {
                if (queryFilter.accept(new ServiceConfigQueryFilterVisitor(), config.getSubConfig(uuid))) {
                    scriptConfigurations.add(get(uuid));
                }
            }
        } catch (SMSException | SSOException e) {
            throw createAndLogError(logger, RETRIEVE_ALL_FAILED, e, realm);
        } catch (UnsupportedOperationException e) {
            throw createAndLogError(logger, ScriptErrorCode.valueOf(e.getMessage()), e);
        }
        return scriptConfigurations;
    }

    /**
     * Add all the script configuration attributes to a map that can be stored in the data store.
     *
     * @param config The script configuration to add to the map.
     * @return The map containing the resource type attributes.
     */
    private Map<String, Set<String>> getScriptConfigurationData(ScriptConfiguration config) {
        final Map<String, Set<String>> dataMap = new HashMap<>();
        dataMap.put(SCRIPT_NAME, Collections.singleton(config.getName()));
        dataMap.put(SCRIPT_DESCRIPTION, Collections.singleton(config.getDescription()));
        dataMap.put(SCRIPT_TEXT, Collections.singleton(config.getScript()));
        dataMap.put(SCRIPT_CONTEXT, Collections.singleton(config.getContext().name()));
        dataMap.put(SCRIPT_LANGUAGE, Collections.singleton(config.getLanguage().name()));
        dataMap.put(SCRIPT_CREATED_BY, Collections.singleton(config.getCreatedBy()));
        dataMap.put(SCRIPT_CREATION_DATE, Collections.singleton(String.valueOf(config.getCreationDate())));
        dataMap.put(SCRIPT_LAST_MODIFIED_BY, Collections.singleton(config.getLastModifiedBy()));
        dataMap.put(SCRIPT_LAST_MODIFIED_DATE, Collections.singleton(String.valueOf(config.getLastModifiedDate())));
        return dataMap;
    }

    /**
     * Create a ResourceType object from a map, mapping strings to sets.
     * @param uuid The uuid of the created resource type object.
     * @param data The data map for the object.
     * @return The newly created ResourceType object.
     */
    private ScriptConfiguration scriptConfigurationFromMap(String uuid, Map<String, Set<String>> data)
            throws ScriptException {

        String script = getMapAttr(data, SCRIPT_TEXT);

        return ScriptConfiguration.builder()
                .setId(uuid)
                .setName(getMapAttr(data, SCRIPT_NAME))
                .setDescription(getMapAttr(data, SCRIPT_DESCRIPTION))
                .setContext(getContextFromString(getMapAttr(data, SCRIPT_CONTEXT)))
                .setLanguage(getLanguageFromString(getMapAttr(data, SCRIPT_LANGUAGE)))
                .setScript(script == null ? EMPTY : script)
                .setCreatedBy(getMapAttr(data, SCRIPT_CREATED_BY, EMPTY))
                .setCreationDate(CollectionHelper.getMapAttrAsDateLong(data, SCRIPT_CREATION_DATE, logger))
                .setLastModifiedBy(getMapAttr(data, SCRIPT_LAST_MODIFIED_BY, EMPTY))
                .setLastModifiedDate(CollectionHelper.getMapAttrAsDateLong(data, SCRIPT_LAST_MODIFIED_DATE, logger))
                .build();
    }

    /**
     * Retrieve the {@code ServiceConfigManager} for the ScriptingService.
     * @return the {@code ServiceConfigManager} for the ScriptingService.
     * @throws SMSException if an error has occurred while performing the operation
     * @throws SSOException if the user's single sign on token is invalid or expired
     */
    protected ServiceConfigManager getConfigManager() throws SSOException, SMSException {
        if (configManager == null) {
            configManager = new ServiceConfigManager(SERVICE_NAME, getToken());
        }
        return configManager;
    }

    private void createCollectionConfig() throws SSOException, SMSException {
        final ServiceConfig orgConfig = getOrgConfig();
        if (orgConfig.getSubConfig(SCRIPT_CONFIGURATIONS) == null) {
            orgConfig.addSubConfig(SCRIPT_CONFIGURATIONS, SCRIPT_CONFIGURATIONS, 0, Collections.EMPTY_MAP);
        }
    }

    /**
     * Get the organization configuration for the ScriptingService service.
     * @return The organization configuration, which is guaranteed to not be null.
     * @throws SMSException If the sub configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private ServiceConfig getOrgConfig() throws SMSException, SSOException {
        final ServiceConfig orgConfig = getConfigManager().getOrganizationConfig(realm, null);
        if (orgConfig == null) {
            throw new SMSException("Configuration '" + SERVICE_NAME + "' in realm '" + realm +
                    "' could not be retrieved.");
        }
        return orgConfig;
    }

    /**
     * Get the global configuration for the ScriptingService service.
     * @return The global configuration, which is guaranteed to not be null.
     * @throws SMSException If the configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private ServiceConfig getGlobalConfig() throws SMSException, SSOException {
        ServiceConfig globalConfig = getConfigManager().getGlobalConfig(null);
        if (globalConfig == null) {
            throw new SMSException("Global Configuration for '" + SERVICE_NAME + "' could not be retrieved.");
        }
        return globalConfig;
    }

    /**
     * Get the sub configuration from the organization configuration for the ScriptingService.
     * @return The sub configuration, which is guaranteed to not be null.
     * @throws SMSException If the sub configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private ServiceConfig getSubOrgConfig()throws SMSException, SSOException {
        final ServiceConfig config = getOrgConfig().getSubConfig(SCRIPT_CONFIGURATIONS);
        if (config == null) {
            throw new SMSException("Configuration '" + SCRIPT_CONFIGURATIONS + "' in organization '" + SERVICE_NAME +
                    "' could not be retrieved.");
        }
        return config;
    }

    /**
     * Get the sub configuration from the global configuration for the ScriptingService.
     * @return The sub configuration, which is guaranteed to not be null.
     * @throws SMSException If the sub configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private ServiceConfig getSubGlobalConfig()throws SMSException, SSOException {
        final ServiceConfig config = getGlobalConfig().getSubConfig("globalScripts");
        if (config == null) {
            throw new SMSException("Global Configuration for '" + SERVICE_NAME + "' could not be retrieved.");
        }
        return config;
    }


    /**
     * Returns an admin SSO token for administrative actions.
     *
     * @return An administrative SSO token.
     */
    private SSOToken getToken() throws SSOException {
        final SSOToken token = SubjectUtils.getSSOToken(subject);
        if (token == null) {
            throw new SSOException("Could not find Admin token.");
        }
        return token;
    }
}
