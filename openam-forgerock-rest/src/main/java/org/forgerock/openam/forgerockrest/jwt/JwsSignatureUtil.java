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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.jwt;

import org.forgerock.openam.utils.SignatureUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * A signature utility class for signing and verifying JWTs.
 */
public class JwsSignatureUtil {

    /**
     * Singleton approach by using a static inner class.
     */
    private static final class SingletonHolder {
        private static final JwsSignatureUtil INSTANCE = new JwsSignatureUtil();
    }

    /**
     * Private constructor to ensure JwsSignatureUtil remains a Singleton.
     */
    private JwsSignatureUtil() {
    }

    /**
     * Gets the JwsSignatureUtil instance.
     *
     * @return The JwsSignatureUtil singleton instance.
     */
    public static JwsSignatureUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final SignatureUtil signatureUtil = SignatureUtil.getInstance();

    /**
     * Signs the given data using one of the support JWS signing algorithms, either HMAC or RSA.
     *
     * @param privateKey The Private Key to sign with.
     * @param algorithm The JwsAlgorithm to use.
     * @param data The data to be signed.
     * @return A byte array representing the signature of the data.
     * @throws SignatureException If there is a problem signing the data.
     */
    public byte[] sign(PrivateKey privateKey, JwsAlgorithm algorithm, String data) throws SignatureException {

        switch (algorithm) {
            case HS256:
                // Fall through
            case HS384:
            // Fall through
            case HS512: {
                return signWithHmac(algorithm, privateKey, data);
            }
            case RS256:
                // Fall through
            case RS384:
                // Fall through
            case RS512: {
                return signWithRSA(algorithm, privateKey, data);
            }
            default: {
                throw new SignatureException(MessageFormat.format("Unknown algorithm: {0}", algorithm.toString()));
            }
        }
    }

    /**
     * Creates a signature for the given data using HMAC.
     *
     * @param algorithm The JwsAlgorithm to use.
     * @param privateKey The Private Key to sign with.
     * @param data The data to sign.
     * @return A byte array representing the signature of the data.
     * @throws SignatureException If there is a problem signing the data.
     */
    private byte[] signWithHmac(JwsAlgorithm algorithm, PrivateKey privateKey, String data) throws SignatureException {
        try {
            Mac mac = Mac.getInstance(algorithm.getAlgorithm());
            byte[] secretByte = privateKey.getEncoded();
            byte[] dataBytes = data.getBytes();
            SecretKey secretKey = new SecretKeySpec(secretByte, algorithm.getAlgorithm().toUpperCase());
            mac.init(secretKey);
            return mac.doFinal(dataBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(MessageFormat.format(
                    "Could not get Signature instance with the algorithm: {0}", algorithm.toString()), e);
        } catch (InvalidKeyException e) {
            throw new SignatureException("Invalid key", e);
        }
    }

    /**
     * Creates a signature for the given data using RSA.
     *
     * @param algorithm The JwsAlgorithm to use.
     * @param privateKey The Private Key to sign with.
     * @param data The data to sign.
     * @return A byte array representing the signature of the data.
     * @throws SignatureException If there is a problem signing the data.
     */
    private byte[] signWithRSA(JwsAlgorithm algorithm, PrivateKey privateKey, String data) throws SignatureException {
        return signatureUtil.sign(privateKey, algorithm.getAlgorithm(), data);
    }

    /**
     * Verifies the given data with the given signature.
     *
     * @param algorithm The JwsAlgorithm that was used.
     * @param privateKey The Private Key that was used to sign the data.
     * @param certificate The Certificate corresponding with the Private Key.
     * @param data The data that was signed.
     * @param signature The signature to verify.
     * @return Whether or not the signature is valid for the data.
     * @throws SignatureException If there is a problem verifying the signature.
     */
    public boolean verify(JwsAlgorithm algorithm, PrivateKey privateKey, X509Certificate certificate, String data,
            byte[] signature) throws SignatureException {

        switch (algorithm) {
            case HS256:
                // Fall through
            case HS384:
                // Fall through
            case HS512: {
                return verifyWithHmac(algorithm, privateKey, data, signature);
            }
            case RS256:
                // Fall through
            case RS384:
                // Fall through
            case RS512: {
                return verifyWithRSA(algorithm, certificate, data, signature);
            }
            default: {
                throw new SignatureException(MessageFormat.format("Unknown algorithm: {0}", algorithm.toString()));
            }
        }
    }

    /**
     * Verifies a signature for the given data using HMAC.
     *
     * @param algorithm The JwsAlgorithm to use.
     * @param privateKey The Private Key to sign with.
     * @param data The data that was signed.
     * @param signature The signature of the data.
     * @return True if the signature matches the data that was signed. Otherwise False.
     * @throws SignatureException If there is a problem verifying the signature.
     */
    private boolean verifyWithHmac(JwsAlgorithm algorithm, PrivateKey privateKey, String data,
            byte[] signature) throws SignatureException {
        byte[] signed = signWithHmac(algorithm, privateKey, data);
        return Arrays.equals(signed, signature);
    }

    /**
     * Verifies a signature for the given data using RSA.
     *
     * @param algorithm The JwsAlgorithm to use.
     * @param certificate The public Certifcate of the Private Key that was used to sign the data.
     * @param data The data that was signed.
     * @return True if the signature matches the data that was signed. Otherwise False.
     * @throws SignatureException If there is a problem verifying the signature.
     */
    private boolean verifyWithRSA(JwsAlgorithm algorithm, X509Certificate certificate, String data,
            byte[] signature) throws SignatureException {
        return signatureUtil.verify(certificate, algorithm.getAlgorithm(), data, signature);
    }
}
