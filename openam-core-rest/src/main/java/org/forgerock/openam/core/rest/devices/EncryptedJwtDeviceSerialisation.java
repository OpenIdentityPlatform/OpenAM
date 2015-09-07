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

package org.forgerock.openam.core.rest.devices;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Reject;

import java.security.KeyPair;
import java.util.LinkedHashMap;

/**
 * Stores device profiles as an encrypted JWT for security.
 */
public final class EncryptedJwtDeviceSerialisation implements DeviceSerialisation {
    private static final JwtBuilderFactory JWT = new JwtBuilderFactory();

    private final KeyPair keyPair;
    private final EncryptionMethod encryptionMethod;
    private final JweAlgorithm jweAlgorithm;

    public EncryptedJwtDeviceSerialisation(final EncryptionMethod encryptionMethod, final JweAlgorithm jweAlgorithm,
                                           final KeyPair encryptionKeyPair) {
        Reject.ifNull(encryptionMethod, jweAlgorithm, encryptionKeyPair);
        Reject.ifNull(encryptionKeyPair.getPublic(), "PublicKey cannot be null");
        Reject.ifNull(encryptionKeyPair.getPrivate(), "PrivateKey cannot be null");

        this.keyPair = encryptionKeyPair;
        this.encryptionMethod = encryptionMethod;
        this.jweAlgorithm = jweAlgorithm;
    }

    @Override
    public String deviceProfileToString(final JsonValue deviceProfile) {
        return JWT.jwe(keyPair.getPublic())
                .headers().enc(encryptionMethod).alg(jweAlgorithm).done()
                .claims(JWT.claims().claims(deviceProfile.asMap()).build())
                .build();
    }

    @Override
    public JsonValue stringToDeviceProfile(final String value) {
        final EncryptedJwt jwt = new JwtReconstruction().reconstructJwt(value, EncryptedJwt.class);
        jwt.decrypt(keyPair.getPrivate());
        return claimsToJson(jwt.getClaimsSet());
    }

    private static JsonValue claimsToJson(JwtClaimsSet claims) {
        final JsonValue json = new JsonValue(new LinkedHashMap<>());
        for (String key : claims.keys()) {
            json.put(key, claims.getClaim(key));
        }
        return json;
    }

}
