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

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.ResourceType;

import javax.security.auth.Subject;
import java.util.Map;

/**
 * Implementations of this interface are responsible for the persistence of the resource type entitlement configuration.
 */
public interface ResourceTypeConfiguration {

    /**
     * Retrieve a map of registered resource types, keyed on their uuid.
     * @param subject The subject for whom the resource type should be retrieved.
     * @param realm The realm in which the resource type resides.
     * @return A map of registered resource types, keyed on their uuid.
     * @throws EntitlementException If the retrieval of the resource type failed.
     */
    public Map<String, ResourceType> getResourceTypes(Subject subject, String realm) throws EntitlementException;

    /**
     * Check to see if a resource type with the given UUID already exists in this realm.
     * @param subject The subject for whom the resource type should be retrieved.
     * @param realm The realm in which the resource type resides.
     * @param uuid The unique identifier for the resource type.
     * @return True if the resource type already exists.
     */
    public boolean containsUUID(Subject subject, String realm, String uuid) throws EntitlementException;

    /**
     * Check to see if a resource type with the given name already exists in this realm.
     * @param subject The subject for whom the resource type should be retrieved.
     * @param realm The realm in which the resource type resides.
     * @param name The name of the resource type.
     * @return True if the resource type already exists.
     */
    public boolean containsName(Subject subject, String realm, String name) throws EntitlementException;

    /**
     * Remove a resource type.
     * @param subject The subject for whom the resource type should be removed.
     * @param realm The realm in which the resource type resides.
     * @param uuid The unique identifier for the resource type.
     * @throws EntitlementException if the resource type cannot be removed.
     */
    public void removeResourceType(Subject subject, String realm, String uuid) throws EntitlementException;

    /**
     * Stores the resource type to the data store.
     * @param subject The subject for whom the resource type should be stored.
     * @param resourceType The resource type to store.
     * @throws EntitlementException if the resource type cannot be stored.
     */
    public void storeResourceType(Subject subject, ResourceType resourceType) throws EntitlementException;

}
