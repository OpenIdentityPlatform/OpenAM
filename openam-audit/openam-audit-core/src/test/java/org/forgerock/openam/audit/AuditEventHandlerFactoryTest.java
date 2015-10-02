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

import static java.util.Collections.singleton;
import static org.forgerock.openam.audit.AuditConstants.EventHandlerType.CSV;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandler;
import org.forgerock.audit.events.handlers.csv.CSVAuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfigurationWrapper;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class AuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private String logDirName = "/tmpLogDir";

    @BeforeMethod
    public void setUp() {
        factory = new AuditEventHandlerFactory();

        File logDir = new File(logDirName);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

    }

    @AfterTest
    public void tearDown() {
        File logDir = new File(logDirName);
        if (logDir.exists()) {
            logDir.deleteOnExit();
        }
    }

    @Test
    private void shouldCreateCsvEventHandler() throws AuditException {
        // Given
        CSVAuditEventHandlerConfiguration csvHandlerConfig = new CSVAuditEventHandlerConfiguration();
        csvHandlerConfig.setLogDirectory(logDirName);
        AuditEventHandlerConfigurationWrapper configWrapper = new AuditEventHandlerConfigurationWrapper(
                csvHandlerConfig, CSV, "CSV Handler", singleton("access"));

        // When
        AuditEventHandler handler = factory.create(configWrapper);

        // Then
        assertThat(handler).isInstanceOf(CSVAuditEventHandler.class);
    }

    @Test(expectedExceptions = AuditException.class)
    private void shouldThrowExceptionWhenHandlerTypeIsNull() throws AuditException {
        // Given
        CSVAuditEventHandlerConfiguration csvHandlerConfig = new CSVAuditEventHandlerConfiguration();
        csvHandlerConfig.setLogDirectory(logDirName);
        AuditEventHandlerConfigurationWrapper configWrapper = new AuditEventHandlerConfigurationWrapper(
                csvHandlerConfig, null, "CSV Handler", singleton("access"));

        // When
        factory.create(configWrapper);

        // Then
        // should throw exception
    }
}
