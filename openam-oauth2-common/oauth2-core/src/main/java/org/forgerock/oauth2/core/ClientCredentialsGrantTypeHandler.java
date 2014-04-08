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
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static org.forgerock.oauth2.core.AccessTokenRequest.ClientCredentialsAccessTokenRequest;

/**
 * Handles the OAuth2 Client Credentials grant type for the 'token' endpoint.
 *
 * @since 12.0.0
 */
public class ClientCredentialsGrantTypeHandler implements GrantTypeHandler<ClientCredentialsAccessTokenRequest> {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientAuthenticator clientAuthenticator;
    private final ScopeValidator scopeValidator;
    private final TokenStore tokenStore;

    /**
     * Constructs a new ClientCredentialsGrantTypeHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param scopeValidator An instance of the ScopeValidator.
     * @param tokenStore An instance of the TokenStore.
     */
    @Inject
    public ClientCredentialsGrantTypeHandler(final ClientAuthenticator clientAuthenticator,
            final ScopeValidator scopeValidator, final TokenStore tokenStore) {
        this.clientAuthenticator = clientAuthenticator;
        this.scopeValidator = scopeValidator;
        this.tokenStore = tokenStore;
    }

    /**
     * Handles the OAuth2 request for the Client Credentials grant type.
     *
     * @param accessTokenRequest {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InvalidClientException If the client's registration could not be found.
     * @throws UnauthorizedClientException If the client is not confidential.
     */
    public AccessToken handle(final ClientCredentialsAccessTokenRequest accessTokenRequest)
            throws InvalidClientException, UnauthorizedClientException {

        final ClientCredentials clientCredentials = accessTokenRequest.getClientCredentials();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientCredentials,
                accessTokenRequest.getContext());

        if (!clientRegistration.isConfidential()) {
            logger.error("Client is not confidential. Public clients cannot use the client credentials grant.");
            throw new UnauthorizedClientException("Public clients can't use client credentials grant.");
        }

        final Set<String> scope = accessTokenRequest.getScope();

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope,
                accessTokenRequest.getContext());

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                clientRegistration.getClientId(), clientRegistration, validatedScope, null,
                accessTokenRequest.getContext());

        scopeValidator.addAdditionalDataToReturnFromTokenEndpoint(accessToken, accessTokenRequest.getContext());

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", Utils.joinScope(validatedScope));
        }

        return accessToken;
    }
}
