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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.service;

import static com.sun.identity.shared.Constants.UNIVERSAL_IDENTIFIER;
import static org.forgerock.openam.ldap.LDAPUtils.rdnValueFromDn;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.session.AMSession;
import org.forgerock.openam.session.SessionConstants;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.AuthenticationSessionStore;
import com.iplanet.dpro.session.service.DsameAdminTokenProvider;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionFactory;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.service.SessionType;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;

/**
 * Factory for creating and caching an Authentication Session which is used by the authentication framework.
 */
@Singleton
public class AuthSessionFactory {

    private SSOToken authSession; // cached auth session
    private final Debug sessionDebug;
    private final AuthenticationSessionStore authenticationSessionStore;
    private final InternalSessionFactory internalSessionFactory;
    private final DsameAdminTokenProvider dsameAdminTokenProvider;

    @Inject
    public AuthSessionFactory(final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                              AuthenticationSessionStore authenticationSessionStore,
                              final InternalSessionFactory internalSessionFactory,
                              final DsameAdminTokenProvider dsameAdminTokenProvider) {
        this.sessionDebug = sessionDebug;
        this.authenticationSessionStore = authenticationSessionStore;
        this.internalSessionFactory = internalSessionFactory;
        this.dsameAdminTokenProvider = dsameAdminTokenProvider;
    }

    /**
     * Returns the Internal Session used by the Auth Services.
     *
     * Will create this session if it has not already been created.
     *
     * @param domain      Authentication Domain
     * @return Non null Authentication SSO Token
     * @throws Exception If there was any unexpected error which prevented the token from being generated.
     */
    public SSOToken getAuthenticationSession(String domain) throws SSOException, SessionException {
        try {
            if (authSession == null) {
                // Create a special InternalSession for Authentication Service
                authSession = initSsoAuthSession(initAuthSession(domain));
            }
            return authSession;
        } catch (SSOException | SessionException e) {
            sessionDebug.error("Error creating service session", e);
            throw e;
        }
    }

    private InternalSession initAuthSession(String domain) throws SSOException, SessionException {
        InternalSession session = internalSessionFactory.newInternalSession(domain, false);
        session.setType(SessionType.APPLICATION);

        String clientID = dsameAdminTokenProvider.getDsameAdminDN();
        session.setClientID(clientID);
        session.setClientDomain(domain);
        session.setNonExpiring();
        session.setState(SessionState.VALID);

        session.putProperty(ISAuthConstants.PRINCIPAL, clientID);
        session.putProperty(ISAuthConstants.ORGANIZATION, domain);
        session.putProperty(ISAuthConstants.HOST, session.getID().getSessionServer());

        String id = "id=" + rdnValueFromDn(clientID) + ",ou=user," + ServiceManager.getBaseDN();
        session.putProperty(UNIVERSAL_IDENTIFIER, id);

        authenticationSessionStore.promoteSession(session.getSessionID());

        return session;
    }

    private SSOToken initSsoAuthSession(AMSession authSession) throws SSOException, SessionException {
        SSOTokenManager ssoManager = SSOTokenManager.getInstance();
        return ssoManager.createSSOToken(authSession.getID().toString());
    }
}
