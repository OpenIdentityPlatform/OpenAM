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

import static com.sun.identity.shared.datastruct.CollectionHelper.*;

import java.util.Map;
import java.util.Set;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.jms.BatchPublisherConfiguration;
import org.forgerock.audit.handlers.jms.DeliveryModeConfig;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandler;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.jms.SessionModeConfig;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;

import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.shared.debug.Debug;

/**
 * This factory is responsible for creating an instance of the {@link JmsAuditEventHandler}.
 *
 * @since 13.5.0
 */
public final class JmsAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");
    private static final MapValueParser MAP_VALUE_PARSER = new MapValueParser();

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {

        Map<String, Set<String>> attributes = configuration.getAttributes();

        JmsAuditEventHandlerConfiguration handlerConfig = new JmsAuditEventHandlerConfiguration();
        handlerConfig.setTopics(attributes.get("topics"));
        handlerConfig.setName(configuration.getHandlerName());
        handlerConfig.setEnabled(getBooleanMapAttr(attributes, "enabled", false));

        handlerConfig.setBatch(getBatchPublisherConfig(attributes));
        handlerConfig.setDeliveryMode(getDeliveryModeConfig(attributes));
        handlerConfig.setJndi(getJndiConfig(attributes));
        handlerConfig.setSessionMode(getSessionModeConfig(attributes));

        try {
            return new JmsAuditEventHandler(null, handlerConfig, configuration.getEventTopicsMetaData());
        } catch (ResourceException e) {
            throw new AuditException(e);
        }
    }

    private SessionModeConfig getSessionModeConfig(Map<String, Set<String>> attributes) {
        return SessionModeConfig.valueOf(getMapAttr(attributes, "sessionMode"));
    }

    private JmsAuditEventHandlerConfiguration.JndiConfiguration getJndiConfig(Map<String, Set<String>> attributes) {
        JmsAuditEventHandlerConfiguration.JndiConfiguration jndiConfig = new JmsAuditEventHandlerConfiguration.
                JndiConfiguration();
        jndiConfig.setContextProperties(MAP_VALUE_PARSER.parse(attributes.get("jndiContextProperties")));
        jndiConfig.setJmsTopicName(getMapAttr(attributes, "jndiTopicName"));
        jndiConfig.setJmsConnectionFactoryName(getMapAttr(attributes, "jndiConnectionFactoryName"));
        return jndiConfig;
    }

    private DeliveryModeConfig getDeliveryModeConfig(Map<String, Set<String>> attributes) {
        return DeliveryModeConfig.valueOf(getMapAttr(attributes, "deliveryMode"));
    }

    private BatchPublisherConfiguration getBatchPublisherConfig(Map<String, Set<String>> attributes) {
        BatchPublisherConfiguration batchPublisherConfig = new BatchPublisherConfiguration();
        batchPublisherConfig.setBatchEnabled(getBooleanMapAttr(attributes, "batchEnabled", false));
        batchPublisherConfig.setCapacity(getIntMapAttr(attributes, "batchCapacity", 1000, DEBUG));
        batchPublisherConfig.setMaxBatchedEvents(getIntMapAttr(attributes, "maxBatchedEvents", 100, DEBUG));
        batchPublisherConfig.setThreadCount(getIntMapAttr(attributes, "batchThreadCount", 3, DEBUG));
        batchPublisherConfig.setInsertTimeoutSec(getLongMapAttr(attributes, "insertTimeoutSec", 60, DEBUG));
        batchPublisherConfig.setPollTimeoutSec(getLongMapAttr(attributes, "pollTimeoutSec", 10, DEBUG));
        batchPublisherConfig.setShutdownTimeoutSec(getLongMapAttr(attributes, "shutdownTimeoutSec", 60, DEBUG));
        return batchPublisherConfig;
    }
}
