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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.AccessController;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

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
     * {@link com.sun.identity.authentication.service.AuthUtils#getSessionIDFromRequest(
     *      javax.servlet.http.HttpServletRequest)}
     *
     * @param request The HttpServletRequest.
     * @return The SessionID from the request.
     */
    public SessionID getSessionIDFromRequest(HttpServletRequest request) {
        return AuthUtils.getSessionIDFromRequest(request);
    }

    /**
     * Gets the environment map from a HttpServletRequest.
     *
     * {@link com.sun.identity.authentication.client.AuthClientUtils#getEnvMap(javax.servlet.http.HttpServletRequest)}
     *
     * @param request The HttpServletRequest.
     * @return The environment map.
     */
    public Map<String, Set<String>> getEnvMap(HttpServletRequest request) {
        return AuthClientUtils.getEnvMap(request);
    }

    /**
     * Gets the resource URL to use for resource-based authentication.
     *
     * {@link AuthClientUtils#getResourceURL(HttpServletRequest)}
     *
     * @param request the servlet request.
     * @return the resource URL to authenticate for.
     */
    public String getResourceURL(HttpServletRequest request) {
        return AuthClientUtils.getResourceURL(request);
    }

    /**
     * Gets the admin SSO Token.
     *
     * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedAction)}
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
     * {@link com.sun.identity.authentication.client.AuthClientUtils#isContain(String, String)}
     *
     * @param value The value.
     * @param key The key.
     * @return If the value does or does not contain the key.
     */
    public boolean doesValueContainKey(String value, String key) {
        return AuthClientUtils.isContain(value, key);
    }

    /**
     * Gets the SSO Token for an existing valid session.
     *
     * @param sessionID The SSO Token Id/Session id of the existing session.
     * @return The SSO Token.
     */
    public SSOToken getExistingValidSSOToken(SessionID sessionID) {
        return AuthUtils.getExistingValidSSOToken(sessionID);
    }

    /**
     * Returns the data from Realm qualified data. This could be authentication scheme or authentication level or
     * service.
     *
     * @param realmQualifiedData Realm qualified data.
     * @return String representing of Realmm qualified data.
     */
    public String getDataFromRealmQualifiedData(String realmQualifiedData) {
        return AMAuthUtils.getDataFromRealmQualifiedData(realmQualifiedData);
    }

    /**
     * Returns the Realm name from Realm qualified data.
     *
     * @param realmQualifiedData Realm qualified data. This could be Realm qualified authentication scheme or
     *                           authentication level or service.
     * @return String representing realm name.
     */
    public String getRealmFromRealmQualifiedData(String realmQualifiedData) {
        return AMAuthUtils.getRealmFromRealmQualifiedData(realmQualifiedData);
    }

    /**
     * Converts organisation name which is "/" separated to DN, else if DN normalize the DN and return.
     *
     * @param orgName The organisation name.
     * @return The Organisation DN.
     */
    public String orgNameToDN(String orgName) {
        return DNMapper.orgNameToDN(orgName);
    }

    /**
     * Returns the authentication service or chain configured for the given organization.
     *
     * @param orgDN organization DN.
     * @return the authentication service or chain configured for the given organization.
     */
    public String getOrgConfiguredAuthenticationChain(String orgDN) {
        return AuthUtils.getOrgConfiguredAuthenticationChain(orgDN);
    }

    /**
     * This method determines the organization parameter and determines the organization DN based on query parameters.
     *
     * {@link com.sun.identity.authentication.client.AuthClientUtils#getDomainNameByRequest(HttpServletRequest,
     *          Hashtable)}
     *
     * @param request The HTTP Servlet Request.
     * @return Organization DN.
     */
    public String getDomainNameByRequest(HttpServletRequest request) {
        return AuthClientUtils.getDomainNameByRequest(request, AuthClientUtils.parseRequestParameters(request));
    }

    /**
     * Checks to see if the Organization is active.
     *
     * {@link com.sun.identity.idm.IdUtils#isOrganizationActive(SSOToken, String)}
     *
     * @param orgDN The organization DN to check the status of.
     * @return True if organization is active, otherwise false.
     * @throws IdRepoException If cannot find any information for organization.
     * @throws SSOException If there is a problem with the admin SSOToken.
     */
    public boolean isOrganizationActive(String orgDN) throws IdRepoException, SSOException {
        return IdUtils.isOrganizationActive(getAdminToken(), orgDN);
    }

    /**
     * Creates and sets the load balancer cookies on the response.
     *
     * @param authContext The AuthContextLocal object.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @throws AuthException If there is a problem setting the load balancer cookies.
     */
    public void setLbCookie(AuthContextLocal authContext, HttpServletRequest request, HttpServletResponse response)
            throws AuthException {
        AuthUtils.setlbCookie(authContext, request, response);
    }

    /**
     * Gets the AMIdentity of a user with username equal to {@literal username} that exists in realm
     *
     * @param username username of the user to get.
     * @param realm realm the user belongs to.
     * @return The AMIdentity of user with username equal to {@literal username}.
     */
    public AMIdentity getIdentity(String username, String realm) {
        return IdUtils.getIdentity(username, realm);
    }
}
