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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.service;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Wrapper class around AuthUtils to facilitate testing.
 *
 * @author Phill Cunnington
 */
public class AuthUtilsWrapper {

    /**
     * Gets the cookie name from System Properties.
     *
     * @return The AM Cookie Name.
     */
    public String getCookieName() {
        return AuthUtils.getCookieName();
    }

    /**
     * Performs a logout on a given token ensuring the post auth classes are called.
     *
     * @param sessionID The token id to logout.
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @return true if the token was still valid before logout was called.
     * @throws com.iplanet.sso.SSOException If token is null or other SSO exceptions.
     */
    public boolean logout(String sessionID, HttpServletRequest request, HttpServletResponse response)
            throws SSOException {
        return AuthUtils.logout(sessionID, request, response);
    }

}
