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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.xacml.v3;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;

import org.apache.commons.lang3.RandomStringUtils;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.util.query.QueryFilter;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceMatch;
import com.sun.identity.entitlement.interfaces.ResourceName;

/**
 * Helper class to map from ResourceType to XACML.
 *
 * @since 13.5.0
 */
public final class XACMLResourceTypeUtils {

    private static final String ID_PREFIX_RESOURCE_TYPE_UNAVAILABLE = "__^^__";

    private XACMLResourceTypeUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Merges patterns and actions from the two Resource Types. Key attributes such as name and uuid are picked from the
     * destination resource type.
     *
     * @param srcType
     *         from which the attributes are picked.
     * @param destType
     *         from which attributes including uuids are picked.
     *
     * @return a new ResourceType instance.
     */
    public static ResourceType mergeResourceType(ResourceType srcType, ResourceType destType) {
        Set<String> patterns = new HashSet<>();
        patterns.addAll(destType.getPatterns());
        patterns.addAll(srcType.getPatterns());

        Map<String, Boolean> actions = new HashMap<>();
        actions.putAll(destType.getActions());
        actions.putAll(srcType.getActions());

        return createResourceType(null, destType.getName(), patterns, actions, destType.getUUID());
    }

    /**
     * Creates and instance of ResourceType from the parameters supplied.
     *
     * @param applicationName
     *         name of the application which uses this resource type. This is used only for generating a name for the
     *         resource type when the next parameter 'resourceTypeName' is not supplied.
     * @param resourceTypeName
     *         name of the resource type
     * @param patterns
     *         patterns of the resource type
     * @param actions
     *         actions of the resource type
     * @param uuid
     *         unique id of the resource type; when not supplied a unique id will be generated.
     *
     * @return a new instance of ResourceType
     */
    public static ResourceType createResourceType(String applicationName, String resourceTypeName,
            Set<String> patterns, Map<String, Boolean> actions, String uuid) {
        return createResourceTypeBuilder(applicationName, resourceTypeName, patterns, actions, uuid).build();
    }

    /**
     * Creates a ResourceType Builder from the parameters supplied.
     *
     * @param applicationName
     *         name of the application which uses this resource type. This is used only for generating a name for the
     *         resource type when the next parameter 'resourceTypeName' is not supplied.
     * @param resourceTypeName
     *         name of the resource type
     * @param patterns
     *         patterns of the resource type
     * @param actions
     *         actions of the resource type
     * @param uuid
     *         unique id of the resource type; when not supplied a unique id will be generated.
     *
     * @return a Builder instance.
     */
    public static ResourceType.Builder createResourceTypeBuilder(String applicationName, String resourceTypeName,
            Set<String> patterns, Map<String, Boolean> actions, String uuid) {
        if ( resourceTypeName == null) {
            resourceTypeName = generateResourceTypeName(applicationName);
        }

        ResourceType.Builder builder = ResourceType.builder()
                .setName(resourceTypeName)
                .setDescription("Generated resource type")
                .addPatterns(patterns)
                .addActions(actions);

        if (uuid == null) {
            builder.generateUUID();
        }
        else {
            builder.setUUID(uuid);
        }

        return builder;
    }

    /**
     * Generates a dummy unique Id which can be used as resource type uuid.
     *
     * @return a unique Id
     */
    public static String generateResourceTypeDummyUuid() {
        return ID_PREFIX_RESOURCE_TYPE_UNAVAILABLE + generateResourceTypeUuid();
    }

    /**
     * Gets all resource types using the resource type service instance.
     *
     * @param resourceTypeService
     *         service class for resource type.
     * @param subject
     *         admin subject.
     * @param realm
     *         realm of the resource type
     *
     * @return all resource types present in the underlying data store.
     *
     * @throws EntitlementException
     *         when any exceptional situation occurs.
     */
    public static Set<ResourceType> getAllResourceTypes(ResourceTypeService resourceTypeService,
            Subject subject, String realm) throws EntitlementException {
        QueryFilter<SmsAttribute> filter = QueryFilter.alwaysTrue();
        Set<ResourceType> resourceTypes = resourceTypeService.getResourceTypes(filter, subject, realm);
        if (resourceTypes == null) {
            resourceTypes = Collections.emptySet();
        }

        return resourceTypes;
    }

    /**
     * Match resources against the resource type.
     *
     * @param resources
     *         to be matched.
     * @param resourceType
     *         to which the resources are compared.
     * @param resourceComparator
     *         used for matching the resources.
     *
     * @return true when there is a match.
     */
    public static boolean matchResources(Set<String> resources, ResourceType resourceType, ResourceName resourceComparator) {
        for (String resource : resources) {
            if (matchResource(resource, resourceType, resourceComparator)) {
                return true;            // Match at-least one pattern
            }
        }
        return false;
    }

    private static String generateResourceTypeName(String applicationName) {
        return applicationName + "ResourceType" + RandomStringUtils.randomNumeric(4);
    }

    private static String generateResourceTypeUuid() {
        return UUID.randomUUID().toString();
    }

    private static boolean matchResource(String resource, ResourceType resourceType, ResourceName resourceComparator) {
        Set<String> patterns = resourceType.getPatterns();
        if (matchResource(resource, resourceComparator, patterns)) {
            return true;
        }
        return false;
    }

    private static boolean matchResource(String resource, ResourceName resourceComparator, Set<String> patterns) {
        for (String pattern : patterns) {
            ResourceMatch match = resourceComparator.compare(resource, pattern, true);
            if (match.equals(ResourceMatch.EXACT_MATCH) || match.equals(ResourceMatch.WILDCARD_MATCH)) {
                return true;
            }
        }
        return false;
    }

}
