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
package org.forgerock.openam.audit;

import static org.forgerock.openam.audit.AuditConstants.EventHandlerType.CSV;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandlerConfiguration;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfigurationWrapper;

import javax.inject.Singleton;

/**
 * Factory for creation of audit event handlers.
 *
 * Facilitates mocking of audit event handlers.
 *
 * @Since 13.0.0
 */
@Singleton
public class AuditEventHandlerFactory {

    /**
     * Create an instance of {@link AuditEventHandler} based on the given configuration.
     *
     * @param config Configuration to apply to the audit event handler.
     * @return An instance of {@link AuditEventHandler}.
     * @throws AuditException If the event handler described by the configuration does not exist or it failed to be
     * configured.
     */
    public AuditEventHandler create(AuditEventHandlerConfigurationWrapper config) throws AuditException {
        if (CSV.equals(config.getType())) {
            getCsvEventHandler((CSVAuditEventHandlerConfiguration) config.getConfiguration());
        }

        throw new AuditException("No event handler exists for " + config.getName());
    }

    private CSVAuditEventHandler getCsvEventHandler(CSVAuditEventHandlerConfiguration config) throws AuditException {
        try {
            CSVAuditEventHandler csvHandler = new CSVAuditEventHandler();
            csvHandler.configure(config);
            return csvHandler;
        } catch (ResourceException e) {
            throw new AuditException(e);
        }
    }
}
