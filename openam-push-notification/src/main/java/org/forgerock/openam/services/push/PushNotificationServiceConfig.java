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
*/
package org.forgerock.openam.services.push;

import java.util.Objects;
import org.forgerock.openam.utils.StringUtils;

/**
 * Config class for a Push Notification Service.
 */
public final class PushNotificationServiceConfig {

    private String accessKey;
    private String appleEndpoint;
    private String googleEndpoint;
    private String secret;
    private String delegateFactory;

    /**
     * Only access is via the Builder.
     */
    private PushNotificationServiceConfig() {
        //This section intentionally left blank
    }

    private void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    private void setAppleEndpoint(String appleEndpoint) {
        this.appleEndpoint = appleEndpoint;
    }

    private void setGoogleEndpoint(String googleEndpoint) {
        this.googleEndpoint = googleEndpoint;
    }

    private void setSecret(String secret) {
        this.secret = secret;
    }

    private void setDelegateFactory(String delegateFactory) {
        this.delegateFactory = delegateFactory;
    }

    private boolean isValid() {
        return StringUtils.isNotBlank(accessKey)
                && StringUtils.isNotBlank(appleEndpoint)
                && StringUtils.isNotBlank(googleEndpoint)
                && StringUtils.isNotBlank(secret)
                && StringUtils.isNotBlank(delegateFactory);
    }

    /**
     * Get the api key to allow access to the remote service.
     * @return the api key.
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Get the sender ID to authenticate to the remote service.
     * @return the sender id.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Get the endpoint for this notification service to connect to.
     * @return the endpoint.
     */
    public String getAppleEndpoint() {
        return appleEndpoint;
    }

    /**
     * Get the endpoint for this notification service to connect to.
     * @return the endpoint.
     */
    public String getGoogleEndpoint() {
        return googleEndpoint;
    }

    /**
     * Get the delegate factory class used to produce delegates.
     * @return the delegate factory.
     */
    public String getDelegateFactory() {
        return delegateFactory;
    }

    @Override
    public boolean equals(Object underTest) {
        if (underTest == null) {
            return false;
        }

        if (this == underTest) {
            return true;
        }

        if (getClass() != underTest.getClass()) {
            return false;
        }

        final PushNotificationServiceConfig that = (PushNotificationServiceConfig) underTest;
        return Objects.equals(this.appleEndpoint, that.appleEndpoint)
                && Objects.equals(this.accessKey, that.accessKey)
                && Objects.equals(this.secret, that.secret)
                && Objects.equals(this.googleEndpoint, that.googleEndpoint)
                && Objects.equals(this.delegateFactory, that.delegateFactory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKey, secret, appleEndpoint, googleEndpoint);
    }

    /**
     * Internal builder for the config to ease creation.
     */
    static class Builder {

        private PushNotificationServiceConfig config;

        /**
         * Generate a new Builder instance to use to construct
         * a PushNotificationServiceConfig.
         */
        public Builder() {
            config = new PushNotificationServiceConfig();
        }

        /**
         * Sets the accessKey of the SNS configuration.
         * @param accessKey The accessKey for SNS.
         * @return The builder.
         */
        public Builder withAccessKey(String accessKey) {
            config.setAccessKey(accessKey);
            return this;
        }

        /**
         * Sets the secret for contacting to SNS.
         * @param secret The API key for SNS.
         * @return The builder.
         */
        public Builder withSecret(String secret) {
            config.setSecret(secret);
            return this;
        }

        /**
         * The address of the remote service for Apple devices.
         * @param appleEndpoint The address of the remote service.
         * @return The builder.
         */
        public Builder withAppleEndpoint(String appleEndpoint) {
            config.setAppleEndpoint(appleEndpoint);
            return this;
        }

        /**
         * The address of the remote service for Google devices.
         * @param googleEndpoint The address of the remote service.
         * @return The builder.
         */
        public Builder withGoogleEndpoint(String googleEndpoint) {
            config.setGoogleEndpoint(googleEndpoint);
            return this;
        }

        /**
         * The address of the remote service for Google devices.
         * @param delegateFactory The class used to produce delegates.
         * @return The builder.
         */
        public Builder withDelegateFactory(String delegateFactory) {
            config.setDelegateFactory(delegateFactory);
            return this;
        }

        /**
         * Returns the constructed config, having checked that it is usable.
         * @return a constructed PushNotificationServiceConfig.
         */
        public PushNotificationServiceConfig build() throws PushNotificationException {
            if (!config.isValid()) {
                throw new PushNotificationException("Attempted to construct a "
                        + "PushNotificationServiceConfig in an invalid state.");
            }
            return config;
        }

    }

}
