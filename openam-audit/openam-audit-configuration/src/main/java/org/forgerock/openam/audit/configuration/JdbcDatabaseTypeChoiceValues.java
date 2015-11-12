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
package org.forgerock.openam.audit.configuration;

import static org.forgerock.json.JsonValue.*;

import com.sun.identity.sm.ChoiceValues;
import org.forgerock.audit.handlers.jdbc.JdbcAuditEventHandler;
import org.forgerock.json.JsonValue;

import java.util.Map;

/**
 * Contains all the possible values for the JDBC handler database types.
 *
 * @since 13.0.0
 */
public final class JdbcDatabaseTypeChoiceValues extends ChoiceValues {

    private static final JsonValue DATABASE_TYPES = json(object(
            field(JdbcAuditEventHandler.ORACLE, "audit.handler.jdbc.oracle"),
            field(JdbcAuditEventHandler.MYSQL, "audit.handler.jdbc.mysql"),
            field("other", "audit.handler.jdbc.other")));

    @Override
    public Map getChoiceValues() {
        return DATABASE_TYPES.asMap(String.class);
    }
}
