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
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.util.Reject;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
            if (containsUuid(config.getUuid())) {
                getSubOrgConfig().getSubConfig(config.getUuid()).setAttributes(data);
            } else {
                getSubOrgConfig().addSubConfig(config.getUuid(), SCRIPT_CONFIGURATION, 0, data);
            }
        } catch (SMSException e) {
            throw createAndLogError(logger, SAVE_FAILED, e, config.getUuid(), realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, SAVE_FAILED, e, config.getUuid(), realm);
        }
    }

    @Override
    public void delete(String uuid) throws ScriptException {
        try {
            getSubOrgConfig().removeSubConfig(uuid);
        } catch (SMSException e) {
            throw createAndLogError(logger, DELETE_FAILED, e, uuid, realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, DELETE_FAILED, e, uuid, realm);
        }
    }

    @Override
    public Set<ScriptConfiguration> getAll() throws ScriptException {
        final Set<ScriptConfiguration> scriptConfigurations = new HashSet<ScriptConfiguration>();
        try {
            final ServiceConfig subOrgConfig = getSubOrgConfig();
            final Set<String> uuids = subOrgConfig.getSubConfigNames();
            for (String uuid : uuids) {
                scriptConfigurations.add(
                        scriptConfigurationFromMap(uuid, subOrgConfig.getSubConfig(uuid).getAttributes()));
            }
        } catch (SMSException e) {
            throw createAndLogError(logger, RETRIEVE_ALL_FAILED, e, realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, RETRIEVE_ALL_FAILED, e, realm);
        }
        return scriptConfigurations;
    }

    @Override
    public ScriptConfiguration get(String uuid) throws ScriptException {
        try {
            return scriptConfigurationFromMap(uuid, getSubOrgConfig().getSubConfig(uuid).getAttributes());
        } catch (SMSException e) {
            throw createAndLogError(logger, RETRIEVE_FAILED, e, uuid, realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, RETRIEVE_FAILED, e, uuid, realm);
        }
    }

    @Override
    public boolean containsUuid(String uuid) throws ScriptException {
        final ServiceConfig serviceConfig;
        try {
            serviceConfig = getSubOrgConfig().getSubConfig(uuid);
        } catch (SMSException e) {
            throw createAndLogError(logger, FIND_BY_UUID_FAILED, e, uuid, realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, FIND_BY_UUID_FAILED, e, uuid, realm);
        }
        return serviceConfig != null && serviceConfig.exists();
    }

    @Override
    public boolean containsName(String name) throws ScriptException {
        try {
            final ServiceConfig subOrgConfig = getSubOrgConfig();
            final Set<String> configNames = subOrgConfig.getSubConfigNames();
            for (String configName : configNames) {
                if (name.equals(getMapAttr(subOrgConfig.getSubConfig(configName).getAttributes(), SCRIPT_NAME))) {
                    return true;
                }
            }
        } catch (SMSException e) {
            throw createAndLogError(logger, FIND_BY_NAME_FAILED, e, name, realm);
        } catch (SSOException e) {
            throw createAndLogError(logger, FIND_BY_NAME_FAILED, e, name, realm);
        }
        return false;
    }

    /**
     * Add all the script configuration attributes to a map that can be stored in the data store.
     *
     * @param config The script configuration to add to the map.
     * @return The map containing the resource type attributes.
     */
    private Map<String, Set<String>> getScriptConfigurationData(ScriptConfiguration config) {
        final Map<String, Set<String>> dataMap = new HashMap<String, Set<String>>();
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

        return ScriptConfiguration.builder()
                .setUuid(uuid)
                .setName(getMapAttr(data, SCRIPT_NAME))
                .setDescription(getMapAttr(data, SCRIPT_DESCRIPTION))
                .setContext(getContextFromString(getMapAttr(data, SCRIPT_CONTEXT)))
                .setLanguage(getLanguageFromString(getMapAttr(data, SCRIPT_LANGUAGE)))
                .setScript(getMapAttr(data, SCRIPT_TEXT))
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
