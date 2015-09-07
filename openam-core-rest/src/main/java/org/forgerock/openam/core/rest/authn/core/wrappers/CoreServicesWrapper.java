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

package org.forgerock.openam.core.rest.authn.core.wrappers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import org.forgerock.openam.core.rest.authn.core.AuthenticationContext;

/**
 * A wrapper class around core static class and methods.
 *
 * Providing a wrapper around these methods allows for easy decoupling and unit testing.
 *
 * This class only contains REST authentication specific methods, all more general
 * core methods should be added to {@link org.forgerock.openam.core.CoreServicesWrapper}.
 */
@Singleton
public class CoreServicesWrapper extends org.forgerock.openam.core.CoreServicesWrapper {

    /**
     * Will either create or retrieve an existing AuthContextLocal.
     *
     * {@link AuthUtils#getAuthContext(HttpServletRequest,
     * HttpServletResponse, SessionID, boolean, boolean)} (
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param sessionID The Session ID of the AuthContextLocal, empty String if initial request.
     * @param isSessionUpgrade Whether the AuthContextLocal should be created for session upgrade.
     * @param isBackPost True if back posting.
     * @return The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     * @throws AuthException If there is a problem creating/retrieving the
     *      AuthContextLocal.
     */
    public AuthContextLocalWrapper getAuthContext(HttpServletRequest request, HttpServletResponse response,
            SessionID sessionID, boolean isSessionUpgrade, boolean isBackPost) throws AuthException {
        AuthContextLocal authContextLocal = AuthUtils.getAuthContext(request, response, sessionID, isSessionUpgrade,
                isBackPost);
        String orgDN = AuthClientUtils.getDomainNameByRequest(request, AuthClientUtils.parseRequestParameters(request));
        authContextLocal.setOrgDN(orgDN);
        return new AuthContextLocalWrapper(authContextLocal);
    }

    /**
     * Checks to see if an AuthContextLocal is a new or an existing login process.
     *
     * {@link AuthUtils#isNewRequest(
     *      AuthContextLocal)}
     *
     * @param authContextLocalWrapper The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     * @return If the AuthContextLocal is a new login request or not.
     */
    public boolean isNewRequest(AuthContextLocalWrapper authContextLocalWrapper) {
        return AuthUtils.isNewRequest(authContextLocalWrapper.getAuthContext());
    }

    /**
     * Gets the Composite Advice Type for the Auth Context.
     *
     * @param authContext The AuthContextLocalWrapper.
     * @return The Composite Advice Type.
     */
    public int getCompositeAdviceType(AuthenticationContext authContext) {
        return AuthUtils.getCompositeAdviceType(authContext.getAuthContext());
    }
}
