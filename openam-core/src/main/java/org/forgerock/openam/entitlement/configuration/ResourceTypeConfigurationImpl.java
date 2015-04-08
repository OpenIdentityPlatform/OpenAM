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

import static com.sun.identity.entitlement.EntitlementException.MODIFY_RESOURCE_TYPE_FAIL;
import static com.sun.identity.entitlement.EntitlementException.REMOVE_RESOURCE_TYPE_FAIL;
import static com.sun.identity.entitlement.EntitlementException.RESOURCE_TYPE_RETRIEVAL_ERROR;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.LogLevel.ERROR;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.LogLevel.MESSAGE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_ATTEMPT_REMOVE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_ATTEMPT_SAVE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_FAILED_REMOVE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_FAILED_SAVE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_SUCCEEDED_REMOVE;
import static com.sun.identity.entitlement.opensso.OpenSSOLogger.Message.RESOURCE_TYPE_SUCCEEDED_SAVE;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_ACTIONS;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_CREATED_BY;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_CREATION_DATE;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_DESCRIPTION;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_LAST_MODIFIED_BY;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_LAST_MODIFIED_DATE;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_NAME;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_PATTERNS;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.CONFIG_RESOURCE_TYPES;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.REALM_DN_TEMPLATE;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.RESOURCE_TYPE;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.SCHEMA_RESOURCE_TYPES;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.getActionSet;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.getActions;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.getAttribute;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.resourceTypeFromMap;
import static org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.OpenSSOLogger;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import javax.security.auth.Subject;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


/**
 * The implementation for <code>ResourceTypeConfiguration</code> that is responsible for the persistence
 * of the resource type entitlement configuration.
 *
 * @since 13.0.0
 */
public class ResourceTypeConfigurationImpl extends AbstractConfiguration implements ResourceTypeConfiguration {

    private static final String REFERENCE_FILTER =
            "(|(sunxmlKeyValue=resourceTypeUuid={0})(sunxmlKeyValue=resourceTypeUuids={0}))";

    private final DNWrapper dnHelper;

    @Inject
    public ResourceTypeConfigurationImpl(DNWrapper dnHelper) {
        this.dnHelper = dnHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType getResourceType(Subject subject, String realm, String uuid) throws EntitlementException {
        if (!containsUUID(subject, realm, uuid)) {
            return null;
        }
        try {
            return resourceTypeFromMap(realm, uuid, getOrgConfig(subject, realm).getSubConfig(CONFIG_RESOURCE_TYPES)
                    .getSubConfig(uuid).getAttributesForRead());
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceType", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceType", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        }
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

            if (isResourceTypeUsed(subject, realm, uuid)) {
                throw new EntitlementException(EntitlementException.RESOURCE_TYPE_REFERENCED, uuid);
            }

            getSubOrgConfig(subject, realm, CONFIG_RESOURCE_TYPES).removeSubConfig(uuid);

            OpenSSOLogger.log(MESSAGE, Level.INFO, RESOURCE_TYPE_SUCCEEDED_REMOVE, logParams, subject);
        } catch (SMSException e) {
            handleRemoveException(subject, realm, uuid, e);
        } catch (SSOException e) {
            handleRemoveException(subject, realm, uuid, e);
        }
    }

