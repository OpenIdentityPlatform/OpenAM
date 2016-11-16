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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.audit.events.handlers;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.splunk.SplunkAuditEventHandler;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SplunkAuditEventHandlerFactoryTest {

    private SplunkAuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;

    @BeforeMethod
    public void setUp() {
        factory = new SplunkAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();
    }

    @Test
    public void shouldCreateSplunkAuditEventHandler() throws AuditException {
        // Given
        final String splunkAuditEventHandlerName = "Splunk Handler";
        final String topic = "access";

        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton(topic));
        configAttributes.put("authzToken", singleton("authorization_token"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName(splunkAuditEventHandlerName)
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(SplunkAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo(splunkAuditEventHandlerName);
        assertThat(handler.getHandledTopics()).containsExactly(topic);
        assertThat(handler.isEnabled()).isTrue();
    }
}
