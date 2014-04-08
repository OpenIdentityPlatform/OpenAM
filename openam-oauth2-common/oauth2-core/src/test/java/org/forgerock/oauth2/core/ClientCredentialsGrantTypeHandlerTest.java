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
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.forgerock.oauth2.core.AccessTokenRequest.ClientCredentialsAccessTokenRequest;

/**
 * @since 12.0.0
 */
public class ClientCredentialsGrantTypeHandlerTest {

    private ClientCredentialsGrantTypeHandler grantTypeHandler;

    private ClientAuthenticator clientAuthenticator;
    private ScopeValidator scopeValidator;
    private TokenStore tokenStore;

    @BeforeMethod
    public void setUp() {

        clientAuthenticator = mock(ClientAuthenticator.class);
        scopeValidator = mock(ScopeValidator.class);
        tokenStore = mock(TokenStore.class);

        grantTypeHandler = new ClientCredentialsGrantTypeHandler(clientAuthenticator, scopeValidator, tokenStore);
    }

    @Test
    public void shouldFailToCreateAccessTokenWhenClientIsNotConfidential() throws UnauthorizedClientException,
            InvalidClientException {

        //Given
        final ClientCredentialsAccessTokenRequest accessTokenRequest = mock(ClientCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(clientRegistration.isConfidential()).willReturn(false);

        //When
        try {
            grantTypeHandler.handle(accessTokenRequest);
            fail();
        } catch (UnauthorizedClientException e) {
            //Then
            assertTrue(e.getMessage().toLowerCase().contains("public clients can't use client credentials grant"));
        }
    }

    @Test
    public void shouldCreateAccessTokenIncludingScope() throws UnauthorizedClientException, InvalidClientException {

        //Given
        final ClientCredentialsAccessTokenRequest accessTokenRequest = mock(ClientCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.singleton("SCOPE");
        final AccessToken accessToken = mock(AccessToken.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(clientRegistration.isConfidential()).willReturn(true);
        given(accessTokenRequest.getScope()).willReturn(scope);
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("CLIENT_ID"), eq(clientRegistration),
                eq(validatedScope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);

        //When
        final AccessToken token = grantTypeHandler.handle(accessTokenRequest);

        //Then
        assertEquals(token, accessToken);
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken).add("scope", "SCOPE");
    }

    @Test
    public void shouldCreateAccessToken() throws UnauthorizedClientException, InvalidClientException {

        //Given
        final ClientCredentialsAccessTokenRequest accessTokenRequest = mock(ClientCredentialsAccessTokenRequest.class);
        final ClientCredentials clientCredentials = new ClientCredentials("USER", "".toCharArray());
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final Set<String> scope = Collections.singleton("SCOPE");
        final Set<String> validatedScope = Collections.emptySet();
        final AccessToken accessToken = mock(AccessToken.class);

        given(accessTokenRequest.getClientCredentials()).willReturn(clientCredentials);
        given(clientAuthenticator.authenticate(eq(clientCredentials), anyMapOf(String.class, Object.class)))
                .willReturn(clientRegistration);
        given(clientRegistration.isConfidential()).willReturn(true);
        given(accessTokenRequest.getScope()).willReturn(scope);
        given(scopeValidator.validateAccessTokenScope(eq(clientRegistration), eq(scope),
                anyMapOf(String.class, Object.class))).willReturn(validatedScope);
        given(accessTokenRequest.getGrantType()).willReturn(AUTHORIZATION_CODE);
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(tokenStore.createAccessToken(eq(AUTHORIZATION_CODE), eq("CLIENT_ID"), eq(clientRegistration),
                eq(validatedScope), Matchers.<RefreshToken>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(accessToken);

        //When
        final AccessToken token = grantTypeHandler.handle(accessTokenRequest);

        //Then
        assertEquals(token, accessToken);
        verify(scopeValidator)
                .addAdditionalDataToReturnFromTokenEndpoint(eq(accessToken), anyMapOf(String.class, Object.class));
        verify(accessToken, never()).add(eq("scope"), anyString());
    }
}
