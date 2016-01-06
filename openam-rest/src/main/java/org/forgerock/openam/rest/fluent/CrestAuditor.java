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

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.*;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.util.Reject;

import java.util.List;

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
    private final String realm;

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

        if (context.containsContext(RealmContext.class)) {
            this.realm = context.asContext(RealmContext.class).getResolvedRealm();
        } else {
            this.realm = NO_REALM;
        }
    }

    /**
     * Publishes an audit event with details of the attempted CREST operation, if the 'access' topic is audited.
     */
    void auditAccessAttempt() {
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_ATTEMPT)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .forHttpRequest(context, request)
                    .timestamp(startTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component);
            addSessionDetailsFromSSOTokenContext(builder, context);

            if (ipAddressHeaderPropertyIsSet()) {
                setClientFromHttpContextHeaderIfExists(builder, context);
            }

            AuditEvent auditEvent = builder.toEvent();
            postProcessEvent(auditEvent);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
        }
    }

    /**
     * Publishes an event with details of the successfully completed CREST operation, if the 'access' topic is audited.
     * Provides additional detail.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     *
     * @param responseDetail Additional details relating to the response (e.g. failure description or summary
     *                       of the payload). Can be null if there are no additional details.
     */
    void auditAccessSuccess(JsonValue responseDetail) {
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .forHttpRequest(context, request)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component);
            if (responseDetail == null) {
                builder.response(SUCCESSFUL, "", elapsedTime, MILLISECONDS);
            } else {
                builder.responseWithDetail(SUCCESSFUL, "", elapsedTime, MILLISECONDS, responseDetail);
            }
            addSessionDetailsFromSSOTokenContext(builder, context);

            if (ipAddressHeaderPropertyIsSet()) {
                setClientFromHttpContextHeaderIfExists(builder, context);
            }

            AuditEvent auditEvent = builder.toEvent();
            postProcessEvent(auditEvent);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
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
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            JsonValue detail = json(object(field(ACCESS_RESPONSE_DETAIL_REASON, message)));
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .forHttpRequest(context, request)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .responseWithDetail(FAILED, Integer.toString(resultCode), elapsedTime, MILLISECONDS, detail);
            addSessionDetailsFromSSOTokenContext(builder, context);

            if (ipAddressHeaderPropertyIsSet()) {
                setClientFromHttpContextHeaderIfExists(builder, context);
            }

            AuditEvent auditEvent = builder.toEvent();
            postProcessEvent(auditEvent);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
        }
    }

    private void addSessionDetailsFromSSOTokenContext(AMAccessAuditEventBuilder builder, Context context) {
        SSOToken callerToken = getTokenFromContext(context, debug);
        builder.trackingIdFromSSOToken(callerToken);
        builder.userId(getUserId(callerToken));
    }

    private void setClientFromHttpContextHeaderIfExists(AMAccessAuditEventBuilder builder, Context context) {
        if (context.containsContext(HttpContext.class)) {
            HttpContext httpContext = context.asContext(HttpContext.class);
            List<String> xForwardedFor = httpContext.getHeader(
                    SystemPropertiesManager.get(Constants.CLIENT_IP_ADDR_HEADER));
            if (xForwardedFor != null && xForwardedFor.size() > 0) {
                builder.client(xForwardedFor.get(0));
            }
        }
    }

    private boolean ipAddressHeaderPropertyIsSet() {
        String ipAddrHeader = SystemPropertiesManager.get(Constants.CLIENT_IP_ADDR_HEADER);
        return StringUtils.isNotBlank(ipAddrHeader);
    }

    /**
     * This function provided for derived classes to modify the audit event before it is published.  For instance,
     * {@link CrestNoPathDetailsAuditor} uses it to remove the session id from the http request path to avoid the
     * possibility of session hijacking.
     *
     * @param auditEvent The audit event, to be modified in whatever way a subclass sees fit.
     */
    protected void postProcessEvent(AuditEvent auditEvent) {
    }
}
