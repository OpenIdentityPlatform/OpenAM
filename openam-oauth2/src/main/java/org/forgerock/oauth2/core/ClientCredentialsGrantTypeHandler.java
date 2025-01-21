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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.SCOPE;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Client Credentials grant.
 *
 * @since 12.0.0
 */
@Singleton
public class ClientCredentialsGrantTypeHandler extends GrantTypeHandler {

    private final List<ClientCredentialsRequestValidator> requestValidators;
    private final TokenStore tokenStore;

    /**
     * Constructs a new ClientCredentialsGrantTypeHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param requestValidators A {@code List} of ClientCredentialsRequestValidators.
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param urisFactory An instance of the OAuthUrisFactory.
     */
    @Inject
    public ClientCredentialsGrantTypeHandler(ClientAuthenticator clientAuthenticator,
            List<ClientCredentialsRequestValidator> requestValidators, TokenStore tokenStore,
            OAuth2UrisFactory urisFactory,OAuth2ProviderSettingsFactory providerSettingsFactory) {
        super(providerSettingsFactory, urisFactory, clientAuthenticator);
        this.requestValidators = requestValidators;
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws InvalidRequestException, ServerException,
            UnauthorizedClientException, InvalidScopeException, NotFoundException, InvalidClientException,
            RedirectUriMismatchException {

        for (final ClientCredentialsRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request, clientRegistration);
        }

        final Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        final Set<String> validatedScope = providerSettings.validateAccessTokenScope(clientRegistration, scope,
                request);
        final String validatedClaims = providerSettings.validateRequestedClaims(
                (String) request.getParameter(OAuth2Constants.Custom.CLAIMS));
        final String grantType = request.getParameter(GRANT_TYPE);

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, BEARER, null,
                clientRegistration.getClientId(), clientRegistration.getClientId(), null, validatedScope,
                null, null, validatedClaims, request);

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData(SCOPE, Utils.joinScope(validatedScope));
        }

        tokenStore.updateAccessToken(request, accessToken);

        return accessToken;

    }
}
