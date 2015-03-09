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

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.IntrospectionEndpoint.*;

import javax.inject.Inject;

import org.forgerock.guava.common.base.Joiner;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2TokenIntrospectionHandler implements TokenIntrospectionHandler {
    private static final Joiner SCOPE_JOINER = Joiner.on(' ');

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final TokenStore tokenStore;

    @Inject
    public OAuth2TokenIntrospectionHandler(OAuth2ProviderSettingsFactory providerSettingsFactory,
            TokenStore tokenStore) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.tokenStore = tokenStore;
    }

    @Override
    public JsonValue introspect(OAuth2Request request, String clientId, String tokenType, String tokenId) throws ServerException, NotFoundException {
        IntrospectableToken token = getIntrospectableToken(request, tokenType, tokenId);

        if (token != null && !token.isExpired()) {
            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            if (token.getClientId().equals(clientId) &&
                    token.getRealm().equals(request.<String>getParameter(OAuth2Constants.Params.REALM))) {
                return renderOAuth2Token(providerSettings, token);
            } else {
                logger.warn("Token {} didn't belong to client {}", request.getParameter(TOKEN), clientId);
            }
        }
        return null;
    }

    /**
     * Render the OAuth 2.0 as a JsonValue according to the specification for introspection of OAuth 2.0 tokens.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-oauth-introspection-04">OAuth 2.0 Token Introspection</a>
     * @param providerSettings The OAuth 2 provider that is connecting.
     * @param token The token.
     * @return A JSON representation of the token attributes.
     * @throws ServerException
     */
    private JsonValue renderOAuth2Token(OAuth2ProviderSettings providerSettings, IntrospectableToken token) throws ServerException {
        return json(object(
                field(ACTIVE, true),
                field(OAuth2Constants.Params.SCOPE, SCOPE_JOINER.join(token.getScope())),
                field(OAuth2Constants.Params.CLIENT_ID, token.getClientId()),
                field(USER_ID, token.getResourceOwnerId()),
                field(TOKEN_TYPE, token instanceof AccessToken ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE),
                field(OAuth2Constants.JWTTokenParams.EXP, token.getExpiryTime() / 1000),
                field(OAuth2Constants.JWTTokenParams.SUB, token.getResourceOwnerId()),
                field(OAuth2Constants.JWTTokenParams.ISS, providerSettings.getIssuer())
        ));
    }

    protected IntrospectableToken getIntrospectableToken(OAuth2Request request, String tokenType, String tokenId)
            throws ServerException {
        IntrospectableToken token = null;

        if (token == null && (tokenType == null || ACCESS_TOKEN_TYPE.equals(tokenType))) {
            try {
                token = tokenStore.readAccessToken(request, tokenId);
            } catch (InvalidGrantException e) {
                // OK, try refresh token.
                logger.debug("Couldn't find access token with ID {}", tokenId, e);
            }
        }
        if (token == null && (tokenType == null || REFRESH_TOKEN_TYPE.equals(tokenType))) {
            try {
                token = tokenStore.readRefreshToken(request, tokenId);
            } catch (InvalidGrantException e) {
                // OK, we'll return not active.
                logger.debug("Couldn't find refresh token with ID {}", tokenId, e);
            }
        }

        return token;
    }

    @Override
    public Integer priority() {
        return 10;
    }

}
