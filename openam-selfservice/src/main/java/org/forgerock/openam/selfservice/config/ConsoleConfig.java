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
package org.forgerock.openam.selfservice.config;

/**
 * Represents self service configuration defined via the admin console.
 *
 * @since 13.0.0
 */
public final class ConsoleConfig {

    private final CommonConfig forgottenPassword;
    private final CommonConfig userRegistration;

    /**
     * Constructs a new configuration instance.
     */
    public ConsoleConfig() {
        forgottenPassword = new CommonConfig();
        userRegistration = new CommonConfig();
    }

    /**
     * Gets the forgotten password configuration.
     *
     * @return forgotten password config
     */
    public CommonConfig getForgottenPassword() {
        return forgottenPassword;
    }

    /**
     * Gets the user registration configuration.
     *
     * @return user registration config
     */
    public CommonConfig getUserRegistration() {
        return userRegistration;
    }

    /**
     * Represents configuration common to all self services.
     */
    public final class CommonConfig {

        private boolean enabled;
        private String emailUrl;
        private long tokenExpiry;

        /**
         * Whether the service is enabled.
         *
         * @return is the service enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the service is enabled.
         *
         * @param enabled
         *         whether the service is enabled
         *
         * @return this config
         */
        public CommonConfig setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Gets the url to be used within emails.
         *
         * @return email url
         */
        public String getEmailUrl() {
            return emailUrl;
        }

        /**
         * Sets the url to be used within emails.
         *
         * @param emailUrl
         *         the email url
         *
         * @return this config
         */
        public CommonConfig setEmailUrl(String emailUrl) {
            this.emailUrl = emailUrl;
            return this;
        }

        /**
         * Gets the token expiry time in seconds.
         *
         * @return the token expiry time
         */
        public long getTokenExpiry() {
            return tokenExpiry;
        }

        /**
         * Sets the token expiry time in seconds.
         *
         * @param tokenExpiry
         *         token expiry time
         *
         * @return this config
         */
        public CommonConfig setTokenExpiry(long tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
            return this;
        }

        /**
         * Completes configuration of the common config.
         *
         * @return the containing config
         */
        public ConsoleConfig done() {
            return ConsoleConfig.this;
        }

    }

}
