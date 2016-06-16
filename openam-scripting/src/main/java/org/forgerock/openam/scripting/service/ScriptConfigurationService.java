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
package org.forgerock.openam.scripting.service;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapSetThrows;
import static java.util.Arrays.asList;
import static org.forgerock.openam.scripting.ScriptConstants.EMPTY;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_CONFIGURATION;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_CONFIGURATIONS;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_CONTEXT;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_CREATED_BY;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_CREATION_DATE;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_DESCRIPTION;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_LANGUAGE;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_LAST_MODIFIED_BY;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_LAST_MODIFIED_DATE;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_NAME;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_TEXT;
import static org.forgerock.openam.scripting.ScriptConstants.SERVICE_NAME;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.DELETE_FAILED;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.DELETING_DEFAULT_SCRIPT;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.DELETING_SCRIPT_IN_USE_PLURAL;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.DELETING_SCRIPT_IN_USE_SINGULAR;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.INSUFFICIENT_PRIVILEGES;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.SAVE_FAILED;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.SCRIPT_NAME_EXISTS;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.SCRIPT_UUID_EXISTS;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.SCRIPT_UUID_NOT_FOUND;
import static org.forgerock.openam.scripting.ScriptConstants.getContextFromString;
import static org.forgerock.openam.scripting.ScriptConstants.getLanguageFromString;
import static org.forgerock.openam.scripting.ScriptException.createAndLogDebug;
import static org.forgerock.openam.scripting.ScriptException.createAndLogError;
import static org.forgerock.openam.utils.Time.newDate;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;
import javax.security.auth.Subject;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.datastruct.ValueNotFoundException;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * The {@code ScriptConfigurationService} for access to the persisted {@code ScriptConfiguration}
 * instances. It is the layer on top of the {@code ScriptConfigurationDataStore}.
 *
 * @since 13.0.0
 */
public class ScriptConfigurationService implements ScriptingService, ServiceListener {

    private static final ScriptConfigurationQueryFilterVisitor SCRIPT_CONFIGURATION_QUERY_FILTER_VISITOR =
            new ScriptConfigurationQueryFilterVisitor();
    private final Logger logger;
    private final String realm;
    private final CoreWrapper coreWrapper;
    private final ServiceConfigManager scm;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    @GuardedBy("lock")
    private Map<String, ScriptConfiguration> realmConfigurations;
    @GuardedBy("lock")
    private Map<String, ScriptConfiguration> globalConfigurations;

    /**
     * Construct a new instance of {@code ScriptConfigurationService}.
     *
     * @param logger           The logger log any error and debug messages to.
     * @param realm            The realm in which the {@code ScriptConfiguration} resides in.
     * @param coreWrapper      Wrapper for access to core services.
     */
    @Inject
    public ScriptConfigurationService(Logger logger, String realm, CoreWrapper coreWrapper, ServiceConfigManager scm) {
        Reject.ifNull(realm);
        this.logger = logger;
        this.realm = realm;
        this.coreWrapper = coreWrapper;
        this.scm = scm;
        init();
    }

    private void init() {
        reload();
        scm.addListener(this);
    }

