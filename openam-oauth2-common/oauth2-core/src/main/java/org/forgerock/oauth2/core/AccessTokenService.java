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

import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * This class is the entry point to request/gain a OAuth2 Access Token, it delegates the handling of access token
 * requests to a GrantTypeHandler depending on the grant type specified in the Access Token Request.
 * <br/>
 * Requests for refreshing access tokens is handled directly by this class as the process is the same regardless of
 * the original grant type.
 *
 * @since 12.0.0
 */
public class AccessTokenService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    /**
     * The injection key for a Map of GrantType to GrantTypeHandlers.
     */
    public static final String GRANT_TYPE_HANDLERS_INJECT_KEY = "GRANT_TYPE_HANDLERS";

    private final Map<? extends GrantType, ? extends GrantTypeHandler> grantTypeHandlers;
    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final ScopeValidator scopeValidator;

    /**
     * Constructs a new AccessTokenService.
     *
     * @param grantTypeHandlers The Map of configured GrantTypeHandlers.
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the TokenStore.
     * @param scopeValidator An instance of the ScopeValidator.
     */
    @Inject
    public AccessTokenService(final @Named(GRANT_TYPE_HANDLERS_INJECT_KEY)
            Map<? extends GrantType, ? extends GrantTypeHandler> grantTypeHandlers,
            final ClientAuthenticator clientAuthenticator, final TokenStore tokenStore,
            final ScopeValidator scopeValidator) {
        this.grantTypeHandlers = grantTypeHandlers;
        this.clientAuthenticator = clientAuthenticator;
        this.tokenStore = tokenStore;
        this.scopeValidator = scopeValidator;
    }

    /**
     * Delegates requests for AccessTokens to the appropriate GrantTypeHandler.
     *
     * @param accessTokenRequest The AccessTokenRequest.
     * @return An AccessToken.
     * @throws OAuth2Exception If a problem occurs during the handling of the Access Token request. See specific
     * GrantTypeHandler for the more detailed exceptions they throw.
     */
    public AccessToken requestAccessToken(final AccessTokenRequest accessTokenRequest) throws OAuth2Exception {
        final GrantTypeHandler grantTypeHandler = grantTypeHandlers.get(accessTokenRequest.getGrantType());
        if (grantTypeHandler == null) {
            throw new InvalidGrantException("Unknown Grant Type, " + accessTokenRequest.getGrantType());
        }
        return grantTypeHandler.handle(accessTokenRequest);
    }

    /**
     * Handles the refreshing of access tokens.
     *
     * @param refreshTokenRequest The RefreshTokenRequest.
     * @return An AccessToken.
     * @throws InvalidRequestException If the refresh token does not exist or was issued to a different client.
     * @throws ExpiredTokenException If the refresh token has expired.
     * @throws InvalidClientException If the client cannot be found.
     */
    public AccessToken refreshToken(final RefreshTokenRequest refreshTokenRequest) throws InvalidRequestException,
            ExpiredTokenException, InvalidClientException {

        final ClientCredentials clientCredentials = refreshTokenRequest.getClientCredentials();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientCredentials,
                refreshTokenRequest.getContext());

        final String tokenId = refreshTokenRequest.getRefreshToken();
        final RefreshToken refreshToken = tokenStore.readRefreshToken(tokenId);

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
            throw new ExpiredTokenException();
        }

        final Set<String> scope = refreshTokenRequest.getScope();

        final Set<String> tokenScope;
        if (refreshToken.getScope() != null) {
            tokenScope = new TreeSet<String>(refreshToken.getScope());
        } else {
            tokenScope = new TreeSet<String>();
        }

        final Set<String> validatedScope = scopeValidator.validateRefreshTokenScope(clientRegistration,
                Collections.unmodifiableSet(scope), Collections.unmodifiableSet(tokenScope),
                refreshTokenRequest.getContext());

        final AccessToken accessToken = tokenStore.createAccessToken(refreshTokenRequest.getGrantType(),
                refreshToken.getUserId(), clientRegistration, validatedScope, refreshToken,
                refreshTokenRequest.getContext());

        scopeValidator.addAdditionalDataToReturnFromTokenEndpoint(accessToken, refreshTokenRequest.getContext());

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", joinScope(validatedScope));
        }

        return accessToken;
    }
}
