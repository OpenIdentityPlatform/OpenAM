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
package org.forgerock.openam.entitlement.service;

import static com.sun.identity.entitlement.EntitlementException.NO_SUCH_RESOURCE_TYPE;
import static com.sun.identity.entitlement.EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.forgerock.openam.entitlement.configuration.ResourceTypeSmsAttributes;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for <code>ResourceTypeService</code> that uses the <code>ResourceTypeConfiguration</code> to access
 * persisted resource type instances.
 */
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private final ResourceTypeConfiguration configuration;

    @Inject
    public ResourceTypeServiceImpl( ResourceTypeConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType saveResourceType(Subject subject, String realm, ResourceType resourceType)
            throws EntitlementException {

        final String name = resourceType.getName();
        if (configuration.containsName(subject, realm, name)) {
            throw new EntitlementException(RESOURCE_TYPE_ALREADY_EXISTS, name);
        }

        final ResourceType updatedResourceType = setMetaData(subject, realm, resourceType);
        configuration.storeResourceType(subject, realm, updatedResourceType);

        return updatedResourceType;
    }

    private String getPrincipalName(Subject subject) {
        final Set<Principal> principals = subject.getPrincipals();
        return (principals != null && !principals.isEmpty()) ? principals.iterator().next().getName() : null;
    }

    private ResourceType setMetaData(Subject subject, String realm, ResourceType resourceType)
            throws EntitlementException {

        final long now = newDate().getTime();
        final String principalName = getPrincipalName(subject);
        final ResourceType oldResourceType = getResourceType(subject, realm, resourceType.getUUID());
        final ResourceType.Builder builder = resourceType.populatedBuilder();

        if (oldResourceType == null) {
            builder.setCreatedBy(principalName);
            builder.setCreationDate(now);

        } else {
            builder.setCreatedBy(oldResourceType.getCreatedBy());
            builder.setCreationDate(oldResourceType.getCreationDate());
        }
        builder.setLastModifiedDate(now);
        builder.setLastModifiedBy(principalName);

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteResourceType(Subject subject, String realm, String uuid) throws EntitlementException {

        if (!configuration.containsUUID(subject, realm, uuid)) {
            throw new EntitlementException(NO_SUCH_RESOURCE_TYPE, uuid, realm);
        }

        configuration.removeResourceType(subject, realm, uuid);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType getResourceType(Subject subject, String realm, String uuid) throws EntitlementException {
        return configuration.getResourceType(subject, realm, uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType updateResourceType(Subject subject, String realm, ResourceType resourceType)
            throws EntitlementException {

        final String uuid = resourceType.getUUID();
        final String name = resourceType.getName();

        if (!configuration.containsUUID(subject, realm, uuid)) {
            throw new EntitlementException(NO_SUCH_RESOURCE_TYPE, uuid, realm);
        }

        final Set<ResourceType> resourceTypes = getResourceTypes(
                QueryFilter.equalTo(ResourceTypeSmsAttributes.NAME, name), subject, realm);
        for (ResourceType rt : resourceTypes) {
            if (!uuid.equals(rt.getUUID())) {
                throw new EntitlementException(RESOURCE_TYPE_ALREADY_EXISTS, name);
            }
        }

        final ResourceType updatedResourceType = setMetaData(subject, realm, resourceType);
        configuration.storeResourceType(subject, realm, updatedResourceType);

        return updatedResourceType;
    }

    @Override
    public boolean contains(Subject subject, String realm, String id) throws EntitlementException {
        return configuration.containsUUID(subject, realm, id);
    }

    @Override
    public Set<ResourceType> getResourceTypes(QueryFilter<SmsAttribute> filter,
                                              Subject subject, String realm) throws EntitlementException {
        return configuration.getResourceTypes(filter, subject, realm);
    }

    @Override
    public Map<String, Map<String, Set<String>>> getResourceTypesData(Subject subject, String realm)
            throws EntitlementException {
        return configuration.getResourceTypesData(subject, realm);
    }

}
