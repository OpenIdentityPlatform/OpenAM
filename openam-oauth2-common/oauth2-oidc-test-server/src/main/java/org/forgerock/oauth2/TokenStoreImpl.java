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

package org.forgerock.oauth2;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.OAuth2Constants;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 12.0.0
 */
public class TokenStoreImpl implements TokenStore {

    private final Map<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<String, AuthorizationCode>();
    private final Map<String, AccessToken> accessTokens = new ConcurrentHashMap<String, AccessToken>();
    private final Map<String, RefreshToken> refreshTokens = new ConcurrentHashMap<String, RefreshToken>();

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    @Inject
    public TokenStoreImpl(final OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    public AuthorizationCode createAuthorizationCode(Set<String> scope, String resourceOwnerId, String clientId,
            String redirectUri, String nonce, OAuth2Request request) throws ServerException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String code = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getAuthorizationCodeLifetime() * 1000) + System.currentTimeMillis();
        final AuthorizationCode authorizationCode = new AuthorizationCode(code, resourceOwnerId, clientId, redirectUri,
                scope, expiryTime, nonce, null, null);

        authorizationCodes.put(code, authorizationCode);

        return authorizationCode;
    }

    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode, String resourceOwnerId, String clientId, String redirectUri, Set<String> scope, RefreshToken refreshToken, String nonce, OAuth2Request request) throws ServerException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String id = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getAccessTokenLifetime() * 1000) + System.currentTimeMillis();

        final AccessToken accessToken;
        if (refreshToken == null) {
            accessToken = new AccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope, expiryTime, null, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType, nonce);
        } else {
            accessToken = new AccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope, expiryTime, refreshToken.getTokenId(), OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType, nonce);
        }

        accessTokens.put(id, accessToken);

        return accessToken;
    }

    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId, String redirectUri, Set<String> scope, OAuth2Request request) throws ServerException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String id = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getRefreshTokenLifetime() * 1000) + System.currentTimeMillis();

        final RefreshToken refreshToken = new RefreshToken(id, resourceOwnerId, clientId, redirectUri, scope, expiryTime, "Bearer", OAuth2Constants.Token.OAUTH_REFRESH_TOKEN, grantType, null, null);

        refreshTokens.put(id, refreshToken);

        return refreshToken;
    }

    public AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) throws InvalidGrantException, ServerException {
        AuthorizationCode token = authorizationCodes.get(code);
        request.setToken(AuthorizationCode.class, token);
        return token;
    }

    public void updateAuthorizationCode(AuthorizationCode authorizationCode) {
        authorizationCodes.put(authorizationCode.getTokenId(), authorizationCode);
    }

    public void deleteAuthorizationCode(String authorizationCode) {
        authorizationCodes.remove(authorizationCode);
    }

    //TODO
    public JsonValue queryForToken(String tokenId) throws InvalidRequestException {

        final AuthorizationCode authorizationCode = authorizationCodes.get(tokenId);
        if (authorizationCode != null) {
//            return
        }

        return null;

    }

    public void deleteAccessToken(String accessTokenId) throws ServerException {
        accessTokens.remove(accessTokenId);
    }

    public void deleteRefreshToken(String refreshTokenId) throws InvalidRequestException {
        refreshTokens.remove(refreshTokenId);
    }

    public AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException, BadRequestException {
        AccessToken token = accessTokens.get(tokenId);
        request.setToken(AccessToken.class, token);
        return token;
    }

    public RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws BadRequestException, InvalidRequestException {
        RefreshToken token = refreshTokens.get(tokenId);
        request.setToken(RefreshToken.class, token);
        return token;
    }
}
