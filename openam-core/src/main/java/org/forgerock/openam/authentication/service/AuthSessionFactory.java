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
 * Portions Copyrighted 2024 3A Systems LLC
 */

package org.forgerock.openam.authentication.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.session.SessionConstants;


import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.AuthenticationSessionStore;
import com.iplanet.dpro.session.service.DsameAdminTokenProvider;
import com.iplanet.dpro.session.service.InternalSessionFactory;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

/**
 * Factory for creating and caching an Authentication Session which is used by the authentication framework.
 */
@Singleton
public class AuthSessionFactory {

    private SSOToken authSession; // cached auth session

    @Inject
    public AuthSessionFactory(final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                              AuthenticationSessionStore authenticationSessionStore,
                              final InternalSessionFactory internalSessionFactory,
                              final DsameAdminTokenProvider dsameAdminTokenProvider) {
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
            if (authSession == null) {
                authSession=AdminTokenAction.getInstance().run();
            }
            return authSession;
    }

}
