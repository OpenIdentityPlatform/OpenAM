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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AuthLoginException;

import javax.security.auth.callback.Callback;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class around AuthContextLocal.
 *
 * Providing a wrapper around these methods allows for easy decoupling and unit testing.
 */
public class AuthContextLocalWrapper implements AuthenticationContext {

    private final AuthContextLocal authContextLocal;

    /**
     * Constructs an instance of the AuthContextWrapper.
     *
     * @param authContextLocal An instance of a AuthContextLocal.
     */
    public AuthContextLocalWrapper(AuthContextLocal authContextLocal) {
        this.authContextLocal = authContextLocal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthContextLocal getAuthContext() {
        return authContextLocal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login() throws AuthLoginException {
        authContextLocal.login();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(AuthContext.IndexType indexType, String indexValue) throws AuthLoginException {
        authContextLocal.login(indexType, indexValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(AuthContext.IndexType indexType, String indexValue, boolean pCookieMode,
            Map<String, Set<String>> envMap, String locale) throws AuthLoginException {
        authContextLocal.login(indexType, indexValue, pCookieMode, envMap, locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMoreRequirements() {
        return authContextLocal.hasMoreRequirements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Callback[] getRequirements() {
        return authContextLocal.getRequirements(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Callback[] getRequirements(boolean noFilter) {
        return authContextLocal.getRequirements(noFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void submitRequirements(Callback[] callbacks) {
        authContextLocal.submitRequirements(callbacks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthIndexType getIndexType() {
        AuthContext.IndexType indexType = authContextLocal.getLoginState().getIndexType();
        if (indexType != null) {
            return AuthIndexType.getAuthIndexType(indexType.toString());
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthContext.Status getStatus() {
        return authContextLocal.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrgDN() {
        return authContextLocal.getOrgDN();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSOToken getSSOToken() {
        return authContextLocal.getSSOToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorCode() {
        return authContextLocal.getErrorCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorMessage() {
        return authContextLocal.getErrorMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SessionID getSessionID() {
        return authContextLocal.getLoginState().getSid();
    }
}
