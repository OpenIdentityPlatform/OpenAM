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

import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILURE;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.TimeUnit.MILLISECONDS;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.getTokenFromContext;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.json.resource.Request;
import org.forgerock.services.context.Context;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.util.Reject;

/**
 * Responsible for publishing audit access events for individual CREST request.
 *
 * @since 13.0.0
 */
class CrestAuditor {

    private final Debug debug;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Context context;
    private final Component component;
    private final Request request;
    private final long startTime;

    /**
     * Create a new CrestAuditor.
     *
     * @param debug               Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory   AuditEventFactory for audit event builders.
     * @param context             Context of the CREST operation being audited.
     * @param request             Request of the CREST operation being audited.
     */
    CrestAuditor(Debug debug, AuditEventPublisher auditEventPublisher,
                 AuditEventFactory auditEventFactory, Context context, Request request) {

        Reject.ifFalse(context.containsContext(AuditInfoContext.class), "CREST auditing expects the audit context");
        component = context.asContext(AuditInfoContext.class).getComponent();

        this.debug = debug;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.context = context;
        this.request = request;
        this.startTime = context.asContext(RequestAuditContext.class).getRequestReceivedTime();
    }

    /**
     * Publishes an audit event with details of the attempted CREST operation, if the 'access' topic is audited.
     *
     * @throws AuditException If an exception occurred that prevented the audit event from being published.
     */
    void auditAccessAttempt() throws AuditException {
        if (auditEventPublisher.isAuditing(ACCESS_TOPIC)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .forHttpCrestRequest(context, request)
                    .timestamp(startTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component);
            addSessionDetailsFromSSOTokenContext(builder, context);

            auditEventPublisher.publish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    /**
     * Publishes an event with details of the successfully completed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     */
    void auditAccessSuccess() {
        if (auditEventPublisher.isAuditing(ACCESS_TOPIC)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .forHttpCrestRequest(context, request)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .response(SUCCESS, "", elapsedTime, MILLISECONDS);
            addSessionDetailsFromSSOTokenContext(builder, context);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    /**
     * Publishes an event with details of the failed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     *
     * @param resultCode The HTTP result code relating to the failure.
     * @param message    A human-readable description of the error that occurred.
     */
    void auditAccessFailure(int resultCode, String message) {
        if (auditEventPublisher.isAuditing(ACCESS_TOPIC)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .forHttpCrestRequest(context, request)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .responseWithDetail(FAILURE, Integer.toString(resultCode), elapsedTime, MILLISECONDS, message);
            addSessionDetailsFromSSOTokenContext(builder, context);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void addSessionDetailsFromSSOTokenContext(AMAccessAuditEventBuilder builder, Context context) {
        SSOToken callerToken = getTokenFromContext(context, debug);
        builder.contextIdFromSSOToken(callerToken);
    }
}