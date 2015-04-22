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
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Password Credentials grant.
 *
 * @since 12.0.0
 */
@Singleton
public class PasswordCredentialsGrantTypeHandler extends GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final List<PasswordCredentialsRequestValidator> requestValidators;
    private final ResourceOwnerAuthenticator resourceOwnerAuthenticator;
    private final TokenStore tokenStore;

    /**
     * Constructs a new PasswordCredentialsGrantTypeHandler.
     *
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param requestValidators A {@code List} of PasswordCredentialsRequestValidators.
     * @param resourceOwnerAuthenticator An instance of the ResourceOwnerAuthenticator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param tokenStore An instance of the TokenStore.
     */
    @Inject
    public PasswordCredentialsGrantTypeHandler(ClientAuthenticator clientAuthenticator,
            List<PasswordCredentialsRequestValidator> requestValidators,
            ResourceOwnerAuthenticator resourceOwnerAuthenticator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, TokenStore tokenStore) {
        super(providerSettingsFactory, clientAuthenticator);
        this.requestValidators = requestValidators;
        this.resourceOwnerAuthenticator = resourceOwnerAuthenticator;
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws InvalidClientException,
            InvalidRequestException, UnauthorizedClientException, InvalidGrantException, ServerException,
            InvalidScopeException, NotFoundException {

        for (final PasswordCredentialsRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request, clientRegistration);
        }

        final ResourceOwner resourceOwner = resourceOwnerAuthenticator.authenticate(request);
        if (resourceOwner == null) {
            logger.error("Unable to verify user");
            throw new InvalidGrantException();
        }

        final Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        final Set<String> validatedScope = providerSettings.validateAccessTokenScope(clientRegistration, scope,
                request);

        final String validatedClaims = providerSettings.validateRequestedClaims(
                (String) request.getParameter(OAuth2Constants.Custom.CLAIMS));

        final String grantType = request.getParameter(GRANT_TYPE);

        RefreshToken refreshToken = null;
        if (providerSettings.issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(grantType, clientRegistration.getClientId(),
                    resourceOwner.getId(), null, validatedScope, request);

            refreshToken.setStringProperty(OAuth2Constants.Custom.CLAIMS, validatedClaims);
            tokenStore.updateRefreshToken(refreshToken);
        }

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, BEARER, null,
                resourceOwner.getId(), clientRegistration.getClientId(), null, validatedScope,
                refreshToken, null, validatedClaims, request);

        if (refreshToken != null) {
            accessToken.addExtraData(REFRESH_TOKEN, refreshToken.getTokenId());
        }

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData(SCOPE, Utils.joinScope(validatedScope));
        }

        tokenStore.updateAccessToken(accessToken);

        return accessToken;
    }
}
