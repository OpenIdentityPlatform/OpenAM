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
import org.forgerock.audit.events.handlers.AuditEventHandler;

import java.util.Set;

/**
 * Implementations of this interface are responsible for supplying the configuring for the audit service.
 *
 * @since 13.0.0
 */
public interface AuditServiceConfigurationProvider {

    /**
     * This will be called once system startup is complete. It should register a listener for SMS configuration changes
     * and notify any audit configuration listeners that the configuration has changed.
     */
    void setupComplete();

    /**
     * Add a listener that will be notified of any changes in the SMS audit service configuration.
     *
     * @param listener The listener to be notified.
     */
    void addConfigurationListener(AuditServiceConfigurationListener listener);

    /**
     * Remove the listener from the notification list.
     *
     * @param listener The listener to remove.
     */
    void removeConfigurationListener(AuditServiceConfigurationListener listener);

    /**
     * Get the default audit service configuration.
     *
     * @return The default audit service configuration.
     */
    AMAuditServiceConfiguration getDefaultConfiguration();

    /**
     * Get the audit service configuration for the specified realm. If no configuration exists for this realm, the
     * default configuration will be returned.
     *
     * @param realm The realm for which the configuration is required.
     * @return The audit service configuration.
     */
    AMAuditServiceConfiguration getRealmConfiguration(String realm);

    /**
     * Get the default audit event handler configuration.
     *
     * @return The default audit event handler configurations.
     */
    Set<AuditEventHandlerConfiguration> getDefaultEventHandlerConfigurations();

    /**
     * Get the audit event handler configuration for the specified realm. If no configuration exists for this realm, the
     * default configuration will be returned.
     *
     * @param realm The realm for which the configuration is required.
     * @return The audit event handler configurations.
     */
    Set<AuditEventHandlerConfiguration> getRealmEventHandlerConfigurations(String realm);

    /**
     * Provides meta-data describing the audit event topics the {@link AuditEventHandler} may have to handle.
     *
     * @return The event topics meta-data.
     */
    EventTopicsMetaData getEventTopicsMetaData();
}
