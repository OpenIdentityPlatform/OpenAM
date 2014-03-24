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
 * Handles the OAuth2 Client Credential Grant flow for the OAuth2 Token endpoint.
 *
 * @since 12.0.0
 */
public class ClientCredentialsGrantTypeHandler implements GrantTypeHandler {

    private final ClientAuthenticator clientAuthenticator; //TODO add type generics
    private final ScopeValidator scopeValidator;
    private final TokenStore tokenStore;

    @Inject
    public ClientCredentialsGrantTypeHandler(final ClientAuthenticator clientAuthenticator,
            final ScopeValidator scopeValidator, final TokenStore tokenStore) {
        this.clientAuthenticator = clientAuthenticator;
        this.scopeValidator = scopeValidator;
        this.tokenStore = tokenStore;
    }

    public AccessToken handle(final AccessTokenRequest accessTokenRequest) throws InvalidClientException, UnauthorizedClientException {

        final ClientAuthentication clientAuthentication = accessTokenRequest.getClientAuthentication();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientAuthentication);

        if (!clientRegistration.isConfidential()) {
            //TODO log
            throw new UnauthorizedClientException("Public clients can't use client credentials grant.");
        }

        final Set<String> scope = accessTokenRequest.getScope();

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope);

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                clientRegistration.getClientId(), clientRegistration, clientAuthentication, validatedScope, null);

        scopeValidator.addAdditionDataToReturnFromTokenEndpoint(accessToken);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", Utils.join(validatedScope));
        }

        return accessToken;
    }
}
