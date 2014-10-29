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

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import java.util.Map;
import java.util.Set;

/**
 * Provided as extension points to allow the OAuth2 provider to customise the requested scope of authorize,
 * access token and refresh token requests and to allow the OAuth2 provider to return additional data from these
 * endpoints as well.
 *
 * @since 12.0.0
 * @supported.all.api
 */
public interface ScopeValidator {

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when authorization
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     */
    Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when an access token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     */
    Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when a refresh token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope from the access token.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     */
    Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Gets the resource owners information based on an issued access token.
     *
     * @param token The access token.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, Object>} of the resource owner's information.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    Map<String, Object> getUserInfo(AccessToken token, OAuth2Request request) throws UnauthorizedClientException;

    /**
     * Gets the specified access token's information.
     *
     * @param accessToken The access token.
     * @return A {@code Map<String, Object>} of the access token's information.
     */
    Map<String, Object> evaluateScope(AccessToken accessToken);

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an authorization
     * request.
     *
     * @param tokens The tokens that will be returned from the authorization call.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, String>} of the additional data to return.
     */
    Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens, OAuth2Request request);

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an access token
     * request.
     * <br/>
     * Any additional data to be returned should be added to the access token by invoking,
     * AccessToken#addExtraData(String, String).
     *
     * @param accessToken The access token.
     * @param request The OAuth2 request.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     */
    void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request) throws ServerException,
            InvalidClientException;
}
