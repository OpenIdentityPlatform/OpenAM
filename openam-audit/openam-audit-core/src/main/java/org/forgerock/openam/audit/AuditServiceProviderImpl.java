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

import static org.forgerock.json.JsonValue.*;

import com.google.inject.Inject;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible for creating and initialising the AuditService from JSON config.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditServiceProviderImpl implements AuditServiceProvider {

    private static Debug debug = Debug.getInstance("amAudit");

    private final AuditServiceConfigurator configurator;

    /**
     * Create an instance of AuditServiceProviderImpl.
     * @param configurator The configurator responsible for configuring the audit service.
     */
    @Inject
    public AuditServiceProviderImpl(AuditServiceConfigurator configurator) {
        this.configurator = configurator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuditService createAuditService() throws AuditException {

        JsonValue extendedEventTypes = readJsonFile("/org/forgerock/openam/audit/events-config.json");
        JsonValue customEventTypes = json(object());

        AuditService auditService = new AuditService(extendedEventTypes, customEventTypes);
        try {
            configurator.initializeAuditServiceConfiguration();
            configurator.registerEventHandlers(auditService);
            auditService.configure(configurator.getAuditServiceConfiguration());
        } catch (ResourceException|AuditException e) {
            debug.error("Unable to configure AuditService", e);
            throw new RuntimeException("Unable to configure AuditService.", e);
        }
        return auditService;
    }

    private JsonValue readJsonFile(String path) throws AuditException {
        try {
            InputStream is = AuditServiceProviderImpl.class.getResourceAsStream(path);
            String contents = IOUtils.readStream(is);
            return JsonValueBuilder.toJsonValue(contents.replaceAll("\\s", ""));
        } catch (IOException e) {
            debug.error("Unable to read configuration file {}", path, e);
            throw new AuditException("Unable to read configuration file " + path, e);
        }
    }
}
