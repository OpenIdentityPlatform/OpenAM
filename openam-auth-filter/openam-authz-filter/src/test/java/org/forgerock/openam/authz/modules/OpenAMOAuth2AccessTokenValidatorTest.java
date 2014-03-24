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
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.oauth2.core.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.utils.Config;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @since 12.0.0
 */
public class OpenAMOAuth2AccessTokenValidatorTest {

    private OpenAMOAuth2AccessTokenValidator accessTokenValidator;

    private OAuth2TokenStore tokenStore;
    private OAuth2UserInfoService userInfoService;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(OAuth2TokenStore.class);
        userInfoService = mock(OAuth2UserInfoService.class);

        Config<OAuth2TokenStore> tokenStoreConfig = new Config<OAuth2TokenStore>() {
            public boolean isReady() {
                return true;
            }

            public OAuth2TokenStore get() {
                return tokenStore;
            }
        };

        accessTokenValidator = new OpenAMOAuth2AccessTokenValidator(tokenStoreConfig, userInfoService);
    }

    @Test (expectedExceptions = OAuth2Exception.class)
    public void shouldThrowOAuth2ExceptionWhenOAuthProblemExceptionCaught() throws OAuth2Exception {

        //Given
        OAuthProblemException exception = mock(OAuthProblemException.class);
        given(exception.getDescription()).willReturn("DESCRIPTION");
        doThrow(exception).when(tokenStore).readAccessToken("ACCESS_TOKEN");

        //When
        accessTokenValidator.validate("ACCESS_TOKEN");

        //Then
        fail();
    }

    @Test
    public void shouldReturnValidResponseWhenTokenIsNotExpired() throws OAuth2Exception {

        //Given
        CoreToken token = mock(CoreToken.class);
        Map<String, Object> userInfo = Collections.emptyMap();
        Set<String> scope = Collections.emptySet();

        given(tokenStore.readAccessToken("ACCESS_TOKEN")).willReturn(token);
        given(token.getExpireTime()).willReturn(System.currentTimeMillis() + 1000);
        given(userInfoService.getUserInfo(token)).willReturn(userInfo);
        given(token.getScope()).willReturn(scope);

        //When
        AccessTokenValidationResponse validationResponse = accessTokenValidator.validate("ACCESS_TOKEN");

        //Then
        assertTrue(validationResponse.isTokenValid());
        assertEquals(validationResponse.getProfileInformation(), userInfo);
        assertEquals(validationResponse.getTokenScopes(), scope);
    }

    @Test
    public void shouldReturnInvalidResponseWhenTokenIsExpired() throws OAuth2Exception {

        //Given
        CoreToken token = mock(CoreToken.class);
        Map<String, Object> userInfo = Collections.emptyMap();
        Set<String> scope = Collections.emptySet();

        given(tokenStore.readAccessToken("ACCESS_TOKEN")).willReturn(token);
        given(token.getExpireTime()).willReturn(System.currentTimeMillis() - 1000);
        given(userInfoService.getUserInfo(token)).willReturn(userInfo);
        given(token.getScope()).willReturn(scope);

        //When
        AccessTokenValidationResponse validationResponse = accessTokenValidator.validate("ACCESS_TOKEN");

        //Then
        assertFalse(validationResponse.isTokenValid());
        assertEquals(validationResponse.getProfileInformation(), userInfo);
        assertEquals(validationResponse.getTokenScopes(), scope);
    }
}
