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

import java.security.MessageDigest;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.services.push.utils.HS256Helper;
import org.forgerock.util.Reject;

/**
 * Checks that the response to a message is appropriate for the
 * user from whom it claims to be sent, by asserting that the
 * contents is the correct response to the challenge (HmacSHA-256 encoded version
 * of the challenge sent out).
 */
public class PushMessageChallengeResponsePredicate extends AbstractPredicate {

    private String answer;
    private String location;

    /**
     * Create a new PushMessagePredicate that will ensure that the contents of the JsonValue found at the
     * location of the JsonPointer location is equal to a predicted challenge response value.
     *
     * @param secret Secret used to generate the response value.
     * @param challenge Random challenge.
     * @param location Json Pointer string of the location of the value expected to the the challenge response.
     */
    public PushMessageChallengeResponsePredicate(byte[] secret, String challenge, String location) {
        Reject.ifNull(secret, challenge, location);
        this.location = location;
        this.answer = new HS256Helper(secret, challenge).answerAsString();
    }

    @Override
    public boolean perform(JsonValue content) {
        if (answer == null) {
            return false;
        }

        Jwt signedJwt = new JwtReconstruction().reconstructJwt(content.get(new JsonPointer(location)).asString(),
                Jwt.class);

        String response = (String) signedJwt.getClaimsSet().getClaim(RESPONSE_LOCATION);

        return MessageDigest.isEqual(answer.getBytes(), response.getBytes());
    }

    /**
     * Default constructor for the PushMessageChallengeResponsePredicate, used for serialization purposes.
     */
    public PushMessageChallengeResponsePredicate() {
    }

    /**
     * Sets the answer for this predicate. Used when deserialized from the CTS.
     * @param answer The answer for this predicate.
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Sets the location for this predicate. Used when deserilialized from the CTS.
     * @param location The location for this predicate.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Reads the answer out. Used so the authenticator system doesn't have to duplicate the HMAC alg.
     * @return The expected answer.
     */
    public String getAnswer() {
        return answer;
    }

}
