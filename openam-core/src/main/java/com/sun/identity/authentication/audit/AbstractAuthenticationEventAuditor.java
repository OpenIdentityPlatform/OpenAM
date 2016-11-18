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
package com.sun.identity.authentication.audit;

import static com.sun.identity.authentication.util.ISAuthConstants.SHARED_STATE_USERNAME;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.forgerock.openam.audit.AuditConstants.NO_REALM;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.DNMapper;

import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

/**
 * Abstract auditor for constructing and logging authentication events.
 *
 * @since 13.0.0
 */
public abstract class AbstractAuthenticationEventAuditor {

    protected final AuditEventPublisher eventPublisher;
    protected final AuditEventFactory eventFactory;

    /**
     * Constructor for {@link AbstractAuthenticationEventAuditor}.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    public AbstractAuthenticationEventAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        this.eventFactory = eventFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get the universal user ID.
     *
     * @param principalName The principal name.
     * @param realm The realm.
     * @return The universal user ID or an empty string if it could not be found.
     */
    protected String getUserId(String principalName, String realm) {
        if (isNotEmpty(principalName) && isNotEmpty(realm)) {
            AMIdentity identity = IdUtils.getIdentity(principalName, realm);
            if (identity != null) {
                return identity.getUniversalId();
            }
        }
        return "";
    }

    /**
     * Get the tracking ID from the login state of the event.
     *
     * @param loginState The login state of the event.
     * @return The tracking ID or an empty string if it could not be found.
     */
    protected Set<String> getTrackingIds(LoginState loginState) {
        InternalSession session = loginState == null ? null : loginState.getSession();
        String sessionContext = session == null ? null : session.getProperty(Constants.AM_CTX_ID);
        return sessionContext == null ? Collections.<String>emptySet() : singleton(sessionContext);
    }

    /**
     * Get the realm from the login state of the event.
     *
     * @param loginState The login state of the event.
     * @return The realm or null if it could not be found.
     */
    protected String getRealmFromState(LoginState loginState) {
        String orgDN = loginState == null ? null : loginState.getOrgDN();
        return orgDN == null ? NO_REALM : DNMapper.orgNameToRealmName(orgDN);
    }

    /**
     * Get the realm from the {@Link SSOToken} of the event.
     *
     * @param token The {@Link SSOToken} of the event.
     * @return The realm or null if it could not be found.
     */
    protected String getRealmFromToken(SSOToken token) {
        try {
            String orgDN = token == null ? null : token.getProperty(ISAuthConstants.ORGANIZATION);
            return orgDN == null ? NO_REALM : DNMapper.orgNameToRealmName(orgDN);
        } catch (SSOException e) {
            return NO_REALM;
        }
    }

    /**
     * Get the failed username from the {@Link LoginState} of the event.
     *
     * @param loginState The login state of the event.
     * @return The username or null if it could not be found.
     */
    protected String getFailedPrincipal(LoginState loginState) {
        if (loginState != null) {
            String principal = loginState.getUserDN();
            if (StringUtils.isNotEmpty(principal)) {
                return DNUtils.DNtoName(principal);
            }

            Map sharedState = loginState == null ? emptyMap() : loginState.getSharedState();
            if (CollectionUtils.isNotEmpty(sharedState)) {
                principal = (String) sharedState.get(SHARED_STATE_USERNAME);
                if (StringUtils.isNotEmpty(principal)) {
                    return principal;
                }
            }

            principal = loginState.getFailureTokenId();
            if (StringUtils.isNotEmpty(principal)) {
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
        }
        return null;
    }
}
