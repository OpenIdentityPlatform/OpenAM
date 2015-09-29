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

import static org.forgerock.openam.audit.AuditConstants.EventHandlerType;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import java.util.Set;

/**
 * Wrapper for adding a name and event topics to an event handler configuration.
 *
 * @since 13.0.0
 */
public final class AuditEventHandlerConfigurationWrapper {

    private final EventHandlerConfiguration handlerConfiguration;
    private final EventHandlerType eventHandlerType;
    private final String name;
    private final Set<String> eventTopics;

    /**
     * Create an instance of {@code AuditEventHandlerConfigurationWrapper}.
     *
     * @param handlerConfiguration The handler configuration to which these properties belong.
     * @param eventHandlerType The type of handler configuration being wrapped.
     * @param name The name of the handler.
     * @param eventTopics The event topics associated with the handler.
     */
    public AuditEventHandlerConfigurationWrapper(EventHandlerConfiguration handlerConfiguration,
            EventHandlerType eventHandlerType, String name, Set<String> eventTopics) {

        this.handlerConfiguration = handlerConfiguration;
        this.eventHandlerType = eventHandlerType;
        this.name = name;
        this.eventTopics = eventTopics;
    }

    /**
     * Get the event handler.
     *
     * @return the event handler.
     */
    public EventHandlerConfiguration getConfiguration() {
        return handlerConfiguration;
    }

    /**
     * Get the event handler type.
     *
     * @return the event handler type.
     */
    public EventHandlerType getType() {
        return eventHandlerType;
    }

    /**
     * Get the event handler name.
     *
     * @return the name of the event handler.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the event handler topics.
     *
     * @return the event topics.
     */
    public Set<String> getEventTopics() {
        return eventTopics;
    }
}
