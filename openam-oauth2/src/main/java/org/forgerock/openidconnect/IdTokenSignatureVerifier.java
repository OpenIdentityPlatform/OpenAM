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

package org.forgerock.openidconnect;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that an {@code id_token} handed back to this provider carries a signature this provider
 * produced.
 *
 * <p>Both the key and the digest come from the client's registration: the algorithm the client is
 * registered to receive id_tokens in selects the handler, and the {@code alg} header of the token
 * has to name that same algorithm. {@link SignedJwt#verify(SigningHandler)} feeds the header
 * algorithm to the handler, and an HMAC handler accepts whichever digest it is given, so without
 * that second check the sender - not the registration - would choose the digest.
 *
 * <p>Everything the caller wrote is treated as hostile. A header or a key the jose library cannot
 * work with makes it throw, so verification is wrapped: the answer to an unusable token is
 * {@code false}, never an exception escaping into the caller's error path.
 *
 * @since 16.2.0
 */
class IdTokenSignatureVerifier {

    private static final String ALGORITHM_HEADER = "alg";

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final int MAX_LOGGED_LENGTH = 128;

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final SigningManager signingManager;

    IdTokenSignatureVerifier() {
        this(new SigningManager());
    }

    IdTokenSignatureVerifier(SigningManager signingManager) {
        this.signingManager = signingManager;
    }

    /**
     * Returns the algorithm named by a client registration's {@code id_token_signed_response_alg}.
     *
     * @param algorithmName The registered algorithm name.
     * @return The algorithm, or {@code null} if the registration names none an id_token can be
     *         verified with - {@code none} among them.
     */
    static JwsAlgorithm registeredAlgorithm(String algorithmName) {
        if (StringUtils.isEmpty(algorithmName)) {
            return null;
        }
        final JwsAlgorithm algorithm;
        try {
            algorithm = JwsAlgorithm.valueOf(algorithmName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        // A client registered for 'none' has no verifiable id_tokens, and no key to go looking for.
        return JwsAlgorithmType.NONE.equals(algorithm.getAlgorithmType()) ? null : algorithm;
    }

    /**
     * Returns the client an id_token was issued to, which is what selects the key its signature is
     * checked against. Read before verification, and so not to be trusted for anything else.
     *
     * @param jwt The token.
     * @return The client id, or {@code null} if the token names no client.
     */
    static String clientIdOf(SignedJwt jwt) {
        final JwtClaimsSet claims = jwt.getClaimsSet();
        final String authorizedParty = claims.get(OAuth2Constants.JWTTokenParams.AZP).asString();
        if (StringUtils.isNotEmpty(authorizedParty)) {
            return authorizedParty;
        }
        final List<String> audience = claims.getAudience();
        return audience == null || audience.isEmpty() ? null : audience.get(0);
    }

    /**
     * Returns whether a token this provider signed names itself an id_token.
     *
     * <p>Access and refresh tokens issued statelessly are signed with the same key and carry an
     * {@code aud} naming their client, so a signature on its own does not say what kind of token
     * this is. The tokens this provider mints name themselves, and the token endpoint reads that
     * claim for the same purpose. A token that carries no name at all predates the claim and is
     * taken as the id_token it says nothing to the contrary about; one whose name is not a string
     * is not a token this provider wrote.
     *
     * @param jwt The token.
     * @return {@code true} if the token is an id_token, or names no kind at all.
     */
    static boolean isIdToken(SignedJwt jwt) {
        final Object tokenName;
        try {
            // Read untyped: a claim that is not a string at all is not a name this provider wrote,
            // and has to read as a refusal rather than as an exception out of an unauthenticated
            // endpoint. A claim that cannot be read at all is refused for the same reason - unlike
            // one that is absent, which predates the claim.
            tokenName = jwt.getClaimsSet().getClaim(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        } catch (RuntimeException e) {
            return false;
        }
        return tokenName == null || OAuth2Constants.JWTTokenParams.ID_TOKEN.equals(tokenName);
    }

    /**
     * Returns the kind of token a token says it is, for reporting what was refused.
     *
     * @param jwt The token.
     * @return The value of the {@code tokenName} claim, or {@code null} if it carries none that can
     *         be read.
     */
    static Object tokenNameOf(SignedJwt jwt) {
        try {
            return jwt.getClaimsSet().getClaim(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Renders a caller-written value for a log line.
     *
     * <p>These are headers and claims of a JWT nothing has verified, on endpoints that need no
     * authentication, so a CR or an LF in one would otherwise forge log lines of its own and an
     * arbitrarily long one would otherwise be written out in full.
     *
     * @param value The value, which may be {@code null}.
     * @return The value, with control characters replaced and its length bounded.
     */
    static String forLog(Object value) {
        if (value == null) {
            return "null";
        }
        final String text = CONTROL_CHARACTERS.matcher(value.toString()).replaceAll("?");
        return text.length() <= MAX_LOGGED_LENGTH ? text : text.substring(0, MAX_LOGGED_LENGTH) + "...";
    }

    /**
     * Verifies the signature of an id_token this provider is supposed to have issued.
     *
     * @param jwt The token to verify.
     * @param algorithm The algorithm the client is registered to receive id_tokens in.
     * @param clientSecret The client's secret, which the HMAC algorithms are verified with.
     * @param signingKey The provider's own public signing key, which the RSA and ECDSA algorithms
     *                   are verified with. May be {@code null} for an HMAC algorithm.
     * @return {@code true} if this provider signed the token with the registered algorithm.
     */
    boolean isSignatureValid(SignedJwt jwt, JwsAlgorithm algorithm, String clientSecret, PublicKey signingKey) {

        if (jwt == null || algorithm == null) {
            return false;
        }

        final String headerAlgorithm = headerAlgorithmOf(jwt);
        if (!algorithm.name().equals(headerAlgorithm)) {
            logger.warn("The id_token is signed with '{}' where its client is registered for '{}'",
                    forLog(headerAlgorithm), algorithm.name());
            return false;
        }

        final SigningHandler signingHandler;
        switch (algorithm.getAlgorithmType()) {
        case HMAC:
            // An HMAC id_token is signed with the client's own secret, not with a provider key.
            if (StringUtils.isEmpty(clientSecret)) {
                logger.warn("No client secret to verify the id_token with");
                return false;
            }
            signingHandler = signingManager.newHmacSigningHandler(clientSecret.getBytes(Utils.CHARSET));
            break;
        case RSA:
            if (signingKey == null) {
                logger.warn("No provider signing key to verify the id_token with");
                return false;
            }
            signingHandler = signingManager.newRsaSigningHandler(signingKey);
            break;
        case ECDSA:
            if (!(signingKey instanceof ECPublicKey)) {
                logger.warn("No provider signing key to verify the id_token with");
                return false;
            }
            signingHandler = signingManager.newEcdsaVerificationHandler((ECPublicKey) signingKey);
            break;
        default:
            // Unreachable: registeredAlgorithm() is the only source of this argument and already
            // returns null for the NONE type, which both callers refuse before reaching here. Kept
            // so that a future algorithm type refuses by default rather than by omission.
            logger.warn("The client is registered for id_tokens signed with '{}', which cannot be verified",
                    algorithm.name());
            return false;
        }

        try {
            return jwt.verify(signingHandler);
        } catch (RuntimeException e) {
            // An unauthenticated caller can drive one of these per request, so the stack goes to
            // debug and the warn line stays short.
            logger.warn("The id_token signature could not be verified: {}", e.getClass().getSimpleName());
            logger.debug("The id_token signature could not be verified", e);
            return false;
        }
    }

    /**
     * Reads the {@code alg} header as it was written, rather than through
     * {@link org.forgerock.json.jose.jws.JwsHeader#getAlgorithm()}, which throws on a value naming
     * no known algorithm - {@code none} among them. An absent header reads as {@code null}; one
     * that is not a string at all makes {@code asString()} throw.
     */
    private String headerAlgorithmOf(SignedJwt jwt) {
        try {
            return jwt.getHeader().get(ALGORITHM_HEADER).asString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
