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

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.util.query.QueryFilter;

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
     * @param realm The realm in which to save the resource type.
     * @param resourceType The resource type to save.
     * @return The modified resource type.
     * @throws EntitlementException If the resource type fails to save.
     */
    public ResourceType saveResourceType(Subject subject, String realm, ResourceType resourceType)
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
     * Retrieve the resource types stored under the specified realm from the data store.
     * @param subject The subject with privilege to access the resource types in this realm.
     * @param realm The realm from which to retrieve the resource types.
     * @return The registered resource types in a map. The outer map holds the resource type UUID as the key. The inner
     * map (value of the outer map) holds the names of the attributes of the resource type as keys and a set of values
     * for that attribute as the value.
     * @throws EntitlementException If the retrieval of the resource type failed.
     */
    public Map<String, Map<String, Set<String>>> getResourceTypesData(Subject subject, String realm)
            throws EntitlementException;

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
     * @param realm The realm in which the resource type resides.
     * @param resourceType The resource type to update.
     * @return The updated resource type.
     * @throws EntitlementException If the update of the resource type failed.
     */
    public ResourceType updateResourceType(Subject subject, String realm, ResourceType resourceType)
            throws EntitlementException;

    /**
     * Determines whether the resource type Id represents a valid and present resource type.
     *
     * @param subject
     *         the calling subject
     * @param realm
     *         the realm location for the resource type
     * @param id
     *         the resource type Id
     *
     * @return whether the resource type is valid
     *
     * @throws EntitlementException
     *         should an error occur
     */
    boolean contains(Subject subject, String realm, String id) throws EntitlementException;

    /**
     * Retrieves a set of resource types based on the passed query filter.
     *
     * @param filter
     *         the query filter
     * @param subject
     *         the calling subject
     * @param realm
     *         the realm to look within
     *
     * @return a set of matching resource types
     *
     * @throws EntitlementException
     *         should an error occur during lookup
     */
    public Set<ResourceType> getResourceTypes(QueryFilter<SmsAttribute> filter,
                                              Subject subject, String realm) throws EntitlementException;

}
