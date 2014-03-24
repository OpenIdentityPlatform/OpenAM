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

import javax.inject.Inject;
import java.util.Set;

/**
 * Handles the OAuth2 Password Credential Grant flow for the OAuth2 Token endpoint.
 *
 * @since 12.0.0
 */
public class PasswordGrantTypeHandler implements GrantTypeHandler {

    private final ClientAuthenticator clientAuthenticator;
    private final ResourceOwnerAuthenticator resourceOwnerAuthenticator;
    private final ScopeValidator scopeValidator;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettings providerSettings;

    @Inject
    public PasswordGrantTypeHandler(final ClientAuthenticator clientAuthenticator,
            final ResourceOwnerAuthenticator resourceOwnerAuthenticator, final ScopeValidator scopeValidator,
            final TokenStore tokenStore, final OAuth2ProviderSettings providerSettings) {
        this.clientAuthenticator = clientAuthenticator;
        this.resourceOwnerAuthenticator = resourceOwnerAuthenticator;
        this.scopeValidator = scopeValidator;
        this.tokenStore = tokenStore;
        this.providerSettings = providerSettings;
    }

    public AccessToken handle(final AccessTokenRequest accessTokenRequest) throws InvalidClientException,
            UnauthorizedClientException, InvalidGrantException {

        final ClientAuthentication clientAuthentication = accessTokenRequest.getClientAuthentication();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientAuthentication);

        final ResourceOwnerAuthentication resourceOwnerAuthentication =
                accessTokenRequest.getResourceOwnerAuthentication();

        final ResourceOwner resourceOwner = resourceOwnerAuthenticator.authenticate(resourceOwnerAuthentication);
        if (resourceOwner == null) {
            //TODO log
//            OAuth2Utils.DEBUG.error("Unable to verify user: " + username);
            throw new InvalidGrantException();
        }

        final Set<String> scope = accessTokenRequest.getScope();

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope);

        RefreshToken refreshToken = null;
        if (providerSettings.issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(accessTokenRequest.getGrantType(), clientRegistration,
                    clientAuthentication, resourceOwner, validatedScope);
        }

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                resourceOwner.getId(), clientRegistration, clientAuthentication, validatedScope, refreshToken);

        if (refreshToken != null) {
            accessToken.add(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getTokenId());
        }

        scopeValidator.addAdditionDataToReturnFromTokenEndpoint(accessToken);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", Utils.join(validatedScope));
        }

        return accessToken;
    }
}