    private void reload() {
        final int readLocked = lock.getReadHoldCount();
        for (int i = 0; i < readLocked; i++) {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        try {
            try {
                realmConfigurations = getScriptConfigurations(getSubOrgConfig());
                globalConfigurations = getScriptConfigurations(getSubGlobalConfig());
            } catch (SSOException | SMSException | ScriptException e) {
                throw new IllegalStateException("Could not initialise script configurations for realm " + realm, e);
            }
        } finally {
            for (int i = 0; i < readLocked; i++) {
                lock.readLock().lock();
            }
            lock.writeLock().unlock();
        }
    }

    private Map<String, ScriptConfiguration> getScriptConfigurations(ServiceConfig config)
            throws SMSException, SSOException, ScriptException {
        Map<String, ScriptConfiguration> configurations = new LinkedHashMap<>();
        Set<String> uuids = config.getSubConfigNames();
        for (String id : uuids) {
            configurations.put(id, scriptConfigurationFromMap(id, config.getSubConfig(id).getAttributesForRead()));
        }
        return configurations;
    }

    @Override
    public ScriptConfiguration create(ScriptConfiguration config, Subject subject) throws ScriptException {
        lock.readLock().lock();
        try {
            failIfUuidExists(config.getId());
            failIfNameExists(config.getName());
            final ScriptConfiguration updatedConfig = setMetaData(config, subject);
            save(updatedConfig);
            return updatedConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ScriptConfiguration update(ScriptConfiguration config, Subject subject) throws ScriptException {
        lock.readLock().lock();
        try {
            final ScriptConfiguration oldConfig = get(config.getId());
            if (!oldConfig.getName().equals(config.getName())) {
                failIfNameExists(config.getName());
            }
            final ScriptConfiguration updatedConfig = setMetaData(config, subject);
            save(updatedConfig);
            return updatedConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(String uuid) throws ScriptException {
        Reject.ifTrue(lock.getReadHoldCount() > 0, "Should not already be locked for reading");
        lock.readLock().lock();
        try {
            failIfUuidDoesNotExist(uuid);
            ScriptConfiguration scriptConfig = get(uuid);
            if (containsGlobalUuid(uuid) || isDefaultScript(scriptConfig)) {
                throw new ScriptException(DELETING_DEFAULT_SCRIPT, scriptConfig.getName());
            }
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                int usageCount = getUsageCount(scriptConfig);
                if (usageCount > 0) {
                    if (usageCount == 1) {
                        throw new ScriptException(DELETING_SCRIPT_IN_USE_SINGULAR, scriptConfig.getName());
                    }
                    throw new ScriptException(DELETING_SCRIPT_IN_USE_PLURAL,
                            scriptConfig.getName(), Integer.toString(usageCount));
                }
                getSubOrgConfig().removeSubConfig(uuid);
                realmConfigurations.remove(uuid);
            } catch (SSOException | SMSException e) {
                throw createAndLogError(logger, DELETE_FAILED, e, uuid, realm);
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<ScriptConfiguration> getAll() throws ScriptException {
        Set<ScriptConfiguration> configurations = new HashSet<>();
        lock.readLock().lock();
        try {
            configurations.addAll(realmConfigurations.values());
            configurations.addAll(globalConfigurations.values());
            return configurations;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ScriptConfiguration get(String uuid) throws ScriptException {
        lock.readLock().lock();
        try {
            failIfUuidDoesNotExist(uuid);
            ScriptConfiguration realmConfiguration = realmConfigurations.get(uuid);
            return realmConfiguration != null ? realmConfiguration : globalConfigurations.get(uuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<ScriptConfiguration> get(QueryFilter<String> queryFilter) throws ScriptException {
        return queryFilter.accept(SCRIPT_CONFIGURATION_QUERY_FILTER_VISITOR, getAll());
    }

    private void failIfNameExists(String name) throws ScriptException {
        if (containsName(name)) {
            throw createAndLogDebug(logger, SCRIPT_NAME_EXISTS, name, realm);
        }
    }

    private void failIfUuidExists(String uuid) throws ScriptException {
        if (containsUuid(uuid)) {
            throw createAndLogError(logger, SCRIPT_UUID_EXISTS, uuid, realm);
        }
    }

    private void failIfUuidDoesNotExist(String uuid) throws ScriptException {
        if (!containsUuid(uuid)) {
            throw createAndLogError(logger, SCRIPT_UUID_NOT_FOUND, uuid, realm);
        }
    }

    private ScriptConfiguration setMetaData(ScriptConfiguration config, Subject subject) throws ScriptException {
        final long now = newDate().getTime();
        final String principalName = SubjectUtils.getPrincipalId(subject);
        final ScriptConfiguration.Builder builder = config.populatedBuilder();

        if (containsUuid(config.getId())) {
            final ScriptConfiguration oldConfig = get(config.getId());
            builder.setCreatedBy(oldConfig.getCreatedBy());
            builder.setCreationDate(oldConfig.getCreationDate());
        } else {
            builder.setCreatedBy(principalName);
            builder.setCreationDate(now);
        }
        builder.setLastModifiedBy(principalName);
        builder.setLastModifiedDate(now);

        return builder.build();
    }

    private void save(ScriptConfiguration config) throws ScriptException {
        lock.readLock().unlock();
        lock.writeLock().lock();
        try {
            createCollectionConfig();
            final Map<String, Set<String>> data = getScriptConfigurationData(config);
            if (containsGlobalUuid(config.getId())) {
                getSubGlobalConfig().getSubConfig(config.getId()).setAttributes(data);
                globalConfigurations.put(config.getId(), config);
            } else {
                if (containsOrgUuid(config.getId())) {
                    getSubOrgConfig().getSubConfig(config.getId()).setAttributes(data);
                } else {
                    getSubOrgConfig().addSubConfig(config.getId(), SCRIPT_CONFIGURATION, 0, data);
                }
                realmConfigurations.put(config.getId(), config);
            }
        } catch (SSOException | SMSException e) {
            if(IUMSConstants.SMS_INSUFFICIENT_ACCESS_RIGHTS.equals(e.getErrorCode())) {
                throw createAndLogError(logger, INSUFFICIENT_PRIVILEGES, e, config.getName());
            }
            throw createAndLogError(logger, SAVE_FAILED, e, config.getId(), realm);
        } finally {
            lock.readLock().lock();
            lock.writeLock().unlock();
        }
    }

    private boolean containsUuid(String uuid) throws ScriptException {
        return containsOrgUuid(uuid) || containsGlobalUuid(uuid);
    }

    private boolean containsOrgUuid(String uuid) throws ScriptException {
        return realmConfigurations.containsKey(uuid);
    }

    private boolean containsGlobalUuid(String uuid) throws ScriptException {
        return globalConfigurations.containsKey(uuid);
    }

    private boolean containsName(String name) throws ScriptException {
        for (Map<String, ScriptConfiguration> configurations : asList(globalConfigurations, realmConfigurations)) {
            for (ScriptConfiguration sc : configurations.values()) {
                if (sc.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
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
        final ServiceConfig orgConfig = scm.getOrganizationConfig(realm, null);
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
        ServiceConfig globalConfig = scm.getGlobalConfig(null);
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
    private ServiceConfig getSubOrgConfig() throws SMSException, SSOException {
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
    private ServiceConfig getSubGlobalConfig() throws SMSException, SSOException {
        final ServiceConfig config = getGlobalConfig().getSubConfig("globalScripts");
        if (config == null) {
            throw new SMSException("Global Configuration for '" + SERVICE_NAME + "' could not be retrieved.");
        }
        return config;
    }

    private SSOToken getToken() throws SSOException {
        return coreWrapper.getAdminToken();
    }

    /**
     * Determine how many times the specified script is used.  Zero is a valid answer.
     * If the uuid is invalid, you will get an exception.
     *
     * @param script The script configuration.
     * @return the context in which the script is used (Policy, Client Side Auth, etc.) and the usage count
     * @throws ScriptException if passed an invalid UUID
     */
    private int getUsageCount(ScriptConfiguration script) throws ScriptException {

        try {
            switch (script.getContext()) {
            case AUTHENTICATION_CLIENT_SIDE:
                return clientSideAuthenticationUsageCount(script.getId());
            case AUTHENTICATION_SERVER_SIDE:
                return serverSideAuthenticationUsageCount(script.getId());
            case OIDC_CLAIMS:
                return oidcClaimsUsageCount(script.getId());
            case POLICY_CONDITION:
                return policyConditionUsageCount(script.getId());
            default:
                throw new IllegalArgumentException(MessageFormat.format("unknown script context {} in script {}",
                        script.getContext(),
                        script.getName()));
            }
        } catch (SSOException | SMSException e) {
            logger.error("getUsageCount failed with exception with script {} id {}",
                    script.getName(),
                    script.getId(),
                    e);
            return 0; // assume not used
        }
    }

    private int getUsageCount(String dn, String search) throws SSOException, SMSException {
        try {
            return SMSEntry.search(getToken(), dn, search, 0, 0, false, false).size();
        } catch (SMSException ignored) {
            // We get this exception if the LDAP entry we are searching for doesn't exist.
            // This can happen when we're looking for data under iPlanetAMAuthDeviceIdMatchService and we don't have
            // any device id match scripts.
        }
        return 0;
    }

    /**
     * Return true if the specifed script is a default script, false otherwise.
     *
     * @param scriptConfig the script config
     * @return true if a default script, false otherwise.
     */
    private boolean isDefaultScript(ScriptConfiguration scriptConfig) {
        try {
            int usageCount = getUsageCount(getScriptingServiceGlobalConfig(),
                    getDefaultScriptSearchString(scriptConfig.getId()));
            return usageCount > 0;
        } catch (SSOException | SMSException e) {
            logger.error("isDefaultScript caught exception with script {} UUID {}",
                    scriptConfig.getName(),
                    scriptConfig.getId(),
                    e);
            return false; // assume it is not a default script
        }
    }

    /**
     * Count how many times the script identified by the specified uuid is used in client side authentication.
     * @param uuid The specified uuid.
     * @return the count of how many times the script is used in client side authentication
     * @throws SMSException If the LDAP node could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private int clientSideAuthenticationUsageCount(String uuid) throws SSOException, SMSException {
        return getUsageCount(getScriptedServiceBaseDN(), getClientSideScriptedAuthSearchString(uuid))
                + getUsageCount(getDeviceIdMatchServiceBaseDN(), getClientSideScriptedAuthSearchString(uuid));
    }

    /**
     * Count how many times the script identified by the specified uuid is used in server side authentication.
     * @param uuid The specified uuid.
     * @return the count of how many times the script is used in server side authentication
     * @throws SMSException If the LDAP node could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private int serverSideAuthenticationUsageCount(String uuid) throws SSOException, SMSException {
        return getUsageCount(getScriptedServiceBaseDN(), getServerSideScriptedAuthSearchString(uuid))
                + getUsageCount(getDeviceIdMatchServiceBaseDN(), getServerSideScriptedAuthSearchString(uuid));
    }

    /**
     * Count how many times the script identified by the specified uuid is used in OIDC claims.
     * @param uuid The specified uuid.
     * @return the count of how many times the script is used in OIDC Claims
     * @throws SMSException If the LDAP node could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private int oidcClaimsUsageCount(String uuid) throws SSOException, SMSException {

        SMSEntry smsEntry = new SMSEntry(getToken(), getOAuth2ProviderBaseDN());
        Map<String, Set<String>> attributes = smsEntry.getAttributes();

        try {
            Set<String> sunKeyValues = getMapSetThrows(attributes, "sunKeyValue");
            if (sunKeyValues.contains("forgerock-oauth2-provider-oidc-claims-extension-script=" + uuid)) {
                return 1;
            }
        } catch (ValueNotFoundException ignored) {
        }

        return 0;
    }

    /**
     * Count how many times the script identified by the specified uuid is used in policy evaluation.  This is done
     * via an LDAP search for a policy which has the script's UUID encoded in the "serializable" field.
     *
     * @param uuid The specified uuid.
     * @return The count of the number of times the script is used in policy evaluation
     * @throws SMSException If the LDAP node could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    private int policyConditionUsageCount(String uuid) throws SSOException, SMSException {
        return getUsageCount(getPolicyBaseDN(), getPolicySearchString(uuid));
    }

    private String getScriptingServiceGlobalConfig() {
        return "ou=default,ou=GlobalConfig,ou=1.0,ou=ScriptingService,ou=services,"
                + DNMapper.orgNameToDN(realm);
    }

    private String getOAuth2ProviderBaseDN() {
        return "ou=default,ou=OrganizationConfig,ou=1.0,ou=OAuth2Provider,ou=services,"
                + DNMapper.orgNameToDN(realm);
    }

    private String getScriptedServiceBaseDN() {
        return "ou=default,ou=OrganizationConfig,ou=1.0,ou=iPlanetAMAuthScriptedService,ou=services,"
                + DNMapper.orgNameToDN(realm);
    }

    private String getDeviceIdMatchServiceBaseDN() {
        return "ou=default,ou=OrganizationConfig,ou=1.0,ou=iPlanetAMAuthDeviceIdMatchService,ou=services,"
                + DNMapper.orgNameToDN(realm);
    }

    private String getPolicyBaseDN() {
        return "ou=default,ou=default,ou=OrganizationConfig,ou=1.0,ou=sunEntitlementIndexes,ou=services,"
                + DNMapper.orgNameToDN(realm);
    }

    private String getDefaultScriptSearchString(String uuid) {
        return "(&(sunserviceID=scriptContext)(sunKeyValue=defaultScript=" + uuid + "))";
    }

    private String getClientSideScriptedAuthSearchString(String uuid) {
        return "(&(sunserviceID=serverconfig)(sunKeyValue=iplanet-am-auth-scripted-client-script=" + uuid + "))";
    }

    private String getServerSideScriptedAuthSearchString(String uuid) {
        return "(&(sunserviceID=serverconfig)(sunKeyValue=iplanet-am-auth-scripted-server-script=" + uuid + "))";
    }

    private String getPolicySearchString(String uuid) {
        return "(&(sunserviceID=indexes)(sunKeyValue=serializable*" + uuid + "*))";
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // ignore.
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
            int type) {
        if (serviceName.equals(SERVICE_NAME)) {
            reload();
        }
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        if (serviceName.equals(SERVICE_NAME)) {
            reload();
        }
    }
}