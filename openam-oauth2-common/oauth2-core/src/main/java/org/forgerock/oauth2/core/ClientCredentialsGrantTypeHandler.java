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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.Bearer.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

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
     */
    @Inject
    public ClientCredentialsGrantTypeHandler(ClientAuthenticator clientAuthenticator,
            List<ClientCredentialsRequestValidator> requestValidators, TokenStore tokenStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        super(providerSettingsFactory, clientAuthenticator);
        this.requestValidators = requestValidators;
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws InvalidRequestException, ServerException,
            UnauthorizedClientException, InvalidScopeException, NotFoundException, InvalidClientException {

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

        tokenStore.updateAccessToken(accessToken);

        return accessToken;

    }
}
