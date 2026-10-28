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
 * Portions copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.audit.events.handlers;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.Response;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportAcceptListener;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportServer;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.jms.DeliveryModeConfig;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandler;
import org.forgerock.audit.handlers.jms.SessionModeConfig;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test the JmsAuditEventHandlerFactoryTest class.
 *
 * @since 13.5.0
 */
public class JmsAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;
    private EventTopicsMetaData eventTopicsMetaData;
    private Map<String, Set<String>> configAttributes;

    @BeforeMethod
    public void setUp() {
        factory = new JmsAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();
        configAttributes = new HashMap<>();
    }

    @Test
    private void canCreateAuditHandlerWithBareMinimumFields() throws AuditException {
        // Given
        AuditEventHandlerConfiguration configuration =
                jmsConfiguration("tcp://localhost:61616", DeliveryModeConfig.NON_PERSISTENT);

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(JmsAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("JMS Handler");
        assertThat(handler.isEnabled()).isTrue();
        assertThat(handler.getHandledTopics()).containsExactly("access");
    }

    @Test(timeOut = 60000)
    public void canPublishAuditEventToBroker() throws Exception {
        // Given
        BlockingQueue<ActiveMQTextMessage> published = new LinkedBlockingQueue<>();
        TransportServer broker = TransportFactory.bind(new URI("tcp://localhost:0"));
        broker.setAcceptListener(new StubBroker(published));
        broker.start();
        try {
            AuditEventHandler handler = factory.create(jmsConfiguration(
                    "tcp://localhost:" + broker.getSocketAddress().getPort(), DeliveryModeConfig.PERSISTENT));
            handler.startup();
            try {
                // When
                handler.publishEvent(new RootContext(), "access",
                        json(object(field("_id", "event-id"), field("eventName", "AM-ACCESS-ATTEMPT"))))
                        .getOrThrow();

                // Then
                ActiveMQTextMessage message = published.poll(10, SECONDS);
                assertThat(message).isNotNull();
                assertThat(message.getDestination().getPhysicalName()).isEqualTo("audit");
                JsonValue body = json(new ObjectMapper().readValue(message.getText(), Object.class));
                assertThat(body.get("auditTopic").asString()).isEqualTo("access");
                assertThat(body.get("event").get("_id").asString()).isEqualTo("event-id");
                assertThat(body.get("event").get("eventName").asString()).isEqualTo("AM-ACCESS-ATTEMPT");
            } finally {
                handler.shutdown();
            }
        } finally {
            broker.stop();
        }
    }

    private AuditEventHandlerConfiguration jmsConfiguration(String providerUrl, DeliveryModeConfig deliveryMode) {
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("batchEnabled", singleton("false"));
        configAttributes.put("deliveryMode", singleton(deliveryMode.name()));
        configAttributes.put("sessionMode", singleton(SessionModeConfig.AUTO.name()));
        Set<String> jndiContext = new HashSet<>();
        jndiContext.add("[java.naming.factory.initial]=org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        jndiContext.add("[java.naming.provider.url]=" + providerUrl);
        jndiContext.add("[topic.audit]=audit");
        configAttributes.put("jndiContextProperties", jndiContext);
        configAttributes.put("jndiTopicName", singleton("audit"));
        configAttributes.put("jndiConnectionFactoryName", singleton("ConnectionFactory"));

        return AuditEventHandlerConfiguration.builder()
                .withName("JMS Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(eventTopicsMetaData).build();
    }

    /**
     * Minimal OpenWire broker: acknowledges every command that expects a response and records the published
     * text messages, so that the test exercises the JMS client end to end without a real broker.
     */
    private static final class StubBroker implements TransportAcceptListener {

        private final BlockingQueue<ActiveMQTextMessage> published;

        private StubBroker(BlockingQueue<ActiveMQTextMessage> published) {
            this.published = published;
        }

        @Override
        public void onAccept(final Transport transport) {
            transport.setTransportListener(new DefaultTransportListener() {
                @Override
                public void onCommand(Object object) {
                    Command command = (Command) object;
                    if (command instanceof ActiveMQTextMessage) {
                        published.add((ActiveMQTextMessage) command);
                    }
                    if (command.isResponseRequired()) {
                        Response response = new Response();
                        response.setCorrelationId(command.getCommandId());
                        try {
                            transport.oneway(response);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            });
            try {
                transport.start();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void onAcceptError(Exception error) {
        }
    }
}
