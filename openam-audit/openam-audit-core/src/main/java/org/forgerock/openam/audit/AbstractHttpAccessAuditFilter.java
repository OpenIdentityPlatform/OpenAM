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
package org.forgerock.openam.audit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getAllAvailableTrackingIds;
import static org.forgerock.openam.audit.AuditConstants.ACCESS_RESPONSE_DETAIL_REASON;
import static org.forgerock.openam.audit.AuditConstants.Component;
import static org.forgerock.openam.audit.AuditConstants.EventName;
import static org.forgerock.openam.utils.Time.*;

import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

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
        auditAccessAttempt(request, context);
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

    private void auditAccessAttempt(Request request, Context context) {
        String realm = getRealm(context);
        if (auditEventPublisher.isAuditing(realm, AuditConstants.ACCESS_TOPIC, EventName.AM_ACCESS_ATTEMPT)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(context.asContext(RequestAuditContext.class).getRequestReceivedTime())
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component)
                    .userId(getUserIdForAccessAttempt(request))
                    .trackingIds(getTrackingIdsForAccessAttempt(request))
                    .forRequest(request, context);

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessSuccess(Request request, Context context, Response response) {
        String realm = getRealm(context);
        if (auditEventPublisher.isAuditing(realm, AuditConstants.ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            long endTime = currentTimeMillis();
            long elapsedTime = endTime - context.asContext(RequestAuditContext.class).getRequestReceivedTime();

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .userId(getUserIdForAccessOutcome(response))
                    .trackingIds(getTrackingIdsForAccessOutcome(response))
                    .response(SUCCESSFUL, "", elapsedTime, MILLISECONDS)
                    .forRequest(request, context);

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessFailure(Request request, Context context, Response response) {
        String realm = getRealm(context);
        if (auditEventPublisher.isAuditing(realm, AuditConstants.ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            long endTime = currentTimeMillis();
            String responseCode = Integer.toString(response.getStatus().getCode());
            long elapsedTime = endTime - context.asContext(RequestAuditContext.class).getRequestReceivedTime();
            JsonValue responseDetail = json(object(
                    field(ACCESS_RESPONSE_DETAIL_REASON, response.getStatus().getReasonPhrase())));

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .userId(getUserIdForAccessOutcome(response))
                    .trackingIds(getTrackingIdsForAccessOutcome(response))
                    .responseWithDetail(FAILED, responseCode, elapsedTime, MILLISECONDS, responseDetail)
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
     * Retrieve the tracking IDs for an access attempt.
     *
     * @param request the restlet request
     * @return the tracking IDs
     */
    protected Set<String> getTrackingIdsForAccessAttempt(Request request) {
        return getAllAvailableTrackingIds();
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
     * Retrieve the tracking IDs for an access outcome.
     *
     * @param response the restlet response
     * @return the tracking IDs
     */
    protected Set<String> getTrackingIdsForAccessOutcome(Response response) {
        return getAllAvailableTrackingIds();
    }

    /**
     * Get the realm from the request context. If the realm is either {@code null} or empty the event will be published
     * to the default audit service.
     *
     * @param context The request context.
     * @return The realm.
     */
    protected abstract String getRealm(Context context);

}
