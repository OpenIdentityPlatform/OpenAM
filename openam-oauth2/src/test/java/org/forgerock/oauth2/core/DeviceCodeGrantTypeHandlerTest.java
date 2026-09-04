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
 * Portions copyright 2026 3A Systems, LLC.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.openam.oauth2.OAuth2Constants.DeviceCode.DEVICE_CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.AuthorizationDeclinedException;
import org.forgerock.oauth2.core.exceptions.AuthorizationPendingException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests for DeviceCodeGrantTypeHandler.
 */
public class DeviceCodeGrantTypeHandlerTest {

    private DeviceCodeGrantTypeHandler grantTypeHandler;

    private TokenStore tokenStore;
    private OAuth2ProviderSettings providerSettings;
    private GrantTypeAccessTokenGenerator accessTokenGenerator;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(TokenStore.class);
        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        ClientAuthenticationFailureFactory failureFactory =
                mock(ClientAuthenticationFailureFactory.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory =
                mock(OAuth2ProviderSettingsFactory.class);
        ClientAuthenticator clientAuthenticator = mock(ClientAuthenticator.class);
        OAuth2UrisFactory urisFactory = mock(OAuth2UrisFactory.class);
        accessTokenGenerator = mock(GrantTypeAccessTokenGenerator.class);

        grantTypeHandler = new DeviceCodeGrantTypeHandler(
                providerSettingsFactory,
                clientAuthenticator,
                tokenStore,
                clientRegistrationStore,
                failureFactory,
                urisFactory,
                accessTokenGenerator);

        providerSettings = mock(OAuth2ProviderSettings.class);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void handleShouldThrowBadRequestExceptionWhenDeviceCodeIsMissing() throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);

        given(request.getParameter(DEVICE_CODE)).willReturn(null);

        // When
        grantTypeHandler.handle(request, client, providerSettings);

