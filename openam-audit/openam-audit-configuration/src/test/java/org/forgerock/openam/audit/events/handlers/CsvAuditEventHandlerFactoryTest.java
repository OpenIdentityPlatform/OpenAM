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
package org.forgerock.openam.audit.events.handlers;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandler;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 13.0.0
 */
public class CsvAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;
    private String logDirName = "/tmpLogDir";

    @BeforeMethod
    public void setUp() {
        factory = new CsvAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();

        File logDir = new File(logDirName);
        if (logDir.exists()) {
            logDir.deleteOnExit();
        }
    }

    @Test
    private void shouldCreateCsvEventHandler() throws AuditException {
        // Given
        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("location", singleton(logDirName));
        configAttributes.put("bufferingEnabled", singleton("true"));
        configAttributes.put("bufferingAutoFlush", singleton("false"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("CSV Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(CsvAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("CSV Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isTrue();
    }

    @Test
    private void shouldCreateCsvEventHandlerWhenDisabled() throws AuditException {
        // Given
        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("false"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("location", singleton(logDirName));
        configAttributes.put("bufferingEnabled", singleton("true"));
        configAttributes.put("bufferingAutoFlush", singleton("false"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("CSV Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(CsvAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("CSV Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isFalse();
    }
}
