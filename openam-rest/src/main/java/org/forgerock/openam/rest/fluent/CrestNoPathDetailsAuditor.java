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
package org.forgerock.openam.rest.fluent;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

/**
 * The sole purpose of this class is to modify the audit event object just before it is logged such that there is no
 * reference to the session id in the http path.  This is to stop the possibility of session stealing by someone with
 * access to relatively recent audit logs.
 */
public class CrestNoPathDetailsAuditor extends CrestAuditor {

    /**
     * Create a new CrestNoSessionDetailsAuditor.
     *
     * @param debug               Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory   AuditEventFactory for audit event builders.
     * @param context             Context of the CREST operation being audited.
     * @param request             Request of the CREST operation being audited.
     */
    CrestNoPathDetailsAuditor(Debug debug, AuditEventPublisher auditEventPublisher,
                              AuditEventFactory auditEventFactory, Context context, Request request) {

        super(debug, auditEventPublisher, auditEventFactory, context, request);
    }

    /**
     * This wipes out the http request path beyond /sessions, i.e. all information relating to the session id.
     */
    @Override
    protected void postProcessEvent(AuditEvent auditEvent) {

        final String SESSIONS = "/sessions";

        Reject.ifNull(auditEvent);

        JsonValue jsonValue = auditEvent.getValue();
        JsonPointer pathComponent = new JsonPointer("/http/request/path");
        String path = jsonValue.get(pathComponent).asString();
        if (path != null) {
            int posLast = path.lastIndexOf(SESSIONS);
            if (posLast > 0) {
                path = path.substring(0, posLast + SESSIONS.length());
                jsonValue.remove(pathComponent);
                jsonValue.add(pathComponent, path);
            }
        }
    }
}
