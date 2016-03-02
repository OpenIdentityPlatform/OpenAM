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

    private String apiKey;
    private String senderId;
    private String endpoint;
    private int port;

    /**
     * Only access is via the Builder.
     */
    private PushNotificationServiceConfig() {
        //This section intentionally left blank
    }

    private void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    private void setPort(int port) {
        this.port = port;
    }

    private void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    private boolean isValid() {
        return StringUtils.isNotBlank(apiKey)
                && StringUtils.isNotBlank(endpoint)
                && StringUtils.isNotBlank(senderId) && port > 0;
    }

    /**
     * Get the api key to allow access to the remote service.
     * @return the api key.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Get the sender ID to authenticate to the remote service.
     * @return the sender id.
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Get the endpoint for this notification service to connect to.
     * @return the endpoint.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Get the port for this notification service to connect to.
     * @return the port.
     */
    public int getPort() {
        return port;
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
        return Objects.equals(this.endpoint, that.endpoint)
                && Objects.equals(this.senderId, that.senderId)
                && Objects.equals(this.apiKey, that.apiKey)
                && Objects.equals(this.port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, senderId, endpoint, port);
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
         * Sets the senderId of the GCM configuration.
         * @param senderId The senderId for GCM.
         * @return The builder.
         */
        public Builder withSenderId(String senderId) {
            config.setSenderId(senderId);
            return this;
        }

        /**
         * Sets the api key for contacting GCM.
         * @param apiKey The API key for GCM.
         * @return The builder.
         */
        public Builder withApiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }

        /**
         * The address of the remote service.
         * @param endpoint The address of the remote service.
         * @return The builder.
         */
        public Builder withEndpoint(String endpoint) {
            config.setEndpoint(endpoint);
            return this;
        }

        /**
         * The port on the remote service to connect to.
         * @param port The port on the remote service to connect to.
         * @return The builder.
         */
        public Builder withPort(int port) {
            config.setPort(port);
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
