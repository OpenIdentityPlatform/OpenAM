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
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.json.JsonStdoutAuditEventHandler;
import org.forgerock.audit.handlers.json.JsonStdoutAuditEventHandlerConfiguration;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;

import java.util.Map;
import java.util.Set;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;

public class JsonStdoutAuditEventHandlerFactory implements AuditEventHandlerFactory {

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
        Map<String, Set<String>> attributes = configuration.getAttributes();

        JsonStdoutAuditEventHandlerConfiguration handlerConfig = new JsonStdoutAuditEventHandlerConfiguration();
        handlerConfig.setTopics(attributes.get("topics"));
        handlerConfig.setName(configuration.getHandlerName());
        handlerConfig.setEnabled(getBooleanMapAttr(attributes, "enabled", false));
        handlerConfig.setElasticsearchCompatible(getBooleanMapAttr(attributes, "elasticsearchCompatible", false));
        return new JsonStdoutAuditEventHandler(handlerConfig, configuration.getEventTopicsMetaData());
    }
}
