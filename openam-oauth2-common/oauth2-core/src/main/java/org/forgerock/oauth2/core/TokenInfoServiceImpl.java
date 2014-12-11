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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.oauth2.core.AccessTokenVerifier.HEADER;
import static org.forgerock.oauth2.core.AccessTokenVerifier.QUERY_PARAM;

/**
 * Service to return the full information of a OAuth2 token.
 *
 * @since 12.0.0
 */
public class TokenInfoServiceImpl implements TokenInfoService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final AccessTokenVerifier headerTokenVerifier;
    private final AccessTokenVerifier queryTokenVerifier;

    /**
     * Constructs a new TokenInfoServiceImpl.
     *
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public TokenInfoServiceImpl(TokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
                                @Named(HEADER) AccessTokenVerifier headerTokenVerifier,
                                @Named(QUERY_PARAM) AccessTokenVerifier queryTokenVerifier) {

        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.headerTokenVerifier = headerTokenVerifier;
        this.queryTokenVerifier = queryTokenVerifier;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getTokenInfo(OAuth2Request request) throws InvalidRequestException, NotFoundException,
            ServerException {

        final AccessTokenVerifier.TokenState headerToken = headerTokenVerifier.verify(request);
        final AccessTokenVerifier.TokenState queryToken = queryTokenVerifier.verify(request);
        final Map<String, Object> response = new HashMap<String, Object>();

        if (!headerToken.isValid() && !queryToken.isValid()) {
            logger.error("Access Token not valid");
            throw new InvalidRequestException("Access Token not valid");
        } else if (headerToken.isValid() && queryToken.isValid()) {
            logger.error("Access Token provided in both query and header in request");
            throw new InvalidRequestException("Access Token cannot be provided in both query and header");
        } else {
            final String token = headerToken.isValid() ? headerToken.getTokenId() : queryToken.getTokenId();
            final AccessToken accessToken;
            try {
                accessToken = tokenStore.readAccessToken(request, token);
            } catch (Exception e) {
                throw new NotFoundException(NotFoundException.ACCESS_TOKEN);
            }

            logger.trace("In Validator resource - got token = " + accessToken);

            final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            final Map<String, Object> scopeEvaluation = providerSettings.evaluateScope(accessToken);
            response.putAll(accessToken.getTokenInfo());
            response.putAll(scopeEvaluation);

            return new JsonValue(response);
        }
    }
}
