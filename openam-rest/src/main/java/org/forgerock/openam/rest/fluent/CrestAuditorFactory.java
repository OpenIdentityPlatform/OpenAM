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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.rest.fluent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.json.resource.Request;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.services.context.Context;

import com.sun.identity.shared.debug.Debug;

/**
 * Factory for creation of CrestAuditor objects.
 * Facilitates mocking of CREST Auditors.
 *
 * @since 13.0.0
 */
@Singleton
public class CrestAuditorFactory {

    private final Debug debug;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;

    @Inject
    public CrestAuditorFactory(@Named("frRest") Debug debug, AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory) {
        this.debug = debug;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
    }

    public CrestAuditor create(Context context, Request request) {

        // If creating an auditor for SESSION, return our own tailored auditor which does not log path information.
        //
        if (context.containsContext(AuditInfoContext.class)) {
            AuditConstants.Component component = context.asContext(AuditInfoContext.class).getComponent();
            if (component == AuditConstants.Component.SESSION) {
                return new CrestNoPathDetailsAuditor(debug, auditEventPublisher, auditEventFactory,
                        context, request);
            }
        }

        return new CrestAuditor(debug, auditEventPublisher, auditEventFactory, context, request);
    }
}
