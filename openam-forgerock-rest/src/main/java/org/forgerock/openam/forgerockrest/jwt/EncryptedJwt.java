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

import java.security.PrivateKey;

/**
 * Represents a JWT which has been encrypted.
 *
 * Not yet supported.
 */
public class EncryptedJwt implements Jwt {

    /**
     * Signs the Plaintext Jwt, resulting in a SignedEncryptedJwt.
     *
     * @param algorithm The Jwt Algorithm used to perform the signing.
     * @param privateKey The private key to use to sign with.
     * @return A SignedEncryptedJwt.
     * @throws JWTBuilderException If there is a problem creating the SignedJwt.
     */
    public SignedEncryptedJwt sign(JwsAlgorithm algorithm, PrivateKey privateKey) throws JWTBuilderException {
        return new SignedEncryptedJwt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build() {
        return null;
    }
}