    /**
     * Looks in the realm for applications and policies that may reference the resource type.
     *
     * @param uuid
     *         the resource type uuid
     *
     * @return whether the resource type is referenced in the policy model for the realm
     *
     * @throws EntitlementException
     *         should an error occur looking up resource type references
     */
    private boolean isResourceTypeUsed(Subject subject, String realm, String uuid) throws EntitlementException {
        SSOToken token = SubjectUtils.getSSOToken(subject);

        try {
            String filter = MessageFormat.format(REFERENCE_FILTER, uuid);
            @SuppressWarnings("unchecked") Set<String> dnEntries =
                    SMSEntry.search(token, dnHelper.orgNameToDN(realm), filter, 0, 0, false, false);

            for (String dnEntry : dnEntries) {

                if (dnEntry.contains(EntitlementUtils.INDEXES_NAME)) {
                    // A DN containing the entitlement index service indicates reference by a policy.
                    return true;
                }

                if (dnEntry.contains(EntitlementUtils.SERVICE_NAME)) {
                    // A DN containing the general entitlement service indicates reference by an application.
                    return true;
                }
            }

            return false;

        } catch (SMSException smsE) {
            throw new EntitlementException(EntitlementException.INTERNAL_ERROR, smsE);
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

    @Override
    public Set<ResourceType> getResourceTypes(final QueryFilter<SmsAttribute> queryFilter,
                                              final Subject subject, final String realm) throws EntitlementException {

        final SSOToken token = SubjectUtils.getSSOToken(subject);
        final String dn = getResourceTypeBaseDN(realm);
        final Filter filter = queryFilter.accept(new SmsQueryFilterVisitor(), null);
        final Set<ResourceType> resourceTypes = new HashSet<ResourceType>();

        try {
            @SuppressWarnings("unchecked") // Interaction with legacy service.
            final Iterator<SMSDataEntry> iterator = (Iterator<SMSDataEntry>)SMSEntry
                    .search(token, dn, filter.toString(), 0, 0, false, false, Collections.emptySet());

            while (iterator.hasNext()) {
                final SMSDataEntry entry = iterator.next();
                final String name = entry.getAttributeValue(CONFIG_NAME);
                // Extract the resource types UUID from the LDAP DN representation.
                final String uuid = LDAPUtils.getName(DN.valueOf(entry.getDN()));

                @SuppressWarnings("unchecked") // Interaction with legacy service.
                final Set<String> actionSet = entry.getAttributeValues(CONFIG_ACTIONS);
                final Map<String, Boolean> actions = getActions(actionSet);

                @SuppressWarnings("unchecked") // Interaction with legacy service.
                final Set<String> resources = entry.getAttributeValues(CONFIG_PATTERNS);

                final String description = entry.getAttributeValue(CONFIG_DESCRIPTION);
                final String createdBy = entry.getAttributeValue(CONFIG_CREATED_BY);
                final String creationDate = entry.getAttributeValue(CONFIG_CREATION_DATE);
                final String modifiedBy = entry.getAttributeValue(CONFIG_LAST_MODIFIED_BY);
                final String modifiedDate = entry.getAttributeValue(CONFIG_LAST_MODIFIED_DATE);

                final ResourceType resourceType = ResourceType
                        .builder(name, realm)
                        .setUUID(uuid)
                        .setActions(actions)
                        .setPatterns(resources)
                        .setDescription(description)
                        .setCreatedBy(createdBy)
                        .setCreationDate(Long.parseLong(creationDate))
                        .setLastModifiedBy(modifiedBy)
                        .setLastModifiedDate(Long.parseLong(modifiedDate))
                        .build();

                resourceTypes.add(resourceType);
            }
        } catch (SMSException smsE) {
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, realm, smsE);
        }

        return resourceTypes;
    }

    @Override
    public Map<String, Map<String, Set<String>>> getResourceTypesData(Subject subject, String realm)
            throws EntitlementException {

        final Map<String, Map<String, Set<String>>> configData = new HashMap<String, Map<String, Set<String>>>();
        try {
            final ServiceConfig subOrgConfig = getOrgConfig(subject, realm).getSubConfig(CONFIG_RESOURCE_TYPES);
            if (subOrgConfig == null) {
                return configData;
            }
            final Set<String> uuids = subOrgConfig.getSubConfigNames();
            for (String uuid : uuids) {
                configData.put(uuid, subOrgConfig.getSubConfig(uuid).getAttributesForRead());
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceTypesData", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("ResourceTypeConfiguration.getResourceTypesData", ex);
            throw new EntitlementException(RESOURCE_TYPE_RETRIEVAL_ERROR, ex, realm);
        }
        return configData;
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

        Set<String> nonSearchableData = new HashSet<String>();
        map.put(SMSEntry.ATTR_KEYVAL, nonSearchableData);

        if (resourceType.getDescription() != null) {
            nonSearchableData.add(CONFIG_DESCRIPTION + "=" + resourceType.getDescription());
        } else {
            nonSearchableData.add(CONFIG_DESCRIPTION + "=");
        }

        nonSearchableData.add(CONFIG_CREATION_DATE + "=" + resourceType.getCreationDate());

        if (resourceType.getLastModifiedBy() != null) {
            nonSearchableData.add(CONFIG_LAST_MODIFIED_BY + "=" + resourceType.getLastModifiedBy());
        } else {
            nonSearchableData.add(CONFIG_LAST_MODIFIED_BY + "=");
        }

        nonSearchableData.add(CONFIG_LAST_MODIFIED_DATE + "=" + resourceType.getLastModifiedDate());

        Set<String> searchableData = new HashSet<String>();
        map.put(SMSEntry.ATTR_XML_KEYVAL, searchableData);

        searchableData.add(CONFIG_NAME + "=" + resourceType.getName());

        for (String pattern : resourceType.getPatterns()) {
            searchableData.add(CONFIG_PATTERNS + "=" + pattern);
        }

        for (String actionPair : getActionSet(resourceType.getActions())) {
            searchableData.add(CONFIG_ACTIONS + "=" + actionPair);
        }

        if (resourceType.getCreatedBy() != null) {
            searchableData.add(CONFIG_CREATED_BY + "=" + resourceType.getCreatedBy());
        } else {
            searchableData.add(CONFIG_CREATED_BY + "=");
        }

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
