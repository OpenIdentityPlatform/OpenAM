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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import javax.inject.Inject;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.StringUtils;

/**
 *
 */
public class GrantTypeAccessTokenGenerator {

    private final TokenStore tokenStore;

    @Inject
    public GrantTypeAccessTokenGenerator(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public AccessToken generateAccessToken(OAuth2ProviderSettings providerSettings, String grantType, String clientId,
            String resourceOwnerId, String redirectUri, Set<String> scope, String validatedClaims,
            String authorizationCode, String nonce, OAuth2Request request) throws ServerException, NotFoundException {
        RefreshToken refreshToken = null;
        if (providerSettings.issueRefreshTokens()) {
            refreshToken = tokenStore.createRefreshToken(grantType, clientId,
                    resourceOwnerId, redirectUri, scope, request, validatedClaims);
        }

        AccessToken accessToken = tokenStore.createAccessToken(grantType, OAuth2Constants.Bearer.BEARER,
                authorizationCode, resourceOwnerId, clientId, redirectUri, scope, refreshToken, nonce, validatedClaims,
                request);

        if (refreshToken != null) {
            accessToken.addExtraData(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getTokenId());
        }

        return accessToken;
    }
}
