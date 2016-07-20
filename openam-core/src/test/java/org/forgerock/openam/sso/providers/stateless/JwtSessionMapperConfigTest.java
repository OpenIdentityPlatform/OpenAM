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
import static org.forgerock.openam.sso.providers.stateless.JwtSessionMapperConfig.*;
import static org.mockito.BDDMockito.given;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.ECDSASigningHandler;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JwtSessionMapperConfigTest {

    @Mock
    private RSAPublicKey mockRSAPublicKey;

    @Mock
    private RSAPrivateKey mockRSAPrivateKey;

    @Mock
    private Key mockSecretKey;

    private KeyPair keyPair;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        keyPair = new KeyPair(mockRSAPublicKey, mockRSAPrivateKey);
    }

    @Test
    public void shouldSupportCompression() {
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "DEF", ENCRYPTION_ALGORITHM, "NONE",
                SIGNING_ALGORITHM, "HS256", SIGNING_HMAC_SHARED_SECRET, "test")).getJwtSessionMapper();
        assertThat(result.compressionAlgorithm).isEqualTo(CompressionAlgorithm.DEF);
    }

    @Test
    public void shouldSupportRSAEncryption() {
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "RSA",
                SIGNING_ALGORITHM, "HS256", SIGNING_HMAC_SHARED_SECRET, "test")).getJwtSessionMapper();
        assertThat(result.jweAlgorithm).isEqualTo(JweAlgorithm.RSA_OAEP_256);
        assertThat(result.encryptionKey).isSameAs(mockRSAPublicKey);
        assertThat(result.decryptionKey).isSameAs(mockRSAPrivateKey);
    }

    @Test
    public void shouldSupportDirectEncryption() {
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "DIRECT",
                SIGNING_ALGORITHM, "HS256", SIGNING_HMAC_SHARED_SECRET, "test")).getJwtSessionMapper();
        assertThat(result.jweAlgorithm).isEqualTo(JweAlgorithm.DIRECT);
        assertThat(result.encryptionKey).isSameAs(mockSecretKey);
        assertThat(result.decryptionKey).isSameAs(mockSecretKey);
    }

    @Test(dataProvider = "keyWrapSizes")
    public void shouldSupportAESKeyWrapEncryption(int keySize, JweAlgorithm expectedAlgorithm) {
        given(mockSecretKey.getEncoded()).willReturn(new byte[keySize / 8]);
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "AES_KEYWRAP",
                SIGNING_ALGORITHM, "HS256", SIGNING_HMAC_SHARED_SECRET, "test")).getJwtSessionMapper();
        assertThat(result.jweAlgorithm).isEqualTo(expectedAlgorithm);
        assertThat(result.encryptionKey).isSameAs(mockSecretKey);
        assertThat(result.decryptionKey).isSameAs(mockSecretKey);
    }

    @Test
    public void shouldSupportRSASignatures() {
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "NONE",
                SIGNING_ALGORITHM, "RS256")).getJwtSessionMapper();
        assertThat(result.jwsAlgorithm).isEqualTo(JwsAlgorithm.RS256);
        assertThat(result.signingHandler).isInstanceOf(RSASigningHandler.class);
        assertThat(result.verificationHandler).isInstanceOf(RSASigningHandler.class);
    }

    @Test(dataProvider = "ecdsaSignatureAlgorithms")
    public void shouldSupportECDSASignatures(int keySize, JwsAlgorithm type) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(keySize);
        keyPair = keyPairGenerator.generateKeyPair();

        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "NONE",
                SIGNING_ALGORITHM, type.name())).getJwtSessionMapper();
        assertThat(result.jwsAlgorithm).isEqualTo(type);
        assertThat(result.signingHandler).isInstanceOf(ECDSASigningHandler.class);
        assertThat(result.verificationHandler).isInstanceOf(ECDSASigningHandler.class);
    }

    @Test(dataProvider = "hmacSignatureAlgorithms")
    public void shouldSupportHmacSignatures(JwsAlgorithm algorithm) {
        JwtSessionMapper result = newConfig(mapOf(COMPRESSION_TYPE, "NONE", ENCRYPTION_ALGORITHM, "NONE",
                SIGNING_ALGORITHM, algorithm.name(), SIGNING_HMAC_SHARED_SECRET, "test")).getJwtSessionMapper();
        assertThat(result.jwsAlgorithm).isEqualTo(algorithm);
        assertThat(result.signingHandler).isInstanceOf(HmacSigningHandler.class);
        assertThat(result.verificationHandler).isInstanceOf(HmacSigningHandler.class);
    }

    @DataProvider
    private Object[][] keyWrapSizes() {
        return new Object[][] {
                {128, JweAlgorithm.A128KW},
                {192, JweAlgorithm.A192KW},
                {256, JweAlgorithm.A256KW}
        };
    }

    @DataProvider
    private Object[][] ecdsaSignatureAlgorithms() {
        return new Object[][] {
                { 256, JwsAlgorithm.ES256 },
                { 384, JwsAlgorithm.ES384 },
                { 521, JwsAlgorithm.ES512 }
        };
    }

    @DataProvider
    public Object[][] hmacSignatureAlgorithms() {
        return new Object[][] {
                { JwsAlgorithm.HS256 },
                { JwsAlgorithm.HS384 },
                { JwsAlgorithm.HS512 }
        };
    }

    private static Map<String, Set<String>> mapOf(String...entries) {
        final Map<String, Set<String>> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(entries[i], Collections.singleton(entries[i+1]));
        }
        return map;
    }

    private JwtSessionMapperConfig newConfig(Map config) {
        return new JwtSessionMapperConfig(config) {
            @Override
            KeyPair getKeyPair(Map attrs, String key) {
                return keyPair;
            }
            @Override
            Key getSecretKey(Map attrs, String key) {
                return mockSecretKey;
            }
        };
    }

}