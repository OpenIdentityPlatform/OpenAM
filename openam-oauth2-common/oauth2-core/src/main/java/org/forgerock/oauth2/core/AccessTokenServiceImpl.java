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

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.joinScope;
import static org.forgerock.oauth2.core.Utils.splitScope;

/**
 * Handles access token requests from OAuth2 clients to the OAuth2 provider to grant access tokens for the requested
 * grant types.
 *
 * @since 12.0.0
 */
public class AccessTokenServiceImpl implements AccessTokenService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final Map<String, ? extends GrantTypeHandler> grantTypeHandlers;
    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new AccessTokenServiceImpl.
     *
     * @param grantTypeHandlers A {@code Map} of the grant type handlers.
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public AccessTokenServiceImpl(Map<String, GrantTypeHandler> grantTypeHandlers,
            final ClientAuthenticator clientAuthenticator, final TokenStore tokenStore,
            final OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.grantTypeHandlers = grantTypeHandlers;
        this.clientAuthenticator = clientAuthenticator;
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken requestAccessToken(OAuth2Request request) throws RedirectUriMismatchException,
            InvalidClientException, InvalidRequestException, ClientAuthenticationFailedException, InvalidCodeException,
            InvalidGrantException, ServerException, UnauthorizedClientException, InvalidScopeException {
        final String grantType = request.getParameter("grant_type");
        final GrantTypeHandler grantTypeHandler = grantTypeHandlers.get(grantType);
        if (grantTypeHandler == null) {
            throw new InvalidGrantException("Unknown Grant Type, " + grantType);
        }
        return grantTypeHandler.handle(request);
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken refreshToken(OAuth2Request request) throws ClientAuthenticationFailedException,
            InvalidClientException, InvalidRequestException, BadRequestException, ServerException,
            ExpiredTokenException, InvalidGrantException, InvalidScopeException {

        Reject.ifTrue(isEmpty(request.<String>getParameter("refresh_token")), "Missing parameter, 'refresh_token'");

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(request);

        final String tokenId = request.getParameter("refresh_token");
        final RefreshToken refreshToken = tokenStore.readRefreshToken(request, tokenId);

        if (refreshToken == null) {
            logger.error("Refresh token does not exist for id: " + tokenId);
            throw new InvalidRequestException("RefreshToken does not exist");
        }

        if (!refreshToken.getClientId().equals(clientRegistration.getClientId())) {
            logger.error("Refresh Token was issued to a different client id: " + clientRegistration.getClientId());
            throw new InvalidRequestException("Token was issued to a different client");
        }

        if (refreshToken.isExpired()) {
            logger.warn("Refresh Token is expired for id: " + refreshToken.getTokenId());
            throw new InvalidGrantException("grant is invalid");
        }

        final Set<String> scope = splitScope(request.<String>getParameter("scope"));
        final String grantType = request.getParameter("grant_type");

        final Set<String> tokenScope;
        if (refreshToken.getScope() != null) {
            tokenScope = new TreeSet<String>(refreshToken.getScope());
        } else {
            tokenScope = new TreeSet<String>();
        }

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final Set<String> validatedScope = providerSettings.validateRefreshTokenScope(clientRegistration,
                Collections.unmodifiableSet(scope), Collections.unmodifiableSet(tokenScope),
                request);

        RefreshToken newRefreshToken = null;
        if (providerSettings.issueRefreshTokensOnRefreshingToken()) {
            newRefreshToken = tokenStore.createRefreshToken(grantType, clientRegistration.getClientId(),
                    refreshToken.getResourceOwnerId(), refreshToken.getRedirectUri(), refreshToken.getScope(), request);
            tokenStore.deleteRefreshToken(refreshToken.getTokenId());
        }

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, "Bearer", null,
                refreshToken.getResourceOwnerId(), clientRegistration.getClientId(), refreshToken.getRedirectUri(),
                validatedScope, newRefreshToken == null ? refreshToken : newRefreshToken, null, request);

        if (newRefreshToken != null) {
            accessToken.addExtraData("refresh_token", newRefreshToken.getTokenId());
        }

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData("scope", joinScope(validatedScope));
        }

        return accessToken;
    }
}
