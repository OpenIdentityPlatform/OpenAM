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

import static org.forgerock.json.fluent.JsonValue.*;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.events.handlers.impl.CSVAuditEventHandler;
import org.forgerock.audit.events.handlers.impl.CSVAuditEventHandlerConfiguration;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible for creating and initialising the AuditService from JSON config.
 *
 * @since 13.0.0
 */
public class AuditServiceProviderImpl implements AuditServiceProvider {

    private static Debug debug = Debug.getInstance("amAudit");

    /**
     * {@inheritDoc}
     */
    @Override
    public AuditService createAuditService() throws AuditException {

        JsonValue extendedEventTypes = readJsonFile("/org/forgerock/openam/audit/events-config.json");
        JsonValue customEventTypes = json(object());

        AuditServiceConfiguration auditServiceConfiguration = new AuditServiceConfiguration();
        JsonValue serviceConfig = readJsonFile("/org/forgerock/openam/audit/service-config.json");
        auditServiceConfiguration.setHandlerForQueries(serviceConfig.get("useForQueries").asString());

        AuditService auditService = new AuditService(extendedEventTypes, customEventTypes);
        try {
            registerCsvAuditEventHandler(auditService);
            auditService.configure(auditServiceConfiguration);
        } catch (ResourceException|AuditException e) {
            debug.error("Unable to configure AuditService", e);
            throw new RuntimeException("Unable to configure AuditService.", e);
        }
        return auditService;
    }

    private void registerCsvAuditEventHandler(AuditService auditService) throws ResourceException, AuditException {
        JsonValue csvConfig = readJsonFile("/org/forgerock/openam/audit/csv-handler-config.json");

        CSVAuditEventHandlerConfiguration csvHandlerConfiguration = new CSVAuditEventHandlerConfiguration();
        csvHandlerConfiguration.setLogDirectory(csvConfig.get("config").get("location").asString());
        csvHandlerConfiguration.setRecordDelimiter(csvConfig.get("config").get("recordDelimiter").asString());

        CSVAuditEventHandler csvAuditEventHandler = new CSVAuditEventHandler();
        csvAuditEventHandler.configure(csvHandlerConfiguration);

        auditService.register(csvAuditEventHandler, "csv", csvConfig.get("events").asSet(String.class));
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
