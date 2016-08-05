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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import java.security.Key;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iplanet.dpro.session.share.SessionInfo;

/**
 * Responsible for converting {@link SessionInfo} objects to/from JWT with optional signing &/or encryption.
 *
 * @since 13.0.0
 */
@Immutable
public final class JwtSessionMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .configure(SerializationFeature.INDENT_OUTPUT, false);
    private static final JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    @VisibleForTesting
    final JwsAlgorithm jwsAlgorithm;
    @VisibleForTesting
    final SigningHandler signingHandler;
    @VisibleForTesting
    final SigningHandler verificationHandler;
    @VisibleForTesting
    final JweAlgorithm jweAlgorithm;
    private final EncryptionMethod encryptionMethod;
    @VisibleForTesting
    final Key encryptionKey;
    @VisibleForTesting
    final Key decryptionKey;
    @VisibleForTesting
    final CompressionAlgorithm compressionAlgorithm;

    /**
     * Constructs a fully-configured, immutable instance of JwtSessionMapper.
     *
     * @param builder the jwt builder.
     */
    JwtSessionMapper(@Nonnull JwtSessionMapperBuilder builder) {
        this.jwsAlgorithm = builder.jwsAlgorithm;
        this.signingHandler = builder.signingHandler;
        this.verificationHandler = builder.verificationHandler;
        this.encryptionKey = builder.encryptionKey;
        this.decryptionKey = builder.decryptionKey;
        this.jweAlgorithm = builder.jweAlgorithm;
        this.encryptionMethod = builder.encryptionMethod;
        this.compressionAlgorithm = builder.compressionAlgorithm;
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
    String asJwt(@Nonnull SessionInfo sessionInfo) {

        Reject.ifNull(sessionInfo, "sessionInfo must not be null.");

        final Map<String, Object> claimMap = MAPPER.convertValue(sessionInfo, MAP_TYPE);
        final JwtClaimsSet claims = new JwtClaimsSet(claimMap);

        if (jweAlgorithm != null) {
            EncryptedJwtBuilder jwtBuilder = jwtBuilderFactory.jwe(encryptionKey)
                                    .headers()
                                        .alg(jweAlgorithm)
                                        .enc(encryptionMethod)
                                        .zip(compressionAlgorithm)
                                    .done()
                                    .claims(claims);

            if (jwsAlgorithm != JwsAlgorithm.NONE) {
                return jwtBuilder.signedWith(signingHandler, jwsAlgorithm).build();
            } else {
                return jwtBuilder.build();
            }
        } else {

            return jwtBuilderFactory.jws(signingHandler)
                .headers().alg(jwsAlgorithm).zip(compressionAlgorithm).done()
                .claims(claims)
                .build();

        }

    }

    /**
     * Extract the SessionInfo stored in the provided JWT's serialized_session claim.
     *
     * @param jwtString Non-null, String which represents a JWT with SessionInfo state assigned to a serialized_session claim.
     *
     * @return SessionInfo A correctly parsed SessionInfo for the given JWT String.
     *
     * @throws JwtRuntimeException If there was a problem reconstructing the JWT
     */
    SessionInfo fromJwt(@Nonnull String jwtString) throws JwtRuntimeException {

        Reject.ifNull(jwtString, "jwtString must not be null.");

        SignedJwt signedJwt;

        if (jweAlgorithm != null) {
            if (jwsAlgorithm != JwsAlgorithm.NONE) {
                // could throw JwtRuntimeException
                EncryptedThenSignedJwt signedEncryptedJwt = jwtBuilderFactory.reconstruct(jwtString,
                        EncryptedThenSignedJwt.class);

                if (!doesJwtAlgorithmMatch(signedEncryptedJwt) || !signedEncryptedJwt.verify(verificationHandler)) {
                    throw new JwtRuntimeException("Invalid JWT!");
                }

                signedEncryptedJwt.decrypt(decryptionKey);
                signedJwt = signedEncryptedJwt;
            } else {
                EncryptedJwt encryptedJwt = jwtBuilderFactory.reconstruct(jwtString, EncryptedJwt.class);
                encryptedJwt.decrypt(decryptionKey);
                return fromJson(encryptedJwt.getClaimsSet());
            }

        } else {

            // could throw JwtRuntimeException
            signedJwt = jwtBuilderFactory.reconstruct(jwtString, SignedJwt.class);

            if (!doesJwtAlgorithmMatch(signedJwt) || !signedJwt.verify(verificationHandler)) {
                throw new JwtRuntimeException("Invalid JWT!");
            }
        }


        JwtClaimsSet claimsSet = signedJwt.getClaimsSet();
        return fromJson(claimsSet);
    }

    private SessionInfo fromJson(JwtClaimsSet claimsSet) {
        return MAPPER.convertValue(toMap(claimsSet), SessionInfo.class);
    }

    private boolean doesJwtAlgorithmMatch(SignedJwt signedJwt) {
        try {
            return jwsAlgorithm.equals(signedJwt.getHeader().getAlgorithm());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Map<String, Object> toMap(JwtClaimsSet claims) {
        final Map<String, Object> map = new TreeMap<>();
        for (String key : claims.keys()) {
            map.put(key, claims.get(key).getObject());
        }
        return map;
    }
}