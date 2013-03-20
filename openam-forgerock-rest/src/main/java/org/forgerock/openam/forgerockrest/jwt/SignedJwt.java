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

import com.sun.identity.shared.encode.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
 * Represents a JWT which has been signed.
 */
public class SignedJwt implements Jwt {

    private final JwsSignatureUtil jwsSignatureUtil = JwsSignatureUtil.getInstance();

    private final PlaintextJwt jwt;
    private final JwsAlgorithm algorithm;
    private PrivateKey privateKey;
    private byte[] signature;

    /**
     * Constructs a SignedJwt, for the given PlaintextJwt using the given JwsAlgorithm and PrivateKey.
     *
     * Is used when a PlaintextJwt has it's sign method called.
     *
     * @param jwt The Plaintext Jwt.
     * @param algorithm The JwsAlgorithm.
     * @param privateKey The PrivateKey.
     */
    SignedJwt(PlaintextJwt jwt, JwsAlgorithm algorithm, PrivateKey privateKey) {
        this.jwt = jwt;
        this.algorithm = algorithm;
        this.privateKey = privateKey;
    }

    /**
     * Constructs a SignedJwt, for the given PlaintextJwt with the given signature.
     *
     * Is used when reconstructing a Jwt string.
     *
     * @param jwt The Plaintext Jwt.
     * @param signature The signature for the Plaintext Jwt.
     */
    SignedJwt(PlaintextJwt jwt, byte[] signature) {
        this.jwt = jwt;
        this.algorithm = JwsAlgorithm.valueOf(jwt.getHeader("alg").toUpperCase());
        this.privateKey = null;
        this.signature = signature;
    }

    /**
     * Returns the Plaintext Jwt being signed.
     *
     * @return The Plaintext Jwt.
     */
    public PlaintextJwt getJwt() {
        return jwt;
    }

    /**
     * Encrypts the SignedJwt.
     *
     * Currently not supported. Will throw an UnsupportedOperationException.
     *
     * @return An EncryptedSignedJwt.
     */
    public EncryptedSignedJwt encrypt() {
        throw new UnsupportedOperationException("Encrypting JWTs is not currently supported");
//        return new EncryptedSignedJwt();
    }

    /**
     * Verifies that the signature is valid for the Plaintext Jwt.
     *
     * @param privateKey Used to verify HMAC signatures.
     * @param certificate Used to verify RSA signatures.
     * @return Whether the signature is valid.
     * @throws JWTBuilderException If the verify method is called on a newly created JWT, or the verification
     *                              procedure failed.
     */
    public boolean verify(PrivateKey privateKey, X509Certificate certificate) throws JWTBuilderException {
        if (signature == null) {
            throw new JWTBuilderException("JWT has not yet been built. Cannot verify.");
        }
        try {
            return jwsSignatureUtil.verify(algorithm, privateKey, certificate, getSigningInput(), signature);
        } catch (SignatureException e) {
            throw new JWTBuilderException("Failed to verify JWT", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build() throws JWTBuilderException {

        try {
            String signingInput = getSigningInput();

            byte[] signature = jwsSignatureUtil.sign(privateKey, algorithm, signingInput);

            String encodedSignature = Base64.encode(signature);

            String thirdPart = encodedSignature;

            return signingInput + JWT_PART_SEPARATOR + thirdPart;

        } catch (SignatureException e) {
            throw new JWTBuilderException("Failed to sign JWT", e);
        }
    }

    /**
     * Builds the first two parts of the JWT to be used as the first part of the JWT string and the input to the
     * signing function.
     *
     * @return The first part of the JWT string consisting of the first and second parts of the JWT separated by '.'.
     * @throws JWTBuilderException If there is a problem creating the JWT string.
     */
    private String getSigningInput() throws JWTBuilderException {

        try {
            JSONObject header = jwt.buildHeader();
            header.put(JWT_HEADER_TYPE_KEY, JwtType.JWT);
            header.put(JWT_HEADER_ALGORITHM_KEY, algorithm.toString());

            JSONObject claimsSet = jwt.buildContent();

            String encodedHeader = Base64.encode(header.toString().getBytes());

            byte[] message = claimsSet.toString().getBytes();

            String encodedPayload = Base64.encode(message);

            return encodedHeader + JWT_PART_SEPARATOR + encodedPayload;

        } catch (JSONException e) {
            throw new JWTBuilderException("Failed to build JWT", e);
        }
    }
}
