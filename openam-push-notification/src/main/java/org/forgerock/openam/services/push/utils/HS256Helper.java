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
package org.forgerock.openam.services.push.utils;

import static org.forgerock.openam.services.push.PushNotificationConstants.HMACSHA256;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.forgerock.util.encode.Base64;

/**
 * Simple class for performing a single task - the HS256 algorithm performed
 * over a supplied challenge using a provided secret.
 */
public class HS256Helper {

    private byte[] secret;
    private String challenge;

    private byte[] result = null;

    /**
     * Generates a new HS256Helper instance.
     *
     * @param secret The secret to use.
     * @param challenge The challenge to apply the algorithm to.
     */
    public HS256Helper(byte[] secret, String challenge) {
        this.secret = secret;
        this.challenge = challenge;

        perform();
    }

    private void perform() {

        if (secret == null) {
            return;
        }

        Mac hmac;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, HMACSHA256);

        try {
            hmac = Mac.getInstance(HMACSHA256);
            hmac.init(key);
            this.result = hmac.doFinal(Base64.decode(challenge));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            //intentionally left blank
        }

    }

    /**
     * Returns the result of the HS256 algorithm over the challenge using the secret. Returns null if no valid answer
     * was determined.
     *
     * @return A Base64 encoded String of the answer, or null.
     */
    public String answerAsString() {
        if (result == null) {
            return null;
        }

        return Base64.encode(result);
    }

    /**
     * Returns the result of the HS256 algorithm over the challenge using the secret. Returns null if no valid answer
     * was determined.
     *
     * @return A byte array of the answer, or null.
     */
    public byte[] answerAsBytes() {
        return result;
    }

    /**
     * Sets the secret.
     * @param secret The secret to use in the algorithm.
     */
    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    /**
     * Sets the challenge.
     * @param challenge The challenge to use in the algorithm.
     */
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }
}
