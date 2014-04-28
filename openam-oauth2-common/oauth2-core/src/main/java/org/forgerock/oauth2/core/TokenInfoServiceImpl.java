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
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to return the full information of a OAuth2 token.
 *
 * @since 12.0.0
 */
public class TokenInfoServiceImpl implements TokenInfoService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new TokenInfoServiceImpl.
     *
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public TokenInfoServiceImpl(TokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getTokenInfo(OAuth2Request request) throws InvalidTokenException, InvalidRequestException,
            ExpiredTokenException, ServerException, BadRequestException {

        final Map<String, Object> response = new HashMap<String, Object>();

        final String token = request.getParameter("access_token");

        if (token == null) {
            logger.error("Missing access token in request");
            throw new InvalidRequestException("Missing access_token");
        } else {
            AccessToken accessToken = tokenStore.readAccessToken(token);

            if (accessToken == null) {
                logger.error("Unable to read token from token store for id: " + token);
                throw new InvalidTokenException();
            } else {

                logger.trace("In Validator resource - got token = " + accessToken);

                if (accessToken.isExpired()) {
                    logger.error("Should response and refresh the token");
                    throw new ExpiredTokenException();
                }

                final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
                final Map<String, Object> scopeEvaluation = providerSettings.evaluateScope(accessToken);
                response.putAll(accessToken.getTokenInfo());
                response.putAll(scopeEvaluation);
            }

            return new JsonValue(response);
        }
    }
}
