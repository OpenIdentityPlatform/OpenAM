/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.openam.oauth2.provider;

import org.forgerock.openam.oauth2.model.AccessToken;

import java.util.Map;
import java.util.Set;

/**
 * This interface needs to be implemented to take advantage of OAuth2's scope feature.
 * Each method of Scope is called with a new instance of the Scope implementation class. Any data that needs to
 * persist between scope method calls should be declared static.
 */
public interface Scope {
    /**
     * scopeToPresentOnAuthorizationPage is called to decide what scopes will appear on the authorization page.
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes available for the client requesting the access token
     * @param defaultScopes The set of scopes set in the client registration as default
     * @return The set of scopes to grant the token
     */
    public Set<String> scopeToPresentOnAuthorizationPage(Set<String> requestedScopes, Set<String> availableScopes, Set<String> defaultScopes);

    /**
     * ScopeRequestedForAccessToken is called when a token is created and the token scope is requested.
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes available for the client requesting the access token
     * @param defaultScopes The set of scopes set in the client registration as default
     * @return The set of scopes to grant the token
     */
    public Set<String> scopeRequestedForAccessToken(Set<String> requestedScopes, Set<String> availableScopes, Set<String> defaultScopes);

    /**
     * ScopeRequestedForRefreshToken is called when the client tries to refresh an Access Token. The scope returned MUST
     * not contain a scope not originally grated to the original Access Token.
     * @param requestedScopes The set of scopes requested
     * @param availableScopes The set of scopes given to the original Access Token
     * @param allScopes All the available scopes for the client
     * @param defaultScopes The set of scopes set in the client registration as default
     * @return The set of scopes to grant the new Access Token
     */
    public Set<String> scopeRequestedForRefreshToken(Set<String> requestedScopes,
                                                     Set<String> availableScopes,
                                                     Set<String> allScopes,
                                                     Set<String> defaultScopes);

    /**
     * This method is called on the /tokeninfo endpoint. The purpose of this function is to evaluate scope and return
     * to the client some information on the scope evaluation if nessesary.
     * @param token An AccessToken that contains all the information about the token
     * @return returns a map of data to be added to the token json object that will be returned to the client,
     *          can be null if no information needs to be returned.
     */
    public Map<String, Object> evaluateScope(AccessToken token);

}
