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
package org.forgerock.openam.entitlement.constraints;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.RegExResourceName;
import com.sun.identity.entitlement.ResourceMatch;
import com.sun.identity.entitlement.interfaces.ResourceName;
import org.forgerock.openam.entitlement.ResourceType;

import java.util.Set;

/**
 * Provides the basic implementation for constraint validation.
 *
 * @since 13.0.0
 */
public final class ConstraintValidatorImpl implements ConstraintValidator {

    @Override
    public ResourceMatchUsing verifyResources(final Set<String> resources) {
        return new ResourceMatchUsing() {

            @Override
            public AgainstResourceType using(final ResourceName resourceHandler) {
                return new AgainstResourceType() {

                    @Override
                    public ConstraintResult against(ResourceType resourceType) {
                        return verifyResources(resources, resourceHandler, resourceType);
                    }

                };
            }

        };
    }

    @Override
    public AgainstResourceType verifyActions(final Set<String> actions) {
        return new AgainstResourceType() {

            @Override
            public ConstraintResult against(ResourceType resourceType) {
                return verifyActions(actions, resourceType);
            }

        };
    }

    /**
     * Verifies the set of actions against the resource type.
     *
     * @param actions
     *         the actions
     * @param resourceType
     *         the resource type
     *
     * @return the verification result
     */
    private ConstraintResult verifyActions(Set<String> actions, ResourceType resourceType) {
        Set<String> acceptableActions = resourceType.getActions().keySet();

        for (String action : actions) {
            if (!acceptableActions.contains(action)) {
                return ConstraintResults.newFailure("actionValues", action);
            }
        }

        return ConstraintResults.newSuccess();
    }

    /**
     * Verifies the set of resources against the resource type with help from the resource handler.
     *
     * @param resources
     *         the resources
     * @param resourceHandler
     *         the resource handler
     * @param resourceType
     *         the resource type
     *
     * @return the verification result
     */
    private ConstraintResult verifyResources(Set<String> resources, ResourceName resourceHandler,
                                             ResourceType resourceType) {

        Set<String> patterns = resourceType.getPatterns();

        for (String resource : resources) {
            try {
                // It may be appropriate to assume the resources are already normalised.
                String normalisedResource = resourceHandler.canonicalize(resource);

                if (!validateResourceNames(normalisedResource, patterns, resourceHandler)) {
                    return ConstraintResults.newFailure("resources", resource);
                }
            } catch (EntitlementException eE) {
                return ConstraintResults.newFailure("resources", resource);
            }
        }

        return ConstraintResults.newSuccess();
    }

    /**
     * Verifies a resource against the set of allowed patterns with help from the resource handler.
     *
     * @param resource
     *         the resource
     * @param patterns
     *         the allowed patterns
     * @param resourceHandler
     *         the resource handler
     *
     * @return whether the resource successfully matched against one of the patterns
     */
    private boolean validateResourceNames(String resource, Set<String> patterns, ResourceName resourceHandler) {
        if (resourceHandler instanceof RegExResourceName) {
            return validateResourceNamesUsingRegex(resource, patterns, (RegExResourceName)resourceHandler);
        }

        for (String pattern : patterns) {
            // This approach of swapping the pattern and the resource during comparison has been inherited.
            // For now this has been preserved until its purpose becomes more clear.
            ResourceMatch match = resourceHandler.compare(pattern, resource, false);

            if (match == ResourceMatch.EXACT_MATCH || match == ResourceMatch.SUB_RESOURCE_MATCH) {
                return true;
            }

            match = resourceHandler.compare(resource, pattern, true);

            if (match == ResourceMatch.WILDCARD_MATCH) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifies a resource against the set of allowed regex patterns with help from the regex resource handler.
     *
     * @param resource
     *         the resource
     * @param regexPatterns
     *         the allowed regex patterns
     * @param resourceHandler
     *         the regex resource handler
     *
     * @return whether the resource successfully matched against one of the patterns
     */
    private boolean validateResourceNamesUsingRegex(String resource, Set<String> regexPatterns,
                                                    RegExResourceName resourceHandler) {
        for (String regex : regexPatterns) {
            ResourceMatch match = resourceHandler.compare(regex, resource, true);

            if (match == ResourceMatch.EXACT_MATCH
                    || match == ResourceMatch.WILDCARD_MATCH
                    || match == ResourceMatch.SUB_RESOURCE_MATCH) {
                return true;
            }
        }

        return false;
    }

}
