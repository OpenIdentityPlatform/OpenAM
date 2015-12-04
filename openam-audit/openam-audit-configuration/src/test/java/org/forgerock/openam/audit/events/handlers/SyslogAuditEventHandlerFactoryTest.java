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
import org.forgerock.audit.handlers.syslog.Facility;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandler;
import org.forgerock.audit.handlers.syslog.TransportProtocol;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test the SyslogAuditEventHandlerFactory class.
 *
 * @since 13.0.0
 */
public class SyslogAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;
    private Map<String, Set<String>> configAttributes;

    @BeforeMethod
    public void setUp() {
        factory = new SyslogAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();

        configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("host", singleton("www.example.com"));
        configAttributes.put("port", singleton("1000"));
        configAttributes.put("transportProtocol", singleton(TransportProtocol.UDP.name()));
        configAttributes.put("connectTimeout", singleton("10"));
        configAttributes.put("bufferingEnabled", singleton("true"));
        configAttributes.put("facility", singleton(Facility.USER.name()));
    }

    @Test
    private void shouldCreateSyslogEventHandler() throws AuditException {
        // Given
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("Syslog Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(SyslogAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("Syslog Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isTrue();
    }
}
