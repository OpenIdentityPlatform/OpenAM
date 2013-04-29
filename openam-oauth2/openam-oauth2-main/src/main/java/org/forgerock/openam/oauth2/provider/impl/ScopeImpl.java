/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.provider.impl;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import java.security.AccessController;
import java.util.*;

/**
 * This is the default scope implementation class. This class by default
 * follows the OAuth2 specs rules regarding how scope should be assigned.
 * The only exceptions is in the retrieveTokenInfoEndPoint method end point
 * the scopes are assumed to be OpenAM user attributes, which will be returned
 * upon the completion of the retrieveTokenInfoEndPoint method
 */
public class ScopeImpl implements Scope {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> scopeToPresentOnAuthorizationPage(Set<String> requestedScope, Set<String> availableScopes, Set<String> defaultScopes){

        if (requestedScope == null || requestedScope.isEmpty()) {
            return defaultScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> scopeRequestedForAccessToken(Set<String> requestedScope, Set<String> availableScopes, Set<String> defaultScopes){

        if (requestedScope == null || requestedScope.isEmpty()) {
            return defaultScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> scopeRequestedForRefreshToken(Set<String> requestedScope,
                                                     Set<String> availableScopes,
                                                     Set<String> allScopes,
                                                     Set<String> defaultScopes){

        if (requestedScope == null || requestedScope.isEmpty()) {
            return availableScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> evaluateScope(AccessToken token){
        Map<String, Object> map = new HashMap<String, Object>();
        Set<String> scopes = token.getScope();
        String resourceOwner = token.getUserID();

        if (resourceOwner != null){
            AMIdentity id = null;
            try {
            Set<String> authAttributes = getAuthenticationAttributesForService(token.getRealm());
            id = getIdentity(resourceOwner, token.getRealm(), authAttributes);

            } catch (Exception e){
                OAuth2Utils.DEBUG.error("Unable to get user identity", e);
            }
            if (id != null && scopes != null){
                for (String scope : scopes){
                    try {
                        Set<String> mail = id.getAttribute(scope);
                        if (mail != null || !mail.isEmpty()){
                            map.put(scope, mail.iterator().next());
                        }
                    } catch (Exception e){
                        OAuth2Utils.DEBUG.error("Unable to get attribute", e);
                    }
                }
            }
        }

        return map;
    }

    private AMIdentity getIdentity(String uName, String realm, Set<String> authAttributes) throws OAuthProblemException {
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        AMIdentity theID = null;

        try {
            AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.EMPTY_SET;
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null && !searchResults.getResultAttributes().isEmpty()) {
                results = searchResults.getSearchResults();
            } else {
                Map<String, Set<String>> avPairs = toAvPairMap(authAttributes, uName);
                idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                searchResults =
                        amIdRepo.searchIdentities(IdType.USER, "*", idsc);
                if (searchResults != null) {
                    results = searchResults.getSearchResults();
                }
            }

            if (results == null || results.size() != 1) {
                OAuth2Utils.DEBUG.error("ScopeImpl.getIdentity()::No user profile or more than one profile found.");
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                        "Not able to get client from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Not able to get client from OpenAM");
        }
    }

    private Map toAvPairMap(Set names, String token) {
        if (token == null) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(token);
        if (names == null || names.isEmpty()) {
            return map;
        }
        Iterator it = names.iterator();
        while (it.hasNext()) {
            map.put((String) it.next(), set);
        }
        return map;
    }

    private Set<String> getAuthenticationAttributesForService(String realm){
        if (realm == null){
            //default realm
            realm = "/";
        }
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            ServiceConfig scm = mgr.getOrganizationConfig(realm, null);
            Map<String, Set<String>> attrs = scm.getAttributes();
            return attrs.get(OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ScopeImpl::Unable to read service settings", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null,
                    "Unable to read service settings");
        }
    }

}
