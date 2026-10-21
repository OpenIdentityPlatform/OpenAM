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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest;

import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.util.Reject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User attribute information.
 *
 * @since 13.0.0
 */
final class UserAttributeInfo {

    private final Set<String> protectedUpdateAttributes;
    private final Set<String> validCreationAttributes;

    private UserAttributeInfo(UserAttributeInfoBuilder builder) {
        protectedUpdateAttributes = builder.protectedUpdateAttributes;
        validCreationAttributes = builder.validCreationAttributes;
    }

    /**
     * Gets the set of protected user attributes for update.
     *
     * @return protected update attributes
     */
    public Set<String> getProtectedUpdateAttributes() {
        return protectedUpdateAttributes;
    }

    /**
     * Gets the set of valid user attribute for creation.
     *
     * @return valid creation attributes
     */
    public Set<String> getValidCreationAttributes() {
        return validCreationAttributes;
    }

    /**
     * Builder for use of {@link UserAttributeInfo} to retrieve user attribute configuration.
     */
    @ConfigSource("selfService")
    public static final class UserAttributeInfoBuilder implements ConsoleConfigBuilder<UserAttributeInfo> {

        private final Set<String> protectedUpdateAttributes;
        private final Set<String> validCreationAttributes;

        /**
         * Constructs a new builder.
         */
        public UserAttributeInfoBuilder() {
            protectedUpdateAttributes = new HashSet<>();
            validCreationAttributes = new HashSet<>();
        }

        /**
         * Sets the protected update attributes.
         *
         * @param protectedUpdateAttributes
         *         the protected update attributes
         */
        @ConfigAttribute(value = "selfServiceProfileProtectedUserAttributes", required = false)
        public void setProtectedUpdateAttributes(Set<String> protectedUpdateAttributes) {
            this.protectedUpdateAttributes.addAll(protectedUpdateAttributes);
        }

        /**
         * Sets the valid creation attributes.
         *
         * @param validCreationAttributes
         *         valid creation attributes
         */
        // Optional, like the setting above it and like the schema says (<IsOptional/>).  What this does NOT do is
        // give the enforcement code an empty whitelist to refuse: ConsoleConfigHandlerImpl throws "Expected
        // attribute" for a key that is absent before it ever consults required(), and selfService.xml declares
        // seven DefaultValues for this key, which AttributeValidator.inheritDefaults puts back - so neither the
        // absent nor the present-but-empty shape occurs through SMS.  It is here to match the schema, not to
        // reach a behaviour; the empty whitelist is refused by IdentityRestUtils.enforceWhiteList for callers that
        // supply the set directly.
        @ConfigAttribute(value = "selfServiceUserRegistrationValidUserAttributes", required = false)
        public void setValidCreationAttributes(Set<String> validCreationAttributes) {
            this.validCreationAttributes.addAll(validCreationAttributes);
        }

        @Override
        public UserAttributeInfo build(Map<String, Set<String>> attributes) {
            Reject.ifNull(protectedUpdateAttributes, "Protected user attributes are required");
            Reject.ifNull(validCreationAttributes, "Valid user creation attributes are required");
            return new UserAttributeInfo(this);
        }

    }

}
