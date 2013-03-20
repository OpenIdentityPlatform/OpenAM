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
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.utils;

import com.sun.identity.security.SecurityDebug;
import com.sun.identity.shared.debug.Debug;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

/**
 * Utility class for signing and verifying signatures.
 */
public class SignatureUtil {

    private Debug logger = SecurityDebug.debug;

    /**
     * Singleton approach by using a static inner class.
     */
    private static final class SingletonHolder {
        private static final SignatureUtil INSTANCE = new SignatureUtil();
    }

    /**
     * Private constructor to ensure SignatureUtil remains a Singleton.
     */
    private SignatureUtil() {
    }

    /**
     * Gets the SignatureUtil instance.
     *
     * @return The SignatureUtil singleton instance.
     */
    public static SignatureUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Signs a String using the given private key. Uses the algorithm from the private key to perform the signature.
     *
     * @param privateKey The private key to use to sign the String.
     * @param algorithm The algorithm to use in the signing.
     * @param message The String to sign.
     * @return The byte array of the signature.
     * @throws SignatureException If there is a problem when performing the signature.
     */
    public byte[] sign(PrivateKey privateKey, String algorithm, String message) throws SignatureException {

        try {
            Signature signature = Signature.getInstance(algorithm);

            signature.initSign(privateKey);

            signature.update(message.getBytes());

            byte[] signatureBytes = signature.sign();

            return signatureBytes;

        } catch (NoSuchAlgorithmException e) {
            logger.error(MessageFormat.format("Could not get Signature instance with the algorithm: {0}", algorithm),
                    e);
            throw new SignatureException(MessageFormat.format(
                    "Could not get Signature instance with the algorithm: {0}", algorithm), e);
        } catch (InvalidKeyException e) {
            logger.error("Invalid key", e);
            throw new SignatureException("Invalid key", e);
        }
    }

    /**
     * Verifies a signature of a String using the certificate. Uses the algorithm from the certificate to perform the
     * verification of the signature.
     *
     * @param certificate The X509Certificate to use to verify the signature.
     * @param algorithm The algorithm to use in the signing.
     * @param message The String that was signed.
     * @param signatureData The byte array of the signature.
     * @return Whether or not the signature is valid for the String that was signed.
     * @throws SignatureException If there is a problem when verifying the signature.
     */
    public boolean verify(X509Certificate certificate, String algorithm, String message, byte[] signatureData) throws
            SignatureException {
        return verify(certificate.getPublicKey(), algorithm, message, signatureData);
    }

    /**
     * Verifies a signature of a String using the public key. Uses the algorithm from the public key to perform the
     * verification of the signature.
     *
     * @param publicKey The public key to use to verify the signature.
     * @param algorithm The algorithm to use in the signing.
     * @param message The String that was signed.
     * @param signatureData The byte array of the signature.
     * @return Whether or not the signature is valid for the String that was signed.
     * @throws SignatureException If there is a problem when verifying the signature.
     */
    public boolean verify(PublicKey publicKey,  String algorithm, String message, byte[] signatureData) throws SignatureException {

        try {
            Signature signature = Signature.getInstance(algorithm);

            signature.initVerify(publicKey);

            signature.update(message.getBytes());

            boolean verified = signature.verify(signatureData);

            return verified;

        } catch (NoSuchAlgorithmException e) {
            logger.error(MessageFormat.format("Could not get Signature instance with the algorithm: {0}", algorithm),
                    e);
            throw new SignatureException(MessageFormat.format(
                    "Could not get Signature instance with the algorithm: {0}", algorithm), e);
        } catch (InvalidKeyException e) {
            logger.error("Invalid key", e);
            throw new SignatureException("Invalid key", e);
        }
    }
}
