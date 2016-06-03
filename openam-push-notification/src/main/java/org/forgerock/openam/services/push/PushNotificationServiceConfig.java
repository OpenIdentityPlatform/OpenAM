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
    private String region;
    private long messageDispatcherSize;
    private int messageDispatcherConcurrency;
    private long messageDispatcherDuration;

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

    private void setRegion(String region) {
        this.region = region;
    }

    private void setMessageDispatcherSize(long messageDispatcherSize) {
        this.messageDispatcherSize = messageDispatcherSize;
    }

    private void setMessageDispatcherConcurrency(int messageDispatcherConcurrency) {
        this.messageDispatcherConcurrency = messageDispatcherConcurrency;
    }

    private void setMessageDispatcherDuration(long messageDispatcherDuration) {
        this.messageDispatcherDuration = messageDispatcherDuration;
    }


    private boolean isValid() {
        return StringUtils.isNotBlank(accessKey)
                && StringUtils.isNotBlank(appleEndpoint)
                && StringUtils.isNotBlank(googleEndpoint)
                && StringUtils.isNotBlank(secret)
                && StringUtils.isNotBlank(delegateFactory)
                && StringUtils.isNotBlank(region)
                && messageDispatcherConcurrency >= 0
                && messageDispatcherDuration >= 0
                && messageDispatcherSize >= 0;
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

    /**
     * Get the region in which this client exists.
     * @return the region for this client.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Get the message dispatcher size from this config.
     * @return The number of entries the message dispatcher should hold.
     */
    public long getMessageDispatcherSize() {
        return messageDispatcherSize;
    }

    /**
     * Get the duration the message dispatcher should hold messages for.
     * @return The time (in seconds) message inboxes should be open for.
     */
    public long getMessageDispatcherDuration() {
        return messageDispatcherDuration;
    }

    /**
     * Gets the level of concurrency to use when accessing the message dispatcher cache.
     * @return The level of concurrency for this service's message dispatcher.
     */
    public int getMessageDispatcherConcurrency() {
        return messageDispatcherConcurrency;
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
                && Objects.equals(this.region, that.region)
                && Objects.equals(this.delegateFactory, that.delegateFactory)
                && Objects.equals(this.messageDispatcherConcurrency, that.messageDispatcherConcurrency)
                && Objects.equals(this.messageDispatcherDuration, that.messageDispatcherDuration)
                && Objects.equals(this.messageDispatcherSize, that.messageDispatcherSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKey, secret, appleEndpoint, googleEndpoint, messageDispatcherConcurrency,
                messageDispatcherDuration, messageDispatcherSize);
    }

    /**
     * Internal builder for the config to ease creation.
     */
    public static class Builder {

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
         * Sets the region of the SNS configuration.
         * @param region The region for the SNS client.
         * @return The builder.
         */
        public Builder withRegion(String region) {
            config.setRegion(region);
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
         * The size of the internal message dispatcher cache.
         * @param maxSize The size of the cache.
         * @return The builder.
         */
        public Builder withMessageDispatcherSize(long maxSize) {
            config.setMessageDispatcherSize(maxSize);
            return this;
        }

        /**
         * The level of concurrency to use for the internal message dispatcher cache.
         * @param concurrency The level of concurrency.
         * @return The builder.
         */
        public Builder withMessageDispatcherConcurrency(int concurrency) {
            config.setMessageDispatcherConcurrency(concurrency);
            return this;
        }

        /**
         * The maximum duration (in seconds) to keep items in the cache.
         * @param duration The maximum duration (in seconds) items should exist in the cache.
         * @return The builder.
         */
        public Builder withMessageDispatcherDuration(long duration) {
            config.setMessageDispatcherDuration(duration);
            return this;
        }

        /**
         * Returns the constructed config, having checked that it is usable.
         * @return a constructed PushNotificationServiceConfig.
         * @throws PushNotificationException if the config is invalid.
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
