/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AuthSSOProvider.java,v 1.2 2008/06/25 05:41:53 qcheng Exp $
 *
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.authentication.internal;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOProvider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;

public class AuthSSOProvider implements SSOProvider {

    public AuthSSOProvider() {
        // do nothing
    }

    @Override
    public SSOToken createSSOToken(HttpServletRequest request)
            throws SSOException, UnsupportedOperationException {
        throw (new UnsupportedOperationException());
    }

    @Override
    public SSOToken createSSOToken(java.security.Principal user,
            String password) throws SSOException, UnsupportedOperationException
    {
        try {
            AuthContext auth = new AuthContext(user, password.toCharArray());
            return (auth.getSSOToken());
        } catch (Exception e) {
            e.printStackTrace();
            throw (new SSOException(e));
        }
    }

    @Override
    public SSOToken createSSOToken(String tokenID) throws SSOException,
            UnsupportedOperationException {
        SSOToken token = null;
        try {
            token = (SSOToken) AuthSSOToken.allSSOTokens.get(tokenID);
            if (token == null)
                throw new SSOException(ISAuthConstants.AUTH_BUNDLE_NAME,
                        "invalidtoken", null);
        } catch (Exception e) {
            throw (new SSOException(e));
        }
        return (token);
    }

    @Override
    public SSOToken createSSOToken(String tokenId, boolean invokedByAuth, boolean possiblyResetIdleTime)
            throws SSOException, UnsupportedOperationException {
        return createSSOToken(tokenId);
    }

    @Override
    public SSOToken createSSOToken(String tokenID, String clientIP)
            throws SSOException, UnsupportedOperationException {
        return null;
    }

    @Override
    public void destroyToken(SSOToken token) throws SSOException {
        AuthSSOToken authToken = (AuthSSOToken) token;
        authToken.invalidate();
    }

    @Override
    public boolean isValidToken(SSOToken token) {
        AuthSSOToken authToken = (AuthSSOToken) token;
        return (authToken.isValid());
    }

    /**
     * This class ignores the "refresh" parameter, which is just not needed here.
     * @param token The SSOToken object to be validated.
     * @param ignored The refresh parameter, which is completely ignored.
     * @return true if the token is valid, false otherwise.
     */
    @Override
    public boolean isValidToken(SSOToken token, boolean ignored) {
        return isValidToken(token);
    }

    @Override
    public void validateToken(SSOToken token) throws SSOException {
        AuthSSOToken authToken = (AuthSSOToken) token;
        authToken.validate();
    }

    @Override
    public void refreshSession(SSOToken token) throws SSOException,
            UnsupportedOperationException {
    }

    @Override
    public void refreshSession(SSOToken token, boolean resetIdleTime)
            throws SSOException, UnsupportedOperationException {
    }

    @Override
    public void destroyToken(SSOToken destroyer, SSOToken destroyed)
            throws SSOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set getValidSessions(SSOToken requester, String server)
            throws SSOException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
