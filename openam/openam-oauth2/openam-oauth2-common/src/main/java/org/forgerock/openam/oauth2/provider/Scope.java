/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.openam.oauth2.provider;

import org.forgerock.openam.oauth2.model.CoreToken;

import java.util.Map;
import java.util.Set;

/**
 * This interface needs to be implemented to take advantage of OAuth2's scope feature.
 * Each method of Scope is called with a new instance of the Scope implementation class. Any data that needs to
 * persist between scope method calls should be declared static.
 *
 * @supported.all.api
 */
public interface Scope {
    /**
     * scopeToPresentOnAuthorizationPage is called to decide what scopes will appear on the authorization page.
     *
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes available for the client requesting the access token
     * @param defaultScopes   The set of scopes set in the client registration as default
     * @return The set of scopes to grant the token
     */
    public Set<String> scopeToPresentOnAuthorizationPage(Set<String> requestedScopes,
                                                         Set<String> availableScopes,
                                                         Set<String> defaultScopes);

    /**
     * ScopeRequestedForAccessToken is called when a token is created and the token scope is requested.
     *
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes available for the client requesting the access token
     * @param defaultScopes   The set of scopes set in the client registration as default
     * @return The set of scopes to grant the token
     */
    public Set<String> scopeRequestedForAccessToken(Set<String> requestedScopes,
                                                    Set<String> availableScopes,
                                                    Set<String> defaultScopes);

    /**
     * ScopeRequestedForRefreshToken is called when the client tries to refresh an Access Token. The scope returned MUST
     * not contain a scope not originally granted to the original Access Token.
     *
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes given to the original Access Token
     * @param allScopes       All the available scopes for the client
     * @param defaultScopes   The set of scopes set in the client registration as default
     * @return The set of scopes to grant the new Access Token
     */
    public Set<String> scopeRequestedForRefreshToken(Set<String> requestedScopes,
                                                     Set<String> availableScopes,
                                                     Set<String> allScopes,
                                                     Set<String> defaultScopes);

    /**
     * This method is called on the /tokeninfo endpoint. The purpose of this function is to evaluate scope and return
     * to the client some information on the scope evaluation if necessary.
     *
     * @param token An AccessToken that contains all the information about the token
     * @return returns a map of data to be added to the token json object that will be returned to the client,
     *         can be null if no information needs to be returned.
     */
    public Map<String, Object> evaluateScope(CoreToken token);

    /**
     * This method is called before the access_token end point returns an access token. Whatever is returned by this
     * method will be added to the json object returned by the access_token endpoint.
     *
     * @param parameters set of extra data to pass into the method
     * @param token      the token created that will be returned with the extra data from this method
     * @return
     */
    public Map<String, Object> extraDataToReturnForTokenEndpoint(Map<String, String> parameters,
                                                                 CoreToken token);

    /**
     * This method is called before the authorize end point returns an response. Whatever is returned by this
     * method will be added to the json object returned by the authorize endpoint.
     *
     * @param parameters map of extra data to pass into the method
     * @param tokens     a map of token return names to the token objects. For example "code"=>tokenObject
     * @return the return map should include the key value pair returnType=>Value where value is either FRAGMENT or QUERY
     */
    public Map<String, String> extraDataToReturnForAuthorizeEndpoint(Map<String, String> parameters,
                                                                     Map<String, CoreToken> tokens);

    /**
     * This method takes the scope values in the token and gets those user profile attributes for the owner of
     * the token.
     *
     * @param token The OAuth2 bearer token containing the user to get the info about
     * @return
     */
    public Map<String, Object> getUserInfo(CoreToken token);

}
