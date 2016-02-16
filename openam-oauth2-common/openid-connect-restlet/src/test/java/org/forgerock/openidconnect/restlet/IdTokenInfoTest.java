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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidconnect.restlet;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OpenAMClientAuthenticationFailureFactory;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdTokenInfoTest {

    @Mock
    private OpenIdConnectClientRegistrationStore mockClientRegistrationStore;

    @Mock
    private OAuth2RequestFactory<?, Request> mockRequestFactory;

    @Mock
    private ExceptionHandler mockExceptionHandler;

    @Mock
    private OAuth2Request mockRequest;

    @Mock
    private OpenIdConnectClientRegistration mockClientRegistration;

    private final String sharedSecret = "not very secret";
    private final JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
    private final SigningHandler signingHandler = new HmacSigningHandler(sharedSecret.getBytes(StandardCharsets.UTF_8));
    private final InvalidClientException invalidClientException = new OpenAMClientAuthenticationFailureFactory(null)
            .getException();

    private IdTokenInfo idTokenInfo;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        idTokenInfo = new IdTokenInfo(mockClientRegistrationStore, mockRequestFactory, mockExceptionHandler);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldRejectRequestsWithoutAnIdToken() throws Exception {
        idTokenInfo.validateIdToken(mockRequest);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldRejectRequestsWithMalformedIdTokens() throws Exception {
        given(mockRequest.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn("not a jwt");
        idTokenInfo.validateIdToken(mockRequest);
    }

    @Test
    public void shouldUseAudienceAsClientId() throws Exception {
        // Given
        final String clientId = "testClient";
        final String jwt = createIdToken(clientId);
        given(mockRequest.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn(jwt);
        given(mockClientRegistrationStore.get(eq(clientId), any(OAuth2Request.class)))
                .willReturn(mockClientRegistration);
        given(mockClientRegistration.verifyJwtIdentity(any(OAuth2Jwt.class))).willReturn(true);

        // When
        idTokenInfo.validateIdToken(mockRequest);

        // Then
        verify(mockClientRegistrationStore).get(eq(clientId), any(OAuth2Request.class));
    }

    @Test(expectedExceptions = InvalidClientException.class)
    public void shouldNotValidateTokensForUnknownAudience() throws Exception {
        // Given
        final String clientId = "testClient";
        final String jwt = createIdToken(clientId);
        given(mockRequest.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn(jwt);
        given(mockClientRegistrationStore.get(eq(clientId), any(OAuth2Request.class)))
                .willThrow(invalidClientException);

        // When
        idTokenInfo.validateIdToken(mockRequest);

        // Then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotValidateExpiredIdTokens() throws Exception {
        // Given
        final String clientId = "testClient";
        final String jwt = createIdToken(new Date(0), clientId);
        given(mockRequest.getParameter(OAuth2Constants.JWTTokenParams.ID_TOKEN)).willReturn(jwt);
        given(mockClientRegistrationStore.get(anyString(), any(OAuth2Request.class)))
                .willReturn(mockClientRegistration);
        given(mockClientRegistration.verifyJwtIdentity(any(OAuth2Jwt.class))).willReturn(true);

        // When
        idTokenInfo.validateIdToken(mockRequest);

        // Then - exception
    }

    private String createIdToken(final String... audience) {
        return createIdToken(new Date(Long.MAX_VALUE), audience);
    }

    private String createIdToken(final Date expiry, final String... audience) {
        return jwtBuilderFactory.jws(signingHandler).headers().alg(JwsAlgorithm.HS256).done()
                                .claims(jwtBuilderFactory.claims()
                                                         .aud(Arrays.asList(audience))
                                                         .exp(expiry).build()).build();
    }
}