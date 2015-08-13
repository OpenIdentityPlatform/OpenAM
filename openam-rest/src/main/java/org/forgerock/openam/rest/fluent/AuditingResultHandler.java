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
package org.forgerock.openam.rest.fluent;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.http.context.ServerContext;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;

/**
 * ResultHandler decorator responsible for publishing audit access events for a single request.
 *
 * @param <T> {@inheritDoc}
 *
 * @since 13.0.0
 */
class AuditingResultHandler<T> extends AbstractAuditingResultHandler<T, ResultHandler<T>> {

    /**
     * Create a new AuditingResultHandler.
     *
     * @param debug Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory AuditEventFactory for audit event builders.
     * @param context Context of the CREST operation being audited.
     * @param request Request of the CREST operation being audited.
     * @param delegate ResultHandler of the CREST operation being audited.
     */
    AuditingResultHandler(Debug debug, AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
                          ServerContext context, Request request, ResultHandler<T> delegate) {
        super(debug, auditEventPublisher, auditEventFactory, context, request, delegate);
    }
}