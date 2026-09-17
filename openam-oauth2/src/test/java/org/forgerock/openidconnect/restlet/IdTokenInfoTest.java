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

package org.forgerock.openidconnect.restlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.engine.adapter.HttpRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdTokenInfoTest {

    private static final String CLIENT_ID = "client1";

    @Mock
    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    @Mock
    private OAuth2RequestFactory requestFactory;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private ClientAuthenticator clientAuthenticator;
    @Mock
    private OAuth2UrisFactory urisFactory;
    @Mock
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    @Mock
    private OAuth2ProviderSettings providerSettings;
    @Mock
    private OpenIdConnectClientRegistration clientRegistration;
    @Mock
    private OAuth2Request request;

    private RealmTestHelper realmTestHelper;
    private IdTokenInfo idTokenInfo;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        idTokenInfo = new IdTokenInfo(clientRegistrationStore, requestFactory, exceptionHandler,
                clientAuthenticator, urisFactory, providerSettingsFactory);

        // setRealmOnRequest() needs a restlet request with attributes and the servlet request behind it.
        HttpRequest restletRequest = mock(HttpRequest.class);
        ServletCall servletCall = mock(ServletCall.class);
        given(restletRequest.getAttributes()).willReturn(new ConcurrentHashMap<String, Object>());
        given(restletRequest.getHttpCall()).willReturn(servletCall);
        given(servletCall.getRequest()).willReturn(mock(HttpServletRequest.class));
        given(request.getRequest()).willReturn(restletRequest);

        given(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).willReturn(clientRegistration);
        given(clientRegistration.getIDTokenSignedResponseAlgorithm()).willReturn("HS256");
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(providerSettings.isIdTokenInfoClientAuthenticationEnabled()).willReturn(false);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldReturnIdTokenIssuedForTheClient() throws Exception {
        given(request.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn(idToken());
        given(clientRegistration.verifyIdTokenIdentity(any(OAuth2Jwt.class))).willReturn(true);

        OAuth2Jwt result = idTokenInfo.validateIdToken(request);

        assertThat(result.getSignedJwt().getClaimsSet().getAudience()).containsExactly(CLIENT_ID);
        verify(clientRegistration).verifyIdTokenIdentity(any(OAuth2Jwt.class));
    }

    /**
     * The id_token is checked as an ID token this server issued (header alg pinned to the client's
     * id_token_signed_response_alg), not as a client assertion: a token that only verifies as the
     * latter is rejected.
     */
    @Test(expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "invalid id_token")
    public void shouldRejectIdTokenThatVerifiesOnlyAsClientAssertion() throws Exception {
        given(request.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn(idToken());
        given(clientRegistration.verifyJwtIdentity(any(OAuth2Jwt.class))).willReturn(true);
        given(clientRegistration.verifyIdTokenIdentity(any(OAuth2Jwt.class))).willReturn(false);

        idTokenInfo.validateIdToken(request);
    }

    private static String idToken() {
        return new JwtBuilderFactory()
                .jws(new SigningManager().newHmacSigningHandler("secret".getBytes(StandardCharsets.UTF_8)))
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(new JwtBuilderFactory().claims()
                        .iss("https://openam.example.com/openam/oauth2")
                        .sub("user1")
                        .aud(Collections.singletonList(CLIENT_ID))
                        .exp(new Date(System.currentTimeMillis() + 60_000L))
                        .iat(new Date())
                        .build())
                .build();
    }
}
