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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.model.JWTToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;

import java.security.*;
import java.util.*;

/**
 * This is the default scope implementation class. This class by default
 * follows the OAuth2 specs rules regarding how scope should be assigned.
 * The only exceptions is in the retrieveTokenInfoEndPoint method end point
 * the scopes are assumed to be OpenAM user attributes, which will be returned
 * upon the completion of the retrieveTokenInfoEndPoint method
 */
public class ScopeImpl implements Scope {

    // TODO remove this temporary keypair generation and use the client keypair
    static KeyPair keyPair;
    static{
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            keyPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e){

        }
    }

    private static Map<String, Object> scopeToUserUserProfileAttributes;

    static {
        scopeToUserUserProfileAttributes = new HashMap<String, Object>();
        scopeToUserUserProfileAttributes.put("email","mail");
        scopeToUserUserProfileAttributes.put("address", "postaladdress");
        scopeToUserUserProfileAttributes.put("phone", "telephonenumber");

        Map<String, Object> profileSet = new HashMap<String, Object>();
        profileSet.put("name", "cn");
        profileSet.put("given_name", "givenname");
        profileSet.put("family_name", "sn");
        profileSet.put("locale", "preferredlocale");
        profileSet.put("zoneinfo", "preferredtimezone");

        scopeToUserUserProfileAttributes.put("profile", profileSet);
    }

    private OAuth2TokenStore store = null;
    private AMIdentity id = null;

    public ScopeImpl(){
        this.store = new DefaultOAuthTokenStoreImpl();
        this.id = null;
    }

    public ScopeImpl(OAuth2TokenStore store, AMIdentity id){
        this.store = store;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
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
    public Map<String, Object> evaluateScope(CoreToken token){
        Map<String, Object> map = new HashMap<String, Object>();
        Set<String> scopes = token.getScope();
        String resourceOwner = token.getUserID();

        if (resourceOwner != null){
            AMIdentity id = null;
            try {
                Set<String> authAttributes = getAuthenticationAttributesForService(token.getRealm());

                if (this.id == null){
                    id = getIdentity(resourceOwner, token.getRealm(), authAttributes);
                } else {
                    id = this.id;
                }
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
    /**
     * {@inheritDoc}
     */
    public Map<String, Object> extraDataToReturnForTokenEndpoint(Map<String, String> parameters, CoreToken token){
        Map<String, Object> map = new HashMap<String, Object>();
        Set<String> scope = token.getScope();

        //OpenID Connect
        // if an openid scope return the id_token
        if (scope != null && scope.contains("openid")){
            DefaultOAuthTokenStoreImpl store = new DefaultOAuthTokenStoreImpl();
            String jwtToken = store.createSignedJWT(token.getRealm(),
                    token.getUserID(),
                    token.getClientID(),
                    OAuth2Utils.getDeploymentURL(Request.getCurrent()),
                    token.getClientID(),
                    keyPair.getPrivate(),
                    parameters.get(OAuth2Constants.Custom.NONCE));
            map.put("id_token", jwtToken);
        }
        //END OpenID Connect
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> extraDataToReturnForAuthorizeEndpoint(Map<String, String> parameters, Map<String, CoreToken> tokens){
        Map<String, String> map = new HashMap<String, String>();

        // OpenID Connect
        boolean fragment = false;
        if (tokens != null && !tokens.isEmpty()){
            for(Map.Entry<String, CoreToken> token : tokens.entrySet() ){
                Set<String> scope = token.getValue().getScope();
                String responseType = null;
                Set<String> responseTypes = null;

                //get the set of response types passed in
                if (parameters != null){
                    responseType = parameters.get(OAuth2Constants.Params.RESPONSE_TYPE);
                    if (responseType != null && !responseType.isEmpty()){
                        responseTypes = new HashSet<String>(Arrays.asList(responseType.split(" ")));
                    }
                }

                //create an id token if requested and we are in an openid flow.
                if (scope != null && scope.contains("openid") && !token.getKey().equalsIgnoreCase(OAuth2Constants.AuthorizationEndpoint.CODE)
                    && responseTypes != null && responseTypes.contains("id_token")){
                    String nonce = parameters.get(OAuth2Constants.Custom.NONCE);
                    if (nonce == null || nonce.isEmpty()){
                        // nonce is required
                        OAuth2Utils.DEBUG.error("Nonce is required for the authorization endpoint.");
                        throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent(),
                                "Nonce is required for the authorization endpoint.");
                    }

                    String jwtToken = store.createSignedJWT(token.getValue().getRealm(),
                                                token.getValue().getUserID(),
                                                token.getValue().getClientID(),
                                                OAuth2Utils.getDeploymentURL(Request.getCurrent()),
                                                token.getValue().getClientID(),
                                                keyPair.getPrivate(),
                                                parameters.get(OAuth2Constants.Custom.NONCE));
                    map.put("id_token", jwtToken);
                    fragment = true;
                    break;
                }
            }
        }
        //set the return type for the redirect uri.
        if (fragment){
            map.put("returnType", "FRAGMENT");
        }
        //end OpenID Connect

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String,Object> getUserInfo(CoreToken token){

        Set<String> scopes = token.getScope();
        Map<String,Object> response = new HashMap<String, Object>();
        AMIdentity id = null;
        Set<String> authAttributes = getAuthenticationAttributesForService(token.getRealm());
        if (this.id == null){
            id = getIdentity(token.getUserID(), token.getRealm(), authAttributes);
        } else {
            id = this.id;
        }

        //add the subject identifier to the response
        response.put("sub", token.getUserID());
        for(String scope: scopes){

            //get the attribute associated with the scope
            Object attributes = scopeToUserUserProfileAttributes.get(scope);
            if (attributes == null){
             OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo()::Invalid Scope in token scope="+ scope);
            } else if (attributes instanceof String){
                Set<String> attr = null;

                //if the attribute is a string get the attribute
                try {
                    attr = id.getAttribute((String)attributes);
                } catch (IdRepoException e) {
                    OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                } catch (SSOException e) {
                    OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                }

                //add a single object to the response.
                if (attr != null && attr.size() == 1){
                    response.put(scope, attr.iterator().next());
                } else if (attr != null && attr.size() > 1){ // add a set to the response
                    response.put(scope, attr);
                } else {
                    //attr is null or attr is empty
                    OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Got an empty result for scope=" + scope);
                }
            } else if (attributes instanceof Map){

                //the attribute is a collection of attributes
                //for example profile can be address, email, etc...
                if (attributes != null && !((Map<String,String>) attributes).isEmpty()){
                    for (Map.Entry<String, String> entry: ((Map<String, String>) attributes).entrySet()){
                        String attribute = null;
                        attribute = (String)entry.getValue();
                        Set<String> attr = null;

                        //get the attribute
                        try {
                            attr = id.getAttribute(attribute);
                        } catch (IdRepoException e) {
                            OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                        } catch (SSOException e) {
                            OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                        }

                        //add the attribute value(s) to the response
                        if (attr != null && attr.size() == 1){
                            response.put(entry.getKey(), attr.iterator().next());
                        } else if (attr != null && attr.size() > 1){
                            response.put(entry.getKey(), attr);
                        } else {
                            //attr is null or attr is empty
                            OAuth2Utils.DEBUG.error("ScopeImpl.getUserInfo(): Got an empty result for scope=" + scope);
                        }
                    }
                }
            }
        }

        return response;
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
