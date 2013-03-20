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

/**
 * An Enum for the possible algorithms that cna be used for signing JWTs.
 */
public enum JwsAlgorithm {

    HS256("HmacSHA256"),   //HMAC using SHA-256 hash algorithm     //REQUIRED SUPPORT
    HS384("HmacSHA384"),   //HMAC using SHA-384 hash algorithm
    HS512("HmacSHA512"),  //HMAC using SHA-512 hash algorithm
    RS256("SHA256withRSA"),  //RSA using SHA-256 hash algorithm       //RECOMMENDED SUPPORT
    RS384("SHA384withRSA"),  //RSA using SHA-384 hash algorithm
    RS512("SHA512withRSA"); //RSA using SHA-512 hash algorithm
//    ES256(""),  //ECDSA using P-256 curve and SHA-256 hash algorithm
//    ES384(""),  //ECDSA using P-384 curve and SHA-384 hash algorithm
//    ES512("");  //ECDSA using P-521 curve and SHA-512 hash algorithm

    private final String algorithm;

    /**
     * Constructs a JwsAlgorithm.
     *
     * @param algorithm The algorithm string.
     */
    private JwsAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Gets the algorithm string.
     *
     * @return The algorithm string.
     */
    public String getAlgorithm() {
        return algorithm;
    }
}