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

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Authorization Code grant.
 *
 * @since 12.0.0
 */
@Singleton
public class AuthorizationCodeGrantTypeHandler implements GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final List<AuthorizationCodeRequestValidator> requestValidators;
    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final TokenInvalidator tokenInvalidator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new AuthorizationCodeGrantTypeHandler.
     *
     * @param requestValidators A {@code List} of AuthorizationCodeRequestValidator.
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the TokenStore.
     * @param tokenInvalidator An instance of the TokenInvalidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public AuthorizationCodeGrantTypeHandler(List<AuthorizationCodeRequestValidator> requestValidators,
            ClientAuthenticator clientAuthenticator, TokenStore tokenStore, TokenInvalidator tokenInvalidator,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.requestValidators = requestValidators;
        this.clientAuthenticator = clientAuthenticator;
        this.tokenStore = tokenStore;
        this.tokenInvalidator = tokenInvalidator;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request) throws RedirectUriMismatchException, InvalidClientException,
            InvalidRequestException, ClientAuthenticationFailedException, InvalidCodeException, InvalidGrantException,
            ServerException {

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(request);

        for (final AuthorizationCodeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request, clientRegistration);
        }

        final String code = request.getParameter("code");
        final String redirectUri = request.getParameter("redirect_uri");

        final AuthorizationCode authorizationCode = tokenStore.readAuthorizationCode(code);

        if (authorizationCode == null) {
            logger.error("Authorization code doesn't exist, " + code);
            throw new InvalidRequestException("Authorization code doesn't exist.");
        }

        if (authorizationCode.isIssued()) {
            tokenInvalidator.invalidateTokens(code);
            tokenStore.deleteAuthorizationCode(code);
            logger.error("Authorization Code has already been issued, " + code);
            throw new InvalidGrantException();
        }

        if (!authorizationCode.getRedirectUri().equalsIgnoreCase(redirectUri)) {
            logger.error("Authorization code was issued with a different redirect URI, " + code + ". Expected, "
                    + authorizationCode.getRedirectUri() + ", actual, " + redirectUri);
            throw new InvalidGrantException();
        }

        if (!authorizationCode.getClientId().equalsIgnoreCase(clientRegistration.getClientId())) {
            logger.error("Authorization Code was issued to a different client, " + code + ". Expected, "
                    + authorizationCode.getClientId() + ", actual, " + clientRegistration.getClientId());
            throw new InvalidGrantException();
        }

        if (authorizationCode.isExpired()) {
            logger.error("Authorization code has expired, " + code);
            throw new InvalidCodeException("Authorization code expired.");
        }

        final String grantType = request.getParameter("grant_type");
        final Set<String> scope = authorizationCode.getScope();
        final String resourceOwnerId = authorizationCode.getResourceOwnerId();

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        RefreshToken refreshToken = null;
        if (providerSettings.issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(grantType, clientRegistration.getClientId(),
                    resourceOwnerId, redirectUri, scope, request);
        }

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, "Bearer", code,
                resourceOwnerId, clientRegistration.getClientId(), redirectUri, scope, refreshToken,
                authorizationCode.getNonce(), request);

        authorizationCode.setIssued();
        tokenStore.updateAuthorizationCode(authorizationCode);

        if (refreshToken != null) {
            accessToken.addExtraData("refresh_token", refreshToken.getTokenId());
        }

        final String nonce = authorizationCode.getNonce();
        accessToken.addExtraData("nonce", nonce);
        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        final Set<String> validatedScope = providerSettings.validateAccessTokenScope(clientRegistration, scope, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData("scope", joinScope(validatedScope));
        }

        return accessToken;
    }
}
