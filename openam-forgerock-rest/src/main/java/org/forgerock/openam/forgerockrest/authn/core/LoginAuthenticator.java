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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for starting or continuing a login process.
 */
@Singleton
public class LoginAuthenticator {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private final CoreServicesWrapper coreServicesWrapper;

    /**
     * Constructs an instance of the LoginAuthenticator.
     *
     * @param coreServicesWrapper An instance of the CoreServicesWrapper.
     */
    @Inject
    public LoginAuthenticator(CoreServicesWrapper coreServicesWrapper) {
        this.coreServicesWrapper = coreServicesWrapper;
    }

    /**
     * Gets the Login Process object using the given Login Configuration.
     *
     * If it is the first request to initiate a login process then a new AuthContextLocal will be created and given
     * to a new Login Process object and startLoginProcess() will be called.
     *
     * Otherwise the request is a continuation of an existing login process, the exiting AuthContextLocal will
     * be retrieved, using the session id set in the Login Configuration, and given to a new Login Process object
     * which will continue the login process. startLoginProcess() will not be called.
     *
     * @param loginConfiguration The LoginConfiguration object to be used to start or continue the login process.
     * @return The LoginProcess object.
     * @throws AuthException If there is a problem retrieving or creating the underlying AuthContextLocal.
     * @throws AuthLoginException If there is a problem retrieving or creating the underlying AuthContextLocal or
     *                              starting the login process.
     * @throws SSOException If there is a problem starting the login process.
     */
    public LoginProcess getLoginProcess(LoginConfiguration loginConfiguration) throws AuthException, AuthLoginException,
            SSOException {

        verifyAuthenticationRealm(loginConfiguration.getHttpRequest());

        AuthContextLocalWrapper authContext = getAuthContext(loginConfiguration);
        LoginProcess loginProcess = new LoginProcess(this, loginConfiguration, authContext, coreServicesWrapper);
        if (coreServicesWrapper.isNewRequest(authContext)) {
            startLoginProcess(loginProcess);
        }

        return loginProcess;
    }

    /**
     * Checks to see if the realm that is being authenticated against exists and can be resolved.
     *
     * Will throw RestAuthException if the realm cannot be verified.
     *
     * @param request The HttpServletRequest.
     * @throws AuthLoginException If there is a problem verifying the realm.
     * @throws com.iplanet.sso.SSOException If there is a problem verifying the realm.
     */
    private void verifyAuthenticationRealm(HttpServletRequest request) throws AuthLoginException,
            SSOException {

        String orgDN = coreServicesWrapper.getDomainNameByRequest(request);

        if (StringUtils.isEmpty(orgDN)) {
            throw new RestAuthException(400, "Invalid Domain Alias");
        } else {
            try {
                coreServicesWrapper.isOrganizationActive(orgDN);
            } catch (IdRepoException e) {
                throw new RestAuthException(400, "Invalid Domain DN");
            }
        }
    }

    /**
     * Starts the login process by calling the appropriate login() method on the underlying AuthContextLocal.
     *
     * @param loginProcess The Login Process object that will maintain the login process state for the request.
     * @return The Login Process object.
     * @throws AuthLoginException If there is a problem starting the login process.
     */
    LoginProcess startLoginProcess(LoginProcess loginProcess) throws AuthLoginException {

        LoginConfiguration loginConfiguration = loginProcess.getLoginConfiguration();
        HttpServletRequest request = loginConfiguration.getHttpRequest();
        AuthIndexType indexType = loginConfiguration.getIndexType();
        String indexValue = loginConfiguration.getIndexValue();
        AuthContextLocalWrapper authContext = loginProcess.getAuthContext();

        if (indexType != null && indexType.equals(AuthIndexType.RESOURCE)) {
            Map<String, Set<String>> envMap = coreServicesWrapper.getEnvMap(request);
            authContext.login(indexType.getIndexType(), indexValue, false, envMap, null);
        } else if (indexType.getIndexType() != null) {
            authContext.login(indexType.getIndexType(), indexValue);
        } else {
            authContext.login();
        }

        return loginProcess;
    }

