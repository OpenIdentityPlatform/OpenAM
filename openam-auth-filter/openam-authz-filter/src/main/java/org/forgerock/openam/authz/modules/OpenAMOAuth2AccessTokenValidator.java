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

package org.forgerock.openam.authz.modules;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2AccessTokenValidator;
import org.forgerock.authz.modules.oauth2.OAuth2Exception;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openidconnect.UserInfoService;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.OpenAMAccessToken;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access Token Validator for validating OAuth2 tokens issued by OpenAM by making internal API calls to OpenAM's
 * OAuth2 token store.
 *
 * @since 12.0.0
 */
public class OpenAMOAuth2AccessTokenValidator implements OAuth2AccessTokenValidator {

    private final Logger logger = LoggerFactory.getLogger(OpenAMOAuth2AccessTokenValidator.class);

    private final Config<TokenStore> tokenStore;
    private final UserInfoService userInfoService;

    /**
     * Creates a new instance of the OpenAMOAuth2AccessTokenValidator.
     */
    public OpenAMOAuth2AccessTokenValidator() {
        this(InjectorHolder.getInstance(Key.get(new TypeLiteral<Config<TokenStore>>() {})),
                InjectorHolder.getInstance(UserInfoService.class));
    }

    /**
     * Constructor for test usage.
     *
     * @param tokenStore An instance of the OAuth2TokenStore.
     * @param userInfoService An instance of the UserInfoService;
     */
    OpenAMOAuth2AccessTokenValidator(final Config<TokenStore> tokenStore, final UserInfoService userInfoService) {
        this.tokenStore = tokenStore;
        this.userInfoService = userInfoService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenValidationResponse validate(final String accessToken) throws OAuth2Exception {

        try {
            final AccessToken token = tokenStore.get().readAccessToken(accessToken);
            final long expireTime = token.getExpiryTime();
            final JsonValue userInfo = userInfoService.getUserInfo(accessToken, new OAuth2Request() {
                public <T> T getRequest() {
                    throw new UnsupportedOperationException();
                }

                public <T> T getParameter(String name) {
                    if ("realm".equals(name)) {
                        return (T) ((OpenAMAccessToken) token).getRealm();
                    }
                    throw new UnsupportedOperationException();
                }

                public JsonValue getBody() {
                    throw new UnsupportedOperationException();
                }
            });

            return new AccessTokenValidationResponse(expireTime, userInfo.asMap(), token.getScope());

        } catch (org.forgerock.oauth2.core.exceptions.OAuth2Exception e) {
            logger.error(e.getMessage(), e);
            throw new OAuth2Exception(e.getMessage(), e);
        }
    }
}
