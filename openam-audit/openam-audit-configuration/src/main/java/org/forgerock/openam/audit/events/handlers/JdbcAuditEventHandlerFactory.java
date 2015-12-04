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

import static com.sun.identity.shared.datastruct.CollectionHelper.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.shared.debug.Debug;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandler;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.handlers.jdbc.TableMapping;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.JdbcFieldToColumnDefaultValues;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This factory is responsible for creating an instance of the {@link JdbcAuditEventHandler}.
 *
 * @since 13.0.0
 */
public final class JdbcAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");
    private static final MapValueParser MAP_VALUE_PARSER = new MapValueParser();

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
        Map<String, Set<String>> attributes = configuration.getAttributes();

        JdbcAuditEventHandlerConfiguration handlerConfig = new JdbcAuditEventHandlerConfiguration();
        handlerConfig.setTopics(attributes.get("topics"));
        handlerConfig.setName(configuration.getHandlerName());
        handlerConfig.setEnabled(getBooleanMapAttr(attributes, "enabled", false));
        handlerConfig.setDatabaseType(getMapAttr(attributes, "databaseType"));

        List<TableMapping> tableMappings = new ArrayList<>();
        tableMappings.add(getTableMapping(AUTHENTICATION_TOPIC, attributes));
        tableMappings.add(getTableMapping(ACTIVITY_TOPIC, attributes));
        tableMappings.add(getTableMapping(ACCESS_TOPIC, attributes));
        tableMappings.add(getTableMapping(CONFIG_TOPIC, attributes));
        handlerConfig.setTableMappings(tableMappings);

        handlerConfig.setBufferingConfiguration(getBufferingConfiguration(attributes));

        return new JdbcAuditEventHandler(handlerConfig, configuration.getEventTopicsMetaData(),
                getDataSource(attributes));
    }

    private TableMapping getTableMapping(String eventTopic, Map<String, Set<String>> attributes) {
        TableMapping tableMapping = new TableMapping();
        tableMapping.setEvent(eventTopic);
        String tableName = getMapAttr(attributes, eventTopic + "EventTable");
        if (isEmpty(tableName)) {
            tableName = "am_audit" + eventTopic;
        }
        tableMapping.setTable(tableName);
        Set<String> fieldToColumnSet = attributes.get(eventTopic + "EventColumns");
        if (isEmpty(fieldToColumnSet)) {
            fieldToColumnSet = JdbcFieldToColumnDefaultValues.getDefaultValues(eventTopic);
        }
        tableMapping.setFieldToColumn(MAP_VALUE_PARSER.parse(fieldToColumnSet));
        return tableMapping;
    }

    private DataSource getDataSource(Map<String, Set<String>> attributes) {
        return new HikariDataSource(createHikariConfig(attributes));
    }

    private HikariConfig createHikariConfig(Map<String, Set<String>> attributes) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getMapAttr(attributes, "jdbcUrl"));
        hikariConfig.setDriverClassName(getMapAttr(attributes, "driverClassName"));
        hikariConfig.setUsername(getMapAttr(attributes, "username"));
        hikariConfig.setPassword(getMapAttr(attributes, "password"));
        hikariConfig.setAutoCommit(getBooleanMapAttr(attributes, "autoCommit", false));

        int connectionTimeoutSeconds = getIntMapAttr(attributes, "connectionTimeout", 30, DEBUG);
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(connectionTimeoutSeconds));

        int idleTimeoutSeconds = getIntMapAttr(attributes, "idleTimeout", 600, DEBUG);
        hikariConfig.setIdleTimeout(TimeUnit.SECONDS.toMillis(idleTimeoutSeconds));

        int maxLifetimeSeconds = getIntMapAttr(attributes, "maxLifetime", 1800, DEBUG);
        hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(maxLifetimeSeconds));

        hikariConfig.setMinimumIdle(getIntMapAttr(attributes, "minIdle", 10, DEBUG));
        hikariConfig.setMaximumPoolSize(getIntMapAttr(attributes, "maxPoolSize", 10, DEBUG));
        return hikariConfig;
    }

    private EventBufferingConfiguration getBufferingConfiguration(Map<String, Set<String>> attributes) {
        EventBufferingConfiguration bufferingConfiguration = new EventBufferingConfiguration();
        bufferingConfiguration.setEnabled(getBooleanMapAttr(attributes, "bufferingEnabled", true));
        bufferingConfiguration.setMaxSize(getIntMapAttr(attributes, "bufferingMaxSize", 100000, DEBUG));
        bufferingConfiguration.setWriteInterval(getMapAttr(attributes, "bufferingWriteInterval", "5") + " seconds");
        bufferingConfiguration.setWriterThreads(getIntMapAttr(attributes, "bufferingWriterThreads", 1, DEBUG));
        bufferingConfiguration.setMaxBatchedEvents(getIntMapAttr(attributes, "bufferingMaxBatchedEvents", 100, DEBUG));
        return bufferingConfiguration;
    }
}
