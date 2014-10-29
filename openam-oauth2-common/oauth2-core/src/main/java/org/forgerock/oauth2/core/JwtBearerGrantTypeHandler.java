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
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import javax.inject.Inject;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * Implementation of the JwtBearerGrantTypeHandler for the JWT Bearer grant.
 *
 * @since 12.0.0
 */
public class JwtBearerGrantTypeHandler implements GrantTypeHandler {

    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    @Inject
    public JwtBearerGrantTypeHandler(ClientAuthenticator clientAuthenticator, TokenStore tokenStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.clientAuthenticator = clientAuthenticator;
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    @Override
    public AccessToken handle(OAuth2Request request) throws RedirectUriMismatchException, InvalidClientException,
            InvalidRequestException, ClientAuthenticationFailedException, InvalidGrantException, InvalidCodeException,
            ServerException, UnauthorizedClientException, InvalidScopeException {

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(request);

        final String jwtParameter = request.getParameter("assertion");
        final OAuth2Jwt jwt = OAuth2Jwt.create(jwtParameter);

        if (!jwt.isValid(clientRegistration.getClientJwtSigningHandler())) {
            throw new InvalidGrantException();
        }

        final String redirectUri = request.getParameter("redirect_uri");

        final String grantType = request.getParameter("grant_type");

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        Set<String> scopes = Utils.splitScope(request.<String>getParameter("scope"));
        Set<String> authorizationScope = providerSettings.validateAccessTokenScope(clientRegistration, scopes, request);

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, "Bearer", null,
                jwt.getSubject(), clientRegistration.getClientId(), redirectUri, authorizationScope, null,
                null, request);

        if (authorizationScope != null && !authorizationScope.isEmpty()) {
            accessToken.addExtraData("scope", joinScope(authorizationScope));
        }

        return accessToken;
    }
}
