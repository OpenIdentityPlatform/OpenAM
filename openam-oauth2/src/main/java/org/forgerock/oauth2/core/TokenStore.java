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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.services.context.Context;
import org.forgerock.util.query.QueryFilter;

/**
 * Interface for a Token Store which the OAuth2 Provider will implement.
 * <br/>
 * The Token Store will be where all types of OAuth2 tokens will be stored and later retrieved.
 *
 * @since 12.0.0
 */
public interface TokenStore {

    /**
     * A constant to identify the realm agnostic version of the {@link TokenStore} implementation when using dependency
     * injection. The realm agnostic TokenStore does not verify whether the incoming {@link OAuth2Request}'s realm is
     * the same as the OAuth2 access token's realm.
     */
    String REALM_AGNOSTIC_TOKEN_STORE = "realm-agnostic-token-store";

    /**
     * Creates an Authorization Code and stores it in the OAuth2 Provider's store.
     *
     * @param scope               The requested scope.
     * @param resourceOwner       The resource owner.
     * @param clientId            The client's id.
     * @param redirectUri         The redirect uri.
     * @param nonce               The nonce.
     * @param request             The OAuth2 request.
     * @param codeChallenge
     * @param codeChallengeMethod
     * @return An AuthorizationCode.
     * @throws ServerException   If any internal server error occurs.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    AuthorizationCode createAuthorizationCode(Set<String> scope, ResourceOwner resourceOwner, String clientId, String redirectUri, String nonce, OAuth2Request request,
            String codeChallenge, String codeChallengeMethod) throws ServerException,
            NotFoundException;

    /**
     * Creates an Access Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType         The grant type.
     * @param accessTokenType   The access token type.
     * @param authorizationCode The authorization code.
     * @param resourceOwnerId   The resource owner's id.
     * @param clientId          The client's id.
     * @param redirectUri       The redirect uri.
     * @param scope             The requested scope.
     * @param refreshToken      The refresh token. May be {@code null}.
     * @param nonce             The nonce.
     * @param claims            Additional claims requested (for id_token or userinfo).
     * @param request           The OAuth2 request.
     * @return An Access Token.
     * @throws ServerException   If any internal server error occurs.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            RefreshToken refreshToken, String nonce, String claims, OAuth2Request request)
            throws ServerException, NotFoundException;

    /**
     * Creates a Refresh Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType       The OAuth2 Grant Type.
     * @param clientId        The client's id.
     * @param resourceOwnerId The resource owner's Id.
     * @param redirectUri     The redirect uri.
     * @param scope           The requested scope.
     * @param request         The OAuth2 request.
     * @return A RefreshToken
     * @throws ServerException   If any internal server error occurs.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request)
            throws ServerException, NotFoundException;

    /**
     * Creates a Refresh Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType       The OAuth2 Grant Type.
     * @param clientId        The client's id.
     * @param resourceOwnerId The resource owner's Id.
     * @param redirectUri     The redirect uri.
     * @param scope           The requested scope.
     * @param request         The OAuth2 request.
     * @param validatedClaims The validated claims.
     * @return A RefreshToken
     * @throws ServerException   If any internal server error occurs.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims)
            throws ServerException, NotFoundException;

    /**
     * Creates a Refresh Token and stores it in the OAuth2 Provider's store.
     *
     * @param grantType       The OAuth2 Grant Type.
     * @param clientId        The client's id.
     * @param resourceOwnerId The resource owner's Id.
     * @param redirectUri     The redirect uri.
     * @param scope           The requested scope.
     * @param request         The OAuth2 request.
     * @param validatedClaims The validated claims.
     * @param authGrantId     The authorization grant Id.
     * @return A RefreshToken
     * @throws ServerException   If any internal server error occurs.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, String authGrantId)
            throws ServerException, NotFoundException;

    /**
     * Creates an Authorization Code and stores it in the OAuth2 Provider's store.
     *
     * @param request The current request.
     * @param code    The authorization code identifier.
     * @return The Authorization Code.
     * @throws InvalidGrantException If a problem occurs whilst retrieving the Authorization Code or if the read token
     *                               is not an Authorization Code.
     * @throws ServerException       If any internal server error occurs.
     * @throws NotFoundException     If the requested realm does not exist.
     */
    AuthorizationCode readAuthorizationCode(OAuth2Request request, String code)
            throws InvalidGrantException, ServerException, NotFoundException;

    /**
     * Updates an Authorization Code.
     *
     * @param request The current request.
     * @param authorizationCode The authorization code.
     * @throws ServerException If the authorization code could not be updated.
     * @throws NotFoundException If the requested realm does not exist.
     */
    void updateAuthorizationCode(OAuth2Request request, AuthorizationCode authorizationCode) throws NotFoundException,
            ServerException;

    /**
     * Updates an Access Token.
     *
     * @param request The current request.
     * @param accessToken The access token.
     * @throws ServerException If the token could not be updated.
     * @throws NotFoundException If the requested realm does not exist.
     */
    void updateAccessToken(OAuth2Request request, AccessToken accessToken) throws NotFoundException, ServerException;

    /**
     * Deletes an Authorization Code from the OAuth2 Provider's store.
     *
     * @param request The current request.
     * @param authorizationCode The authorization code.
     * @throws ServerException If the authorization code could not be deleted.
     * @throws NotFoundException If the requested realm does not exist.
     */
    void deleteAuthorizationCode(OAuth2Request request, String authorizationCode) throws NotFoundException,
            ServerException;

