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

package org.forgerock.openam.core.rest.server;

import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.util.Reject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Self service information.
 *
 * @since 13.0.0
 */
final class SelfServiceInfo {

    private final boolean userRegistrationEnabled;
    private final boolean forgottenUsernameEnabled;
    private final boolean forgottenPasswordEnabled;
    private final boolean kbaEnabled;
    private final Set<String> protectedUserAttributes;

    private SelfServiceInfo(SelfServiceInfoBuilder builder) {
        userRegistrationEnabled = builder.userRegistrationEnabled;
        forgottenUsernameEnabled = builder.forgottenUsernameEnabled;
        forgottenPasswordEnabled = builder.forgottenPasswordEnabled;
        kbaEnabled = builder.userRegistrationKbaEnabled ||
                builder.forgottenUsernameKbaEnabled ||
                builder.forgottenPasswordKbaEnabled;
        protectedUserAttributes = builder.protectedUserAttributes;
    }

    /**
     * Gets whether user registration is enabled.
     *
     * @return whether user registration is enabled
     */
    public boolean isUserRegistrationEnabled() {
        return userRegistrationEnabled;
    }

    /**
     * Gets whether forgotten username is enabled.
     *
     * @return whether forgotten username is enabled
     */
    public boolean isForgottenUsernameEnabled() {
        return forgottenUsernameEnabled;
    }

    /**
     * Gets whether forgotten password is enabled.
     *
     * @return whether forgotten password is enabled
     */
    public boolean isForgottenPasswordEnabled() {
        return forgottenPasswordEnabled;
    }

    /**
     * Gets whether KBA is enabled.
     *
     * @return whether KBA is enabled
     */
    public boolean isKbaEnabled() {
        return kbaEnabled;
    }

    /**
     * Gets the set of protected user attributes.
     *
     * @return protected user attributess
     */
    public Set<String> getProtectedUserAttributes() {
        return protectedUserAttributes;
    }

    /**
     * Builder indented for the use by {@link SelfServiceInfo} to retrieve self service configuration.
     */
    @ConfigSource("selfService")
    public static final class SelfServiceInfoBuilder implements ConsoleConfigBuilder<SelfServiceInfo> {

        private boolean userRegistrationEnabled;
        private boolean forgottenUsernameEnabled;
        private boolean forgottenPasswordEnabled;
        private boolean userRegistrationKbaEnabled;
        private boolean forgottenUsernameKbaEnabled;
        private boolean forgottenPasswordKbaEnabled;
        private final Set<String> protectedUserAttributes;

        /**
         * Constructs a new self service info builder.
         */
        public SelfServiceInfoBuilder() {
            protectedUserAttributes = new HashSet<>();
        }

        /**
         * Sets whether user registration is enabled.
         *
         * @param userRegistrationEnabled
         *         whether user registration is enabled
         */
        @ConfigAttribute("selfServiceUserRegistrationEnabled")
        public void setUserRegistrationEnabled(boolean userRegistrationEnabled) {
            this.userRegistrationEnabled = userRegistrationEnabled;
        }

        /**
         * Sets whether forgotten username is enabled.
         *
         * @param forgottenUsernameEnabled
         *         whether forgotten username is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameEnabled")
        public void setForgottenUsernameEnabled(boolean forgottenUsernameEnabled) {
            this.forgottenUsernameEnabled = forgottenUsernameEnabled;
        }

        /**
         * Sets whether forgotten password is enabled.
         *
         * @param forgottenPasswordEnabled
         *         whether forgotten password is enabled
         */
        @ConfigAttribute("selfServiceForgottenPasswordEnabled")
        public void setForgottenPasswordEnabled(boolean forgottenPasswordEnabled) {
            this.forgottenPasswordEnabled = forgottenPasswordEnabled;
        }

        /**
         * Sets whether KBA is enabled for user registration.
         *
         * @param userRegistrationKbaEnabled
         *         whether KBA is enabled for user registration
         */
        @ConfigAttribute("selfServiceUserRegistrationKbaEnabled")
        public void setUserRegistrationKbaEnabled(boolean userRegistrationKbaEnabled) {
            this.userRegistrationKbaEnabled = userRegistrationKbaEnabled;
        }

        /**
         * Sets whether KBA is enabled for forgotten username.
         *
         * @param forgottenUsernameKbaEnabled
         *         whether KBA is enabled for forgotten username
         */
        @ConfigAttribute("selfServiceForgottenUsernameKbaEnabled")
        public void setForgottenUsernameKbaEnabled(boolean forgottenUsernameKbaEnabled) {
            this.forgottenUsernameKbaEnabled = forgottenUsernameKbaEnabled;
        }

        /**
         * Sets whether KBA is enabled for forgotten password.
         *
         * @param forgottenPasswordKbaEnabled
         *         whether KBA is enabled for forgotten password
         */
        @ConfigAttribute("selfServiceForgottenPasswordKbaEnabled")
        public void setForgottenPasswordKbaEnabled(boolean forgottenPasswordKbaEnabled) {
            this.forgottenPasswordKbaEnabled = forgottenPasswordKbaEnabled;
        }

        /**
         * Sets the set protected user attributes.
         *
         * @param protectedUserAttributes
         *         protected user attributes
         */
        @ConfigAttribute(value = "selfServiceProfileProtectedUserAttributes", required = false)
        public void setProtectedUserAttributes(Set<String> protectedUserAttributes) {
            this.protectedUserAttributes.addAll(protectedUserAttributes);
        }

        @Override
        public SelfServiceInfo build(Map<String, Set<String>> attributes) {
            Reject.ifNull(protectedUserAttributes, "Protected user attributes are required");
            return new SelfServiceInfo(this);
        }

    }

}
