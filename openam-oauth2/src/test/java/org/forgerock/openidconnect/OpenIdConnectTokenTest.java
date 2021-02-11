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

package org.forgerock.openidconnect;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweAlgorithmType;
import org.forgerock.json.jose.jwe.SignedThenEncryptedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class OpenIdConnectTokenTest {

    private KeyPair rsaKeyPair;

    private KeyPair p256KeyPair;
    private KeyPair p384KeyPair;
    private KeyPair p521KeyPair;

    @BeforeClass
    public void generateKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        rsaKeyPair = keyPairGenerator.generateKeyPair();

        KeyPairGenerator ecKeyPairGenerator = KeyPairGenerator.getInstance("EC");
        ecKeyPairGenerator.initialize(256);
        p256KeyPair = ecKeyPairGenerator.generateKeyPair();

        ecKeyPairGenerator.initialize(384);
        p384KeyPair = ecKeyPairGenerator.generateKeyPair();

        ecKeyPairGenerator.initialize(521);
        p521KeyPair = ecKeyPairGenerator.generateKeyPair();
    }

    @Test(dataProvider = "algorithms")
    public void shouldSupportAllSigningAndEncryptionModes(JwsAlgorithm signingAlgorithm,
            JweAlgorithm encryptionAlgorithm, EncryptionMethod encryptionMethod) throws Exception {

        // Given
        byte[] clientSecret = "sekret".getBytes(StandardCharsets.UTF_8);
        Key encryptionKey = getEncryptionKey(encryptionAlgorithm, encryptionMethod);
        Key decryptionKey = getDecryptionKey(encryptionAlgorithm, encryptionMethod);
        KeyPair signingKeyPair = getSigningKeyPair(signingAlgorithm);
        JwtReconstruction jwtReconstruction = new JwtReconstruction();

        OpenIdConnectToken oidcToken = new OpenIdConnectToken(null, null, clientSecret, signingKeyPair, encryptionKey,
                signingAlgorithm.toString(), encryptionAlgorithm == null ? null : encryptionAlgorithm.toString(),
                encryptionMethod == null ? null : encryptionMethod.toString(), encryptionAlgorithm != null, "issuer",
                "subject", "audience", "azp", 123456L, 123456L, 1234567L, "nonce", "ops", "atHash", "cHash", "acr",
                Collections.singletonList("amr"), "auditTrackingId", "realm");

        // When
        String jwt = oidcToken.getTokenId();

        // Then
        if (encryptionAlgorithm == null) {
            SignedJwt signedJwt = jwtReconstruction.reconstructJwt(jwt, SignedJwt.class);
            assertThat(signedJwt.getHeader().getParameters()).containsEntry("alg", signingAlgorithm.toString());
        } else {
            SignedThenEncryptedJwt signedThenEncryptedJwt = jwtReconstruction.reconstructJwt(jwt,
                    SignedThenEncryptedJwt.class);
            assertThat(signedThenEncryptedJwt.getHeader().getParameters())
                    .containsEntry("alg", encryptionAlgorithm.toString())
                    .containsEntry("enc", encryptionMethod.toString());

            signedThenEncryptedJwt.decrypt(decryptionKey);
            assertThat(signedThenEncryptedJwt.getClaimsSet()).isNotNull();
        }
    }

    private KeyPair getSigningKeyPair(JwsAlgorithm algorithm) {
        switch (algorithm) {
            case RS256:
                return rsaKeyPair;
            case RS384:
                return rsaKeyPair;
            case RS512:
                return rsaKeyPair;
            case ES256:
                return p256KeyPair;
            case ES384:
                return p384KeyPair;
            case ES512:
                return p521KeyPair;
            default:
                return null;
        }
    }

    private Key getEncryptionKey(JweAlgorithm algorithm, EncryptionMethod encryptionMethod) {
        if (algorithm == null) {
            return null;
        }
        switch (algorithm) {
            case RSA_OAEP:
            case RSA_OAEP_256:
            case RSAES_PKCS1_V1_5:
                return rsaKeyPair.getPublic();
            case DIRECT:
                return new SecretKeySpec(new byte[encryptionMethod.getKeySize() / 8], "AES");
            case A128KW:
                return new SecretKeySpec(new byte[16], "AES");
            case A192KW:
                return new SecretKeySpec(new byte[24], "AES");
            case A256KW:
                return new SecretKeySpec(new byte[32], "AES");
            default:
                return null;

        }
    }

    private Key getDecryptionKey(JweAlgorithm algorithm, EncryptionMethod encryptionMethod) {
        if (algorithm == null) {
            return null;
        }
        switch (algorithm) {
            case RSA_OAEP:
            case RSA_OAEP_256:
            case RSAES_PKCS1_V1_5:
                return rsaKeyPair.getPrivate();
            case DIRECT:
                return new SecretKeySpec(new byte[encryptionMethod.getKeySize() / 8], "AES");
            case A128KW:
                return new SecretKeySpec(new byte[16], "AES");
            case A192KW:
                return new SecretKeySpec(new byte[24], "AES");
            case A256KW:
                return new SecretKeySpec(new byte[32], "AES");
            default:
                return null;

        }
    }


    @DataProvider
    private Object[][] algorithms() throws NoSuchAlgorithmException {
        final List<Object[]> results = new ArrayList<>();
        for (JwsAlgorithm jwsAlgorithm : JwsAlgorithm.values()) {
            if (jwsAlgorithm == JwsAlgorithm.NONE) {
                continue;
            }
            results.add(new Object[] { jwsAlgorithm, null, null }); // Signing only
            for (JweAlgorithm jweAlgorithm : JweAlgorithm.values()) {
                if (jweAlgorithm.getAlgorithmType() == JweAlgorithmType.AES_KEYWRAP) {
                    if (jweAlgorithm != JweAlgorithm.A128KW && Cipher.getMaxAllowedKeyLength("AES") < 192) {
                        // Key size not supported on this platform
                        continue;
                    }
                }
                for (EncryptionMethod encryptionMethod : EncryptionMethod.values()) {
                    if (encryptionMethod.getKeyOffset() * 8 > Cipher.getMaxAllowedKeyLength("AES")) {
                        // Key size not supported
                        continue;
                    }

                    try {
                        Cipher.getInstance(encryptionMethod.getTransformation());
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                        // AES-GCM not supported
                        continue;
                    }

                    results.add(new Object[] { jwsAlgorithm, jweAlgorithm, encryptionMethod });
                }
            }
        }
        return results.toArray(new Object[0][]);
    }
}