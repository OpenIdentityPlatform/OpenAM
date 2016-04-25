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

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.Reject;

/**
 * Checks that the response to a message is appropriate for the
 * user from whom it claims to be sent, by validating that the JWT sent in is
 * signed by the appropriate user (their shared secret is retrieved from the user store).
 */
public class SignedJwtVerificationPredicate implements Predicate {

    private final byte[] secret;
    private final JsonPointer location;

    /**
     * Create a new SNS Predicate, for use with the supplied secret and challenge.
     *
     * @param secret Used to verify JWT messages and content.
     * @param location Used to locate the jwt within the JsonValue passed to perform().
     */
    public SignedJwtVerificationPredicate(byte[] secret, JsonPointer location) {
        Reject.ifNull(secret);
        this.secret = secret;
        this.location = location;
    }

    @Override
    public boolean perform(JsonValue content) {
        SigningHandler signingHandler = new SigningManager().newHmacSigningHandler(secret);

        SignedJwt signedJwt = new JwtReconstruction().reconstructJwt(content.get(location).asString(),
                SignedJwt.class);

        return signedJwt.verify(signingHandler);
    }
}
