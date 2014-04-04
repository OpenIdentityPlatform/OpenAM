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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;

import java.util.Map;
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
     * Creates an Access Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType The OAuth2 Grant Type.
     * @param resourceOwnerId The resource owner's identifier.
     * @param client The client registration.
     * @param scope The requested scope.
     * @param refreshToken The refresh token. May be {@code null}.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return An Access Token.
     */
    AccessToken createAccessToken(final GrantType grantType, final String resourceOwnerId,
            final ClientRegistration client, final Set<String> scope, final RefreshToken refreshToken,
            final Map<String, Object> context);

    /**
     * Creates a Refresh Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType The OAuth2 Grant Type.
     * @param clientRegistration The client registration.
     * @param resourceOwnerId The resource owner's identifier.
     * @param redirectUri The requested redirect uri.
     * @param scope The requested scope.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return A RefreshToken.
     */
    RefreshToken createRefreshToken(final GrantType grantType, final ClientRegistration clientRegistration,
            final String resourceOwnerId, final String redirectUri, final Set<String> scope,
            final Map<String, Object> context);

    /**
     * Creates an Authorization Code and stores it in the OAuth2 Provider's store.
     *
     * @param code The authorization code identifier.
     * @return The Authorization Code.
     * @throws InvalidGrantException If a problem occurs whilst retrieving the Authorization Code.
     */
    AuthorizationCode getAuthorizationCode(final String code) throws InvalidGrantException;

    /**
     * Updates an Authorization Code.
     *
     * @param authorizationCode The authorization code.
     */
    void updateAuthorizationCode(final AuthorizationCode authorizationCode);

    /**
     * Deletes an Authorization Code from the OAuth2 Provider's store.
     *
     * @param authorizationCode The authorization code.
     */
    void deleteAuthorizationCode(final String authorizationCode);

    /**
     * Queries the OAuth2 Provider's store for a specified token.
     *
     * @param tokenId The token identifier.
     * @return A {@link JsonValue} containing the token.
     */
    JsonValue queryForToken(final String tokenId);

    /**
     * Deletes an Access Token from the OAuth2 Provider's store.
     *
     * @param accessTokenId The access token identifier.
     */
    void deleteAccessToken(final String accessTokenId);

    /**
     * Deletes a Refresh Token from the OAuth2 Provider's store.
     *
     * @param refreshTokenId The refresh token identifier.
     */
    void deleteRefreshToken(final String refreshTokenId);

    /**
     * Reads an Access Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param tokenId The token identifier.
     * @return The Access Token.
     */
    AccessToken readAccessToken(final String tokenId);

    /**
     * Reads a Refresh Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param tokenId The token identifier.
     * @return The Refresh Token.
     */
    RefreshToken readRefreshToken(final String tokenId);
}
