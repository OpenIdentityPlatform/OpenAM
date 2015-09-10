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

package org.forgerock.openam.http.audit;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getAllAvailableContexts;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.util.promise.Promises.*;

import org.forgerock.audit.AuditException;
import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.context.RequestAuditContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import java.util.Map;

/**
 * Responsible for logging access audit events for CHF requests.
 *
 * @since 13.0.0
 */
public abstract class AbstractHttpAccessAuditFilter implements Filter {

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Component component;

    /**
     * Create a new filter for the given component and handler.
     *
     * @param component The component for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     */
    public AbstractHttpAccessAuditFilter(Component component,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.component = component;
    }


    @Override
    public Promise<Response, NeverThrowsException> filter(final Context context, final Request request, Handler next) {
        try {
            auditAccessAttempt(request, context);
        } catch (AuditException e) {
            return newResultPromise(new Response().setStatus(Status.INTERNAL_SERVER_ERROR).setCause(e));
        }
        return next.handle(context, request).then(new Function<Response, Response, NeverThrowsException>() {
            @Override
            public Response apply(Response response) {
                if (response.getStatus().isSuccessful()) {
                    auditAccessSuccess(request, context, response);
                } else {
                    auditAccessFailure(request, context, response);
                }
                return response;
            }
        });
    }

    private void auditAccessAttempt(Request request, Context context) throws AuditException {
        if (auditEventPublisher.isAuditing(AuditConstants.ACCESS_TOPIC)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .timestamp(context.asContext(RequestAuditContext.class).getRequestReceivedTime())
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component)
                    .authentication(getUserIdForAccessAttempt(request))
                    .contexts(getContextsForAccessAttempt(request))
                    .forRequest(request, context);

            auditEventPublisher.publish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessSuccess(Request request, Context context, Response response) {
        if (auditEventPublisher.isAuditing(AuditConstants.ACCESS_TOPIC)) {

            long endTime = System.currentTimeMillis();
            final RequestAuditContext requestAuditContext = context.asContext(RequestAuditContext.class);
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .authentication(getUserIdForAccessOutcome(response))
                    .contexts(getContextsForAccessOutcome(response))
                    .response("SUCCESS", endTime - requestAuditContext.getRequestReceivedTime())
                    .forRequest(request, context);

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessFailure(Request request, Context context, Response response) {
        if (auditEventPublisher.isAuditing(AuditConstants.ACCESS_TOPIC)) {

            long endTime = System.currentTimeMillis();
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent()
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .authentication(getUserIdForAccessOutcome(response))
                    .contexts(getContextsForAccessOutcome(response))
                    .responseWithMessage("FAILED - " + response.getStatus().getCode(),
                            endTime - context.asContext(RequestAuditContext.class).getRequestReceivedTime(),
                            response.getStatus().getReasonPhrase())
                    .forRequest(request, context);

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        }
    }

    /**
     * Retrieve the user ID for an access attempt.
     *
     * @param request the restlet request
     * @return the user ID
     */
    protected String getUserIdForAccessAttempt(Request request) {
        String userId = AuditRequestContext.getProperty(AuditConstants.USER_ID);
        return userId == null ? "" : userId;
    }

    /**
     * Retrieve the context IDs for an access attempt.
     *
     * @param request the restlet request
     * @return the context IDs
     */
    protected Map<String, String> getContextsForAccessAttempt(Request request) {
        return getAllAvailableContexts();
    }

    /**
     * Retrieve the user ID for an access outcome.
     *
     * @param response the restlet response
     * @return the user ID
     */
    protected String getUserIdForAccessOutcome(Response response) {
        String userId = AuditRequestContext.getProperty(AuditConstants.USER_ID);
        return userId == null ? "" : userId;
    }

    /**
     * Retrieve the Context IDs for an access outcome.
     *
     * @param response the restlet response
     * @return the context IDs
     */
    protected Map<String, String> getContextsForAccessOutcome(Response response) {
        return getAllAvailableContexts();
    }

}
