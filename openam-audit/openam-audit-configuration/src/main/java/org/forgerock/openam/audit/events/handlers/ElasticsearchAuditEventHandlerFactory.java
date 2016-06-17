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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandler;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.ConnectionConfiguration;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.IndexMappingConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.sun.identity.shared.debug.Debug;

/**
 * This factory is responsible for creating an instance of the {@link ElasticsearchAuditEventHandler}.
 *
 * @since 13.5.0
 */
public final class ElasticsearchAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
        final ElasticsearchAuditEventHandlerConfiguration esHandlerConfiguration = new
                ElasticsearchAuditEventHandlerConfiguration();

        Map<String, Set<String>> attributes = configuration.getAttributes();

        esHandlerConfiguration.setName(configuration.getHandlerName());
        esHandlerConfiguration.setEnabled(getBooleanMapAttr(attributes, "enabled", false));
        esHandlerConfiguration.setTopics(attributes.get("topics"));
        esHandlerConfiguration.setIndexMapping(getIndexMapping(attributes));
        esHandlerConfiguration.setBuffering(getBufferingConfiguration(attributes));
        esHandlerConfiguration.setConnection(getConnection(attributes));

        final Client client = createClient();

        return new ElasticsearchAuditEventHandler(esHandlerConfiguration, configuration.getEventTopicsMetaData(),
                client);
    }

    private Client createClient() throws AuditException {
        try {
            final HttpClientHandler httpClientHandler = new HttpClientHandler();

            // Register listener to tidy up resulting threads on shutdown
            ShutdownManager shutdownManager = InjectorHolder.getInstance(ShutdownManager.class);
            shutdownManager.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdown() {
                    try {
                        httpClientHandler.close();
                    } catch (IOException e) {
                        // Abandon attempt to close the handler. Handler may have already been closed.
                        DEBUG.message("Unable to close the HttpClientHandler", e);
                    }
                }
            });

            return new Client(httpClientHandler);
        } catch (HttpApplicationException e) {
            throw new AuditException("Failed to create HttpClientHandler", e);
        }
    }

    private EventBufferingConfiguration getBufferingConfiguration(Map<String, Set<String>> attributes) {
        final EventBufferingConfiguration bufferingConfiguration = new EventBufferingConfiguration();

        bufferingConfiguration.setEnabled(getBooleanMapAttr(attributes, "bufferingEnabled", true));
        bufferingConfiguration.setMaxBatchedEvents(getIntMapAttr(attributes, "batchSize", 500, DEBUG));
        bufferingConfiguration.setMaxSize(getIntMapAttr(attributes, "maxEvents", 10000, DEBUG));

        Integer writeInterval = getIntMapAttr(attributes, "writeInterval", 1000, DEBUG);
        bufferingConfiguration.setWriteInterval(String.valueOf(writeInterval) + " millis");

        return bufferingConfiguration;
    }

    private IndexMappingConfiguration getIndexMapping(Map<String, Set<String>> attributes) {
        final IndexMappingConfiguration indexMapping = new IndexMappingConfiguration();

        indexMapping.setIndexName(getMapAttr(attributes, "index"));

        return indexMapping;
    }

    private ConnectionConfiguration getConnection(Map<String, Set<String>> attributes) {
        final ConnectionConfiguration connection = new ConnectionConfiguration();

        connection.setHost(getMapAttr(attributes, "host"));
        connection.setPort(getIntMapAttr(attributes, "port", 9200, DEBUG));
        connection.setUseSSL(getBooleanMapAttr(attributes, "sslEnabled", false));
        connection.setUsername(getMapAttr(attributes, "username"));
        connection.setPassword(getMapAttr(attributes, "password"));

        return connection;
    }
}
