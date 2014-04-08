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

import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.forgerock.oauth2.core.AccessTokenRequest.PasswordCredentialsAccessTokenRequest;

/**
 * @since 12.0.0
 */
public class PasswordGrantTypeHandlerTest {

    private PasswordGrantTypeHandler grantTypeHandler;

    private ClientAuthenticator clientAuthenticator;
    private ScopeValidator scopeValidator;
    private TokenStore tokenStore;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;

    @BeforeMethod
    public void setUp() {

        clientAuthenticator = mock(ClientAuthenticator.class);
        scopeValidator = mock(ScopeValidator.class);
        tokenStore = mock(TokenStore.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);

        grantTypeHandler = new PasswordGrantTypeHandler(clientAuthenticator, scopeValidator, tokenStore,
                providerSettingsFactory);
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldFailToCreateAccessTokenWhenResourceOwnerCannotBeFound() throws OAuth2Exception {

        //Given
        final PasswordCredentialsAccessTokenRequest accessTokenRequest =
                mock(PasswordCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getAuthenticationHandler()).willReturn(authenticationHandler);
        given(authenticationHandler.authenticate()).willReturn(null);

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
    }

    @Test
    public void shouldCreateAccessTokenWithoutRefreshToken() throws OAuth2Exception {

        //Given
        final PasswordCredentialsAccessTokenRequest accessTokenRequest =
                mock(PasswordCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        final ResourceOwner resourceOwner = mock(ResourceOwner.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.singleton("SCOPE");
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final AccessToken accessToken = mock(AccessToken.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getAuthenticationHandler()).willReturn(authenticationHandler);
        given(authenticationHandler.authenticate()).willReturn(resourceOwner);
        given(accessTokenRequest.getScope()).willReturn(scope);
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(resourceOwner.getId()).willReturn("RESOURCE_OWNER_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(scope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
        verify(accessToken, never()).add(eq(OAuth2Constants.Params.REFRESH_TOKEN), anyString());
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken).add("scope", "SCOPE");
    }

    @Test
    public void shouldCreateAccessTokenWithoutRefreshTokenAndScope() throws OAuth2Exception {

        //Given
        final PasswordCredentialsAccessTokenRequest accessTokenRequest =
                mock(PasswordCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        final ResourceOwner resourceOwner = mock(ResourceOwner.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.emptySet();
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final AccessToken accessToken = mock(AccessToken.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getAuthenticationHandler()).willReturn(authenticationHandler);
        given(authenticationHandler.authenticate()).willReturn(resourceOwner);
        given(accessTokenRequest.getScope()).willReturn(scope);
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(resourceOwner.getId()).willReturn("RESOURCE_OWNER_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(validatedScope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
        verify(accessToken, never()).add(eq(OAuth2Constants.Params.REFRESH_TOKEN), anyString());
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken, never()).add(eq("scope"), anyString());
    }

    @Test
    public void shouldCreateAccessTokenWithRefreshTokenAndScope() throws OAuth2Exception {

        //Given
        final PasswordCredentialsAccessTokenRequest accessTokenRequest =
                mock(PasswordCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        final ResourceOwner resourceOwner = mock(ResourceOwner.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.singleton("SCOPE");
        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        final RefreshToken refreshToken = mock(RefreshToken.class);
        final AccessToken accessToken = mock(AccessToken.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(accessTokenRequest.getAuthenticationHandler()).willReturn(authenticationHandler);
        given(authenticationHandler.authenticate()).willReturn(resourceOwner);
        given(accessTokenRequest.getScope()).willReturn(scope);
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(providerSettingsFactory.getProviderSettings(anyMapOf(String.class, Object.class)))
                .willReturn(providerSettings);
        given(providerSettings.issueRefreshTokens()).willReturn(true);
        given(tokenStore.createRefreshToken(eq(AUTHORIZATION_CODE), eq(clientRegistration), eq("RESOURCE_OWNER_ID"),
                anyString(), eq(validatedScope), anyMapOf(String.class, Object.class))).willReturn(refreshToken);
        given(refreshToken.getTokenId()).willReturn("REFRESH_TOKEN_ID");
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(resourceOwner.getId()).willReturn("RESOURCE_OWNER_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("RESOURCE_OWNER_ID"), eq(clientRegistration),
                eq(scope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);

        //When
        grantTypeHandler.handle(accessTokenRequest);

        //Then
        verify(accessToken).add(OAuth2Constants.Params.REFRESH_TOKEN, "REFRESH_TOKEN_ID");
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken).add("scope", "SCOPE");
    }
}
