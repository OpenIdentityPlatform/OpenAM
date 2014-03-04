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
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Access Token Validator for validating OAuth2 tokens issued by OpenAM by making internal API calls to OpenAM's
 * OAuth2 token store.
 *
 * @since 12.0.0
 */
public class OpenAMOAuth2AccessTokenValidator implements OAuth2AccessTokenValidator {

    private final Logger logger = LoggerFactory.getLogger(OpenAMOAuth2AccessTokenValidator.class);

    private final Config<OAuth2TokenStore> tokenStore;
    private final OAuth2UserInfoService userInfoService;

    /**
     * Creates a new instance of the OpenAMOAuth2AccessTokenValidator.
     */
    public OpenAMOAuth2AccessTokenValidator() {
        this(InjectorHolder.getInstance(Key.get(new TypeLiteral<Config<OAuth2TokenStore>>() {})),
                InjectorHolder.getInstance(OAuth2UserInfoService.class));
    }

    /**
     * Constructor for test usage.
     *
     * @param tokenStore An instance of the OAuth2TokenStore.
     * @param userInfoService An instance of the OAuth2UserInfoService;
     */
    OpenAMOAuth2AccessTokenValidator(final Config<OAuth2TokenStore> tokenStore, final OAuth2UserInfoService userInfoService) {
        this.tokenStore = tokenStore;
        this.userInfoService = userInfoService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenValidationResponse validate(final String accessToken) throws OAuth2Exception {

        try {
            final CoreToken token = tokenStore.get().readAccessToken(accessToken);
            final long expireTime = token.getExpireTime();
            final Map<String, Object> userInfo = userInfoService.getUserInfo(token);

            return new AccessTokenValidationResponse(expireTime, userInfo, token.getScope());

        } catch (OAuthProblemException e) {
            logger.error(e.getDescription(), e);
            throw new OAuth2Exception(e.getDescription(), e);
        }
    }
}
