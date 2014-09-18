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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;

import javax.security.auth.callback.Callback;

/**
 * This is used by the rest authentication handler whilst processing an already valid ssoToken
 * to stop any callbacks being sent to the client
 */
public class CompletedLoginProcess extends LoginProcess {
    private final SSOToken ssoToken;
    private final String orgDn;

    /**
     * Constructs an instance of the LoginProcess.
     * @param loginAuthenticator  An instance of the LoginAuthenticator.
     * @param loginConfiguration  The LoginConfiguration object used to start the login process.
     * @param coreServicesWrapper An instance of the CoreServicesWrapper.
     * @param ssoToken            The valid SSOToken for this loginProcess
     */
    public CompletedLoginProcess(LoginAuthenticator loginAuthenticator, LoginConfiguration loginConfiguration,
            CoreServicesWrapper coreServicesWrapper, SSOToken ssoToken) {
        super(loginAuthenticator, loginConfiguration, null, coreServicesWrapper);
        this.ssoToken = ssoToken;
        orgDn = coreServicesWrapper.getDomainNameByRequest(loginConfiguration.getHttpRequest());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginStage getLoginStage() {
        return LoginStage.COMPLETE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Callback[] getCallbacks() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PagePropertiesCallback getPagePropertiesCallback() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginProcess next(Callback[] callbacks) throws AuthLoginException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuccessful() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFailed() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSOToken getSSOToken() {
        return ssoToken;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSuccessURL() {
        try {
            return ssoToken.getProperty(ISAuthConstants.SUCCESS_URL);
        } catch (SSOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the failure url from the underlying authcontext
     * @return The failure url
     */
    public String getFailureURL() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrgDN() {
        return orgDn;
    }

    /**
     * Returns the underlying AuthContextLocal.
     *
     * @return The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     */
    public AuthenticationContext getAuthContext() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        //nothing to clean up
    }

}



