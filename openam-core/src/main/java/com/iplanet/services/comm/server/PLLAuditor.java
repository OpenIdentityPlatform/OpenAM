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
package com.iplanet.services.comm.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.StringUtils.*;
import static org.forgerock.openam.utils.Time.*;

import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
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
    private String method;
    private String trackingId;
    private String userId;
    private String realm;
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
        this.reset();
    }

    /**
     * Publishes an audit event with details of the attempted CREST operation, if the 'access' topic is audited.
     *
     * @throws AuditException If an exception occurred that prevented the audit event from being published.
     */
    public void auditAccessAttempt() {
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_ATTEMPT)) {

            AuditEvent auditEvent = auditEventFactory.accessEvent(realm)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(startTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_ATTEMPT)
                    .component(component)
                    .userId(userId)
                    .request(PLL, method)
                    .trackingId(trackingId)
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
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            AuditEvent auditEvent = auditEventFactory.accessEvent(realm)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .response(SUCCESSFUL, "", elapsedTime, MILLISECONDS)
                    .userId(userId)
                    .request(PLL, method)
                    .trackingId(trackingId)
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
        if (auditEventPublisher.isAuditing(realm, ACCESS_TOPIC, EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            final JsonValue detail = json(object(field(ACCESS_RESPONSE_DETAIL_REASON, message)));
            AuditEvent auditEvent = auditEventFactory.accessEvent(realm)
                    .forHttpServletRequest(httpServletRequest)
                    .timestamp(endTime)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(EventName.AM_ACCESS_OUTCOME)
                    .component(component)
                    .responseWithDetail(FAILED, errorCode, elapsedTime, MILLISECONDS, detail)
                    .userId(userId)
                    .request(PLL, method)
                    .trackingId(trackingId)
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
        startTime = currentTimeMillis();
        method = "unknown";
        userId = "";
        trackingId = "";
        component = null;
        realm = NO_REALM;
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
     * Provide SSOToken of originating client in order to lookup session trackingId and realm.
     *
     * If the current server is not the 'home server' for the session, obtaining an SSOToken can itself
     * lead to PLL communication between servers; therefore, it's worth considering whether or not this
     * method should be used on a case-by-case basis. When obtaining an SSOToken may not be appropriate,
     * the setDomain and setTrackingId methods may be useful alternatives if this information is available
     * via other means.
     *
     * @param ssoToken SSOToken of the originating client from which the session trackingId and realm are obtained.
     */
    public void setSsoToken(SSOToken ssoToken) {
        this.trackingId = getTrackingIdFromSSOToken(ssoToken);
        this.userId = getUserId(ssoToken);
    }

    /**
     * @param trackingId Unique alias of session.
     */
    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    /**
     * @param userId Identifies Subject of authentication.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @param realm The realm for which the event is being logged.
     */
    public void setRealm(String realm) {
        this.realm = isEmpty(realm) ? NO_REALM : DNMapper.orgNameToRealmName(realm);
    }
}
