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

package org.forgerock.openam.forgerockrest.authn.core.wrappers;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class around core static class and methods.
 *
 * Providing a wrapper around these methods allows for easy decoupling and unit testing.
 */
@Singleton
public class CoreServicesWrapper {

    /**
     * Gets the Session Id from the HttpServletRequest.
     *
     * {@link com.sun.identity.authentication.service.AuthUtils.getSessionIDFromRequest( javax.servlet.http.HttpServletRequest)}
     *
     * @param request The HttpServletRequest.
     * @return The SessionID from the request.
     */
    public SessionID getSessionIDFromRequest(HttpServletRequest request) {
        return AuthUtils.getSessionIDFromRequest(request);
    }

    /**
     * Will either create or retrieve an existing AuthContextLocal.
     *
     * {@link com.sun.identity.authentication.service.AuthUtils.getAuthContext( com.sun.identity.authentication.server.AuthContextLocal)}
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param sessionID The Session ID of the AuthContextLocal, empty String if initial request.
     * @param isSessionUpgrade Whether the AuthContextLocal should be created for session upgrade.
     * @param isBackPost True if back posting.
     * @return The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     * @throws com.sun.identity.authentication.service.AuthException If there is a problem creating/retrieving the AuthContextLocal.
     */
    public AuthContextLocalWrapper getAuthContext(HttpServletRequest request, HttpServletResponse response,
            SessionID sessionID, boolean isSessionUpgrade, boolean isBackPost) throws AuthException {
        AuthContextLocal authContextLocal = AuthUtils.getAuthContext(request, response, sessionID, isSessionUpgrade,
                isBackPost);
        String orgDN = AuthClientUtils.getDomainNameByRequest(request, AuthClientUtils.parseRequestParameters(request));
        authContextLocal.setOrgDN(orgDN);       //TODO not sure if this is right, will it get right orgDN when using sub realm??
        return new AuthContextLocalWrapper(authContextLocal);
    }

    /**
     * Checks to see if an AuthContextLocal is a new or an existing login process.
     *
     * {@link com.sun.identity.authentication.service.AuthUtils.isNewRequest( com.sun.identity.authentication.server.AuthContextLocal)}
     *
     * @param authContextLocalWrapper The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     * @return If the AuthContextLocal is a new login request or not.
     */
    public boolean isNewRequest(AuthContextLocalWrapper authContextLocalWrapper) {
        return AuthUtils.isNewRequest(authContextLocalWrapper.getAuthContext());
    }

    /**
     * Gets the environment map from a HttpServletRequest.
     *
     * {@link com.sun.identity.authentication.client.AuthClientUtils.getEnvMap( javax.servlet.http.HttpServletRequest)}
     *
     * @param request The HttpServletRequest.
     * @return The environment map.
     */
    public Map<String, Set<String>> getEnvMap(HttpServletRequest request) {
        return AuthClientUtils.getEnvMap(request);
    }

    /**
     * Gets the admin SSO Token.
     *
     * {@link java.security.AccessController.doPrivileged( com.sun.identity.security.AdminTokenAction)}
     *
     * @return The SSOToken.
     */
    public SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Gets a ServiceConfigManager instance.
     *
     * {@link com.sun.identity.sm.ServiceConfigManager(String, com.iplanet.sso.SSOToken)}
     *
     * @param serviceName The service name.
     * @param token The SSOToken.
     * @return A ServiceConfigManager instance.
     * @throws com.iplanet.sso.SSOException If there is a problem when creating the ServiceConfigManager instance.
     * @throws com.sun.identity.sm.SMSException If the SSO Token is invalid or expired.
     */
    public ServiceConfigManager getServiceConfigManager(String serviceName, SSOToken token) throws SSOException,
            SMSException {
        return new ServiceConfigManager(serviceName, token);
    }

    /**
     * Checks to see if the given value contains the given key. The value being a concatenated array.
     *
     * {@link com.sun.identity.authentication.client.AuthClientUtils.isContain(String, String)}
     *
     * @param value The value.
     * @param key The key.
     * @return If the value does or does not contain the key.
     */
    public boolean doesValueContainKey(String value, String key) {
        return AuthClientUtils.isContain(value, key);
    }

    public SSOToken getExistingValidSSOToken(SessionID sessionID) {
        return AuthUtils.getExistingValidSSOToken(sessionID);
    }

    public String getDataFromRealmQualifiedData(String realmQualifiedData) {
        return AMAuthUtils.getDataFromRealmQualifiedData(realmQualifiedData);
    }

    public String getRealmFromRealmQualifiedData(String realmQualifiedData) {
        return AMAuthUtils.getRealmFromRealmQualifiedData(realmQualifiedData);
    }

    public String orgNameToDN(String orgName) {
        return DNMapper.orgNameToDN(orgName);
    }

    public int getCompositeAdviceType(AuthContextLocalWrapper authContext) {
        return AuthUtils.getCompositeAdviceType(authContext.getAuthContext());
    }

    public String getOrgConfiguredAuthenticationChain(String orgDN) {
        return AuthUtils.getOrgConfiguredAuthenticationChain(orgDN);
    }
}
