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
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.configuration.ISystemProperties;

public class JwtSessionMapperBuilderTest {
    private static final String SECRET = "sekret";

    private JwtSessionMapperBuilder testBuilder;

    @Mock
    private SigningManager mockSigningManager;

    @Mock
    private ISystemProperties mockSystemProperties;

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
        testBuilder = new JwtSessionMapperBuilder(mockSigningManager, mockSystemProperties){
            @Override
            boolean isConfigured() { return true; }
        };
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

    @Test
    public void shouldCreateCorrectHandlerForRSAEncryptionWithDefaultPaddingAndMethod() {
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.rsa.padding", "RSA-OAEP-256"))
                .willReturn("RSA-OAEP-256");
        testBuilder.encryptedUsingKeyPair(rsaKeyPair);

        assertThat(testBuilder.encryptionKey).isSameAs(mockRSAPublicKey);
        assertThat(testBuilder.decryptionKey).isSameAs(mockRSAPrivateKey);
        assertThat(testBuilder.jweAlgorithm).isEqualTo(JweAlgorithm.RSA_OAEP_256);
    }

    @Test(dataProvider = "paddingAndEncryptionModes")
    public void shouldCreateCorrectHandlerForRSAEncryption(String paddingMode, String encryptionMethod) {
        // Given
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.rsa.padding", "RSA-OAEP-256"))
                .willReturn(paddingMode);
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.encryption.method",
                "A128CBC-HS256")).willReturn(encryptionMethod);
        given(mockSigningManager.newHmacSigningHandler(any(byte[].class))).willReturn(mock(HmacSigningHandler.class));

        // When
        testBuilder.signedUsingHS256("test").encryptedUsingKeyPair(rsaKeyPair).build();

        // Then
        assertThat(testBuilder.jweAlgorithm).isEqualTo(JweAlgorithm.parseAlgorithm(paddingMode));
        assertThat(testBuilder.encryptionMethod).isEqualTo(EncryptionMethod.parseMethod(encryptionMethod));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullRSAEncryptionKeyPair() {
        testBuilder.encryptedUsingKeyPair(null);
    }

    @Test
    public void shouldCreateCorrectHandlerForDirectEncryption() {
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.encryption.method",
                "A128CBC-HS256")).willReturn("A192GCM");
        Key key = mock(Key.class);
        testBuilder.encryptedUsingDirectKey(key).build();
        assertThat(testBuilder.encryptionKey).isSameAs(key);
        assertThat(testBuilder.decryptionKey).isSameAs(key);
        assertThat(testBuilder.jweAlgorithm).isEqualTo(JweAlgorithm.DIRECT);
        assertThat(testBuilder.encryptionMethod).isEqualTo(EncryptionMethod.A192GCM);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDirectEncryptionKey() {
        testBuilder.encryptedUsingDirectKey(null);
    }

    @Test(dataProvider = "keyWrapModes")
    public void shouldCreateCorrectHandlerForAESKeyWrapEncryption(int keySize, JweAlgorithm expectedAlgorithm) {
        final Key key = new SecretKeySpec(new byte[keySize / 8], "AES");
        testBuilder.encryptedUsingKeyWrap(key);
        assertThat(testBuilder.jweAlgorithm).isEqualTo(expectedAlgorithm);
        assertThat(testBuilder.encryptionKey).isSameAs(key);
        assertThat(testBuilder.decryptionKey).isSameAs(key);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullKeyWrapEncryptionKey() {
        testBuilder.encryptedUsingKeyWrap(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectInvalidKeySizeForKeyWrapping() {
        testBuilder.encryptedUsingKeyWrap(new SecretKeySpec(new byte[17], "AES"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectRSAEncryptionWithoutSigning() {
        // RSA encryption does not provide authentication and so should never be used on its own
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.encryption.method",
                "A128CBC-HS256")).willReturn("A128CBC-HS256");
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.rsa.padding", "RSA-OAEP-256"))
                .willReturn("RSA-OAEP-256");
        testBuilder.encryptedUsingKeyPair(rsaKeyPair).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNoSigningOrEncryptionAtAll() {
        testBuilder.build();
    }

    @DataProvider
    private Object[][] paddingAndEncryptionModes() {
        return new Object[][] {
                {"RSA1_5", "A128CBC-HS256"},
                {"RSA-OAEP", "A192GCM"},
                {"RSA-OAEP-256", "A256CBC-HS512"}
        };
    }

    @DataProvider
    private Object[][] keyWrapModes() {
        return new Object[][] {
                {128, JweAlgorithm.A128KW},
                {192, JweAlgorithm.A192KW},
                {256, JweAlgorithm.A256KW}
        };
    }
}