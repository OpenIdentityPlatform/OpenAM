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

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.forgerock.oauth2.core.AccessTokenRequest.AuthorizationCodeAccessTokenRequest;

/**
 * @since 12.0.0
 */
public class AuthorizationCodeGrantTypeHandlerTest {

    private AuthorizationCodeGrantTypeHandler grantTypeHandler;

    private ClientAuthenticator clientAuthenticator;
    private TokenStore tokenStore;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    private ScopeValidator scopeValidator;
    private RedirectUriValidator redirectUriValidator;
    private TokenInvalidator tokenInvalidator;

    @BeforeMethod
    public void setUp() {

        clientAuthenticator = mock(ClientAuthenticator.class);
        tokenStore = mock(TokenStore.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        scopeValidator = mock(ScopeValidator.class);
        redirectUriValidator = mock(RedirectUriValidator.class);
        tokenInvalidator = mock(TokenInvalidator.class);

        grantTypeHandler = new AuthorizationCodeGrantTypeHandler(clientAuthenticator, tokenStore,
                providerSettingsFactory, scopeValidator, redirectUriValidator, tokenInvalidator);
    }

    @Test
    public void shouldFailToCreateAccessTokenWhenAuthorizationCodeDoesNotExist() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(null);

        //When
        try {
            grantTypeHandler.handle(accessTokenRequest);
            fail();
        } catch (InvalidRequestException e) {
            //Then
            assertTrue(e.getMessage().toLowerCase().contains("code doesn't exist"));
        }
    }

    @Test
    public void shouldFailToCreateAccessTokenWhenAuthorizationCodeHasAlreadyBeenIssued() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(true);

        //When
        try {
            grantTypeHandler.handle(accessTokenRequest);
            fail();
        } catch (InvalidGrantException e) {
            //Then
            verify(redirectUriValidator).validate(clientRegistration, "REDIRECT_URI");
            verify(tokenInvalidator).invalidateTokens("CODE");
            verify(tokenStore).deleteAuthorizationCode("CODE");
        }
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldFailToCreateAccessTokenWhenRedirectUriDoesNotMatch() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("OTHER_REDIRECT_URI");

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldFailToCreateAccessTokenWhenClientIdDoesNotMatch() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("OTHER_CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
    }

    @Test
    public void shouldFailToCreateAccessTokenWhenAuthorizationCodeHasExpired() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.isExpired()).willReturn(true);

        //When
        try {
            grantTypeHandler.handle(accessTokenRequest);
            fail();
        } catch (InvalidCodeException e) {
            //Then
            verify(redirectUriValidator).validate(clientRegistration, "REDIRECT_URI");
            assertTrue(e.getMessage().toLowerCase().contains("code expired"));
        }
    }

    @Test
    public void shouldCreateAccessTokenWithoutRefreshToken() throws InvalidGrantException,
            InvalidCodeException, UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException,
            InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final AccessToken accessToken = mock(AccessToken.class);
        final Set<String> validatedScope = Collections.singleton("SCOPE");

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.isExpired()).willReturn(false);
        given(authorizationCode.getResourceOwnerId()).willReturn("RESOURCE_OWNER_ID");
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(scope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);
        given(authorizationCode.getNonce()).willReturn("NONCE");
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);

        //When
        final AccessToken token = grantTypeHandler.handle(accessTokenRequest);

        //Then
        assertEquals(token, accessToken);
        verify(redirectUriValidator).validate(clientRegistration, "REDIRECT_URI");

        verify(tokenStore, never()).createRefreshToken(eq(AUTHORIZATION_CODE), eq(clientRegistration),
                eq("RESOURCE_OWNER_ID"), eq("REDIRECT_URI"), eq(scope), anyMapOf(String.class, Object.class));
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken, never()).add(eq(OAuth2Constants.Params.REFRESH_TOKEN), anyString());
        verify(accessToken).add(OAuth2Constants.Custom.NONCE, "NONCE");
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken).add("scope", "SCOPE");
    }

    @Test
    public void shouldCreateAccessTokenWithoutRefreshTokenAndScope() throws InvalidGrantException, InvalidCodeException,
            UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException, InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final AccessToken accessToken = mock(AccessToken.class);
        final Set<String> validatedScope = Collections.emptySet();

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.isExpired()).willReturn(false);
        given(authorizationCode.getResourceOwnerId()).willReturn("RESOURCE_OWNER_ID");
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(scope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);
        given(authorizationCode.getNonce()).willReturn("NONCE");
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);

        //When
        final AccessToken token = grantTypeHandler.handle(accessTokenRequest);

        //Then
        assertEquals(token, accessToken);
        verify(redirectUriValidator).validate(clientRegistration, "REDIRECT_URI");

        verify(tokenStore, never()).createRefreshToken(eq(AUTHORIZATION_CODE), eq(clientRegistration),
                eq("RESOURCE_OWNER_ID"), eq("REDIRECT_URI"), eq(scope), anyMapOf(String.class, Object.class));
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken, never()).add(eq(OAuth2Constants.Params.REFRESH_TOKEN), anyString());
        verify(accessToken).add(OAuth2Constants.Custom.NONCE, "NONCE");
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken, never()).add(eq("scope"), anyString());
    }

    @Test
    public void shouldCreateAccessTokenWithRefreshToken() throws InvalidGrantException, InvalidCodeException,
            UnauthorizedClientException, RedirectUriMismatchException, InvalidRequestException, InvalidClientException {

        //Given
        final AuthorizationCodeAccessTokenRequest accessTokenRequest = mock(AuthorizationCodeAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final AccessToken accessToken = mock(AccessToken.class);
        final Set<String> validatedScope = Collections.emptySet();

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getCode()).willReturn("CODE");
        given(tokenStore.getAuthorizationCode("CODE")).willReturn(authorizationCode);
        given(accessTokenRequest.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getScope()).willReturn(scope);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.isExpired()).willReturn(false);
        given(authorizationCode.getResourceOwnerId()).willReturn("RESOURCE_OWNER_ID");
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(true);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(scope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);
        given(authorizationCode.getNonce()).willReturn("NONCE");
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);

        //When
        final AccessToken token = grantTypeHandler.handle(accessTokenRequest);

        //Then
        assertEquals(token, accessToken);
        verify(redirectUriValidator).validate(clientRegistration, "REDIRECT_URI");

        verify(tokenStore).createRefreshToken(eq(AUTHORIZATION_CODE), eq(clientRegistration),
                eq("RESOURCE_OWNER_ID"), eq("REDIRECT_URI"), eq(scope), anyMapOf(String.class, Object.class));
        verify(authorizationCode).setIssued();
        verify(tokenStore).updateAuthorizationCode(authorizationCode);
        verify(accessToken, never()).add(eq(OAuth2Constants.Params.REFRESH_TOKEN), anyString());
        verify(accessToken).add(OAuth2Constants.Custom.NONCE, "NONCE");
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken, never()).add(eq("scope"), anyString());
    }
}
