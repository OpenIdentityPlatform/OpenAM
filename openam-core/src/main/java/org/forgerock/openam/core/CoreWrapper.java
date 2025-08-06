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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core;

import java.security.AccessController;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.common.FqdnValidator;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * A wrapper around core utility class like, {@link AMAuthUtils} and {@link IdUtils} to facilitate testing.
 *
 * @since 12.0.0
 */
public class CoreWrapper {

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
     * Returns the set of all authenticated levels.
     *
     * @param token valid user {@code SSOToken}.
     * @return Set containing String values representing levels.
     * @throws SSOException if {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAuthenticatedLevels(SSOToken token) throws SSOException {
        return AMAuthUtils.getAuthenticatedLevels(token);
    }

    /**
     * Returns the set of all authenticated realm qualified authentication levels.
     *
     * @param token valid user {@code SSOToken}.
     * @return Set containing String values representing realm qualified authentication levels.
     * @throws SSOException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRealmQualifiedAuthenticatedLevels(SSOToken token) throws SSOException {
        return AMAuthUtils.getRealmQualifiedAuthenticatedLevels(token);
    }

    /**
     * Returns the data from Realm qualified data. This could be authentication scheme or authentication level or
     * service.
     *
     * @param realmQualifiedData Realm qualified data. This could be Realm qualified authentication scheme or
     *                           authentication level or service.
     * @return String representing data. This could be authentication scheme or authentication level or service.
     */
    public String getDataFromRealmQualifiedData(String realmQualifiedData) {
        return AMAuthUtils.getDataFromRealmQualifiedData(realmQualifiedData);
    }

    /**
     * Returns the set of all authenticated realm qualified service names.
     *
     * @param token valid user {@code SSOToken}.
     * @return Set containing String values representing realm qualified service names.
     * @throws SSOException if {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRealmQualifiedAuthenticatedServices(SSOToken token) throws SSOException {
        return AMAuthUtils.getRealmQualifiedAuthenticatedServices(token);
    }

    /**
     * Returns the set of all authenticated Realm names.
     *
     * @param token Valid user {@code SSOToken}
     * @return Set containing String values representing Realm names.
     * @throws SSOException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAuthenticatedRealms(SSOToken token) throws SSOException {
        return AMAuthUtils.getAuthenticatedRealms(token);
    }

    /**
     * Returns a handle of the Identity object based on the SSO Token passed in ({@code AMIdentity} object of the user
     * who is authenticated).
     *
     * @param token Single sign on token of user.
     * @return Identity object.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     */
    public AMIdentity getIdentity(SSOToken token) throws IdRepoException, SSOException {
        return IdUtils.getIdentity(token);
    }

    /**
     * Returns an {@code AMIdentity} object, if provided with a string identifier for the object.
     *
     * @param token SSOToken of the administrator.
     * @param univId String representation of the identity.
     * @return Identity object.
     */
    public AMIdentity getIdentity(SSOToken token, String univId) throws IdRepoException, SSOException {
        return IdUtils.getIdentity(token, univId);
    }

    /**
     * Destroys a single sign on token.
     *
     * @param token The single sign on token object to be destroyed.
     * @exception SSOException If there was an error while destroying the token, or the corresponding session reached
     * its maximum session/idle time, or the session was destroyed.
     */
    public void destroyToken(SSOToken token) throws SSOException {
        SSOTokenManager.getInstance().destroyToken(token);
    }

    /**
     * Returns a cached instance {@code AdminTokenAction}.
     *
     * @return instance of {@code AdminTokenAction}.
     */
    public SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Validates that the hostname is a valid FQDN that is known to OpenAM.
     * @param hostname The FQDN to test.
     * @return {@code true} if the FQDN is valid.
     */
    public boolean isValidFQDN(String hostname) {
        return FqdnValidator.getInstance().isHostnameValid(hostname);
    }

    /**
     * Parses a string into a set using the specified delimiter.
     *
     * @param str The string to be parsed.
     * @param delimiter The delimiter used in the string.
     * @return The parsed set.
     */
    @SuppressWarnings("unchecked")
    public Set<String> delimStringToSet(String str, String delimiter) {
        return PolicyUtils.delimStringToSet(str, delimiter);
    }

    /**
     * Returns the set of all authenticated realm qualified scheme names.
     *
     * @param token A valid user {@code SSOToken}.
     * @return A {@code Set} containing String values representing realm qualified scheme names.
     * @throws SSOException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRealmQualifiedAuthenticatedSchemes(SSOToken token) throws SSOException {
        return AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(token);
    }

    /**
     * Returns the set of all authenticated Scheme names.
     *
     * @param token A A valid user {@code SSOToken}.
     * @return A {@code Set} containing String values representing Scheme names.
     * @throws SSOException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAuthenticatedSchemes(SSOToken token) throws SSOException {
        return AMAuthUtils.getAuthenticatedSchemes(token);
    }

    /**
     * Returns an organization which maps to the identifier used by application.
     *
     * @param orgIdentifier Organization identifier.
     * @return Organization mapping to that identifier.
     * @throws IdRepoException If the {@code getOrganization} fails.
     * @throws SSOException If the {@code getOrganization} fails.
     */
    public String getOrganization(SSOToken adminToken, String orgIdentifier) throws IdRepoException, SSOException {
        return IdUtils.getOrganization(adminToken, orgIdentifier);
    }

    /**
     * Returns realm name in "/" separated format for the provided realm/organization name in DN format.
     *
     * @param orgName Name of organization.
     * @return DN format "/" separated realm name of organization name.
     */
    public String convertOrgNameToRealmName(String orgName) {
        return DNMapper.orgNameToRealmName(orgName);
    }

    /**
     * Returns realm name in DN format for the provided realm/organization name in "/" separated format.
     *
     * @param realm DN format "/" separated realm name of organization name.
     * @return The realm in DN format.
     */
    public String convertRealmNameToOrgName(String realm) {
        return DNMapper.orgNameToDN(realm);
    }

    /**
     * Parses the policy condition advice and checks for realm advices.
     *
     * @param advice The policy advice XML.
     * @return The realm defined in the policy advice, if defined, or {@code null}.
     * @throws IllegalArgumentException if more than one realm is defined within the advice.
     */
    public String getRealmFromPolicyAdvice(String advice) {
        return AuthClientUtils.getRealmFromPolicyAdvice(advice);
    }

    /**
     * Gets the configured cookie domains for the server matching the current request's domains.
     *
     * @param request The HTTP request.
     * @return The matching cookie domains for the current request.
     */
    public Collection<String> getCookieDomainsForRequest(HttpServletRequest request) {
        return AuthUtils.getCookieDomainsForRequest(request);
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

    /**
     * Gets a {@link com.sun.identity.sm.ServiceConfigManager} for the given service.
     * @param serviceName The name of the service.
     * @param token The token to use.
     * @return The service config manager.
     */
    public ServiceConfigManager getServiceConfigManager(String serviceName, SSOToken token)
            throws SSOException, SMSException {
        return new ServiceConfigManager(serviceName, token);
    }
}
