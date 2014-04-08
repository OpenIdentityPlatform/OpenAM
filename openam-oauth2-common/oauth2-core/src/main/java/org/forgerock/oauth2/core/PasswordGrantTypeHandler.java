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
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static org.forgerock.oauth2.core.AccessTokenRequest.PasswordCredentialsAccessTokenRequest;

/**
 * Handles the OAuth2 Password Credentials grant type for the 'token' endpoint.
 *
 * @since 12.0.0
 */
public class PasswordGrantTypeHandler implements GrantTypeHandler<PasswordCredentialsAccessTokenRequest> {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientAuthenticator clientAuthenticator;
    private final ScopeValidator scopeValidator;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new PasswordGrantTypeHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param scopeValidator An instance of the ScopeValidator.
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public PasswordGrantTypeHandler(final ClientAuthenticator clientAuthenticator, final ScopeValidator scopeValidator,
            final TokenStore tokenStore, final OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.clientAuthenticator = clientAuthenticator;
        this.scopeValidator = scopeValidator;
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * Handles the OAuth2 request for the Password Credentials grant type.
     *
     * @param accessTokenRequest {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InvalidClientException If the client's registration could not be found.
     * @throws InvalidGrantException If the resource owner could not be verified.
     * @throws OAuth2Exception If the resource owner could not be authenticated.
     */
    public AccessToken handle(final PasswordCredentialsAccessTokenRequest accessTokenRequest)
            throws InvalidClientException, InvalidGrantException, OAuth2Exception {

        final ClientCredentials clientCredentials = accessTokenRequest.getClientCredentials();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientCredentials,
                accessTokenRequest.getContext());

        final ResourceOwner resourceOwner = accessTokenRequest.getAuthenticationHandler().authenticate();
        if (resourceOwner == null) {
            logger.error("Unable to verify user");
            throw new InvalidGrantException();
        }

        final Set<String> scope = accessTokenRequest.getScope();

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope,
                accessTokenRequest.getContext());

        RefreshToken refreshToken = null;
        if (providerSettingsFactory.getProviderSettings(accessTokenRequest.getContext()).issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(accessTokenRequest.getGrantType(), clientRegistration,
                    resourceOwner.getId(), null, validatedScope, accessTokenRequest.getContext());
        }

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                resourceOwner.getId(), clientRegistration, validatedScope, refreshToken,
                accessTokenRequest.getContext());

        if (refreshToken != null) {
            accessToken.add(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getTokenId());
        }

        scopeValidator.addAdditionalDataToReturnFromTokenEndpoint(accessToken, accessTokenRequest.getContext());

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", Utils.joinScope(validatedScope));
        }

        return accessToken;
    }
}
