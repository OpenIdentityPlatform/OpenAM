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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @since 12.0.0
 */
public class AuthorizationCodeGrantTypeHandlerTest {

    private AuthorizationCodeGrantTypeHandler grantTypeHandler;

    private AuthorizationCodeRequestValidator requestValidator;
    private ClientAuthenticator clientAuthenticator;
    private TokenStore tokenStore;
    private TokenInvalidator tokenInvalidator;
    private OAuth2ProviderSettings providerSettings;
    private OAuth2Uris uris;

    @BeforeMethod
    public void setUp() throws Exception {

        List<AuthorizationCodeRequestValidator> requestValidators = new ArrayList<AuthorizationCodeRequestValidator>();
        requestValidator = mock(AuthorizationCodeRequestValidator.class);
        requestValidators.add(requestValidator);
        clientAuthenticator = mock(ClientAuthenticator.class);
        tokenStore = mock(TokenStore.class);
        tokenInvalidator = mock(TokenInvalidator.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        GrantTypeAccessTokenGenerator accessTokenGenerator = new GrantTypeAccessTokenGenerator(tokenStore);

        OAuth2UrisFactory urisFactory = mock(OAuth2UrisFactory.class);
        grantTypeHandler = new AuthorizationCodeGrantTypeHandler(requestValidators, clientAuthenticator, tokenStore,
                tokenInvalidator, providerSettingsFactory, urisFactory, accessTokenGenerator);

        providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);

        uris = mock(OAuth2Uris.class);
        given(urisFactory.get(any(OAuth2Request.class))).willReturn(uris);
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void handleShouldThrowInvalidRequestExceptionWhenAuthorizationCodeNotFound() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = null;

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);

        //When
        grantTypeHandler.handle(request);

        //Then
        // Expect InvalidRequestException
    }

    @Test
    public void handleShouldThrowInvalidGrantExceptionWhenAuthorizationCodeHasAlreadyBeenIssued()
            throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(true);

        try {
            //When
            grantTypeHandler.handle(request);
            fail("Expected exception as authorization code has already been issued");
        } catch (InvalidGrantException e) {
            //Then
            verify(requestValidator).validateRequest(request, clientRegistration);
            verify(tokenInvalidator).invalidateTokens(anyString());
            verify(tokenStore).deleteAuthorizationCode(anyString());
        }
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void handleShouldThrowInvalidGrantExceptionWhenRedirectUriDontMatch() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("OTHER_REDIRECT_URI");

        //When
        grantTypeHandler.handle(request);

        //Then
        // Expect InvalidGrantException
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void handleShouldThrowInvalidGrantExceptionWhenClientDoesNotMatch() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("OTHER_CLIENT_ID");

        //When
        grantTypeHandler.handle(request);

        //Then
        // Expect InvalidGrantException
    }

    @Test (expectedExceptions = InvalidCodeException.class)
    public void handleShouldThrowInvalidCodeExceptionWhenAuthorizationCodeHasExpired() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(currentTimeMillis() - 10);

        //When
        grantTypeHandler.handle(request);

        //Then
        // Expect InvalidCodeException
    }

    @Test
    public void shouldHandleAndIssueRefreshToken() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        RefreshToken refreshToken = mock(RefreshToken.class);
        AccessToken accessToken = mock(AccessToken.class);
        Set<String> validatedScope = new HashSet<String>();

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(true);
        given(tokenStore.createRefreshToken(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                eq(request), isNull(String.class))).willReturn(refreshToken);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), anyString(), eq(request)))
                .willReturn(accessToken);
        given(providerSettings.validateAccessTokenScope(eq(clientRegistration), anySetOf(String.class), eq(request)))
                .willReturn(validatedScope);

        //When
        AccessToken actualAccessToken = grantTypeHandler.handle(request);

        //Then
        verify(requestValidator).validateRequest(request, clientRegistration);
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken).addExtraData(eq("refresh_token"), anyString());
        verify(accessToken).addExtraData(eq("nonce"), anyString());
        verify(providerSettings).additionalDataToReturnFromTokenEndpoint(accessToken, request);
        verify(accessToken, never()).addExtraData(eq("scope"), anyString());
        assertEquals(actualAccessToken, accessToken);
    }

    @Test
    public void shouldHandle() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        AccessToken accessToken = mock(AccessToken.class);
        Set<String> validatedScope = new HashSet<String>();

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), anyString(), eq(request)))
                .willReturn(accessToken);
        given(providerSettings.validateAccessTokenScope(eq(clientRegistration), anySetOf(String.class), eq(request)))
                .willReturn(validatedScope);

        //When
        AccessToken actualAccessToken = grantTypeHandler.handle(request);

        //Then
        verify(requestValidator).validateRequest(request, clientRegistration);
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken, never()).addExtraData(eq("refresh_token"), anyString());
        verify(accessToken).addExtraData(eq("nonce"), anyString());
        verify(providerSettings).additionalDataToReturnFromTokenEndpoint(accessToken, request);
        verify(accessToken, never()).addExtraData(eq("scope"), anyString());
        assertEquals(actualAccessToken, accessToken);
    }

    @Test
    public void shouldHandleAndIncludeScopeInAccessToken() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        AccessToken accessToken = mock(AccessToken.class);
        Set<String> validatedScope = Collections.singleton("SCOPE");

        given(uris.getTokenEndpoint()).willReturn("Token Endpoint");
        given(clientAuthenticator.authenticate(request, "Token Endpoint")).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), anyString(), eq(request)))
                .willReturn(accessToken);
        given(authorizationCode.getScope()).willReturn(validatedScope);

        //When
        AccessToken actualAccessToken = grantTypeHandler.handle(request);

        //Then
        verify(requestValidator).validateRequest(request, clientRegistration);
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken, never()).addExtraData(eq("refresh_token"), anyString());
        verify(accessToken).addExtraData(eq("nonce"), anyString());
        verify(providerSettings).additionalDataToReturnFromTokenEndpoint(accessToken, request);
        verify(accessToken).addExtraData(eq("scope"), anyString());
        assertEquals(actualAccessToken, accessToken);
    }

    private static class Holder {
        int value = 0;
        Throwable thread1Failure;
        Throwable thread2Failure;
    }
}
