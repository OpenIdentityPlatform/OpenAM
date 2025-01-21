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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.Utils.joinScope;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.*;

import jakarta.inject.Inject;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;

/**
 * Implementation of the JwtBearerGrantTypeHandler for the JWT Bearer grant.
 *
 * @since 12.0.0
 */
public class JwtBearerGrantTypeHandler extends GrantTypeHandler {

    private final TokenStore tokenStore;

    @Inject
    public JwtBearerGrantTypeHandler(ClientAuthenticator clientAuthenticator, TokenStore tokenStore,
            OAuth2UrisFactory urisFactory, OAuth2ProviderSettingsFactory providerSettingsFactory) {
        super(providerSettingsFactory, urisFactory, clientAuthenticator);
        this.tokenStore = tokenStore;
    }

    @Override
    public AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws RedirectUriMismatchException,
            InvalidRequestException, InvalidGrantException, InvalidCodeException,
            ServerException, UnauthorizedClientException, InvalidScopeException, 
            InvalidClientException, NotFoundException {

        final String jwtParameter = request.getParameter(OAuth2Constants.SAML20.ASSERTION);
        final OAuth2Jwt jwt = OAuth2Jwt.create(jwtParameter);

        if (jwt.isExpired()) {
            throw new InvalidGrantException("JWT has expired");
        }

        if (!clientRegistration.verifyJwtIdentity(jwt)) {
            throw new InvalidGrantException("JWT is not valid");
        }

        final String redirectUri = request.getParameter(REDIRECT_URI);
        final String grantType = request.getParameter(GRANT_TYPE);

        Set<String> scopes = Utils.splitScope(request.<String>getParameter(SCOPE));
        Set<String> authorizationScope = providerSettings.validateAccessTokenScope(clientRegistration, scopes, request);

        final String validatedClaims = providerSettings.validateRequestedClaims(
                (String) request.getParameter(OAuth2Constants.Custom.CLAIMS));

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, BEARER, null,
                jwt.getSubject(), clientRegistration.getClientId(), redirectUri, authorizationScope,
                null, null, validatedClaims, request);

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (authorizationScope != null && !authorizationScope.isEmpty()) {
            accessToken.addExtraData(SCOPE, joinScope(authorizationScope));
        }

        tokenStore.updateAccessToken(request, accessToken);

        return accessToken;
    }
}
