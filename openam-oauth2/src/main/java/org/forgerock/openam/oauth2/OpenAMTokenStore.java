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
 */
package org.forgerock.openam.oauth2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openidconnect.OpenIdConnectToken;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;

/**
 * Implementation of the OpenId Connect Token Store which the OpenId Connect Provider will implement.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMTokenStore implements OpenIdConnectTokenStore {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectTokenStore statefulTokenStore;
    private final TokenStore statelessTokenStore;

    @Inject
    public OpenAMTokenStore(OAuth2ProviderSettingsFactory providerSettingsFactory,
            StatefulTokenStore statefulTokenStore, StatelessTokenStore statelessTokenStore) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.statefulTokenStore = statefulTokenStore;
        this.statelessTokenStore = statelessTokenStore;
    }

    @Override
    public AuthorizationCode createAuthorizationCode(Set<String> scope, ResourceOwner resourceOwner, String clientId,
            String redirectUri, String nonce, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
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
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId,
                    clientId, redirectUri, scope, refreshToken, nonce, claims, request);
        } else {
            return statefulTokenStore.createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId,
                    clientId, redirectUri, scope, refreshToken, nonce, claims, request);
        }
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request)
            throws ServerException, NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
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
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims);
        } else {
            return statefulTokenStore.createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope,
                    request, validatedClaims);
        }
    }

    @Override
    public AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) 
            throws InvalidGrantException, ServerException, NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.readAuthorizationCode(request, code);
        } else {
            return statefulTokenStore.readAuthorizationCode(request, code);
        }
    }

    @Override
    public void updateAuthorizationCode(OAuth2Request request, AuthorizationCode authorizationCode)
            throws NotFoundException, ServerException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.updateAuthorizationCode(request, authorizationCode);
        } else {
            statefulTokenStore.updateAuthorizationCode(request, authorizationCode);
        }
    }

    @Override
    public void updateAccessToken(OAuth2Request request, AccessToken accessToken) throws NotFoundException,
            ServerException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.updateAccessToken(request, accessToken);
        } else {
            statefulTokenStore.updateAccessToken(request, accessToken);
        }
    }

    @Override
    public void deleteAuthorizationCode(OAuth2Request request, String authorizationCode) throws NotFoundException,
            ServerException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.deleteAuthorizationCode(request, authorizationCode);
        } else {
            statefulTokenStore.deleteAuthorizationCode(request, authorizationCode);
        }
    }

    @Override
    public JsonValue queryForToken(OAuth2Request request, String tokenId) throws InvalidRequestException,
            NotFoundException, ServerException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.queryForToken(request, tokenId);
        } else {
            return statefulTokenStore.queryForToken(request, tokenId);
        }
    }

    @Override
    public void deleteAccessToken(OAuth2Request request, String accessTokenId) throws ServerException,
            NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.deleteAccessToken(request, accessTokenId);
        } else {
            statefulTokenStore.deleteAccessToken(request, accessTokenId);
        }
    }

    @Override
    public void deleteRefreshToken(OAuth2Request request, String refreshTokenId) throws InvalidRequestException,
            NotFoundException, ServerException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.deleteRefreshToken(request, refreshTokenId);
        } else {
            statefulTokenStore.deleteRefreshToken(request, refreshTokenId);
        }
    }

    @Override
    public AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.readAccessToken(request, tokenId);
        } else {
            return statefulTokenStore.readAccessToken(request, tokenId);
        }
    }

    @Override
    public RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
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
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
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
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.readDeviceCode(clientId, code, request);
        } else {
            return statefulTokenStore.readDeviceCode(clientId, code, request);
        }
    }

    @Override
    public DeviceCode readDeviceCode(String userCode, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            return statelessTokenStore.readDeviceCode(userCode, request);
        } else {
            return statefulTokenStore.readDeviceCode(userCode, request);
        }
    }

    @Override
    public void updateDeviceCode(DeviceCode code, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.updateDeviceCode(code, request);
        } else {
            statefulTokenStore.updateDeviceCode(code, request);
        }
    }

    @Override
    public void deleteDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        if (providerSettingsFactory.get(request).isStatelessTokensEnabled()) {
            statelessTokenStore.deleteDeviceCode(clientId, code, request);
        } else {
            statefulTokenStore.deleteDeviceCode(clientId, code, request);
        }
    }
}
