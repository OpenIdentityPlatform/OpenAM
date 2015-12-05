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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.audit.servlet;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AuditConstants.ACCESS_RESPONSE_DETAIL_REASON;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_ACCESS_ATTEMPT;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_ACCESS_OUTCOME;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.util.time.TimeService;

import com.google.inject.assistedinject.Assisted;

/**
 * Creates access audit events from information collected from HTTP request and response objects.
 *
 * @since 13.0.0
 */
public class Auditor {

    private final TimeService timeService;
    private final HttpServletRequest request;
    private final AuditableHttpServletResponse response;
    private final Component component;
    private final long startTime;

    /**
     * Constructs a new Auditor instance.
     *
     * @param timeService A {@code TimeService} instance.
     * @param request The {@code HttpServletRequest}.
     * @param response The {@code HttpServletResponse}.
     * @param component The component.
     */
    @Inject
    public Auditor(TimeService timeService, @Assisted HttpServletRequest request,
            @Assisted AuditableHttpServletResponse response, @Assisted Component component) {
        this.timeService = timeService;
        this.request = request;
        this.response = response;
        this.component = component;
        this.startTime = timeService.now();
    }

    /**
     * Creates an audit event that captures details of an attempted HTTP call.
     *
     * @return An AuditEvent.
     */
    public AuditEvent auditAccessAttempt() {
        return accessEvent()
                .forHttpServletRequest(request)
                .timestamp(startTime)
                .transactionId(AuditRequestContext.getTransactionIdValue())
                .eventName(AM_ACCESS_ATTEMPT)
                .component(component)
                .toEvent();
    }

    /**
     * Creates an audit event that captures details of the outcome from a HTTP call.
     *
     * @return An AuditEvent.
     */
    public AuditEvent auditAccessOutcome() {
        if (response.hasSuccessStatusCode()) {
            return auditAccessSuccess();
        } else {
            return auditAccessFailure();
        }
    }

    /**
     * Creates an audit event that captures details of a successfully completed HTTP call.
     *
     * @return An AuditEvent.
     */
    public AuditEvent auditAccessSuccess() {
        long endTime = timeService.now();
        long elapsedTime = endTime - startTime;
        return accessEvent()
                .forHttpServletRequest(request)
                .timestamp(endTime)
                .transactionId(AuditRequestContext.getTransactionIdValue())
                .eventName(AM_ACCESS_OUTCOME)
                .component(component)
                .response(SUCCESSFUL, "", elapsedTime, MILLISECONDS)
                .toEvent();
    }

    /**
     * Creates an audit event that captures details of an unsuccessfully completed HTTP call.
     *
     * @return An AuditEvent.
     */
    public AuditEvent auditAccessFailure() {
        long endTime = timeService.now();
        long elapsedTime = endTime - startTime;
        String statusCode = Integer.toString(response.getStatusCode());
        JsonValue responseDetail = json(object(
                field(ACCESS_RESPONSE_DETAIL_REASON, response.getMessage())));
        return accessEvent()
                .forHttpServletRequest(request)
                .timestamp(endTime)
                .transactionId(AuditRequestContext.getTransactionIdValue())
                .eventName(AM_ACCESS_OUTCOME)
                .component(component)
                .responseWithDetail(FAILED, statusCode, elapsedTime, MILLISECONDS, responseDetail)
                .toEvent();
    }

    private AMAccessAuditEventBuilder accessEvent() {
        return new AMAccessAuditEventBuilder();
    }
}
