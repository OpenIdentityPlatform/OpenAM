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

import org.forgerock.util.encode.Base64;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility for decoding PEM formatted keys and certificates.
 *
 * @since 12.0.0
 */
public final class PEMDecoder {

    /**
     * Decodes a PEM encoded Public Key.
     *
     * @param encodedKey The Base64 encoded public key bytes.
     * @return The decoded Public Key.
     * @throws NoSuchAlgorithmException If the key cannot be decoded.
     * @throws InvalidKeySpecException If the key cannot be decoded.
     */
    public RSAPublicKey decodeRSAPublicKey(String encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (encodedKey == null) {
            return null;
        }
        encodedKey = encodedKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .trim();
        byte[] decodedKey = Base64.decode(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Decodes a PEM encoded X.509 Certificate.
     *
     * @param encodedCert The Base64 encoded certificate bytes.
     * @return The decoded X.509 Certificate.
     * @throws CertificateException If the certificate cannot be decoded.
     */
    public X509Certificate decodeX509Certificate(String encodedCert) throws CertificateException {
        if (encodedCert == null) {
            return null;
        }
        encodedCert = encodedCert
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .trim();

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(Base64.decode(encodedCert)));
    }
}
