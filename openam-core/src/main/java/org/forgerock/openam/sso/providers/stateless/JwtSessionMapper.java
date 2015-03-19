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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.share.SessionInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Reject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.security.KeyPair;

/**
 * Responsible for converting {@link SessionInfo} objects to/from JWT with optional signing &/or encryption.
 *
 * @since 13.0.0
 */
@Immutable
public final class JwtSessionMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
    private static final String SERIALIZED_SESSION_CLAIM = "serialized_session";

    private final JwsAlgorithm jwsAlgorithm;
    private final SigningHandler signingHandler;
    private final SigningHandler verificationHandler;
    private final KeyPair encryptionKeyPair;

    /**
     * Constructs a fully-configured, immutable instance of JwtSessionMapper.
     *
     * @param jwsAlgorithm Non-null, JwtAlgorithm to use for signing and verification.
     * @param signingHandler Non-null, delegate to call for signing.
     * @param verificationHandler Non-null, delegate to call for signature verification.
     * @param encryptionKeyPair Nullable, public-private key-pair to use for encryption.
     *                          If null, no encryption is applied.
     */
    public JwtSessionMapper(@Nonnull JwsAlgorithm jwsAlgorithm,
                            @Nonnull SigningHandler signingHandler,
                            @Nonnull SigningHandler verificationHandler,
                            @Nullable KeyPair encryptionKeyPair) {

        Reject.ifNull(jwsAlgorithm, "jwsAlgorithm must not be null.");
        Reject.ifNull(signingHandler, "signingHandler must not be null.");
        Reject.ifNull(verificationHandler, "verificationHandler must not be null.");

        this.jwsAlgorithm = jwsAlgorithm;
        this.signingHandler = signingHandler;
        this.verificationHandler = verificationHandler;
        this.encryptionKeyPair = encryptionKeyPair;
    }

    /**
     * Store the SessionInfo as a serialized_session claim in a JWT.
     *
     * The returned JWT will be signed using the specified {@link JwsAlgorithm}.
     *
     * @param sessionInfo Non-null, SessionInfo state to be stored in the returned JWT.
     *
     * @return String JWT with SessionInfo stored in serialized_session claim.
     */
    public String asJwt(@Nonnull SessionInfo sessionInfo) {

        Reject.ifNull(sessionInfo, "sessionInfo must not ne null.");

        String json = asJson(sessionInfo);
        // TODO: Make serialized_session value actual JSON rather than a String

        JwtClaimsSet claimsSet = jwtBuilderFactory.claims().claim(SERIALIZED_SESSION_CLAIM, json).build();

        if (encryptionKeyPair != null) {

            return jwtBuilderFactory.jwe(encryptionKeyPair.getPublic())
                    .headers().alg(JweAlgorithm.RSAES_PKCS1_V1_5).enc(EncryptionMethod.A128CBC_HS256).done()
                    .claims(claimsSet)
                    .sign(signingHandler, jwsAlgorithm)
                    .build();

        } else {

            return jwtBuilderFactory.jws(signingHandler)
                    .headers().alg(jwsAlgorithm).done()
                    .claims(claimsSet)
                    .build();

        }

    }

    /**
     * Extract the SessionInfo stored in the provided JWT's serialized_session claim.
     *
     * @param jwtString Non-null, String which represents a JWT with SessionInfo state assigned to a serialized_session claim.
     *
     * @throws JwtRuntimeException If the provided JWT was signed but the signature cannot be verified.
     *
     * @return SessionInfo
     */
    public SessionInfo fromJwt(@Nonnull String jwtString) {

        Reject.ifNull(jwtString, "jwtString must not ne null.");

        SignedJwt signedJwt;

        if (encryptionKeyPair != null) {

            SignedEncryptedJwt signedEncryptedJwt = jwtBuilderFactory.reconstruct(jwtString, SignedEncryptedJwt.class);
            signedEncryptedJwt.decrypt(encryptionKeyPair.getPrivate());
            signedJwt = signedEncryptedJwt;

        } else {

            signedJwt = jwtBuilderFactory.reconstruct(jwtString, SignedJwt.class);

        }

        if (!signedJwt.verify(verificationHandler)) {
            throw new JwtRuntimeException("Invalid JWT!");
        }

        JwtClaimsSet claimsSet = signedJwt.getClaimsSet();
        String serializedSession = claimsSet.getClaim(SERIALIZED_SESSION_CLAIM, String.class);
        return fromJson(serializedSession);
    }

    /**
     * @param inputSessionInfo Non-null, SessionInfo to convert to JSON.
     *
     * @return JSON representation of the provided SessionInfo state.
     */
    @VisibleForTesting
    String asJson(@Nonnull SessionInfo inputSessionInfo) {
        try {
            return MAPPER.writeValueAsString(inputSessionInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param jsonString Non-null, JSON representation of SessionInfo state.
     *
     * @return SessionInfo deserialized from JSON.
     */
    @VisibleForTesting
    SessionInfo fromJson(@Nonnull String jsonString) {
        try {
            return MAPPER.readValue(jsonString, SessionInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}