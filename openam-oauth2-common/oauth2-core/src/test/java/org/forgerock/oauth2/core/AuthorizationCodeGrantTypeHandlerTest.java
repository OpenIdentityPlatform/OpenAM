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

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

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

    @BeforeMethod
    public void setUp() {

        List<AuthorizationCodeRequestValidator> requestValidators = new ArrayList<AuthorizationCodeRequestValidator>();
        requestValidator = mock(AuthorizationCodeRequestValidator.class);
        requestValidators.add(requestValidator);
        clientAuthenticator = mock(ClientAuthenticator.class);
        tokenStore = mock(TokenStore.class);
        tokenInvalidator = mock(TokenInvalidator.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);

        grantTypeHandler = new AuthorizationCodeGrantTypeHandler(requestValidators, clientAuthenticator, tokenStore,
                tokenInvalidator, providerSettingsFactory);

        providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void handleShouldThrowInvalidRequestExceptionWhenAuthorizationCodeNotFound() throws InvalidGrantException,
            RedirectUriMismatchException, ClientAuthenticationFailedException, InvalidRequestException,
            InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = null;

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);

        //When
        grantTypeHandler.handle(request);

        //Then
        // Expect InvalidRequestException
    }

    @Test
    public void handleShouldThrowInvalidGrantExceptionWhenAuthorizationCodeHasAlreadyBeenIssued()
            throws InvalidGrantException, RedirectUriMismatchException, ClientAuthenticationFailedException,
            InvalidRequestException, InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(true);

        try {
            //When
            grantTypeHandler.handle(request);
            fail();
        } catch (InvalidGrantException e) {
            //Then
            verify(requestValidator).validateRequest(request, clientRegistration);
            verify(tokenInvalidator).invalidateTokens(anyString());
            verify(tokenStore).deleteAuthorizationCode(anyString());
        }
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void handleShouldThrowInvalidGrantExceptionWhenRedirectUriDontMatch() throws InvalidGrantException,
            RedirectUriMismatchException, ClientAuthenticationFailedException, InvalidRequestException,
            InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
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
    public void handleShouldThrowInvalidGrantExceptionWhenClientDoesNotMatch() throws InvalidGrantException,
            RedirectUriMismatchException, ClientAuthenticationFailedException, InvalidRequestException,
            InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
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
    public void handleShouldThrowInvalidCodeExceptionWhenAuthorizationCodeHasExpired() throws InvalidGrantException,
            RedirectUriMismatchException, ClientAuthenticationFailedException, InvalidRequestException,
            InvalidCodeException, InvalidClientException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(System.currentTimeMillis() - 10);

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

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(System.currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(true);
        given(tokenStore.createRefreshToken(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                eq(request))).willReturn(refreshToken);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), eq(request)))
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

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(System.currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), eq(request)))
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
    public void shouldHandleOneAccessTokenRequestPerCodeAtATime() throws Exception {

        //Given
        final OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        final AccessToken accessToken = mock(AccessToken.class);
        Set<String> validatedScope = new HashSet<String>();

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        final Holder holder = new Holder();
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                holder.value++;
                return authorizationCode;
            }
        });
        given(authorizationCode.isIssued()).willAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return holder.value > 1;
            }
        });
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(System.currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), eq(request)))
                .willAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        while(holder.value < 2) {
                            Thread.sleep(100);
                        }
                        Thread.sleep(100);
                        return accessToken;
                    }
                });
        given(providerSettings.validateAccessTokenScope(eq(clientRegistration), anySetOf(String.class), eq(request)))
                .willReturn(validatedScope);

        // When/Then
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AccessToken actualAccessToken = grantTypeHandler.handle(request);
                    assertEquals(actualAccessToken, accessToken);
                } catch (Exception e) {
                    holder.thread1Failure = new AssertionError("First thread should succeed");
                }
            }
        });
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    grantTypeHandler.handle(request);
                    holder.thread1Failure = new AssertionError("Second thread should have already issued authorization code");
                } catch (InvalidGrantException e) {
                    // pass
                } catch (Exception e) {
                    holder.thread1Failure = new AssertionError("Wrong exception");
                }
            }
        });
        thread2.start();

        thread1.join();
        thread2.join();

        if (holder.thread1Failure != null) {
            fail(holder.thread1Failure.getMessage());
        }
        if (holder.thread2Failure != null) {
            fail(holder.thread2Failure.getMessage());
        }
    }

    @Test
    public void shouldHandleAndIncludeScopeInAccessToken() throws InvalidGrantException, RedirectUriMismatchException,
            ClientAuthenticationFailedException, InvalidRequestException, InvalidCodeException, InvalidClientException,
            ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("code")).willReturn("abc123");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        AccessToken accessToken = mock(AccessToken.class);
        Set<String> validatedScope = Collections.singleton("SCOPE");

        given(clientAuthenticator.authenticate(request)).willReturn(clientRegistration);
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");
        given(tokenStore.readAuthorizationCode(eq(request), anyString())).willReturn(authorizationCode);
        given(authorizationCode.isIssued()).willReturn(false);
        given(authorizationCode.getRedirectUri()).willReturn("REDIRECT_URI");
        given(authorizationCode.getClientId()).willReturn("CLIENT_ID");
        given(clientRegistration.getClientId()).willReturn("CLIENT_ID");
        given(authorizationCode.getExpiryTime()).willReturn(System.currentTimeMillis() + 100);
        given(providerSettings.issueRefreshTokens()).willReturn(false);
        given(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), Matchers.<RefreshToken>anyObject(), anyString(), eq(request)))
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
