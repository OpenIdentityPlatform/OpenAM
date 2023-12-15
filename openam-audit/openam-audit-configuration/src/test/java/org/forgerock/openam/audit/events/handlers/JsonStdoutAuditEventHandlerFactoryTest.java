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
* Copyright 2023 3A Systems LLC
*/
package org.forgerock.openam.audit.events.handlers;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.json.JsonStdoutAuditEventHandler;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the JsonStdoutAuditEventHandlerFactoryTest class.
 *
 * @since 14.8.3
 */
public class JsonStdoutAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;
    private Map<String, Set<String>> configAttributes;

    @BeforeMethod
    public void setUp() {
        factory = new JsonStdoutAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();

        configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("elasticsearchCompatible", singleton("true"));
    }

    @Test
    void shouldCreateJsonStdoutEventHandler() throws AuditException {
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("JSONStdout")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        AuditEventHandler handler = factory.create(configuration);

        assertThat(handler).isInstanceOf(JsonStdoutAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("JSONStdout");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isTrue();
    }
}
