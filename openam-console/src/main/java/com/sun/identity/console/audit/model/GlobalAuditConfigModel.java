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
package com.sun.identity.console.audit.model;

import static com.sun.identity.console.base.model.AMAdminUtils.getSuperAdminSSOToken;
import static java.util.Collections.singleton;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * Global Audit configuration UI model.
 *
 * @since 13.0.0
 */
public class GlobalAuditConfigModel extends AbstractAuditModel {

    /**
     * Create a new {@code GlobalAuditConfigModel}.
     *
     * @param request The {@code HttpServletRequest}
     * @param sessionAttributes The session attributes.
     * @throws AMConsoleException If construction fails.
     */
    public GlobalAuditConfigModel(HttpServletRequest request, Map sessionAttributes) throws AMConsoleException {
        super(request, sessionAttributes);
    }

    @Override
    protected ServiceSchema getServiceSchema() throws SMSException, SSOException {
        ServiceSchemaManager schemaManager = new ServiceSchemaManager(serviceName, getSuperAdminSSOToken());
        return schemaManager.getGlobalSchema();
    }

    @Override
    protected ServiceConfig getServiceConfig() throws SMSException, SSOException {
        ServiceConfigManager configManager = new ServiceConfigManager(serviceName, getSuperAdminSSOToken());
        return configManager.getGlobalConfig(null);
    }

    @Override
    public Set<SchemaType> getDisplaySchemaTypes() {
        return singleton(SchemaType.GLOBAL);
    }

}
