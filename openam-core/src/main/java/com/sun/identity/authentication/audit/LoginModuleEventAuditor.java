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
import static java.util.Collections.*;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_LOGIN_MODULE_COMPLETED;
import static org.forgerock.openam.audit.context.AuditRequestContext.getTransactionIdValue;
import static org.forgerock.openam.utils.StringUtils.*;

import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.DNMapper;
import org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This auditor is specifically aimed at constructing and logging events for login modules.
 *
 * @since 13.0.0
 */
@Singleton
public class LoginModuleEventAuditor {

    protected final AuditEventPublisher eventPublisher;
    protected final AuditEventFactory eventFactory;

    /**
     * Constructor for {@link LoginModuleEventAuditor}.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    @Inject
    public LoginModuleEventAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        this.eventFactory = eventFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Log an authentication module successful completion event.
     *
     * @param moduleName Name of the authentication module.
     * @param loginState The login state object.
     * @param principal The principal for this module.
     * @param auditEntryDetail A map containing audit entry details.
     */
    public void auditAuthenticationSuccess(String moduleName, LoginState loginState, Principal principal,
            Map<String, String> auditEntryDetail) {

        String realm = getRealm(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC)) {
            String principalName = principal == null ? null : principal.getName();
            String authId = getAuthenticationId(principalName, realm);
            auditAuthenticationEvent(moduleName, loginState, realm, principalName, authId, SUCCESSFUL, auditEntryDetail);
        }
    }

    /**
     * Log an authentication module failure completion event.
     *
     * @param moduleName Name of the authentication module.
     * @param loginState The login state object.
     * @param principal The principal for this module.
     * @param auditEntryDetail A map containing audit entry details.
     */
    public void auditAuthenticationFailure(String moduleName, LoginState loginState, Principal principal,
            Map<String, String> auditEntryDetail) {

        String realm = getRealm(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC)) {
            String principalName = principal == null ? null : principal.getName();
            Map sharedState = loginState.getSharedState();
            String authId = getAuthenticationId(principalName, realm);

            if ((isEmpty(principalName) || isEmpty(authId)) && sharedState.containsKey(SHARED_STATE_USERNAME)) {
                principalName = (String) sharedState.get(SHARED_STATE_USERNAME);
                authId = getAuthenticationId(principalName, realm);
            }

            auditAuthenticationEvent(moduleName, loginState, realm, principalName, authId, FAILED, auditEntryDetail);
        }
    }

    private void auditAuthenticationEvent(String moduleName, LoginState loginState, String realm, String principal,
            String authentication, Status result, Map<String, String> auditEntryDetail) {

        AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent()
                .transactionId(getTransactionIdValue())
                .component(AuditConstants.Component.AUTHENTICATION)
                .eventName(AM_LOGIN_MODULE_COMPLETED)
                .result(result)
                .realm(realm)
                .entryList(getAuditEntries(moduleName, auditEntryDetail))
                .trackingIds(getTrackingIds(loginState))
                .authentication(authentication)
                .principal(principal);

        eventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
    }

    private String getAuthenticationId(String principalName, String realm) {
        if (isNotEmpty(principalName) && isNotEmpty(realm)) {
            AMIdentity identity = IdUtils.getIdentity(principalName, realm);
            if (identity != null) {
                return identity.getUniversalId();
            }
        }
        return "";
    }

    private Set<String> getTrackingIds(LoginState loginState) {
        InternalSession session = loginState.getSession();
        String sessionContext = null;
        if (session != null) {
            sessionContext = session.getProperty(Constants.AM_CTX_ID);
        }
        if (isNotEmpty(sessionContext)) {
            return singleton(sessionContext);
        }

        return emptySet();
    }

    private String getRealm(LoginState loginState) {
        String orgDN = loginState.getOrgDN();
        return orgDN == null ? NO_REALM : DNMapper.orgNameToRealmName(orgDN);
    }

    protected List<AuthenticationAuditEntry> getAuditEntries(String moduleName, Map<String, String> auditEntryDetail) {
        AuthenticationAuditEntry authenticationAuditEntry = new AuthenticationAuditEntry();
        authenticationAuditEntry.setModuleId(moduleName);
        authenticationAuditEntry.setInfo(auditEntryDetail);
        return singletonList(authenticationAuditEntry);
    }
}
