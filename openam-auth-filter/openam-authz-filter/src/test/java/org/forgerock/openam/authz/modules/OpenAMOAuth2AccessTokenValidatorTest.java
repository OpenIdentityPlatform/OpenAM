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

import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2Exception;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openidconnect.UserInfoService;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.Config;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @since 12.0.0
 */
public class OpenAMOAuth2AccessTokenValidatorTest {

    private OpenAMOAuth2AccessTokenValidator accessTokenValidator;

    private TokenStore tokenStore;
    private UserInfoService userInfoService;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(TokenStore.class);
        userInfoService = mock(UserInfoService.class);

        Config<TokenStore> tokenStoreConfig = new Config<TokenStore>() {
            public boolean isReady() {
                return true;
            }

            public TokenStore get() {
                return tokenStore;
            }
        };

        accessTokenValidator = new OpenAMOAuth2AccessTokenValidator(tokenStoreConfig, userInfoService);
    }

    @Test
    public void shouldReturnValidResponseWhenTokenIsNotExpired() throws OAuth2Exception,
            org.forgerock.oauth2.core.exceptions.OAuth2Exception {

        //Given
        AccessToken accessToken = mock(AccessToken.class);
        Map<String, Object> userInfo = Collections.emptyMap();
        Set<String> scope = Collections.emptySet();

        given(tokenStore.readAccessToken("ACCESS_TOKEN")).willReturn(accessToken);
        given(accessToken.getExpiryTime()).willReturn(System.currentTimeMillis() + 1000);
        given(userInfoService.getUserInfo(eq("ACCESS_TOKEN"), Matchers.<OAuth2Request>anyObject()))
                .willReturn(new JsonValue(userInfo));
        given(accessToken.getScope()).willReturn(scope);

        //When
        AccessTokenValidationResponse validationResponse = accessTokenValidator.validate("ACCESS_TOKEN");

        //Then
        assertTrue(validationResponse.isTokenValid());
        assertEquals(validationResponse.getProfileInformation(), userInfo);
        assertEquals(validationResponse.getTokenScopes(), scope);
    }

    @Test
    public void shouldReturnInvalidResponseWhenTokenIsExpired() throws OAuth2Exception,
            org.forgerock.oauth2.core.exceptions.OAuth2Exception {

        //Given
        AccessToken accessToken = mock(AccessToken.class);
        Map<String, Object> userInfo = Collections.emptyMap();
        Set<String> scope = Collections.emptySet();

        given(tokenStore.readAccessToken("ACCESS_TOKEN")).willReturn(accessToken);
        given(accessToken.getExpiryTime()).willReturn(System.currentTimeMillis() - 1000);
        given(userInfoService.getUserInfo(eq("ACCESS_TOKEN"), Matchers.<OAuth2Request>anyObject()))
                .willReturn(new JsonValue(userInfo));
        given(accessToken.getScope()).willReturn(scope);

        //When
        AccessTokenValidationResponse validationResponse = accessTokenValidator.validate("ACCESS_TOKEN");

        //Then
        assertFalse(validationResponse.isTokenValid());
        assertEquals(validationResponse.getProfileInformation(), userInfo);
        assertEquals(validationResponse.getTokenScopes(), scope);
    }
}