    /**
     * Either creates or retrieves an existing AuthContextLocal dependent on whether this request is a new
     * authentication request or the continuation of an existing one.
     *
     * This method will also determine whether the request is a new authentication request for session upgrade.
     *
     * NOTE: A new authentication request, which includes a user's current SSO Token Id, which is not a session upgrade
     * request, will result in a new AuthContextLocal object being created and a new login process being started.
     * It does not check if the user's current SSO Token Id is valid and return if valid.
     *
     * @param loginConfiguration The LoginConfiguration object to be used to start or continue the login process.
     * @return The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     * @throws AuthException If there is a problem creating/retrieving the AuthContextLocal.
     * @throws AuthLoginException If there is a problem checking if the authentication request requires session upgrade.
     * @throws SSOException If there is a problem checking if the authentication request requires session upgrade.
     */
    private AuthContextLocalWrapper getAuthContext(LoginConfiguration loginConfiguration) throws AuthException,
            AuthLoginException, SSOException {

        HttpServletRequest request = loginConfiguration.getHttpRequest();
        HttpServletResponse response = loginConfiguration.getHttpResponse();
        SessionID sessionID = new SessionID(loginConfiguration.getSessionId());
        boolean isSessionUpgrade = false;
        if (loginConfiguration.isSessionUpgradeRequest() && sessionID.isNull()) {
            sessionID = new SessionID(loginConfiguration.getSSOTokenId());
            SSOToken ssoToken = coreServicesWrapper.getExistingValidSSOToken(
                    new SessionID(loginConfiguration.getSSOTokenId()));
            isSessionUpgrade = checkSessionUpgrade(ssoToken, loginConfiguration.getIndexType(),
                    loginConfiguration.getIndexValue());
        }
        boolean isBackPost = false;
        return coreServicesWrapper.getAuthContext(request, response, sessionID, isSessionUpgrade, isBackPost);
    }

    /**
     * Checks to see if the authentication method, used to retrieve the user's current SSO Token ID, meets the required
     * authentication requirements.
     *
     * @param ssoToken The user's current SSO Token ID for their session.
     * @param indexType The Authentication Index Type for the authentication requirements that must be met.
     * @param indexValue The Authentication Index value for the authentication requirements that must be met.
     * @return Whether the user's current session meets the authentication requirements.
     * @throws AuthLoginException If there is a problem determining whether a user's session needs upgrading.
     * @throws SSOException If there is a problem determining whether a user's session needs upgrading.
     */
    private boolean checkSessionUpgrade(SSOToken ssoToken, AuthIndexType indexType, String indexValue)
            throws AuthLoginException, SSOException {

        String value;
        boolean upgrade = false;
        switch (indexType) {
        case USER: {
            value = ssoToken.getProperty("UserToken");
            if (!indexValue.equals(value)) {
                upgrade = true;
            }
            break;
        }
        case ROLE: {
            value = ssoToken.getProperty("Role");
            if (!coreServicesWrapper.doesValueContainKey(value, indexValue)) {
                upgrade = true;
            }
            break;
        }
        case SERVICE: {
            value = ssoToken.getProperty("Service");
            if (!coreServicesWrapper.doesValueContainKey(value, indexValue)) {
                upgrade = true;
            }
            break;
        }
        case MODULE: {
            value = ssoToken.getProperty("AuthType");
            if (!coreServicesWrapper.doesValueContainKey(value, indexValue)) {
                upgrade = true;
            }
            break;
        }
        case LEVEL: {
            int i = Integer.parseInt(indexValue);
            if (i > Integer.parseInt(ssoToken.getProperty("AuthLevel"))) {
                upgrade = true;
            }
            break;
        }
        case COMPOSITE: {
            upgrade = true;
            break;
        }
        }

        return upgrade;
    }
}
