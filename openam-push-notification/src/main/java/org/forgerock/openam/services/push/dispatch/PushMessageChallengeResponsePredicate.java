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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push.dispatch;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.sun.identity.shared.debug.Debug;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.util.encode.Base64;

/**
 *
 * Checks that the response to a message is appropriate for the
 * user from whom it claims to be sent, by asserting that the
 * contents is the correct response to the challenge (HmacSHA-256 encoded version
 * of the challenge sent out).
 */
public class PushMessageChallengeResponsePredicate implements Predicate {

    private final byte[] secret;
    private final String challenge;
    private final JsonPointer location;
    private final Debug debug;

    /**
     * Create a new PushMessagePredicate that will ensure that the contents of the JsonValue found at the
     * location of the JsonPointer location is equal to a predicted challenge response value.
     *
     * @param secret Secret used to generate the response value.
     * @param challenge Random challenge.
     * @param location Location of the value expected to the the challenge response.
     * @param debug Debug object for logging purposes.
     */
    public PushMessageChallengeResponsePredicate(byte[] secret, String challenge, JsonPointer location, Debug debug) {
        this.secret = secret;
        this.challenge = challenge;
        this.location = location;
        this.debug = debug;
    }

    @Override
    public boolean perform(JsonValue content) {
        Jwt signedJwt = new JwtReconstruction().reconstructJwt(content.get(location).asString(),
                Jwt.class);

        String response = (String) signedJwt.getClaimsSet().getClaim(RESPONSE_LOCATION);

        Mac hmac;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, HMACSHA256);

        try {
            hmac = Mac.getInstance(HMACSHA256);
            hmac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            debug.error("SignedJwtVerificationPredicate :: perform() :: failed due to invalid use of Mac.", e);
            return false;
        }

        byte[] output = hmac.doFinal(Base64.decode(challenge));

        return Base64.encode(output).equals(response);
    }
}
