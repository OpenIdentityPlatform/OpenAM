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

import static com.sun.identity.authentication.util.ISAuthConstants.*;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.*;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getTrackingIdFromSSOToken;
import static org.forgerock.openam.audit.AuditConstants.AUTHENTICATION_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.*;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.*;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.AUTH_LEVEL;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.audit.context.AuditRequestContext.getTransactionIdValue;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.common.DNUtils;
import org.forgerock.openam.audit.AMAuditEventBuilderUtils;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.utils.CollectionUtils;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import java.security.Principal;

/**
 * This auditor is specifically aimed at constructing and logging authentication events for the login process.
 *
 * @since 13.0.0
 */
public class AuthenticationProcessEventAuditor extends AbstractAuthenticationEventAuditor {

    /**
     * Constructor for {@link AuthenticationProcessEventAuditor}.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    @Inject
    public AuthenticationProcessEventAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        super(eventPublisher, eventFactory);
    }

    /**
     * Log an authentication process successful completion event.
     *
     * @param loginState The login state object.
     */
    public void auditLoginSuccess(LoginState loginState) {
        String realm = getRealmFromState(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_LOGIN_COMPLETED)) {
            String moduleName = null;
            String userDN = null;
            if (loginState != null) {
                moduleName = loginState.getAuthModuleNames();
                userDN = loginState.getUserDN();
            }

            AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent(realm)
                    .transactionId(getTransactionIdValue())
                    .component(AUTHENTICATION)
                    .eventName(AM_LOGIN_COMPLETED)
                    .result(SUCCESSFUL)
                    .entry(getAuditEntryDetail(moduleName, loginState))
                    .trackingIds(getTrackingIds(loginState))
                    .userId(userDN == null ? "" : userDN)
                    .principal(DNUtils.DNtoName(userDN));

            eventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
        }
    }

    /**
     * Log an authentication process failure event.
     *
     * @param loginState The login state object.
     */
    public void auditLoginFailure(LoginState loginState) {
        auditLoginFailure(loginState, findFailureReason(loginState));
    }

    /**
     * Log an authentication process failure event.
     *
     * @param loginState The login state object.
     * @param failureReason The reason for the failure. If {@literal failureReason} is null then the value of
     * {@link LoginState#getErrorCode()} will be mapped to an {@link AuthenticationFailureReason} with
     * {@link AuthenticationFailureReason#LOGIN_FAILED} as default if the value could not be mapped.
     */
    public void auditLoginFailure(LoginState loginState, AuthenticationFailureReason failureReason) {
        String realm = getRealmFromState(loginState);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_LOGIN_COMPLETED)) {
            String principal = getFailedPrincipal(loginState);
            String moduleName = loginState == null ? null : loginState.getFailureModuleNames();
            AuthenticationAuditEntry entryDetail = getAuditEntryDetail(moduleName, loginState);
            if (failureReason == null) {
                failureReason = findFailureReason(loginState);
            }
            entryDetail.addInfo(FAILURE_REASON, failureReason.name());

            AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent(realm)
                    .transactionId(getTransactionIdValue())
                    .component(AUTHENTICATION)
                    .eventName(AM_LOGIN_COMPLETED)
                    .result(FAILED)
                    .entry(entryDetail)
                    .trackingIds(getTrackingIds(loginState))
                    .userId(getUserId(principal, realm))
                    .principal(principal);

            eventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
        }
    }

    /**
     * Log a logout event.
     *
     * @param token The {@Link SSOToken} of the event.
     */
    public void auditLogout(SSOToken token) {
        String realm = getRealmFromToken(token);

        if (eventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_LOGOUT)) {
            String principalName;
            try {
                Principal principal = token == null ? null : token.getPrincipal();
                principalName = principal == null ? null : DNUtils.DNtoName(principal.getName());
            } catch (SSOException e) {
                principalName = null;
            }

            AuthenticationAuditEntry entryDetail = new AuthenticationAuditEntry();
            entryDetail.setModuleId(getSSOTokenProperty(token, AUTH_TYPE));

            String host = getSSOTokenProperty(token, HOST);
            if (isNotEmpty(host)) {
                entryDetail.addInfo(IP_ADDRESS, host);
            }

            String trackingId = getTrackingIdFromSSOToken(token);
            String userId = AMAuditEventBuilderUtils.getUserId(token);

            AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent(realm)
                    .transactionId(getTransactionIdValue())
                    .component(AUTHENTICATION)
                    .eventName(AM_LOGOUT)
                    .result(SUCCESSFUL)
                    .entry(entryDetail)
                    .trackingId(trackingId == null ? "" : trackingId)
                    .userId(userId == null ? "" : userId)
                    .principal(principalName);

            eventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
        }
    }

    private AuthenticationAuditEntry getAuditEntryDetail(String moduleName, LoginState loginState) {
        AuthenticationAuditEntry entryDetail = new AuthenticationAuditEntry();
        entryDetail.setModuleId(moduleName == null ? "" : moduleName);

        if (loginState != null) {
            String ip = loginState.getClient();
            if (isNotEmpty(ip)) {
                entryDetail.addInfo(IP_ADDRESS, ip);
            }
            AuthContext.IndexType indexType = loginState.getIndexType();
            if (indexType != null) {
                entryDetail.addInfo(AUTH_INDEX, indexType.toString());
            }
            entryDetail.addInfo(AUTH_LEVEL, String.valueOf(loginState.getAuthLevel()));
        }

        return entryDetail;
    }

    private String getSSOTokenProperty(SSOToken ssoToken, String name) {
        try {
            return (ssoToken == null || name == null) ? null : ssoToken.getProperty(name);
        } catch (SSOException e) {
            return null;
        }
    }

    private String getFailedPrincipal(LoginState loginState) {
        if (loginState == null) {
            return null;
        }

        String principal = loginState.getUserDN();
        if (principal != null) {
            return DNUtils.DNtoName(principal);
        }

        principal = loginState.getFailureTokenId();
        if (principal != null) {
            return principal;
        }

        if (CollectionUtils.isNotEmpty(loginState.getAllReceivedCallbacks())) {
            for (Callback[] cb : loginState.getAllReceivedCallbacks().values()) {
                for (Callback aCb : cb) {
                    if (aCb instanceof NameCallback) {
                        return ((NameCallback) aCb).getName();
                    }
                }
            }
        }
        return null;
    }

    private AuditConstants.AuthenticationFailureReason findFailureReason(LoginState loginState) {
        String errorCode = loginState == null ? null : loginState.getErrorCode();

        if (errorCode == null) {
            return LOGIN_FAILED;
        }

        switch (errorCode) {
            case AMAuthErrorCode.AUTH_PROFILE_ERROR:
                return NO_USER_PROFILE;
            case AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED:
                return ACCOUNT_EXPIRED;
            case AMAuthErrorCode.AUTH_INVALID_PASSWORD:
                return INVALID_PASSWORD;
            case AMAuthErrorCode.AUTH_USER_INACTIVE:
                return USER_INACTIVE;
            case AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND:
                return NO_CONFIG;
            case AMAuthErrorCode.AUTH_INVALID_DOMAIN:
                return INVALID_REALM;
            case AMAuthErrorCode.AUTH_ORG_INACTIVE:
                return REALM_INACTIVE;
            case AMAuthErrorCode.AUTH_TIMEOUT:
                return LOGIN_TIMEOUT;
            case AMAuthErrorCode.AUTH_MODULE_DENIED:
                return MODULE_DENIED;
            case AMAuthErrorCode.AUTH_USER_LOCKED:
                return LOCKED_OUT;
            case AMAuthErrorCode.AUTH_USER_NOT_FOUND:
                return USER_NOT_FOUND;
            case AMAuthErrorCode.AUTH_TYPE_DENIED:
                return AUTH_TYPE_DENIED;
            case AMAuthErrorCode.AUTH_MAX_SESSION_REACHED:
                return MAX_SESSION_REACHED;
            case AMAuthErrorCode.AUTH_SESSION_CREATE_ERROR:
                return SESSION_CREATE_ERROR;
            case AMAuthErrorCode.INVALID_AUTH_LEVEL:
                return INVALID_LEVEL;
            case AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED:
                return MODULE_DENIED;
            default:
                return LOGIN_FAILED;
        }
    }
}
