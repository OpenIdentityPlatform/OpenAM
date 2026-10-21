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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * PKCE verification in the device code grant (GHSA-h93p-35jm-rj83): a {@code code_challenge} bound
 * to a device code at {@code /oauth2/device/code} must be matched by a {@code code_verifier} at the
 * token endpoint.
 */
public class DeviceCodeGrantTypeHandlerPkceTest {

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String REALM = "/";
    private static final String DEVICE_CODE = "aDeviceCode";
    private static final String S256_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String S256_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private DeviceCodeGrantTypeHandler grantTypeHandler;
    private TokenStore tokenStore;
    private GrantTypeAccessTokenGenerator accessTokenGenerator;
    private OAuth2ProviderSettings providerSettings;
    private OAuth2Request request;
    private ClientRegistration client;
    private DeviceCode deviceCode;
    private AccessToken accessToken;

    @BeforeMethod
    public void setUp() throws Exception {
        tokenStore = mock(TokenStore.class);
        accessTokenGenerator = mock(GrantTypeAccessTokenGenerator.class);
        grantTypeHandler = new DeviceCodeGrantTypeHandler(mock(OAuth2ProviderSettingsFactory.class),
                mock(ClientAuthenticator.class), tokenStore, mock(ClientRegistrationStore.class),
                mock(ClientAuthenticationFailureFactory.class), mock(OAuth2UrisFactory.class),
                accessTokenGenerator);

        providerSettings = mock(OAuth2ProviderSettings.class);
        request = mock(OAuth2Request.class);
        client = mock(ClientRegistration.class);
        deviceCode = mock(DeviceCode.class);
        accessToken = mock(AccessToken.class);

        given(request.getParameter("device_code")).willReturn(DEVICE_CODE);
        given(request.getParameter("realm")).willReturn(REALM);
        given(request.getParameter("grant_type")).willReturn("urn:ietf:params:oauth:grant-type:device_code");
        given(client.getClientId()).willReturn(CLIENT_ID);
        given(tokenStore.readDeviceCode(CLIENT_ID, DEVICE_CODE, request)).willReturn(deviceCode);
        given(deviceCode.getClientId()).willReturn(CLIENT_ID);
        given(deviceCode.getRealm()).willReturn(REALM);
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() + 60000);
        Set<String> scope = Collections.singleton("openid");
        given(deviceCode.getScope()).willReturn(scope);
        given(accessTokenGenerator.generateAccessToken(eq(providerSettings), anyString(), eq(CLIENT_ID),
                nullable(String.class), nullable(String.class), any(), nullable(String.class),
                nullable(String.class), nullable(String.class), eq(request))).willReturn(accessToken);
    }

    private void givenApproved(String codeChallenge, String codeChallengeMethod, String codeVerifier) {
        given(deviceCode.isAuthorized()).willReturn(true);
        given(deviceCode.getCodeChallenge()).willReturn(codeChallenge);
        given(deviceCode.getCodeChallengeMethod()).willReturn(codeChallengeMethod);
        given(request.getParameter("code_verifier")).willReturn(codeVerifier);
    }

    @Test
    public void shouldIssueTokenWhenS256VerifierMatches() throws Exception {
        givenApproved(S256_CHALLENGE, "S256", S256_VERIFIER);

        AccessToken actual = grantTypeHandler.handle(request, client, providerSettings);

        assertEquals(actual, accessToken);
        verify(tokenStore).deleteDeviceCode(CLIENT_ID, DEVICE_CODE, request);
    }

    @Test
    public void shouldIssueTokenWhenChallengeMethodOmittedAndPlainVerifierMatches() throws Exception {
        givenApproved("aPlainVerifier", null, "aPlainVerifier");

        AccessToken actual = grantTypeHandler.handle(request, client, providerSettings);

        assertEquals(actual, accessToken);
    }

    @Test
    public void shouldIssueTokenWhenExplicitPlainVerifierMatches() throws Exception {
        givenApproved("aPlainVerifier", "plain", "aPlainVerifier");

        AccessToken actual = grantTypeHandler.handle(request, client, providerSettings);

        assertEquals(actual, accessToken);
    }

    /**
     * The attack from the advisory: a device code redeemed without the verifier. The failed attempt
     * must not consume the code either, or anyone holding the device code could cancel the
     * legitimate client's flow.
     */
    @Test
    public void shouldRejectApprovedCodeWithChallengeWhenVerifierMissing() throws Exception {
        givenApproved(S256_CHALLENGE, "S256", null);

        try {
            grantTypeHandler.handle(request, client, providerSettings);
            fail("Expected InvalidRequestException as no code_verifier was supplied");
        } catch (InvalidRequestException e) {
            verifyNoTokenIssuedAndCodeKept();
        }
    }

    @Test
    public void shouldRejectApprovedCodeWithChallengeWhenVerifierDoesNotMatch() throws Exception {
        givenApproved(S256_CHALLENGE, "S256", "wrongVerifier");

        try {
            grantTypeHandler.handle(request, client, providerSettings);
            fail("Expected InvalidGrantException as the code_verifier does not match");
        } catch (InvalidGrantException e) {
            verifyNoTokenIssuedAndCodeKept();
        }
    }

    /**
     * The verifier is checked before the expiry check, so a request without it gets no expired_token
     * answer for an approved code past its expiry time and does not delete the code either.
     */
    @Test
    public void shouldRejectExpiredApprovedCodeWithChallengeWhenVerifierMissing() throws Exception {
        givenApproved(S256_CHALLENGE, "S256", null);
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() - 1000);

        try {
            grantTypeHandler.handle(request, client, providerSettings);
            fail("Expected InvalidRequestException as no code_verifier was supplied");
        } catch (InvalidRequestException e) {
            verifyNoTokenIssuedAndCodeKept();
        }
    }

    /**
     * A poll without the verifier is refused before the authorization state is looked at, so it
     * neither reveals whether the user has approved the code nor counts as the client's poll.
     */
    @Test
    public void shouldRejectPendingCodeWithChallengeWhenVerifierMissing() throws Exception {
        given(deviceCode.isAuthorized()).willReturn(false);
        given(deviceCode.getCodeChallenge()).willReturn(S256_CHALLENGE);
        given(deviceCode.getCodeChallengeMethod()).willReturn("S256");
        given(request.getParameter("code_verifier")).willReturn(null);

        try {
            grantTypeHandler.handle(request, client, providerSettings);
            fail("Expected InvalidRequestException as no code_verifier was supplied");
        } catch (InvalidRequestException e) {
            verify(deviceCode, never()).poll();
            verify(tokenStore, never()).updateDeviceCode(any(DeviceCode.class), any(OAuth2Request.class));
        }
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void shouldRejectCodeWithUnknownChallengeMethod() throws Exception {
        givenApproved(S256_CHALLENGE, "bogus", S256_VERIFIER);

        grantTypeHandler.handle(request, client, providerSettings);
    }

    /**
     * The Code Verifier Parameter Required property governs the authorization endpoint only; a
     * device code minted without a challenge stays redeemable without a verifier.
     */
    @Test
    public void shouldIssueTokenForCodeWithoutChallengeEvenWhenEnforcementEnabled() throws Exception {
        given(providerSettings.isCodeVerifierRequired()).willReturn(true);
        givenApproved(null, null, null);

        AccessToken actual = grantTypeHandler.handle(request, client, providerSettings);

        assertEquals(actual, accessToken);
    }

    private void verifyNoTokenIssuedAndCodeKept() throws Exception {
        verify(accessTokenGenerator, never()).generateAccessToken(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any());
        verify(tokenStore, never()).deleteDeviceCode(anyString(), anyString(), any(OAuth2Request.class));
    }
}
