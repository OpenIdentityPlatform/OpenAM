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
package org.forgerock.openam.rest.audit;

import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.TimeUnit.MILLISECONDS;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getAllAvailableContexts;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.rest.service.RestletRealmRouter.REALM;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.restlet.ext.servlet.ServletUtils.getRequest;

import org.forgerock.audit.AuditException;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Responsible for logging access audit events for restlet requests.
 *
 * @since 13.0.0
 */
public abstract class AbstractRestletAccessAuditFilter extends Filter {

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Component component;

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param component The component for which events will be logged.
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     */
    public AbstractRestletAccessAuditFilter(Component component, Restlet restlet,
                                            AuditEventPublisher auditEventPublisher,
                                            AuditEventFactory auditEventFactory) {
        setNext(restlet);
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.component = component;
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        try {
            Representation representation = request.getEntity();
            // If the representation is transient we can only read it's entity once, so we have to wrap it in a
            // buffer in order to read from it during the event logging and later during authentication
            if (representation.isTransient()) {
                request.setEntity(new BufferingRepresentation(request.getEntity()));
            }
            auditAccessAttempt(request);
        } catch (AuditException e) {
            response.setStatus(Status.SERVER_ERROR_INTERNAL, e);
            return STOP;
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
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC)) {

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(request.getDate().getTime())
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component)
                    .authentication(getUserIdForAccessAttempt(request))
                    .contexts(getContextsForAccessAttempt(request));

            addHttpData(request, builder);

            auditEventPublisher.publish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessSuccess(Request request, Response response) {
        String realm = getRealmFromRequest(request);
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC)) {

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - request.getDate().getTime();

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .authentication(getUserIdForAccessOutcome(request, response))
                    .contexts(getContextsForAccessOutcome(request, response))
                    .response(SUCCESS, "", elapsedTime, MILLISECONDS);

            addHttpData(request, builder);

            auditEventPublisher.tryPublish(ACCESS_TOPIC, builder.toEvent());
        }
    }

    private void auditAccessFailure(Request request, Response response) {
        String realm = getRealmFromRequest(request);
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC)) {

            long endTime = System.currentTimeMillis();
            String responseCode = Integer.toString(response.getStatus().getCode());
            long elapsedTime = endTime - request.getDate().getTime();
            String responseDetail = response.getStatus().getDescription();

            AMAccessAuditEventBuilder builder = auditEventFactory.accessEvent(realm)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .authentication(getUserIdForAccessOutcome(request, response))
                    .contexts(getContextsForAccessOutcome(request, response))
                    .responseWithDetail(FAILURE, responseCode, elapsedTime, MILLISECONDS, responseDetail);

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
    protected String getUserIdForAccessOutcome(Request request, Response response) {
        String userId = AuditRequestContext.getProperty(USER_ID);
        return userId == null ? "" : userId;
    }

    /**
     * Retrieve the Context IDs for an access outcome.
     *
     * @param response the restlet response
     * @return the context IDs
     */
    protected Map<String, String> getContextsForAccessOutcome(Request request, Response response) {
        return getAllAvailableContexts();
    }
}
