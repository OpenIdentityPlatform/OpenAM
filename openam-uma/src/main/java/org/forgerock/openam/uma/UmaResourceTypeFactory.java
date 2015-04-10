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

package org.forgerock.openam.uma;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.uma.UmaConstants;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * Provides access to a {@link ResourceType} instance for UMA providers on a realm.
 */
@Singleton
public class UmaResourceTypeFactory {

    private static final String RESOURCE_TYPE_NAME = "UMA Resources";
    private static final String RESOURCE_TYPE_DESCRIPTION = "Dynamically created Resource Type for UMA Resources.";
    private Map<String, String> realmResourceTypeUuids = new HashMap<String, String>();
    private final ResourceTypeService resourceTypeService;
    private final Debug logger = Debug.getInstance("UmaProvider");

    @Inject
    public UmaResourceTypeFactory(ResourceTypeService resourceTypeService) {
        this.resourceTypeService = resourceTypeService;
    }

    /**
     * Gets the string representation of the UUID for the realm's UMA ResourceType.
     * @param realm The realm.
     * @return The String identifier.
     * @throws EntitlementException If an error occurs during retrieval.
     */
    public String getResourceTypeId(String realm) throws EntitlementException {
        if (realmResourceTypeUuids.containsKey(realm)) {
            return realmResourceTypeUuids.get(realm);
        }
        return findResourceTypeId(realm);
    }

    private synchronized String findResourceTypeId(String realm) throws EntitlementException {
        if (!realmResourceTypeUuids.containsKey(realm)) {
            Subject subject = SubjectUtils.createSuperAdminSubject();
            String uuid = null;
            for (Map.Entry<String, Map<String, Set<String>>> resourceType : resourceTypeService.getResourceTypesData(subject, realm).entrySet()) {
                if (RESOURCE_TYPE_NAME.equals(resourceType.getValue().get("name").iterator().next())) {
                    if (uuid == null) {
                        uuid = resourceType.getKey();
                    } else {
                        throw new IllegalStateException("Found more than one UMA resource type for realm " + realm);
                    }
                }
            }
            if (uuid == null) {
                throw new IllegalStateException("Found no UMA resource type for realm " + realm);
            }
            realmResourceTypeUuids.put(realm, uuid);
        }
        return realmResourceTypeUuids.get(realm);
    }

    /**
     * Get the ResourceType for the given realm, creating it if it does not already exist.
     * @param realm The realm.
     * @return The uuid for the ResourceType.
     * @throws EntitlementException If an error occurred when creating/loading the ResourceType.
     */
    public String getOrCreateResourceType(String realm) throws EntitlementException {
        if (realmResourceTypeUuids.containsKey(realm)) {
            return realmResourceTypeUuids.get(realm);
        }
        createResourceType(realm);
        return findResourceTypeId(realm);
    }

    private synchronized void createResourceType(String realm) throws EntitlementException {
        if (!realmResourceTypeUuids.containsKey(realm)) {
            ResourceType resourceType = ResourceType.builder(RESOURCE_TYPE_NAME, realm)
                    .addPattern(UmaConstants.UMA_POLICY_SCHEME_PATTERN)
                    .setDescription(RESOURCE_TYPE_DESCRIPTION)
                    .setUUID(UUID.randomUUID().toString())
                    .build();
            Subject adminSubject = SubjectUtils.createSuperAdminSubject();
            resourceTypeService.saveResourceType(adminSubject, resourceType);
        }
    }

}
