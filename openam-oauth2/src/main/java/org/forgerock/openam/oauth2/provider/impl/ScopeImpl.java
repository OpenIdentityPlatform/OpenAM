/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
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
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.legacy.AccessTokenToLegacyAdapter;
import org.forgerock.openam.oauth2.legacy.CoreToken;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openidconnect.OpenIDTokenIssuer;
import org.restlet.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the default scope implementation class. This class by default
 * follows the OAuth2 specs rules regarding how scope should be assigned.
 * The only exceptions is in the retrieveTokenInfoEndPoint method end point
 * the scopes are assumed to be OpenAM user attributes, which will be returned
 * upon the completion of the retrieveTokenInfoEndPoint method
 *
 * @deprecated Use {@link org.forgerock.oauth2.core.ScopeValidator} instead.
 */
@Deprecated
@Singleton
public class ScopeImpl implements Scope {

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
    private final OAuth2RequestFactory<Request> requestFactory;
    private final OpenIDTokenIssuer openIDTokenIssuer;

    @Inject
    public ScopeImpl(IdentityManager identityManager, OAuth2RequestFactory<Request> requestFactory,
            final OpenIDTokenIssuer openIDTokenIssuer) {
        this.identityManager = identityManager;
        this.requestFactory = requestFactory;
        this.openIDTokenIssuer = openIDTokenIssuer;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> scopeToPresentOnAuthorizationPage(Set<String> requestedScope, Set<String> availableScopes, Set<String> defaultScopes) {

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
    public Set<String> scopeRequestedForAccessToken(Set<String> requestedScope, Set<String> availableScopes, Set<String> defaultScopes) {

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
    public Set<String> scopeRequestedForRefreshToken(Set<String> requestedScope, Set<String> availableScopes,
            Set<String> allScopes, Set<String> defaultScopes) {

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
    public Map<String, Object> evaluateScope(CoreToken token) {
        Map<String, Object> map = new HashMap<String, Object>();
        Set<String> scopes = token.getScope();
        String resourceOwner = token.getUserID();

        if ((resourceOwner != null) && (scopes != null) && (!scopes.isEmpty())){
            try {
                final AMIdentity id = identityManager.getResourceOwnerIdentity(resourceOwner, token.getRealm());
                if (id != null) {
                    for (final String scope : scopes){
                            final Set<String> attributes = id.getAttribute(scope);
                            if (attributes != null || !attributes.isEmpty()) {
                                final Iterator<String> iter = attributes.iterator();
                                final StringBuilder builder = new StringBuilder();
                                while (iter.hasNext()) {
                                    builder.append(iter.next());
                                    if (iter.hasNext()) {
                                        builder.append(MULTI_ATTRIBUTE_SEPARATOR);
                                    }
                                }
                                map.put(scope, builder.toString());
                            }
                    }
                }
            } catch (UnauthorizedClientException e) {
                logger.error("Unable to get user identity", e);
            } catch (SSOException e) {
                logger.error("Unable to get attribute", e);
            } catch (IdRepoException e) {
                logger.error("Unable to get attribute", e);
            }
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> extraDataToReturnForTokenEndpoint(Map<String, String> parameters, CoreToken token) {
        final Map<String, Object> map = new HashMap<String, Object>();
        final Set<String> scope = token.getScope();
        if (scope != null && scope.contains("openid")) {
            final Map.Entry<String, String> tokenEntry;
            try {
                tokenEntry = openIDTokenIssuer.issueToken(new AccessTokenToLegacyAdapter(token), requestFactory.create(Request.getCurrent()));
            } catch (ServerException e) {
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, e.getMessage());
            } catch (InvalidClientException e) {
                throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(null, e.getMessage());
            }
            if (tokenEntry != null) {
                map.put(tokenEntry.getKey(), tokenEntry.getValue());
            }
        }
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
        try {
            id = identityManager.getResourceOwnerIdentity(token.getUserID(), token.getRealm());
        } catch (UnauthorizedClientException e) {
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, e.getMessage());
        }

        //add the subject identifier to the response
        response.put("sub", token.getUserID());
        for (String scope: scopes) {

            //get the attribute associated with the scope
            Object attributes = scopeToUserUserProfileAttributes.get(scope);
            if (attributes == null) {
                logger.error("ScopeImpl.getUserInfo()::Invalid Scope in token scope=" + scope);
            } else if (attributes instanceof String) {
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
                if (attr != null && attr.size() == 1) {
                    response.put(scope, attr.iterator().next());
                } else if (attr != null && attr.size() > 1) { // add a set to the response
                    response.put(scope, attr);
                } else {
                    //attr is null or attr is empty
                    logger.error("ScopeImpl.getUserInfo(): Got an empty result for scope=" + scope);
                }
            } else if (attributes instanceof Map) {

                //the attribute is a collection of attributes
                //for example profile can be address, email, etc...
                if (attributes != null && !((Map<String,String>) attributes).isEmpty()) {
                    for (Map.Entry<String, String> entry: ((Map<String, String>) attributes).entrySet()) {
                        String attribute;
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
                        if (attr != null && attr.size() == 1) {
                            response.put(entry.getKey(), attr.iterator().next());
                        } else if (attr != null && attr.size() > 1) {
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

}
