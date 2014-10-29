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
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Client Credentials grant.
 *
 * @since 12.0.0
 */
@Singleton
public class ClientCredentialsGrantTypeHandler implements GrantTypeHandler {

    private final ClientAuthenticator clientAuthenticator;
    private final List<ClientCredentialsRequestValidator> requestValidators;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new ClientCredentialsGrantTypeHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param requestValidators A {@code List} of ClientCredentialsRequestValidators.
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public ClientCredentialsGrantTypeHandler(ClientAuthenticator clientAuthenticator,
            List<ClientCredentialsRequestValidator> requestValidators, TokenStore tokenStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.clientAuthenticator = clientAuthenticator;
        this.requestValidators = requestValidators;
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request) throws ClientAuthenticationFailedException, InvalidClientException,
            InvalidRequestException, ServerException, UnauthorizedClientException, InvalidScopeException {

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(request);

        for (final ClientCredentialsRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request, clientRegistration);
        }

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final Set<String> scope = Utils.splitScope(request.<String>getParameter("scope"));
        final Set<String> validatedScope = providerSettings.validateAccessTokenScope(clientRegistration, scope,
                request);
        final String grantType = request.getParameter("grant_type");

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, "Bearer", null,
                clientRegistration.getClientId(), clientRegistration.getClientId(), null, validatedScope, null, null,
                request);

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData("scope", Utils.joinScope(validatedScope));
        }

        return accessToken;

    }
}
