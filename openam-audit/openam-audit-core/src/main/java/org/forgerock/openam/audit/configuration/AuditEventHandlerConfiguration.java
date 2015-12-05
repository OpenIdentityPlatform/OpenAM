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
package org.forgerock.openam.audit.configuration;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.util.Reject;
import org.forgerock.audit.events.handlers.AuditEventHandler;

import java.util.Map;
import java.util.Set;

/**
 * Configuration properties to use when creating an instance of {@link AuditEventHandler} with an
 * {@link AuditEventHandlerFactory}.
 *
 * @since 13.0.0
 */
public final class AuditEventHandlerConfiguration {

    private final String handlerName;
    private final Map<String, Set<String>> configAttributes;
    private final EventTopicsMetaData eventTopicsMetaData;

    private AuditEventHandlerConfiguration(Builder builder) {
        this.handlerName = builder.handlerName;
        this.configAttributes = builder.configAttributes;
        this.eventTopicsMetaData = builder.eventTopicsMetaData;
    }

    /**
     * Get the name provided for this handler during configuration.
     *
     * @return The event handler name.
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * The configuration attributes retrieved from the SMS. It contains the attribute values keyed with the attribute
     * name, e.g. {@code Map<"topics", Set<"access","activity","authentication","config">>}.
     *
     * @return The configuration attributes.
     */
    public Map<String, Set<String>> getAttributes() {
        return configAttributes;
    }

    /**
     * Provides meta-data describing the audit event topics the {@link AuditEventHandler} may have to handle.
     *
     * @return The event topics meta-data.
     */
    public EventTopicsMetaData getEventTopicsMetaData() {
        return eventTopicsMetaData;
    }

    /**
     * Retrieve a builder for {@link AuditEventHandlerConfiguration}.
     *
     * @return A builder for {@link AuditEventHandlerConfiguration}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link AuditEventHandlerConfiguration}.
     */
    public static final class Builder {
        private String handlerName;
        private Map<String, Set<String>> configAttributes;
        private EventTopicsMetaData eventTopicsMetaData;

        /**
         * Add the handler name.
         *
         * @param name Name of the handler.
         * @return Instance of the builder.
         */
        public Builder withName(String name) {
            this.handlerName = name;
            return this;
        }

        /**
         * Add the handler attributes.
         *
         * @param configAttributes The handler attributes.
         * @return Instance of the builder.
         */
        public Builder withAttributes(Map<String, Set<String>> configAttributes) {
            this.configAttributes = configAttributes;
            return this;
        }

        /**
         * Add the event topics meta data.
         *
         * @param eventTopicsMetaData The event topics meta data.
         * @return Instance of the builder.
         */
        public Builder withEventTopicsMetaData(EventTopicsMetaData eventTopicsMetaData) {
            this.eventTopicsMetaData = eventTopicsMetaData;
            return this;
        }

        /**
         * Create an instance of {@link AuditEventHandlerConfiguration}.
         *
         * @return Instance of {@link AuditEventHandlerConfiguration}.
         */
        public AuditEventHandlerConfiguration build() {
            Reject.ifNull(handlerName);
            Reject.ifNull(configAttributes);
            Reject.ifNull(eventTopicsMetaData);

            return new AuditEventHandlerConfiguration(this);
        }
    }
}
