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
package com.iplanet.services.comm.server;

import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILURE;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.TimeUnit.MILLISECONDS;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.Context.SESSION;

import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;

import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for publishing audit access events for individual PLL request.
 */
public class PLLAuditor {

    public static final String PLL = "PLL";

    private final Debug debug;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final HttpServletRequest httpServletRequest;

    private long startTime;
    private String service;
    private String method;
    private String contextId;
    private String authenticationId;
    private Component component;
    private boolean accessAttemptAudited;

    /**
     * Create a new Auditor.
     * @param debug               Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory   AuditEventFactory for audit event builders.
     * @param httpServletRequest
     */
    public PLLAuditor(Debug debug, AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
                      HttpServletRequest httpServletRequest) {
        this.debug = debug;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.httpServletRequest = httpServletRequest;
        this.service = "unknown";
        this.reset();
    }

    /**
     * Publishes an audit event with details of the attempted CREST operation, if the 'access' topic is audited.
     *
     * @throws AuditException If an exception occurred that prevented the audit event from being published.
     */
    public void auditAccessAttempt() {
        if (auditEventPublisher.isAuditing(DEFAULT_AUDIT_REALM, ACCESS_TOPIC)) {

            AuditEvent auditEvent = auditEventFactory.accessEvent(DEFAULT_AUDIT_REALM)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(startTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(Component.PLL)
                    .authentication(authenticationId)
                    .resourceOperation(service, PLL, method)
                    .context(SESSION, contextId)
                    .toEvent();
            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
        }
        accessAttemptAudited = true;
    }

    /**
     * Publishes an event with details of the successfully completed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     */
    public void auditAccessSuccess() {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(DEFAULT_AUDIT_REALM, ACCESS_TOPIC)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AuditEvent auditEvent = auditEventFactory.accessEvent(DEFAULT_AUDIT_REALM)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(Component.PLL)
                    .response(SUCCESS, "", elapsedTime, MILLISECONDS)
                    .authentication(authenticationId)
                    .resourceOperation(service, PLL, method)
                    .context(SESSION, contextId)
                    .toEvent();

            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
            reset();
        }
    }

    /**
     * Publishes an event with details of the failed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     *
     * @param message   A human-readable description of the error that occurred.
     */
    public void auditAccessFailure(String message) {
        auditAccessFailure(null, message);
    }

    /**
     * Publishes an event with details of the failed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     *
     * @param errorCode A unique code that identifies the error condition.
     * @param message   A human-readable description of the error that occurred.
     */
    public void auditAccessFailure(String errorCode, String message) {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(DEFAULT_AUDIT_REALM, ACCESS_TOPIC)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AuditEvent auditEvent = auditEventFactory.accessEvent(DEFAULT_AUDIT_REALM)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(Component.PLL)
                    .responseWithDetail(FAILURE, errorCode == null ? "" : errorCode, elapsedTime, MILLISECONDS, message)
                    .authentication(authenticationId)
                    .resourceOperation(service, PLL, method)
                    .context(SESSION, contextId)
                    .toEvent();

            auditEventPublisher.tryPublish(ACCESS_TOPIC, auditEvent);
            reset();
        }
    }

    /**
     * Resets the auditor in preparation for handling the next {@link Request} in a given {@link RequestSet}.
     */
    private void reset() {
        accessAttemptAudited = false;
        startTime = System.currentTimeMillis();
        method = "unknown";
        authenticationId = "";
        contextId = "";
        component = Component.PLL;
    }

    /**
     * @param service Identifies the {@link RequestHandler} invoked.
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * @param component Identifies the functional area of OpenAM with which this PLL service interacts.
     */
    public void setComponent(Component component) {
        this.component = component;
    }

    /**
     * @param method Identifies the {@link RequestHandler} operation invoked.
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Provide SSOToken of originating client in order to lookup session contextId and realm.
     *
     * If the current server is not the 'home server' for the session, obtaining an SSOToken can itself
     * lead to PLL communication between servers; therefore, it's worth considering whether or not this
     * method should be used on a case-by-case basis. When obtaining an SSOToken may not be appropriate,
     * the setDomain and setContextId methods may be useful alternatives if this information is available
     * via other means.
     *
     * @param ssoToken SSOToken of the originating client from which the session contextId and realm are obtained.
     */
    public void setSsoToken(SSOToken ssoToken) {
        this.contextId = getContextFromSSOToken(ssoToken);
        this.authenticationId = getUserId(ssoToken);
    }

    /**
     * @param contextId Unique alias of session.
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * @param authenticationId Identifies Subject of authentication.
     */
    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }
}
