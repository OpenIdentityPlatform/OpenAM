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
package org.forgerock.openam.entitlement.configuration;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.OpenSSOLogger;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.forgerock.openam.entitlement.ResourceType;

import javax.security.auth.Subject;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;
import static com.sun.identity.entitlement.EntitlementException.*;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.LogLevel.*;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.*;


/**
 * The implementation for <code>ResourceTypeConfiguration</code> that is responsible for the persistence
 * of the resource type entitlement configuration.
 */
public class ResourceTypeConfigurationImpl extends AbstractConfiguration implements ResourceTypeConfiguration {

    private static final String SCHEMA_RESOURCE_TYPES = "resourceTypes";
    private static final String CONFIG_RESOURCE_TYPES = "registeredResourceTypes";
    private static final String CONFIG_PATTERNS = "patterns";
    private static final String RESOURCE_TYPE = "resourceType";

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ResourceType> getResourceTypes(Subject subject, String realm) throws EntitlementException {
        final Map<String, ResourceType> resourceTypeMap = new HashMap<String, ResourceType>();
        try {
            final ServiceConfig subOrgConfig = getOrgConfig(subject, realm).getSubConfig(CONFIG_RESOURCE_TYPES);
            if (subOrgConfig == null) {
                return resourceTypeMap;
            }
            final Set<String> uuids = subOrgConfig.getSubConfigNames();

            for (String uuid : uuids) {
                final Map<String, Set<String>> data = subOrgConfig.getSubConfig(uuid).getAttributes();
                final ResourceType resourceType = resourceTypeFromMap(realm, uuid, data);
                resourceTypeMap.put(uuid, resourceType);
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceTypes", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceTypes", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        }
        return resourceTypeMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType resourceTypeFromMap(String realm, String uuid, Map<String, Set<String>> data) {
        return ResourceType.builder(getAttribute(data, CONFIG_NAME), realm)
                .setUUID(uuid)
                .setDescription(getAttribute(data, CONFIG_DESCRIPTION, EMPTY))
                .addPatterns(data.get(CONFIG_PATTERNS))
                .addActions(getActions(data))
                .setCreatedBy(getAttribute(data, CONFIG_CREATED_BY, EMPTY))
                .setCreationDate(getDateAttributeAsLong(data, CONFIG_CREATION_DATE))
                .setLastModifiedBy(getAttribute(data, CONFIG_LAST_MODIFIED_BY, EMPTY))
                .setLastModifiedDate(getDateAttributeAsLong(data, CONFIG_LAST_MODIFIED_DATE))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsUUID(Subject subject, String realm, String uuid) throws EntitlementException {
        final ServiceConfig resourceTypeConf;
        try {
            final ServiceConfig subOrgConfig = getOrgConfig(subject, realm).getSubConfig(CONFIG_RESOURCE_TYPES);
            if (subOrgConfig == null) {
                return false;
            }
            resourceTypeConf = subOrgConfig.getSubConfig(uuid);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.containsUUID", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.containsUUID", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        }
        return resourceTypeConf != null && resourceTypeConf.exists();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsName(Subject subject, String realm, String name) throws EntitlementException {
        try {
            final ServiceConfig subOrgConfig = getOrgConfig(subject, realm).getSubConfig(CONFIG_RESOURCE_TYPES);
            if (subOrgConfig == null) {
                return false;
            }
            final Set<String> configNames = subOrgConfig.getSubConfigNames();
            for (String configName : configNames) {
                if (name.equals(getAttribute(subOrgConfig.getSubConfig(configName).getAttributes(), CONFIG_NAME))) {
                    return true;
                }
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.containsName", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.containsName", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeResourceType(Subject subject, String realm, String uuid) throws EntitlementException {
        try {
            final String[] logParams = {realm, uuid};
            OpenSSOLogger.log(MESSAGE, Level.INFO, RESOURCE_TYPE_ATTEMPT_REMOVE, logParams, subject);
            getSubOrgConfig(subject, realm, CONFIG_RESOURCE_TYPES).removeSubConfig(uuid);
            OpenSSOLogger.log(MESSAGE, Level.INFO, RESOURCE_TYPE_SUCCEEDED_REMOVE, logParams, subject);
        } catch (SMSException e) {
            handleRemoveException(subject, realm, uuid, e);
        } catch (SSOException e) {
            handleRemoveException(subject, realm, uuid, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeResourceType(Subject subject, ResourceType resourceType) throws EntitlementException {
        final String realm = resourceType.getRealm();
        final String uuid = resourceType.getUUID();
        createResourceTypeCollectionConfig(subject, realm, uuid);

        final SSOToken token = SubjectUtils.getSSOToken(subject);
        try {
            final SMSEntry entry = new SMSEntry(token, getResourceTypeDN(realm, uuid));
            final String[] logParams = {realm, uuid};

            entry.setAttributes(getResourceTypeData(resourceType));
            OpenSSOLogger.log(MESSAGE, Level.INFO, RESOURCE_TYPE_ATTEMPT_SAVE, logParams, subject);
            entry.save();
            OpenSSOLogger.log(MESSAGE, Level.INFO, RESOURCE_TYPE_SUCCEEDED_SAVE, logParams, subject);
        } catch (SMSException ex) {
            handleSaveException(subject, realm, uuid, ex);
        } catch (SSOException ex) {
            handleSaveException(subject, realm, uuid, ex);
        }
    }

    /**
     * Create the config instance in the data store where new resource types can be added to.
     * @param realm The realm in which to create the config instance.
     * @param uuid The unique identifier for the resource type.
     * @throws EntitlementException
     */
    private void createResourceTypeCollectionConfig(Subject subject, String realm, String uuid)
            throws EntitlementException {

        try {
            final ServiceConfig orgConfig = getOrgConfig(subject, realm);
            if (orgConfig.getSubConfig(CONFIG_RESOURCE_TYPES) == null) {
                orgConfig.addSubConfig(CONFIG_RESOURCE_TYPES, SCHEMA_RESOURCE_TYPES, 0, Collections.EMPTY_MAP);
            }
        } catch (SMSException ex) {
            handleSaveException(subject, realm, uuid, ex);
        } catch (SSOException ex) {
            handleSaveException(subject, realm, uuid, ex);
        }
    }

    /**
     * Get the DN for the resource type.
     * @param realm The realm in which the resource type will be stored.
     * @param uuid The unique identifier for the resource type.
     * @return the resource type DN.
     */
    private String getResourceTypeDN(String realm, String uuid) {
        return "ou=" + uuid + "," + getResourceTypeBaseDN(realm);
    }

    /**
     * Get the base DN for the resource type.
     * @param realm The realm in which the resource type will be stored.
     * @return the resource type base DN.
     */
    private String getResourceTypeBaseDN(String realm) {
        return MessageFormat.format(REALM_DN_TEMPLATE, CONFIG_RESOURCE_TYPES, DNMapper.orgNameToDN(realm));
    }

    /**
     * Add all the resource type attributes to a map that can be stored in the data store.
     * @param resourceType The resource type to add to the map.
     * @return The map containing the resource type attributes.
     */
    private Map<String, Set<String>> getResourceTypeData(ResourceType resourceType) {
        final Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        prepareAttributeMap(map, RESOURCE_TYPE);

        Set<String> data = new HashSet<String>();
        map.put(SMSEntry.ATTR_KEYVAL, data);

        data.add(CONFIG_NAME + "=" + resourceType.getName());

        if (resourceType.getDescription() != null) {
            data.add(CONFIG_DESCRIPTION + "=" + resourceType.getDescription());
        } else {
            data.add(CONFIG_DESCRIPTION + "=");
        }

        for (String pattern : resourceType.getPatterns()) {
            data.add(CONFIG_PATTERNS + "=" + pattern);
        }

        for (String actionPair : getActionSet(resourceType.getActions())) {
            data.add(CONFIG_ACTIONS + "=" + actionPair);
        }

        if (resourceType.getCreatedBy() != null) {
            data.add(CONFIG_CREATED_BY + "=" + resourceType.getCreatedBy());
        } else {
            data.add(CONFIG_CREATED_BY + "=");
        }

        data.add(CONFIG_CREATION_DATE + "=" + resourceType.getCreationDate());

        if (resourceType.getLastModifiedBy() != null) {
            data.add(CONFIG_LAST_MODIFIED_BY + "=" + resourceType.getLastModifiedBy());
        } else {
            data.add(CONFIG_LAST_MODIFIED_BY + "=");
        }

        data.add(CONFIG_LAST_MODIFIED_DATE + "=" + resourceType.getLastModifiedDate());

        return map;
    }

    /**
     * Log the exception and throw the appropriate <code>EntitlementException</code>.
     * @param subject The subject for which the problem occurred.
     * @param realm The realm in which the entry was to be saved.
     * @param uuid The unique identifier for the resource type.
     *@param e The exception to be handled.  @throws EntitlementException will always be thrown.
     */
    private void handleSaveException(Subject subject, String realm, String uuid, Exception e)
            throws EntitlementException {

        OpenSSOLogger.log(ERROR, Level.INFO, RESOURCE_TYPE_FAILED_SAVE, new String[]{realm, uuid, e.getMessage()},
                subject);
        throw new EntitlementException(MODIFY_RESOURCE_TYPE_FAIL, e, uuid);
    }

    /**
     * Log the exception and throw the appropriate <code>EntitlementException</code>.
     * @param subject The subject for which the problem occurred.
     * @param realm The realm from which the entry was to be removed.
     * @param uuid The unique identifier for the resource type.
     *@param e The exception to be handled.  @throws EntitlementException will always be thrown.
     */
    private void handleRemoveException(Subject subject, String realm, String uuid, Exception e)
            throws EntitlementException {

        OpenSSOLogger.log(ERROR, Level.INFO, RESOURCE_TYPE_FAILED_REMOVE, new String[]{realm, uuid, e.getMessage()},
                subject);
        throw new EntitlementException(REMOVE_RESOURCE_TYPE_FAIL, e, uuid);
    }
}
