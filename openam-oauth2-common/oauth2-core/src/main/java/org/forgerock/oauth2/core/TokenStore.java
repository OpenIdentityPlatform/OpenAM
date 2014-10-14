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

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.json.fluent.JsonValue;

import java.util.Set;

/**
 * Interface for a Token Store which the OAuth2 Provider will implement.
 * <br/>
 * The Token Store will be where all types of OAuth2 tokens will be stored and later retrieved.
 *
 * @since 12.0.0
 */
public interface TokenStore {

    /**
     * Creates an Authorization Code and stores it in the OAuth2 Provider's store.
     *
     * @param scope The requested scope.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param nonce The nonce.
     * @param request The OAuth2 request.
     * @return An AuthorizationCode.
     * @throws ServerException If any internal server error occurs.
     */
    AuthorizationCode createAuthorizationCode(Set<String> scope, String resourceOwnerId, String clientId,
            String redirectUri, String nonce, OAuth2Request request) throws ServerException;

    /**
     * Creates an Access Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType The grant type.
     * @param accessTokenType The access token type.
     * @param authorizationCode The authorization code.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param scope The requested scope.
     * @param refreshToken The refresh token. May be {@code null}.
     * @param nonce The nonce.
     * @param request The OAuth2 request.
     * @return An Access Token.
     * @throws ServerException If any internal server error occurs.
     */
    AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope, RefreshToken refreshToken,
            String nonce, OAuth2Request request) throws ServerException;

    /**
     * Creates a Refresh Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType The OAuth2 Grant Type.
     * @param clientId The client's id.
     * @param resourceOwnerId The resource owner's id.
     * @param redirectUri The redirect uri.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return A RefreshToken
     * @throws ServerException If any internal server error occurs.
     */
    RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId, String redirectUri,
            Set<String> scope, OAuth2Request request) throws ServerException;

    /**
     * Creates an Authorization Code and stores it in the OAuth2 Provider's store.
     *
     * @param request The current request.
     * @param code The authorization code identifier.
     * @return The Authorization Code.
     * @throws InvalidGrantException If a problem occurs whilst retrieving the Authorization Code or if the read token
     * is not an Authorization Code.
     * @throws ServerException If any internal server error occurs.
     */
    AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) throws InvalidGrantException, ServerException;

    /**
     * Updates an Authorization Code.
     *
     * @param authorizationCode The authorization code.
     */
    void updateAuthorizationCode(AuthorizationCode authorizationCode);

    /**
     * Deletes an Authorization Code from the OAuth2 Provider's store.
     *
     * @param authorizationCode The authorization code.
     */
    void deleteAuthorizationCode(String authorizationCode);

    /**
     * Queries the OAuth2 Provider's store for a specified token.
     *
     * @param tokenId The token identifier.
     * @return A {@link JsonValue} containing the token.
     */
    JsonValue queryForToken(String tokenId) throws InvalidRequestException;

    /**
     * Deletes an Access Token from the OAuth2 Provider's store.
     *
     * @param accessTokenId The access token identifier.
     */
    void deleteAccessToken(String accessTokenId) throws ServerException;

    /**
     * Deletes a Refresh Token from the OAuth2 Provider's store.
     *
     * @param refreshTokenId The refresh token identifier.
     */
    void deleteRefreshToken(String refreshTokenId) throws InvalidRequestException;

    /**
     * Reads an Access Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param request The current request.
     * @param tokenId The token identifier.
     * @return The Access Token.
     * @throws InvalidGrantException If the read token is not an Access Token.
     */
    AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException, BadRequestException,
            InvalidGrantException;

    /**
     * Reads a Refresh Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param request The current request.
     * @param tokenId The token identifier.
     * @return The Refresh Token.
     * @throws InvalidGrantException If the read token is not a Refresh Token.
     */
    RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws BadRequestException, InvalidRequestException,
            InvalidGrantException;
}