    /**
     * Queries the OAuth2 Provider's store for a specified token.
     *
     * @param request The current request.
     * @param tokenId The token identifier.
     * @return A {@link JsonValue} containing the token.
     * @throws InvalidRequestException If the tokens could not be queried.
     * @throws ServerException If the tokens could not be queried.
     * @throws NotFoundException If the requested realm does not exist.
     */
    JsonValue queryForToken(OAuth2Request request, String tokenId) throws InvalidRequestException, NotFoundException,
            ServerException;

    /**
     * Deletes an Access Token from the OAuth2 Provider's store.
     *
     * @param request The current request.
     * @param accessTokenId The access token identifier.
     * @throws ServerException If the token could not be deleted.
     * @throws NotFoundException If the requested realm does not exist.
     */
    void deleteAccessToken(OAuth2Request request, String accessTokenId) throws ServerException, NotFoundException;

    /**
     * Deletes a Refresh Token from the OAuth2 Provider's store.
     *
     * @param request The current request.
     * @param refreshTokenId The refresh token identifier.
     * @throws InvalidRequestException If the token could not be deleted.
     * @throws ServerException If the token could not be deleted.
     * @throws NotFoundException If the requested realm does not exist.
     */
    void deleteRefreshToken(OAuth2Request request, String refreshTokenId) throws InvalidRequestException,
            NotFoundException, ServerException;

    /**
     * Reads an Access Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param request The current request.
     * @param tokenId The token identifier.
     * @return The Access Token.
     * @throws InvalidGrantException If the token is not an Access Token.
     * @throws ServerException       If the token could not be read by the server.
     * @throws NotFoundException     If the requested realm does not exist.
     */
    AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException;

    /**
     * Reads a Refresh Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param request The current request.
     * @param tokenId The token identifier.
     * @return The Refresh Token.
     * @throws InvalidGrantException If the token is not a Refresh Token.
     * @throws ServerException       If the token could not be read by the server.
     * @throws NotFoundException     If the requested realm does not exist.
     */
    RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException;

    /**
     * Creates a new device code token.
     * @param scope The scope of the requested access token.
     * @param resourceOwner The resource owner ID.
     * @param clientId The client ID.
     * @param nonce The nonce for the ID token.
     * @param responseType The response type string.
     * @param state The client-side state token.
     * @param acrValues The requested ACR values.
     * @param prompt The prompt request parameter.
     * @param uiLocales The ui_locales request parameter.
     * @param loginHint The login_hint request parameter.
     * @param maxAge The max_age request parameter.
     * @param claims The claims request parameter for ID token claims.
     * @param request The request.
     * @param codeChallenge The submitted code challenge.
     * @param codeChallengeMethod The code challenge method.
     * @return The created device code object.
     * @throws ServerException If there was an error in constructing the code.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    DeviceCode createDeviceCode(Set<String> scope, ResourceOwner resourceOwner, String clientId, String nonce,
            String responseType, String state, String acrValues, String prompt, String uiLocales, String loginHint,
            Integer maxAge, String claims, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException;

    /**
     * Reads a device code token.
     * @param clientId The client ID.
     * @param request The request.
     * @param code The device code.
     * @return The device code object.
     * @throws ServerException If there was an error in constructing the code.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    DeviceCode readDeviceCode(String clientId, String code, OAuth2Request request)
            throws ServerException, NotFoundException, InvalidGrantException;

    /**
     * Reads a device code token.
     * @param request The request.
     * @param userCode The device code's user code.
     * @return The device code object.
     * @throws ServerException If there was an error in constructing the code.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    DeviceCode readDeviceCode(String userCode, OAuth2Request request)
            throws ServerException, NotFoundException, InvalidGrantException;

    /**
     * Updates a device code token.
     * @param request The request.
     * @param code The device code object.
     * @throws ServerException If there was an error in constructing the code.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    void updateDeviceCode(DeviceCode code, OAuth2Request request)
            throws ServerException, NotFoundException, InvalidGrantException;

    /**
     * Deletes a device code token.
     * @param clientId The client ID.
     * @param request The request.
     * @param code The device code.
     * @throws ServerException If there was an error in constructing the code.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    void deleteDeviceCode(String clientId, String code, OAuth2Request request)
            throws ServerException, NotFoundException, InvalidGrantException;

    /**
     *  Queries the OAuth2 Provider's store for tokens.
     *
     * @param realm The Realm.
     * @param queryFilter The query  keyed by auth grant id, client id and resource owner.
     * @return A JsonValue of the query results.
     * @throws ServerException If there was an error in reading the token using the id.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    JsonValue queryForToken(String realm, QueryFilter<CoreTokenField> queryFilter) throws ServerException, NotFoundException;


    /**
     * Deletes the Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param realm The Realm
     * @param tokenId The token ID
     * @throws ServerException If there was an error in reading the token using the id.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    void delete(String realm, String tokenId) throws ServerException, NotFoundException;

    /**
     * Reads the Token from the OAuth2 Provider's store with the specified identifier.
     *
     * @param tokenId The token ID
     * @return  A JsonValue of the token. May be {@code null} if the token is not found.
     * @throws ServerException If there was an error in reading the token using the id.
     * @throws NotFoundException If the realm does not have an OAuth2Provider configured.
     */
    JsonValue read(String tokenId) throws ServerException, NotFoundException;
}