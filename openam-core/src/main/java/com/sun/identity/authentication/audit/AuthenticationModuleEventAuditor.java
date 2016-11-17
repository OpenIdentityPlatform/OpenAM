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
package com.sun.identity.authentication.audit;

import static com.sun.identity.authentication.util.ISAuthConstants.SHARED_STATE_USERNAME;
import static java.util.Collections.emptyMap;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.*;
import static org.forgerock.openam.audit.AuditConstants.AUTHENTICATION_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.AUTH_CONTROL_FLAG;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_LOGIN_MODULE_COMPLETED;
import static org.forgerock.openam.audit.AuditConstants.LOGIN_MODULE_CONTROL_FLAG;
import static org.forgerock.openam.audit.context.AuditRequestContext.getTransactionIdValue;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.sun.identity.authentication.service.LoginState;
import org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Map;

/**
 * This auditor is specifically aimed at constructing and logging authentication events for login modules.
 *
 * @since 13.0.0
 */
public class AuthenticationModuleEventAuditor extends AbstractAuthenticationEventAuditor {

    /**
     * Constructor for {@link AuthenticationModuleEventAuditor}.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    @Inject
    public AuthenticationModuleEventAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        super(eventPublisher, eventFactory);
    }

    /**
     * Log an authentication module successful completion event.
     *
     * @param loginState The login state object.
     * @param principal The principal for this module.
     * @param auditEntryDetail A map containing audit entry details.
     */
    public void auditModuleSuccess(LoginState loginState, Principal principal,
            AuthenticationAuditEntry auditEntryDetail) {

        String realm = getRealmFromState(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_LOGIN_MODULE_COMPLETED)) {
            String principalName = principal == null ? null : principal.getName();
            String authId = getUserId(principalName, realm);
            auditModuleEvent(loginState, realm, principalName, authId, SUCCESSFUL, auditEntryDetail);
        }
    }

    /**
     * Log an authentication module failure completion event.
     *
     * @param loginState The login state object.
     * @param principal The principal for this module.
     * @param auditEntryDetail A map containing audit entry details.
     */
    public void auditModuleFailure(LoginState loginState, Principal principal,
            AuthenticationAuditEntry auditEntryDetail) {

        String realm = getRealmFromState(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_LOGIN_MODULE_COMPLETED)) {
            String principalName = principal == null ? null : principal.getName();
            Map sharedState = loginState == null ? emptyMap() : loginState.getSharedState();
            String authId = getUserId(principalName, realm);

            if ((isEmpty(principalName) || isEmpty(authId)) && sharedState.containsKey(SHARED_STATE_USERNAME)) {
                principalName = (String) sharedState.get(SHARED_STATE_USERNAME);
                authId = getUserId(principalName, realm);
            }

            auditModuleEvent(loginState, realm, principalName, authId, FAILED, auditEntryDetail);
        }
    }

    private void auditModuleEvent(LoginState loginState, String realm, String principal, String userId,
            Status result, AuthenticationAuditEntry auditEntryDetail) {

        String controlFlag = AuditRequestContext.getProperty(LOGIN_MODULE_CONTROL_FLAG);
        if (auditEntryDetail != null && isNotEmpty(controlFlag)) {
            auditEntryDetail.addInfo(AUTH_CONTROL_FLAG, controlFlag);
        }

        AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent(realm)
                .transactionId(getTransactionIdValue())
                .component(AUTHENTICATION)
                .eventName(AM_LOGIN_MODULE_COMPLETED)
                .result(result)
                .entry(auditEntryDetail)
                .trackingIds(getTrackingIds(loginState))
                .userId(userId)
                .principal(principal);

        eventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
    }
}
