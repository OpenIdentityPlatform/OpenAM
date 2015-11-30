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
package org.forgerock.openam.saml2.audit;

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.StringUtils.*;

import com.iplanet.sso.SSOToken;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AMAuditEventBuilderUtils;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for publishing audit access events for individual SAML2 requests.  A SAML2Auditor is not thread safe
 * and a new SAML2Auditor should be used for each request.
 */
public class SAML2Auditor implements SAML2EventLogger {

    private static final String PROXY_MESSAGE = "Forwarding request to a proxy";
    private static final String LOCAL_USER_LOGIN_MESSAGE = "Forwarding request to local user login";

    private String trackingId;
    private String userId;
    private String realm;
    private String method;

    private boolean accessAttemptAudited = false;
    private long startTime;

    private final HttpServletRequest request;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private String message;

    private String SSOTokenId;
    private String authnRequestId;
    private String authTokenId;

    /**
     * Constructor for SAML2Auditor
     *
     * @param auditEventPublisher The AuditEventPublisher
     * @param auditEventFactory The AuditEventFactory
     * @param request The HttpServletReqeust associated with the SAML2 request
     */
    public SAML2Auditor(final AuditEventPublisher auditEventPublisher,
                        final AuditEventFactory auditEventFactory, final HttpServletRequest request) {
        this.request = request;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void auditAccessAttempt() {
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_ATTEMPT)) {

            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(startTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_ATTEMPT)
                    .toEvent();
            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
        accessAttemptAudited = true;
    }

    private Set<String> collateTrackingIds() {
        Set<String> trackingIds = new HashSet<>(AMAuditEventBuilderUtils.getAllAvailableTrackingIds());
        if (StringUtils.isNotEmpty(trackingId)) {
            trackingIds.add(trackingId);
        }
        if (StringUtils.isNotEmpty(SSOTokenId)) {
            trackingIds.add(SSOTokenId);
        }
        if (StringUtils.isNotEmpty(authTokenId)) {
            trackingIds.add(authTokenId);
        }
        if (StringUtils.isNotEmpty(authnRequestId)) {
            trackingIds.add(authnRequestId);
        }
        return trackingIds;
    }


    @Override
    public void auditAccessSuccess() {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;

            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(endTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME)
                    .response(SUCCESSFUL, message, elapsedTime, MILLISECONDS)
                    .toEvent();
            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
    }

    @Override
    public void auditAccessFailure(String errorCode, String message) {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            final JsonValue detail = json(object(field(AuditConstants.ACCESS_RESPONSE_DETAIL_REASON, message)));
            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(endTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME)
                    .responseWithDetail(FAILED, errorCode, elapsedTime, MILLISECONDS, detail)
                    .toEvent();

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
    }

    private AMAccessAuditEventBuilder getDefaultSAML2AccessAuditEventBuilder() {
        return auditEventFactory.accessEvent(realm)
                .forHttpServletRequest(request)
                .transactionId(AuditRequestContext.getTransactionIdValue())
                .component(AuditConstants.Component.SAML2)
                .userId(userId)
                .request(AuditConstants.Component.SAML2.toString(), method)
                .trackingIds(collateTrackingIds());
    }

    @Override
    public void setSessionTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void setRealm(String realm) {
        this.realm = isEmpty(realm) ? null : realm;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public void auditForwardToProxy() {
        this.message = PROXY_MESSAGE;
        auditAccessSuccess();
    }

    @Override
    public void auditForwardToLocalUserLogin() {
        this.message = LOCAL_USER_LOGIN_MESSAGE;
        auditAccessSuccess();
    }

    @Override
    public void setRequestId(String authnRequestId) {
        this.authnRequestId = authnRequestId;
    }

    @Override
    public void setSSOTokenId(Object session) {
        if (null != session && session instanceof SSOToken) {
            this.SSOTokenId = AMAuditEventBuilderUtils.getTrackingIdFromSSOToken((SSOToken) session);
        }
    }

    @Override
    public void setAuthTokenId(Object session) {
        if (null != session && session instanceof SSOToken) {
            this.authTokenId = AMAuditEventBuilderUtils.getTrackingIdFromSSOToken((SSOToken) session);
        }
    }
}
