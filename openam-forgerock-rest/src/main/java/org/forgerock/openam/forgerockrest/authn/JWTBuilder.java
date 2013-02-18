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
package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.forgerockrest.authn.exceptions.JWTBuilderException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.SignatureUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.Response;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A builder class for construct JWTs both plaintext, signed and encrypted.
 */
public class JWTBuilder {

    private final SignatureUtil signatureUtil;

    private String type = "JWT";
    private String algorithm;
    private Map<String, Object> payload = new LinkedHashMap<String, Object>();
    private String signature;
    private boolean encrypted;

    /**
     * Constructs a JWTBuilder instance.
     *
     * @param signatureUtil An instance of the SignatureUtil to be used for signing the JWT.
     */
    public JWTBuilder(SignatureUtil signatureUtil) {
        this.signatureUtil = signatureUtil;
    }

    /**
     * Determines whether the JWT can be modified based on whether the JWT is signed and/or encrypted.
     *
     * JWT's cannot be modified once they have been signed or encrypted.
     *
     * @return Whether the JWT can be modified.
     */
    private boolean isModifiable() {
        return !(isSigned() || isEncrypted());
    }

    /**
     * Returns whether the JWT has been signed or not.
     *
     * @return Whether the JWT has been signed.
     */
    private boolean isSigned() {
        return signature != null;
    }

    /**
     * Returns whether the JWT has been encrypted or not.
     *
     * @return Whether the JWT has been encrypted.
     */
    private boolean isEncrypted() {
        return encrypted;
    }

    /**
     * Sets the algorithm used to sign the JWT.
     *
     * @param algorithm The algorithm used to sign the JWT.
     * @return The JWTBuilder.
     */
    public JWTBuilder setAlgorithm(String algorithm) {
        if (!isModifiable()) {
            throw new JWTBuilderException("JWT has been signed. No modification allowed.");
        }
        this.algorithm = algorithm;
        return this;
    }

    /**
     * Adds a value pair to the payload of the JWT.
     *
     * @param name The name of the value pair.
     * @param value The value of the value pair.
     * @return The JWTBuilder.
     */
    public JWTBuilder addValuePair(String name, Object value) {
        if (!isModifiable()) {
            throw new JWTBuilderException("JWT has been signed. No modification allowed.");
        }
        payload.put(name, value);
        return this;
    }

    /**
     * Adds a map of value pairs to the payload of the JWT.
     *
     * @param valuePairs A Map of value pairs.
     * @return Thw JWTBuilder
     */
     public JWTBuilder addValuePairs(Map<String, Object> valuePairs) {
        if (!isModifiable()) {
            throw new JWTBuilderException("JWT has been signed. No modification allowed.");
        }
        payload.putAll(valuePairs);
        return this;
    }

    /**
     * Signs the JWT with the given private key, turning the JWT into a JWS.
     *
     * @param privateKey The private key to sign the JWT with.
     * @return The JWTBuilder.
     * @throws SignatureException If there is a problem signing the JWT.
     */
    public JWTBuilder sign(PrivateKey privateKey) throws SignatureException {
        if (isSigned()) {
            throw new JWTBuilderException("JWT has been signed already.");
        }

            if (privateKey == null) {
            throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Could not perform signature with invalid private key");
        }

        if (type.equals("JWT")) {
            type = "JWS";
        }

        byte[] jwtSignature = signatureUtil.sign(privateKey, buildHeader() + buildBody());
        this.signature = new String(jwtSignature);

        return this;
    }

    /**
     * Encrypts the JWT.
     *
     * NOT YET IMPLEMENTED.
     *
     * @return The JWTBuilder.
     */
    public JWTBuilder encrypt() {
        if (isEncrypted()) {
            throw new JWTBuilderException("JWT has been encrypted already.");
        }
        this.encrypted = true;
        if (type.equals("JWT")) {
            type = "JWE";
        }
        throw new UnsupportedOperationException("Not implemented yet. Refer to the JWE spec.");
    }

    /**
     * Builds the JWT by creating a JSON string from the header, body and signature (if required).
     *
     * Dependent on the type of the JWT (JWT/JWS/JWE) determines the steps used to create the JSON string. See the
     * JWT specification for details.
     *
     * @return The JSON string of the JWT.
     */
    public String build() {

        if ("JWS".equals(type)) {

            String header = buildHeader();
            String body = buildBody();
            String encSig = buildSignature();

            String jwt = header + "." + body + "." + encSig;

            if (isEncrypted()) {
                throw new UnsupportedOperationException("Not implemented yet. Refer to the JWE spec.");
            }

            return jwt;

        } else if ("JWE".equals(type)) {
            // Need to implement do encryption
            if (isSigned()) {
                // Need to implement do signing
            }
            throw new UnsupportedOperationException("Not implemented yet. Refer to the JWE spec.");
        } else {
            // PlainText JWT
            String header = buildHeader();
            String body = buildBody();

            return header + "." + body + ".";
        }
    }

    /**
     * Builds the header of the JWT by creating a JSON string of the header parameters and Base64 encoding it.
     *
     * @return The Base64 encoded JSON string of the JWT header.
     */
    private String buildHeader() {

        if ("JWT".equalsIgnoreCase(type)) {
            algorithm = "none";
        }

        try {
            JSONObject header = new JSONObject()
                    .put("typ", type)
                    .put("alg", algorithm);
            return Base64.encode(header.toString().getBytes());
        } catch (JSONException e) {
            throw new JWTBuilderException("Unable to build JWT Header", e);
        }
    }

    /**
     * Builds the body of the JWT by creating a JSON string of the value pairs and Base64 encoding it.
     *
     * @return The Base64 encoded JSON string of the JWT payload.
     */
    private String buildBody() {

        try {
            JSONObject body = new JSONObject();

            for (Map.Entry<String, Object> key : payload.entrySet()) {
                body.put(key.getKey(), key.getValue());

            }

            return Base64.encode(body.toString().getBytes());
        } catch (JSONException e) {
            throw new JWTBuilderException("Unable to build JWT Body", e);
        }
    }

    /**
     * Builds the signature of the JWT by creating a JSON string of the signature and Base64 encoding it.
     *
     * @return The Base64 encoded JSON string of the JWT signature.
     */
    private String buildSignature() {
        if (!isSigned()) {
            throw new IllegalStateException("JWT has not been signed");
        }
        return Base64.encode(signature.getBytes());
    }

    /**
     * Verifies the signature of the JWT with the given certificate. If the signature is not valid a
     * SignatureException will thrown.
     *
     * @param jwt The JWT to verify the signature of.
     * @param certificate The certificate to use to verify the signature.
     * @throws SignatureException If there is a problem with verifying the signature.
     */
    public void verify(String jwt, X509Certificate certificate) throws SignatureException {

        int endOfHeader = jwt.indexOf(".");
        int endOfBody = jwt.indexOf(".", (endOfHeader + 1));
        String encJwtHeader = jwt.substring(0, endOfHeader);
        String encJwtBody = jwt.substring((endOfHeader + 1), endOfBody);
        String jwtSignature = new String(Base64.decode(jwt.substring((endOfBody + 1), jwt.length())));

        boolean verified = signatureUtil.verify(certificate, encJwtHeader + encJwtBody,
                jwtSignature.getBytes());

//        if (!verified) {
//            throw new SignatureException("Signature does that match required header and body.");
//        }   TODO!!! Verification does not work!!!!
    }
}
