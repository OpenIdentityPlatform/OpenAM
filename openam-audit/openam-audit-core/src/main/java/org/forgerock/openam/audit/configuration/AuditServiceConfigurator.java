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

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.json.resource.ResourceException;

/**
 * Implementations of this interface are responsible for configuring the audit service.
 *
 * @since 13.0.0
 */
public interface AuditServiceConfigurator {

    /**
     * Register the required event handlers on the given audit service.
     *
     * @param auditService The audit service to which the event handlers should be registered.
     * @throws ResourceException if there is a problem with the configuration
     * @throws AuditException if there is a problem with the registration
     */
    void registerEventHandlers(AuditService auditService) throws ResourceException, AuditException;

    /**
     * Create an instance of and populate {@link org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration}
     * from the given Json config and register the the service config listener.
     */
    void initializeAuditServiceConfiguration();

    /**
     * Get the pre-configured audit service configuration.
     * @return The pre-configured audit service configuration.
     */
    AMAuditServiceConfiguration getAuditServiceConfiguration();
}
