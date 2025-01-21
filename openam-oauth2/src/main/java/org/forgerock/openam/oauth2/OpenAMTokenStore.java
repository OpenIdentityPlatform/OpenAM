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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openidconnect.OpenIdConnectToken;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.util.query.QueryFilter;

/**
 * Implementation of the OpenId Connect Token Store which the OpenId Connect Provider will implement.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMTokenStore implements OpenIdConnectTokenStore {

    private final OpenIdConnectTokenStore statefulTokenStore;
    private final TokenStore statelessTokenStore;
    private final StatelessCheck<Boolean> statelessCheck;

    @Inject
    public OpenAMTokenStore(StatefulTokenStore statefulTokenStore, StatelessTokenStore statelessTokenStore,
            StatelessCheck<Boolean> statelessCheck) {
        this.statefulTokenStore = statefulTokenStore;
        this.statelessTokenStore = statelessTokenStore;
        this.statelessCheck = statelessCheck;
    }

    @Override
    public AuthorizationCode createAuthorizationCode(Set<String> scope, ResourceOwner resourceOwner, String clientId,
            String redirectUri, String nonce, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createAuthorizationCode(scope, resourceOwner, clientId, redirectUri, nonce, request,
                    codeChallenge, codeChallengeMethod);
        } else {
            return statefulTokenStore.createAuthorizationCode(scope, resourceOwner, clientId, redirectUri, nonce, request,
                    codeChallenge, codeChallengeMethod);
        }
    }

    @Override
    public OpenIdConnectToken createOpenIDToken(ResourceOwner resourceOwner, String clientId,
            String authorizationParty, String nonce, String ops, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {
        return statefulTokenStore.createOpenIDToken(resourceOwner, clientId, authorizationParty, nonce, ops, request);
    }

    @Override
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            RefreshToken refreshToken, String nonce, String claims, OAuth2Request request)
            throws ServerException, NotFoundException {
        return createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId,
                    clientId, redirectUri, scope, refreshToken, nonce, claims, request,
                    TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            RefreshToken refreshToken, String nonce, String claims, OAuth2Request request, long authTime)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId,
                    clientId, redirectUri, scope, refreshToken, nonce, claims, request, authTime);
        } else {
            return statefulTokenStore.createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId,
                    clientId, redirectUri, scope, refreshToken, nonce, claims, request, authTime);
        }
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request);
        } else {
            return statefulTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request);
        }
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims)
            throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                request, validatedClaims, TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, long authTime)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims, authTime);
        } else {
            return statefulTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims, authTime);
        }
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, String authGrantId)
            throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                request, validatedClaims, authGrantId, TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, 
            String authGrantId, long authTime)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims, authGrantId, authTime);
        } else {
            return statefulTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims, authGrantId, authTime);
        }
    }

    @Override
    public AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) 
            throws InvalidGrantException, ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.readAuthorizationCode(request, code);
        } else {
            return statefulTokenStore.readAuthorizationCode(request, code);
        }
    }

    @Override
    public void updateAuthorizationCode(OAuth2Request request, AuthorizationCode authorizationCode)
            throws NotFoundException, ServerException {
        if (statelessCheck.byRequest(request)) {
            statelessTokenStore.updateAuthorizationCode(request, authorizationCode);
        } else {
            statefulTokenStore.updateAuthorizationCode(request, authorizationCode);
        }
    }

    @Override
    public void updateAccessToken(OAuth2Request request, AccessToken accessToken) throws NotFoundException,
            ServerException {
        if (statelessCheck.byToken(accessToken.getTokenId())) {
            statelessTokenStore.updateAccessToken(request, accessToken);
        } else {
            statefulTokenStore.updateAccessToken(request, accessToken);
        }
    }

    @Override
    public void deleteAuthorizationCode(OAuth2Request request, String authorizationCode) throws NotFoundException,
            ServerException {
        if (statelessCheck.byRequest(request)) {
            statelessTokenStore.deleteAuthorizationCode(request, authorizationCode);
        } else {
            statefulTokenStore.deleteAuthorizationCode(request, authorizationCode);
        }
    }

    @Override
    public void deleteAccessToken(OAuth2Request request, String accessTokenId) throws ServerException,
            NotFoundException {
        if (statelessCheck.byToken(accessTokenId)) {
            statelessTokenStore.deleteAccessToken(request, accessTokenId);
        } else {
            statefulTokenStore.deleteAccessToken(request, accessTokenId);
        }
    }

    @Override
    public void deleteRefreshToken(OAuth2Request request, String refreshTokenId) throws InvalidRequestException,
            NotFoundException, ServerException {
        if (statelessCheck.byToken(refreshTokenId)) {
            statelessTokenStore.deleteRefreshToken(request, refreshTokenId);
        } else {
            statefulTokenStore.deleteRefreshToken(request, refreshTokenId);
        }
    }

    @Override
    public AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        if (statelessCheck.byToken(tokenId)) {
            return statelessTokenStore.readAccessToken(request, tokenId);
        } else {
            return statefulTokenStore.readAccessToken(request, tokenId);
        }
    }

    @Override
    public RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        if (statelessCheck.byToken(tokenId)) {
            return statelessTokenStore.readRefreshToken(request, tokenId);
        } else {
            return statefulTokenStore.readRefreshToken(request, tokenId);
        }
    }

    @Override
    public DeviceCode createDeviceCode(Set<String> scope, ResourceOwner resourceOwner, String clientId, String nonce,
            String responseType, String state, String acrValues, String prompt, String uiLocales, String loginHint,
            Integer maxAge, String claims, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.createDeviceCode(scope, resourceOwner, clientId, nonce, responseType, state,
                    acrValues, prompt, uiLocales, loginHint, maxAge, claims, request, codeChallenge, codeChallengeMethod);
        } else {
            return statefulTokenStore.createDeviceCode(scope, resourceOwner, clientId, nonce, responseType, state,
                    acrValues, prompt, uiLocales, loginHint, maxAge, claims, request, codeChallenge, codeChallengeMethod);
        }
    }

    @Override
    public DeviceCode readDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.readDeviceCode(clientId, code, request);
        } else {
            return statefulTokenStore.readDeviceCode(clientId, code, request);
        }
    }

    @Override
    public DeviceCode readDeviceCode(String userCode, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        if (statelessCheck.byRequest(request)) {
            return statelessTokenStore.readDeviceCode(userCode, request);
        } else {
            return statefulTokenStore.readDeviceCode(userCode, request);
        }
    }

    @Override
    public void updateDeviceCode(DeviceCode code, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        if (statelessCheck.byRequest(request)) {
            statelessTokenStore.updateDeviceCode(code, request);
        } else {
            statefulTokenStore.updateDeviceCode(code, request);
        }
    }

    @Override
    public void deleteDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        if (statelessCheck.byRequest(request)) {
            statelessTokenStore.deleteDeviceCode(clientId, code, request);
        } else {
            statefulTokenStore.deleteDeviceCode(clientId, code, request);
        }
    }

    @Override
    public JsonValue queryForToken(String realm, QueryFilter<CoreTokenField> queryFilter) throws ServerException, NotFoundException {
        if (statelessCheck.byRealm(realm)) {
            return statelessTokenStore.queryForToken(realm, queryFilter);
        } else {
            return statefulTokenStore.queryForToken(realm, queryFilter);
        }
    }

    @Override
    public void delete(String realm, String tokenId) throws ServerException, NotFoundException {
        if (statelessCheck.byRealm(realm)) {
            statelessTokenStore.delete(realm, tokenId);
        } else {
            statefulTokenStore.delete(realm, tokenId);
        }
    }

    @Override
    public JsonValue read(String tokenId) throws ServerException, NotFoundException {
        if (statelessCheck.byToken (tokenId)) {
            return statelessTokenStore.read(tokenId);
        } else {
            return statefulTokenStore.read(tokenId);
        }
    }
}
