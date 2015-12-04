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
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandler;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test the JdbcAuditEventHandlerFactory class.
 *
 * @since 13.0.0
 */
public class JdbcAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;
    private Map<String, Set<String>> configAttributes;

    @BeforeMethod
    public void setUp() {
        factory = new JdbcAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();

        configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("databaseType", singleton("h2"));
        configAttributes.put("jdbcUrl", singleton("jdbc:h2:mem:audit"));
        configAttributes.put("driverClassName", singleton("org.h2.Driver"));
        configAttributes.put("username", singleton("test"));
        configAttributes.put("password", singleton("password"));
        configAttributes.put("autoCommit", singleton("true"));
        configAttributes.put("connectionTimeout", singleton("31200"));
        configAttributes.put("idleTimeout", singleton("65400"));
        configAttributes.put("maxLifetime", singleton("1230000"));
        configAttributes.put("minIdle", singleton("16"));
        configAttributes.put("maxPoolSize", singleton("17"));
        configAttributes.put("authenticationEventTable", singleton("authenticationtable"));
        configAttributes.put("activityEventTable", singleton("activitytable"));
        configAttributes.put("accessEventTable", singleton("accesstable"));
        configAttributes.put("configEventTable", singleton("configtable"));
        configAttributes.put("authenticationEventColumns", singleton("[result]=result"));
        configAttributes.put("activityEventColumns", singleton("[runAs]=runas"));
        configAttributes.put("accessEventColumns", singleton("[server/ip]=server_ip"));
        configAttributes.put("configEventColumns", singleton("[objectId]=objectid"));
        configAttributes.put("bufferingEnabled", singleton("true"));
        configAttributes.put("bufferingMaxSize", singleton("5000"));
        configAttributes.put("bufferingWriteInterval", singleton("60"));
        configAttributes.put("bufferingWriterThreads", singleton("1"));
        configAttributes.put("bufferingMaxBatchedEvents", singleton("100"));
    }

    @Test
    private void shouldCreateJdbcEventHandler() throws AuditException {
        // Given
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("JDBC Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(JdbcAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("JDBC Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isTrue();
    }
}
