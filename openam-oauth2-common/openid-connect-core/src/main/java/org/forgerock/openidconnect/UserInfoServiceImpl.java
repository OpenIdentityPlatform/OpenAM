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

package org.forgerock.openidconnect;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for retrieving user's information from the access token the user granted the authorization.
 *
 * @since 12.0.0
 */
@Singleton
public class UserInfoServiceImpl implements UserInfoService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final AccessTokenVerifier tokenVerifier;

    /**
     * Constructs a new UserInfoServiceImpl.
     *
     * @param tokenStore An instance of the TokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param tokenVerifier An instance of the AccessTokenVerifier.
     */
    @Inject
    public UserInfoServiceImpl(TokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            AccessTokenVerifier tokenVerifier) {
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.tokenVerifier = tokenVerifier;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getUserInfo(String tokenId, OAuth2Request request) throws OAuth2Exception {

        if (!tokenVerifier.verify(request)) {
            throw new ServerException("Access Token not valid");
        }

        final AccessToken token = tokenStore.readAccessToken(request, tokenId);

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        return new JsonValue(providerSettings.getUserInfo(token, request));
    }
}