        // Then
        // Expect BadRequestException
    }

    @Test(expectedExceptions = AuthorizationDeclinedException.class)
    public void handleShouldThrowAuthorizationDeclinedExceptionWhenDeviceCodeIsNotFound()
            throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);

        given(request.getParameter(DEVICE_CODE)).willReturn("DEVICE_CODE");
        given(client.getClientId()).willReturn("CLIENT_ID");
        given(tokenStore.readDeviceCode("CLIENT_ID", "DEVICE_CODE", request)).willReturn(null);

        // When
        grantTypeHandler.handle(request, client, providerSettings);

        // Then
        // Expect AuthorizationDeclinedException
    }

    @Test
    public void shouldGenerateAccessTokenAndDeleteDeviceCodeWhenAuthorized()
            throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);
        DeviceCode deviceCode = mock(DeviceCode.class);
        AccessToken accessToken = mock(AccessToken.class);

        Set<String> scope = Collections.singleton("openid");

        given(request.getParameter(DEVICE_CODE)).willReturn("DEVICE_CODE");
        given(request.getParameter(REALM)).willReturn("/REALM");
        given(request.getParameter(OAuth2Constants.Params.GRANT_TYPE))
                .willReturn("urn:ietf:params:oauth:grant-type:device_code");

        given(client.getClientId()).willReturn("CLIENT_ID");

        given(tokenStore.readDeviceCode("CLIENT_ID", "DEVICE_CODE", request))
                .willReturn(deviceCode);

        given(deviceCode.getClientId()).willReturn("CLIENT_ID");
        given(deviceCode.getRealm()).willReturn("/REALM");
        given(deviceCode.isAuthorized()).willReturn(true);
        given(deviceCode.getScope()).willReturn(scope);
        given(deviceCode.getResourceOwnerId()).willReturn("RESOURCE_OWNER");
        given(deviceCode.getNonce()).willReturn("NONCE");

        given(providerSettings.validateRequestedClaims(any()))
                .willReturn(null);

        given(accessTokenGenerator.generateAccessToken(
                eq(providerSettings),
                any(),
                eq("CLIENT_ID"),
                eq("RESOURCE_OWNER"),
                any(),
                eq(scope),
                any(),
                any(),
                eq("NONCE"),
                eq(request)))
                .willReturn(accessToken);

        // When
        AccessToken actualAccessToken =
                grantTypeHandler.handle(request, client, providerSettings);

        // Then
        verify(providerSettings)
                .additionalDataToReturnFromTokenEndpoint(accessToken, request);

        verify(tokenStore)
                .deleteDeviceCode("CLIENT_ID", "DEVICE_CODE", request);

        assertEquals(actualAccessToken, accessToken);
    }

    @Test(expectedExceptions = ExpiredTokenException.class)
    public void handleShouldThrowExpiredTokenExceptionWhenDeviceCodeHasExpired()
            throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);
        DeviceCode deviceCode = mock(DeviceCode.class);

        given(request.getParameter(DEVICE_CODE)).willReturn("DEVICE_CODE");
        given(request.getParameter(REALM)).willReturn("/REALM");

        given(client.getClientId()).willReturn("CLIENT_ID");

        given(tokenStore.readDeviceCode("CLIENT_ID", "DEVICE_CODE", request))
                .willReturn(deviceCode);

        given(deviceCode.getClientId()).willReturn("CLIENT_ID");
        given(deviceCode.getRealm()).willReturn("/REALM");
        given(deviceCode.isAuthorized()).willReturn(false);
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() - 100);

        // When
        grantTypeHandler.handle(request, client, providerSettings);

        // Then
        // Expect ExpiredTokenException
    }

    @Test
    public void shouldUpdateDeviceCodeAfterAuthorizationPending()
            throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);
        DeviceCode deviceCode = mock(DeviceCode.class);

        given(request.getParameter(DEVICE_CODE)).willReturn("DEVICE_CODE");
        given(request.getParameter(REALM)).willReturn("/REALM");

        given(client.getClientId()).willReturn("CLIENT_ID");

        given(tokenStore.readDeviceCode("CLIENT_ID", "DEVICE_CODE", request))
                .willReturn(deviceCode);

        given(deviceCode.getClientId()).willReturn("CLIENT_ID");
        given(deviceCode.getRealm()).willReturn("/REALM");
        given(deviceCode.isAuthorized()).willReturn(false);
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() + 10000);
        given(deviceCode.getLastPollTime()).willReturn(0L);

        given(providerSettings.getDeviceCodePollInterval()).willReturn(5);

        try {
            // When
            grantTypeHandler.handle(request, client, providerSettings);
        } catch (AuthorizationPendingException e) {
            // Then
            verify(deviceCode).poll();
            verify(tokenStore).updateDeviceCode(deviceCode, request);
        }
    }
    
    @Test
    public void shouldNotDeleteDeviceCodeWhenAccessTokenGenerationFails()
            throws Exception {

        // Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration client = mock(ClientRegistration.class);
        DeviceCode deviceCode = mock(DeviceCode.class);

        Set<String> scope = Collections.singleton("openid");

        given(request.getParameter(DEVICE_CODE)).willReturn("DEVICE_CODE");
        given(request.getParameter(REALM)).willReturn("/REALM");
        given(request.getParameter(OAuth2Constants.Params.GRANT_TYPE))
                .willReturn("urn:ietf:params:oauth:grant-type:device_code");

        given(client.getClientId()).willReturn("CLIENT_ID");

        given(tokenStore.readDeviceCode("CLIENT_ID", "DEVICE_CODE", request))
                .willReturn(deviceCode);

        given(deviceCode.getClientId()).willReturn("CLIENT_ID");
        given(deviceCode.getRealm()).willReturn("/REALM");
        given(deviceCode.isAuthorized()).willReturn(true);
        given(deviceCode.getScope()).willReturn(scope);
        given(deviceCode.getResourceOwnerId()).willReturn("RESOURCE_OWNER");

        given(providerSettings.validateRequestedClaims(any()))
                .willReturn(null);

        given(accessTokenGenerator.generateAccessToken(
                eq(providerSettings),
                any(),
                eq("CLIENT_ID"),
                eq("RESOURCE_OWNER"),
                any(),
                eq(scope),
                any(),
                any(),
                any(),
                eq(request)))
                .willThrow(new RuntimeException("Access token generation failed"));

        // When
        try {
            grantTypeHandler.handle(request, client, providerSettings);
            fail("Expected access token generation to fail");
        } catch (RuntimeException e) {
            // Then
            verify(tokenStore, never())
                    .deleteDeviceCode("CLIENT_ID", "DEVICE_CODE", request);
        }
    }
}