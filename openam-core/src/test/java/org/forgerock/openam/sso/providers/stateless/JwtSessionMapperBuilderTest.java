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

package org.forgerock.openam.sso.providers.stateless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JwtSessionMapperBuilderTest {
    private static final String SECRET = "sekret";

    private JwtSessionMapperBuilder testBuilder;

    @Mock
    private SigningManager mockSigningManager;

    @Mock
    private ECPublicKey mockECPublicKey;

    @Mock
    private ECPrivateKey mockECPrivateKey;

    @Mock
    private RSAPublicKey mockRSAPublicKey;

    @Mock
    private RSAPrivateKey mockRSAPrivateKey;

    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;

    @BeforeMethod
    public void createBuilder() {
        MockitoAnnotations.initMocks(this);
        testBuilder = new JwtSessionMapperBuilder(mockSigningManager);
        ecKeyPair = new KeyPair(mockECPublicKey, mockECPrivateKey);
        rsaKeyPair = new KeyPair(mockRSAPublicKey, mockRSAPrivateKey);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForES256() {
        testBuilder.signedUsingES256(ecKeyPair);
        verify(mockSigningManager).newEcdsaSigningHandler(mockECPrivateKey);
        verify(mockSigningManager).newEcdsaVerificationHandler(mockECPublicKey);
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.ES256);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForES384() {
        testBuilder.signedUsingES384(ecKeyPair);
        verify(mockSigningManager).newEcdsaSigningHandler(mockECPrivateKey);
        verify(mockSigningManager).newEcdsaVerificationHandler(mockECPublicKey);
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.ES384);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForES512() {
        testBuilder.signedUsingES512(ecKeyPair);
        verify(mockSigningManager).newEcdsaSigningHandler(mockECPrivateKey);
        verify(mockSigningManager).newEcdsaVerificationHandler(mockECPublicKey);
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.ES512);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForRS256() {
        testBuilder.signedUsingRS256(rsaKeyPair);
        // Expect two calls: once for signing handler, once for verification handler
        verify(mockSigningManager).newRsaSigningHandler(mockRSAPublicKey);
        verify(mockSigningManager).newRsaSigningHandler(mockRSAPrivateKey);
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.RS256);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForHS256() {
        testBuilder.signedUsingHS256(SECRET);
        verify(mockSigningManager, times(2)).newHmacSigningHandler(SECRET.getBytes(StandardCharsets.UTF_8));
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.HS256);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForHS384() {
        testBuilder.signedUsingHS384(SECRET);
        verify(mockSigningManager, times(2)).newHmacSigningHandler(SECRET.getBytes(StandardCharsets.UTF_8));
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.HS384);
    }

    @Test
    public void shouldCreateCorrectSigningHandlerForHS512() {
        testBuilder.signedUsingHS512(SECRET);
        verify(mockSigningManager, times(2)).newHmacSigningHandler(SECRET.getBytes(StandardCharsets.UTF_8));
        assertThat(testBuilder.getJwsAlgorithm()).isEqualTo(JwsAlgorithm.HS512);
    }
}