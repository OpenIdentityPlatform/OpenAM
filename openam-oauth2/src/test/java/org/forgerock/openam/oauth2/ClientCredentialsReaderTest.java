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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.restlet.Request;
import org.restlet.data.Reference;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression coverage for the GHSA-f2cx-463q-7m2c {@code private_key_jwt} cross-client
 * impersonation fix: enforce {@code iss == sub} per RFC 7523 §3 before any client lookup or
 * key resolution is performed.
 */
public class ClientCredentialsReaderTest {

    private static final String VICTIM_CLIENT_ID = "victim-client";
    private static final String ATTACKER_CLIENT_ID = "attacker-client";
    private static final String ENDPOINT = "https://am.example.com/oauth2/access_token";
    private static final String HMAC_SECRET = "0123456789abcdef0123456789abcdef";

    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private ClientAuthenticationFailureFactory failureFactory;
    private ClientCredentialsReader reader;

    @BeforeMethod
    public void setUp() {
        clientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        // Use a real (anonymous) factory so that getException() returns a real
        // InvalidClientException whose constructors are package-private and cannot be invoked
        // from this test package directly.
        failureFactory = new ClientAuthenticationFailureFactory() {
            @Override protected boolean hasAuthorizationHeader(OAuth2Request request) { return false; }
            @Override protected String getRealm(OAuth2Request request) { return "/"; }
        };
        reader = new ClientCredentialsReader(clientRegistrationStore, failureFactory);
    }

    @Test
    public void rejectsAssertionWhenIssDiffersFromSub() throws Exception {
        String jwt = buildHmacJwt(ATTACKER_CLIENT_ID /* iss */, VICTIM_CLIENT_ID /* sub */, ENDPOINT);
        OAuth2Request request = mockOauth2Request(jwt);

        try {
            reader.extractCredentials(request, ENDPOINT);
            fail("Expected InvalidClientException for iss/sub mismatch");
        } catch (InvalidClientException expected) {
            assertThat(expected.getMessage()).contains("'iss'").contains("'sub'");
        }
        // Critical: must NOT look up any registration when iss != sub - this is what
        // prevented cross-client impersonation. Reject before reaching the store.
        verify(clientRegistrationStore, never()).get(any(String.class), any(OAuth2Request.class));
    }

    @Test
    public void rejectsAssertionWhenIssMissing() throws Exception {
        String jwt = buildHmacJwt(null, VICTIM_CLIENT_ID, ENDPOINT);
        OAuth2Request request = mockOauth2Request(jwt);
        try {
            reader.extractCredentials(request, ENDPOINT);
            fail("Expected InvalidClientException for missing iss");
        } catch (InvalidClientException expected) {
            // ok
        }
        verify(clientRegistrationStore, never()).get(any(String.class), any(OAuth2Request.class));
    }

    @Test
    public void rejectsAssertionWhenSubMissing() throws Exception {
        String jwt = buildHmacJwt(ATTACKER_CLIENT_ID, null, ENDPOINT);
        OAuth2Request request = mockOauth2Request(jwt);
        try {
            reader.extractCredentials(request, ENDPOINT);
            fail("Expected InvalidClientException for missing sub");
        } catch (InvalidClientException expected) {
            // ok
        }
        verify(clientRegistrationStore, never()).get(any(String.class), any(OAuth2Request.class));
    }

    @Test
    public void acceptingIssEqualsSubReachesRegistration() throws Exception {
        String jwt = buildHmacJwt(VICTIM_CLIENT_ID, VICTIM_CLIENT_ID, ENDPOINT);
        OAuth2Request request = mockOauth2Request(jwt);

        OpenIdConnectClientRegistration registration = mock(OpenIdConnectClientRegistration.class);
        given(clientRegistrationStore.get(VICTIM_CLIENT_ID, request)).willReturn(registration);
        given(registration.verifyJwtIdentity(any())).willReturn(true);
        given(registration.getAllowedScopes()).willReturn(java.util.Collections.<String>emptySet());

        // When iss == sub the iss/sub check passes and the flow continues into the registration.
        // We don't care about the final outcome here; we only assert the store IS consulted with
        // the same client id (sub == iss).
        try {
            reader.extractCredentials(request, ENDPOINT);
        } catch (RuntimeException ignored) {
            // downstream checks may still fail in this stripped-down test environment
        }
        verify(clientRegistrationStore, org.mockito.Mockito.atLeastOnce())
                .get(eq(VICTIM_CLIENT_ID), eq(request));
    }

    // --- helpers ---------------------------------------------------------------------------

    private OAuth2Request mockOauth2Request(String clientAssertion) {
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);
        given(oAuth2Request.<String>getParameter(OAuth2Constants.JwtProfile.CLIENT_ASSERTION_TYPE))
                .willReturn(OAuth2Constants.JwtProfile.JWT_PROFILE_CLIENT_ASSERTION_TYPE);
        given(oAuth2Request.<String>getParameter(OAuth2Constants.JwtProfile.CLIENT_ASSERTION))
                .willReturn(clientAssertion);
        Request restletRequest = mock(Request.class);
        given(restletRequest.getChallengeResponse()).willReturn(null);
        given(restletRequest.getResourceRef()).willReturn(new Reference(ENDPOINT));
        given(oAuth2Request.getRequest()).willReturn(restletRequest);
        return oAuth2Request;
    }

    private String buildHmacJwt(String iss, String sub, String aud) {
        org.forgerock.json.jose.builders.JwtClaimsSetBuilder b = new JwtBuilderFactory().claims();
        if (iss != null) {
            b.iss(iss);
        }
        if (sub != null) {
            b.sub(sub);
        }
        JwtClaimsSet claims = b
                .aud(Arrays.asList(aud))
                .exp(new Date(System.currentTimeMillis() + 60_000L))
                .iat(new Date())
                .build();
        SigningHandler signer = new SigningManager().newHmacSigningHandler(HMAC_SECRET.getBytes());
        return new JwtBuilderFactory()
                .jws(signer)
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(claims)
                .build();
    }
}








