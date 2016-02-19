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
package org.forgerock.openam.rest.audit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getAllAvailableTrackingIds;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.rest.service.RestletRealmRouter.REALM;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.Time.*;
import static org.restlet.ext.servlet.ServletUtils.getRequest;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.representation.BufferingRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;

import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for logging access audit events for restlet requests.
 *
 * @since 13.0.0
 */
public abstract class AbstractRestletAccessAuditFilter extends Filter {

    private static Debug debug = Debug.getInstance("amAudit");

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Component component;
    private final RestletBodyAuditor<?> requestDetailCreator;
    private final RestletBodyAuditor<?> responseDetailCreator;

    /**
     * Create a new filter for the given component and restlet.
     *  @param component The component for which events will be logged.
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param requestDetailCreator
     * @param responseDetailCreator
     */
    public AbstractRestletAccessAuditFilter(Component component, Restlet restlet,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
            RestletBodyAuditor<?> requestDetailCreator, RestletBodyAuditor<?> responseDetailCreator) {
        this.requestDetailCreator = requestDetailCreator;
        this.responseDetailCreator = responseDetailCreator;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.component = component;
        setNext(restlet);
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        try {
            Representation representation = request.getEntity();
            // If the representation is transient we can only read its entity once, so we have to wrap it in a
            // buffer in order to read from it during the event logging and later during authentication
            if (representation.isTransient()) {
                request.setEntity(new BufferingRepresentation(request.getEntity()));
            }
            auditAccessAttempt(request);
        } catch (AuditException e) {
            debug.error("Unable to publish {} audit event '{}' due to error: {} [{}]",
                    ACCESS_TOPIC, EventName.AM_ACCESS_ATTEMPT, e.getMessage(), e);
        }
        return CONTINUE;
    }

    @Override
    protected void afterHandle(Request request, Response response) {
        super.afterHandle(request, response);

        if (response.getStatus().isError()) {
            auditAccessFailure(request, response);
        } else {
            auditAccessSuccess(request, response);
        }
    }

    private void auditAccessAttempt(Request request) throws AuditException {
        String realm = getRealmFromRequest(request);
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_ATTEMPT)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(request.getDate().getTime())
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component)
                    .userId(getUserIdForAccessAttempt(request))
                    .trackingIds(getTrackingIdsForAccessAttempt(request));

            if (requestDetailCreator != null) {
                builder.requestDetail(requestDetailCreator.apply(request.getEntity()));
            }

            addHttpData(request, builder);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessSuccess(Request request, Response response) {
        String realm = getRealmFromRequest(request);
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            long endTime = currentTimeMillis();
            long elapsedTime = endTime - request.getDate().getTime();

            final Representation entity = response.getEntity();
            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .userId(getUserIdForAccessOutcome(request, response))
                    .trackingIds(getTrackingIdsForAccessOutcome(request, response));

            JsonValue detail = null;
            if (responseDetailCreator != null) {
                try {
                    detail = responseDetailCreator.apply(entity);
                } catch (AuditException e) {
                    debug.warning("An error occurred when fetching response body details for audit", e);
                }
            }
            if (detail == null) {
                builder.response(SUCCESSFUL, "", elapsedTime, MILLISECONDS);
            } else {
                builder.responseWithDetail(SUCCESSFUL, "", elapsedTime, MILLISECONDS, detail);
            }

            addHttpData(request, builder);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessFailure(Request request, Response response) {
        String realm = getRealmFromRequest(request);
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            long endTime = currentTimeMillis();
            String responseCode = Integer.toString(response.getStatus().getCode());
            long elapsedTime = endTime - request.getDate().getTime();
            JsonValue responseDetail = json(object(
                    field(ACCESS_RESPONSE_DETAIL_REASON, response.getStatus().getDescription())));

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .userId(getUserIdForAccessOutcome(request, response))
                    .trackingIds(getTrackingIdsForAccessOutcome(request, response))
                    .responseWithDetail(FAILED, responseCode, elapsedTime, MILLISECONDS, responseDetail);

            addHttpData(request, builder);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void addHttpData(Request request, AMAccessAuditEventBuilder builder) {
        HttpServletRequest servletRequest = getRequest(request);
        if (servletRequest != null) {
            builder.forHttpServletRequest(servletRequest);
        }
    }

    private String getRealmFromRequest(Request request) {
        String realm = (String) request.getAttributes().get(REALM);
        return isBlank(realm) ? NO_REALM : realm;
    }

    /**
     * Retrieve the user ID for an access attempt.
     *
     * @param request the restlet request
     * @return the user ID
     */
    protected String getUserIdForAccessAttempt(Request request) {
        String userId = AuditRequestContext.getProperty(USER_ID);
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
    protected String getUserIdForAccessOutcome(Request request, Response response) {
        String userId = AuditRequestContext.getProperty(USER_ID);
        return userId == null ? "" : userId;
    }

    /**
     * Retrieve the tracking IDs for an access outcome.
     *
     * @param response the restlet response
     * @return the tracking IDs
     */
    protected Set<String> getTrackingIdsForAccessOutcome(Request request, Response response) {
        return getAllAvailableTrackingIds();
    }
}
