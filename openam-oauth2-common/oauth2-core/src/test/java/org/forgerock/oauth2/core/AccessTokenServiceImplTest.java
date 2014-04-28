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

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @since 12.0.0
 */
public class AccessTokenServiceImplTest {

    private AccessTokenServiceImpl accessTokenService;

    private GrantTypeHandler grantTypeHandler;
    private ClientAuthenticator clientAuthenticator;
    private TokenStore tokenStore;
    private OAuth2ProviderSettings providerSettings;

    @BeforeMethod
    public void setUp() {

        Map<String, GrantTypeHandler> grantTypeHandlers = new HashMap<String, GrantTypeHandler>();
        grantTypeHandler = mock(GrantTypeHandler.class);
        grantTypeHandlers.put("GRANT_TYPE", grantTypeHandler);
        clientAuthenticator = mock(ClientAuthenticator.class);
        tokenStore = mock(TokenStore.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);

        accessTokenService = new AccessTokenServiceImpl(grantTypeHandlers, clientAuthenticator, tokenStore,
                providerSettingsFactory);

        providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
    }

    @Test
    public void shouldRequestAccessToken() throws InvalidGrantException, RedirectUriMismatchException,
            ClientAuthenticationFailedException, UnauthorizedClientException, InvalidRequestException,
            InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("grant_type")).willReturn("GRANT_TYPE");

        //When
        accessTokenService.requestAccessToken(request);

        //Then
        verify(grantTypeHandler).handle(request);
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void requestAccessTokenShouldThrowInvalidGrantExceptionWhenGrantTypeDoesNotMatchHandler()
            throws InvalidGrantException, RedirectUriMismatchException, ClientAuthenticationFailedException,
            UnauthorizedClientException, InvalidRequestException, InvalidCodeException, InvalidClientException,
            ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("grant_type")).willReturn("UNKNOWN_GRANT_TYPE");

        //When
        accessTokenService.requestAccessToken(request);

        //Then
        // Expect InvalidGrantException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void refreshTokenShouldThrowIllegalArgumentExceptionWhenRefreshTokenMissing() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        //When
        accessTokenService.refreshToken(request);

        //Then
        // Expect IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void refreshTokenShouldThrowIllegalArgumentExceptionWhenRefreshTokenIsEmpty() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("refresh_token")).willReturn("");

        //When
        accessTokenService.refreshToken(request);

        //Then
        // Expect IllegalArgumentException
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void refreshTokenShouldThrowInvalidRequestExceptionWhenRefreshTokenNotFound() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        RefreshToken refreshToken = null;

        given(request.getParameter("refresh_token")).willReturn("REFRESH_TOKEN_ID");
        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);

        //When
        accessTokenService.refreshToken(request);

        //Then
        // Expect InvalidRequestException
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void refreshTokenShouldThrowInvalidRequestExceptionWhenClientIdsDontMatch() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        RefreshToken refreshToken = mock(RefreshToken.class);

        given(request.getParameter("refresh_token")).willReturn("REFRESH_TOKEN_ID");
        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("OTHER_CLIENT_ID");

        //When
        accessTokenService.refreshToken(request);

        //Then
        // Expect InvalidRequestException
    }

    @Test (expectedExceptions = ExpiredTokenException.class)
    public void refreshTokenShouldThrowExpiredTokenExceptionWhenRefreshTokenHasExpired() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        RefreshToken refreshToken = mock(RefreshToken.class);

        given(request.getParameter("refresh_token")).willReturn("REFRESH_TOKEN_ID");
        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.getExpiryTime()).willReturn(System.currentTimeMillis() - 10);

        //When
        accessTokenService.refreshToken(request);

        //Then
        // Expect ExpiredTokenException
    }

    @Test
    public void shouldRefreshToken() throws BadRequestException, ClientAuthenticationFailedException,
            InvalidRequestException, InvalidClientException, ServerException, ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        RefreshToken refreshToken = mock(RefreshToken.class);
        Set<String> validatedScope = new HashSet<String>();
        AccessToken accessToken = mock(AccessToken.class);

        given(request.getParameter("refresh_token")).willReturn("REFRESH_TOKEN_ID");
        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.getExpiryTime()).willReturn(System.currentTimeMillis() + 10);
        given(providerSettings.validateRefreshTokenScope(eq(clientRegistration), anySetOf(String.class),
                anySetOf(String.class), eq(request))).willReturn(validatedScope);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anySetOf(String.class), eq(refreshToken), anyString(), eq(request)))
                .willReturn(accessToken);

        //When
        AccessToken actualAccessToken = accessTokenService.refreshToken(request);

        //Then
        verify(providerSettings).additionalDataToReturnFromTokenEndpoint(accessToken, request);
        verify(accessToken, never()).addExtraData(eq("scope"), anyString());
        assertEquals(actualAccessToken, accessToken);
    }

    @Test
    public void shouldRefreshTokenAndIncludeScopeInAccessToken() throws BadRequestException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidClientException, ServerException,
            ExpiredTokenException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        RefreshToken refreshToken = mock(RefreshToken.class);
        Set<String> validatedScope = Collections.singleton("SCOPE");
        AccessToken accessToken = mock(AccessToken.class);

        given(request.getParameter("refresh_token")).willReturn("REFRESH_TOKEN_ID");
        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readRefreshToken("REFRESH_TOKEN_ID")).willReturn(refreshToken);
        given(refreshToken.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(refreshToken.getExpiryTime()).willReturn(System.currentTimeMillis() + 10);
        given(providerSettings.validateRefreshTokenScope(eq(clientRegistration), anySetOf(String.class),
                anySetOf(String.class), eq(request))).willReturn(validatedScope);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anySetOf(String.class), eq(refreshToken), anyString(), eq(request)))
                .willReturn(accessToken);

        //When
        AccessToken actualAccessToken = accessTokenService.refreshToken(request);

        //Then
        verify(providerSettings).additionalDataToReturnFromTokenEndpoint(accessToken, request);
        verify(accessToken).addExtraData(eq("scope"), anyString());
        assertEquals(actualAccessToken, accessToken);
    }
}
