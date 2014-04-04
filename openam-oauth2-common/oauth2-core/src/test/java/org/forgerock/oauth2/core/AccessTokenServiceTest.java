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

import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.PASSWORD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @since 12.0.0
 */
public class AccessTokenServiceTest {

    private AccessTokenService accessTokenService;

    private Map<GrantType, GrantTypeHandler> grantTypeHandlers;
    private ClientAuthenticator clientAuthenticator;
    private TokenStore tokenStore;
    private ScopeValidator scopeValidator;

    private GrantTypeHandler grantTypeHandler;

    @BeforeMethod
    public void setUp() {

        grantTypeHandlers = new HashMap<GrantType, GrantTypeHandler>();
        clientAuthenticator = mock(ClientAuthenticator.class);
        tokenStore = mock(TokenStore.class);
        scopeValidator = mock(ScopeValidator.class);

        accessTokenService = new AccessTokenService(grantTypeHandlers, clientAuthenticator, tokenStore, scopeValidator);

        grantTypeHandler = mock(GrantTypeHandler.class);
        grantTypeHandlers.put(AUTHORIZATION_CODE, grantTypeHandler);
    }

    @Test
    public void shouldRequestAccessToken() throws OAuth2Exception {

        //Given
        final AccessTokenRequest accessTokenRequest = mock(AccessTokenRequest.class);

        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);

        //When
        accessTokenService.requestAccessToken(accessTokenRequest);

        //Then
        verify(grantTypeHandler).handle(accessTokenRequest);
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldFailToRequestAccessTokenIfGrantTypeNotKnown() throws OAuth2Exception {

        //Given
        final AccessTokenRequest accessTokenRequest = mock(AccessTokenRequest.class);

        given(accessTokenRequest.getGrantType()).willReturn(PASSWORD);

        //When
        accessTokenService.requestAccessToken(accessTokenRequest);

        //Then
    }

    @Test
    public void shouldFailToRefreshTokenWhenRefreshTokenDoesNotExistInTokenStore() throws OAuth2Exception {

        //Given
        final RefreshTokenRequest refreshTokenRequest = mock(RefreshTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(refreshTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(refreshTokenRequest.getRefreshToken()).willReturn("REFRESH_TOKEN_ID");
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(null);

        //When
        try {
            accessTokenService.refreshToken(refreshTokenRequest);
            fail();
        } catch (InvalidRequestException e) {
            //Then
            assertTrue(e.getMessage().toLowerCase().contains("does not exist"));
        }
    }

    @Test
    public void shouldFailToRefreshTokenWhenTokenIssuedToDifferentClient() throws OAuth2Exception {

        //Given
        final RefreshTokenRequest refreshTokenRequest = mock(RefreshTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final RefreshToken refreshToken = mock(RefreshToken.class);

        given(refreshTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(refreshTokenRequest.getRefreshToken()).willReturn("REFRESH_TOKEN_ID");
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("OTHER_CLIENT_ID");

        //When
        try {
            accessTokenService.refreshToken(refreshTokenRequest);
            fail();
        } catch (InvalidRequestException e) {
            //Then
            assertTrue(e.getMessage().toLowerCase().contains("issued to a different client"));
        }
    }

    @Test (expectedExceptions = ExpiredTokenException.class)
    public void shouldFailToRefreshTokenWhenTokenHasExpired() throws OAuth2Exception {

        //Given
        final RefreshTokenRequest refreshTokenRequest = mock(RefreshTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final RefreshToken refreshToken = mock(RefreshToken.class);

        given(refreshTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(refreshTokenRequest.getRefreshToken()).willReturn("REFRESH_TOKEN_ID");
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.isExpired()).willReturn(true);

        //When
        accessTokenService.refreshToken(refreshTokenRequest);

        //Then
    }

    @Test
    public void shouldRefreshTokenIncludingScope() throws OAuth2Exception {

        //Given
        final RefreshTokenRequest refreshTokenRequest = mock(RefreshTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final RefreshToken refreshToken = mock(RefreshToken.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.singleton("SCOPE");
        final AccessToken accessToken = mock(AccessToken.class);

        given(refreshTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(refreshTokenRequest.getRefreshToken()).willReturn("REFRESH_TOKEN_ID");
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.isExpired()).willReturn(false);
        given(refreshToken.getScope()).willReturn(scope);
        given(scopeValidator.validateRefreshTokenScope(eq(clientRegistration), anySetOf(String.class),
                anySetOf(String.class), anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(refreshTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(refreshToken.getUserId()).willReturn("USER_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("USER_ID"), eq(clientRegistration),
                eq(validatedScope), eq(refreshToken), anyMapOf(String.class, Object.class))).willReturn(accessToken);

        //When
        accessTokenService.refreshToken(refreshTokenRequest);

        //Then
        verify(scopeValidator).addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken),
                anyMapOf(String.class, Object.class));
        verify(accessToken).add("scope", "SCOPE");
    }

    @Test
    public void shouldRefreshToken() throws OAuth2Exception {

        //Given
        final RefreshTokenRequest refreshTokenRequest = mock(RefreshTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final RefreshToken refreshToken = mock(RefreshToken.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.emptySet();
        final AccessToken accessToken = mock(AccessToken.class);

        given(refreshTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(refreshTokenRequest.getRefreshToken()).willReturn("REFRESH_TOKEN_ID");
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.isExpired()).willReturn(false);
        given(refreshToken.getScope()).willReturn(scope);
        given(scopeValidator.validateRefreshTokenScope(eq(clientRegistration), eq(scope), anySetOf(String.class),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(refreshTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(refreshToken.getUserId()).willReturn("USER_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("USER_ID"), eq(clientRegistration),
                eq(validatedScope), eq(refreshToken), anyMapOf(String.class, Object.class))).willReturn(accessToken);

        //When
        accessTokenService.refreshToken(refreshTokenRequest);

        //Then
        verify(scopeValidator).addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken),
                anyMapOf(String.class, Object.class));
        verify(accessToken, never()).add(eq("scope"), anyString());
    }
}
