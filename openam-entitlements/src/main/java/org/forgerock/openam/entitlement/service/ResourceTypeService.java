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
package org.forgerock.openam.entitlement.service;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.ResourceType;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

/**
 * The <code>ResourceTypeService</code> is responsible for access to the persisted <code>ResourceType</code> instances.
 * It is the layer on top of the <code>EntitlementService</code>, which is responsible for access to all the persisted
 * policy model instances.
 */
public interface ResourceTypeService {

    /**
     * Save the ResourceType in the data store under the resource type's realm. This will also add the creation meta
     * data, if this resource type does not already exist, and the last modified meta data.
     * @param subject The subject with privilege to create resource types.
     * @param resourceType The resource type to save.
     * @return The modified resource type.
     * @throws com.sun.identity.entitlement.EntitlementException If the resource type fails to save.
     */
    public ResourceType saveResourceType(Subject subject, ResourceType resourceType) throws EntitlementException;

    /**
     * Create a ResourceType from the data map and persist in the data store under the realm specified in the map.
     * This will also add the creation meta data, if this resource type does not already exist, and the last modified meta data.
     * @param subject The subject with privilege to create resource types.
     * @param realm The realm in which to create the ResourceType.
     * @param uuid The resource type's UUID.
     * @param data Map of data from which to create the ResourceType to be saved.
     * @return The saved resource type.
     * @throws com.sun.identity.entitlement.EntitlementException If the resource type fails to save.
     */
    public ResourceType saveResourceType(Subject subject, String realm, String uuid, Map<String, Set<String>> data)
            throws EntitlementException;

    /**
     * Delete the resource type with the given UUID stored under the given realm from the data store.
     * @param subject The subject with privilege to delete resource types in this realm.
     * @param realm The realm from which to delete the resource type.
     * @param uuid The unique identifier for the resource type.
     * @throws EntitlementException If the resource type delete fails.
     */
    public void deleteResourceType(Subject subject, String realm, String uuid) throws EntitlementException;

    /**
     * Retrieve the resource types stored under the specified realm from the data store. Also indicate if the resource
     * types from the parent realms should be included.
     * @param subject The subject with privilege to access the resource types in this realm.
     * @param realm The realm from which to retrieve the resource types.
     * @return A set of registered resource types.
     * @throws EntitlementException If the retrieval of the resource type failed.
     */
    public Set<ResourceType> getResourceTypes(Subject subject, String realm) throws EntitlementException;

    /**
     * Retrieve the resource type with the given UUID, stored under the specified realm.
     * @param subject The subject with privilege to access the resource types in this realm.
     * @param realm The realm from which to retrieve the resource type.
     * @param uuid The unique identifier for the resource type.
     * @return The resource type with the given UUID or null if it cannot be found.
     * @throws EntitlementException If the retrieval of the resource type failed.
     */
    public ResourceType getResourceType(Subject subject, String realm, String uuid) throws EntitlementException;

    /**
     * Update the given resource type. If it does not exist, it will be created.
     * @param subject The subject with privilege to update the resource type.
     * @param resourceType The resource type to update.
     * @return The updated resource type.
     * @throws EntitlementException If the update of the resource type failed.
     */
    public ResourceType updateResourceType(Subject subject, ResourceType resourceType) throws EntitlementException;
}
