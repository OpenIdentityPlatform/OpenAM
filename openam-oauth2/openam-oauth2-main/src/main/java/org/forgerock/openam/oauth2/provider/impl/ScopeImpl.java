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
import com.sun.identity.idm.*;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.json.jwt.SignedJwt;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import java.util.*;

/**
 * This is the default scope implementation class. This class by default
 * follows the OAuth2 specs rules regarding how scope should be assigned.
 * The only exceptions is in the retrieveTokenInfoEndPoint method end point
 * the scopes are assumed to be OpenAM user attributes, which will be returned
 * upon the completion of the retrieveTokenInfoEndPoint method
 */
public class ScopeImpl implements Scope {

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

        if ((resourceOwner != null) && (scopes != null) && (!scopes.isEmpty())){
            AMIdentity id = null;
            try {

                if (this.id == null){
                    id = OAuth2Utils.getIdentity(resourceOwner, token.getRealm());
                } else {
                    id = this.id;
                }
            } catch (Exception e){
                OAuth2Utils.DEBUG.error("Unable to get user identity", e);
            }
            if (id != null){
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
            CoreToken jwtToken = store.createJWT(token.getRealm(),
                    token.getUserID(),
                    token.getClientID(),
                    token.getClientID(),
                    parameters.get(OAuth2Constants.Custom.NONCE));

            //TODO setting for this
            //sign the JWT token
            SignedJwt sjwt = OAuth2Utils.signJWT(jwtToken);
            map.put("id_token", sjwt.build());
        }
        //END OpenID Connect
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> extraDataToReturnForAuthorizeEndpoint(Map<String, String> parameters, Map<String, CoreToken> tokens){
        Map<String, String> map = new HashMap<String, String>();
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String,Object> getUserInfo(CoreToken token){

        Set<String> scopes = token.getScope();
        Map<String,Object> response = new HashMap<String, Object>();
        AMIdentity id = null;
        if (this.id == null){
            id = OAuth2Utils.getIdentity(token.getUserID(), token.getRealm());
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

}
