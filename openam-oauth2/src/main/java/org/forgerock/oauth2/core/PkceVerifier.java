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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.Utils.isEmpty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token endpoint side of Proof Key for Code Exchange (RFC 7636 §4.6), shared by every grant whose
 * code can carry a {@code code_challenge}.
 */
final class PkceVerifier {

    private static final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private PkceVerifier() {
    }

    /**
     * Whether a code was issued with a {@code code_challenge}, and so must be redeemed with the
     * matching {@code code_verifier} whatever the provider-wide enforcement setting says.
     */
    static boolean hasChallenge(String codeChallenge) {
        return codeChallenge != null && !codeChallenge.isEmpty();
    }

    /**
     * Verifies the {@code code_verifier} supplied at the token endpoint against the challenge
     * stored on the code.
     *
     * @throws InvalidRequestException if the verifier is missing or the stored method is not one
     *         OpenAM supports.
     * @throws InvalidGrantException if the verifier does not match the challenge.
     */
    static void verify(String codeChallenge, String codeChallengeMethod, String codeVerifier)
            throws InvalidRequestException, InvalidGrantException {
        if (isEmpty(codeVerifier)) {
            throw new InvalidRequestException("code_verifier parameter required");
        }

        // RFC 7636 4.3: an omitted code_challenge_method defaults to "plain". An explicitly empty
        // value is malformed, not omitted, so it falls through to the rejection below.
        if (codeChallengeMethod == null
                || OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_PLAIN.equals(codeChallengeMethod)) {
            checkCodeChallenge(codeChallenge, codeVerifier);
        } else if (OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_S_256.equals(codeChallengeMethod)){
            String encodedCodeVerifier = null;
            try {
                encodedCodeVerifier = Base64url.encode(
                        MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
                checkCodeChallenge(codeChallenge, encodedCodeVerifier);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Error encoding code verifier.");
                throw new InvalidGrantException();
            }
        } else {
            throw new InvalidRequestException("Invalid code challenge method specified.");
        }
    }

    private static void checkCodeChallenge(String codeChallenge, String codeVerifier) throws InvalidGrantException {
        if (!MessageDigest.isEqual(codeVerifier.getBytes(), codeChallenge.getBytes())) {
            logger.error("Incorrect Code Verifier,");
            throw new InvalidGrantException();
        }
    }
}
