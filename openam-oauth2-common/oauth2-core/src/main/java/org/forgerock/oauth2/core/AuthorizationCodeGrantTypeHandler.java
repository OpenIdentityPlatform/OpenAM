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
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

/**
 * Handles the OAuth2 Authorization Code grant type for the 'token' endpoint.
 *
 * @since 12.0.0
 */
public class AuthorizationCodeGrantTypeHandler implements GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ScopeValidator scopeValidator;
    private final RedirectUriValidator redirectUriValidator;
    private final TokenInvalidator tokenInvalidator;

    /**
     * Constructs a new AuthorizationCodeGrantHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param scopeValidator An instance of the ScopeValidator.
     * @param redirectUriValidator An instance of the RedirectUriValidator.
     * @param tokenInvalidator An instance of the TokenInvalidator.
     */
    @Inject
    public AuthorizationCodeGrantTypeHandler(final ClientAuthenticator clientAuthenticator,
            final TokenStore tokenStore, final OAuth2ProviderSettingsFactory providerSettingsFactory,
            final ScopeValidator scopeValidator, final RedirectUriValidator redirectUriValidator,
            final TokenInvalidator tokenInvalidator) {
        this.clientAuthenticator = clientAuthenticator;
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.scopeValidator = scopeValidator;
        this.redirectUriValidator = redirectUriValidator;
        this.tokenInvalidator = tokenInvalidator;
    }

    /**
     * Handles the OAuth2 request for the Authorization Code grant type.
     *
     * @param accessTokenRequest {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InvalidClientException If the client's registration could not be found.
     * @throws InvalidGrantException If the authorization code not be retrieved, the authorization code is already
     * issued, for a different client or for a different redirect uri.
     * @throws InvalidRequestException If the authorization code could not be found or the redirect URI is not valid.
     * @throws InvalidCodeException If the authorization code has expired.
     * @throws RedirectUriMismatchException If the redirect URI is not valid.
     */
    public AccessToken handle(final AccessTokenRequest accessTokenRequest) throws InvalidClientException,
            InvalidGrantException, InvalidRequestException, InvalidCodeException,
            RedirectUriMismatchException {

        final ClientCredentials clientCredentials = accessTokenRequest.getClientCredentials();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientCredentials,
                accessTokenRequest.getContext());

        final String code = accessTokenRequest.getCode();

        final AuthorizationCode authorizationCode = tokenStore.getAuthorizationCode(code);

        if (authorizationCode == null) {
            logger.error("Authorization code doesn't exist, " + code);
            throw new InvalidRequestException("Authorization code doesn't exist.");
        }

        final String redirectUri = accessTokenRequest.getRedirectUri();
        redirectUriValidator.validate(clientRegistration, redirectUri);

        final Set<String> scope = authorizationCode.getScope();

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
                    + authorizationCode.getClientId() + ", actual, " + clientCredentials.getClientId());
            throw new InvalidGrantException();
        }

        if (authorizationCode.isExpired()) {
            logger.error("Authorization code has expired, " + code);
            throw new InvalidCodeException("Authorization code expired.");
        }

        final String resourceOwnerId = authorizationCode.getResourceOwnerId();

        RefreshToken refreshToken = null;
        if (providerSettingsFactory.getProviderSettings(accessTokenRequest.getContext()).issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(accessTokenRequest.getGrantType(), clientRegistration,
                    resourceOwnerId, redirectUri, authorizationCode.getScope(), accessTokenRequest.getContext());
        }

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                resourceOwnerId, clientRegistration, scope, refreshToken, accessTokenRequest.getContext());

        authorizationCode.setIssued();
        tokenStore.updateAuthorizationCode(authorizationCode);

        if (refreshToken != null) {
            accessToken.add(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getTokenId());
        }

        final String nonce = authorizationCode.getNonce();
        accessToken.add(OAuth2Constants.Custom.NONCE, nonce);
        scopeValidator.addAdditionalDataToReturnFromTokenEndpoint(accessToken, accessTokenRequest.getContext());

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope,
                accessTokenRequest.getContext());

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", Utils.joinScope(validatedScope));
        }

        return accessToken;
    }
}
