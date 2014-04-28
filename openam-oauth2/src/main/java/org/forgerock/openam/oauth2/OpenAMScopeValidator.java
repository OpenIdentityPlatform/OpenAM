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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openidconnect.OpenIDTokenIssuer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provided as extension points to allow the OpenAM OAuth2 provider to customise the requested scope of authorize,
 * access token and refresh token requests and to allow the OAuth2 provider to return additional data from these
 * endpoints as well.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMScopeValidator implements ScopeValidator {

    private static final String MULTI_ATTRIBUTE_SEPARATOR = ",";
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

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final IdentityManager identityManager;
    private final OpenIDTokenIssuer openIDTokenIssuer;

    /**
     * Constructs a new OpenAMScopeValidator.
     *
     * @param identityManager An instance of the IdentityManager.
     * @param openIDTokenIssuer An instance of the OpenIDTokenIssuer.
     */
    @Inject
    public OpenAMScopeValidator(IdentityManager identityManager, OpenIDTokenIssuer openIDTokenIssuer) {
        this.identityManager = identityManager;
        this.openIDTokenIssuer = openIDTokenIssuer;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope) {
        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<String>(clientRegistration.getAllowedScopes());
        scopes.retainAll(scope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) {
        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<String>(clientRegistration.getAllowedScopes());
        scopes.retainAll(scope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) {

        if (requestedScope == null || requestedScope.isEmpty()) {
            return tokenScope;
        }

        Set<String> scopes = new HashSet<String>(tokenScope);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getUserInfo(AccessToken token, OAuth2Request request)
            throws UnauthorizedClientException {

        Set<String> scopes = token.getScope();
        Map<String,Object> response = new HashMap<String, Object>();
        AMIdentity id = identityManager.getResourceOwnerIdentity(token.getResourceOwnerId(),
                ((OpenAMAccessToken) token).getRealm());

        //add the subject identifier to the response
        response.put("sub", token.getResourceOwnerId());
        for(String scope: scopes){

            //get the attribute associated with the scope
            Object attributes = scopeToUserUserProfileAttributes.get(scope);
            if (attributes == null){
                logger.error("ScopeImpl.getUserInfo()::Invalid Scope in token scope=" + scope);
            } else if (attributes instanceof String){
                Set<String> attr = null;

                //if the attribute is a string get the attribute
                try {
                    attr = id.getAttribute((String)attributes);
                } catch (IdRepoException e) {
                    logger.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                } catch (SSOException e) {
                    logger.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                }

                //add a single object to the response.
                if (attr != null && attr.size() == 1){
                    response.put(scope, attr.iterator().next());
                } else if (attr != null && attr.size() > 1){ // add a set to the response
                    response.put(scope, attr);
                } else {
                    //attr is null or attr is empty
                    logger.error("ScopeImpl.getUserInfo(): Got an empty result for scope=" + scope);
                }
            } else if (attributes instanceof Map){

                //the attribute is a collection of attributes
                //for example profile can be address, email, etc...
                if (attributes != null && !((Map<String,String>) attributes).isEmpty()){
                    for (Map.Entry<String, String> entry: ((Map<String, String>) attributes).entrySet()){
                        String attribute = null;
                        attribute = entry.getValue();
                        Set<String> attr = null;

                        //get the attribute
                        try {
                            attr = id.getAttribute(attribute);
                        } catch (IdRepoException e) {
                            logger.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                        } catch (SSOException e) {
                            logger.error("ScopeImpl.getUserInfo(): Unable to retrieve atrribute", e);
                        }

                        //add the attribute value(s) to the response
                        if (attr != null && attr.size() == 1){
                            response.put(entry.getKey(), attr.iterator().next());
                        } else if (attr != null && attr.size() > 1){
                            response.put(entry.getKey(), attr);
                        } else {
                            //attr is null or attr is empty
                            logger.error("ScopeImpl.getUserInfo(): Got an empty result for scope=" + scope);
                        }
                    }
                }
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> evaluateScope(AccessToken accessToken) {
        Map<String, Object> map = new HashMap<String, Object>();
        Set<String> scopes = accessToken.getScope();
        String resourceOwner = accessToken.getResourceOwnerId();

        if ((resourceOwner != null) && (scopes != null) && (!scopes.isEmpty())){
            AMIdentity id = null;
            try {
                id = identityManager.getResourceOwnerIdentity(resourceOwner,
                        ((OpenAMAccessToken) accessToken).getRealm());
            } catch (Exception e){
                logger.error("Unable to get user identity", e);
            }
            if (id != null){
                for (String scope : scopes){
                    try {
                        Set<String> attributes = id.getAttribute(scope);
                        if (attributes != null || !attributes.isEmpty()) {
                            Iterator<String> iter = attributes.iterator();
                            StringBuilder builder = new StringBuilder();
                            while (iter.hasNext()) {
                                builder.append(iter.next());
                                if (iter.hasNext()) {
                                    builder.append(MULTI_ATTRIBUTE_SEPARATOR);
                                }
                            }
                            map.put(scope, builder.toString());
                        }
                    } catch (Exception e) {
                        logger.error("Unable to get attribute", e);
                    }
                }
            }
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
            OAuth2Request request) {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    public void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException {
        final Set<String> scope = accessToken.getScope();
        if (scope != null && scope.contains("openid")) {
            final Map.Entry<String, String> tokenEntry = openIDTokenIssuer.issueToken(accessToken, request);
            if (tokenEntry != null) {
                accessToken.addExtraData(tokenEntry.getKey(), tokenEntry.getValue());
            }
        }
    }
}
