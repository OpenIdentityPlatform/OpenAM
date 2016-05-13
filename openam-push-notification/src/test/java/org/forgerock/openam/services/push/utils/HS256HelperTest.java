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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.forgerock.util.encode.Base64;
import org.testng.annotations.Test;

public class HS256HelperTest {

    SecureRandom secureRandom = new SecureRandom();

    @Test
    public void shouldPass() {
        //given
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);

        String challenge = "challenge";
        String response = figureResponse(secretBytes, challenge);

        //when
        HS256Helper helper = new HS256Helper(secretBytes, challenge);
        String result = helper.answerAsString();

        //then
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void shouldReturnNullWithNullSecret() {
        //given
        byte[] secretBytes = null;

        String challenge = "challenge";

        //when
        HS256Helper helper = new HS256Helper(secretBytes, challenge);
        String result = helper.answerAsString();

        //then
        assertThat(result).isNull();
    }

    @Test
    public void shouldReturnWithNullChallenge() {
        //given
        byte[] secretBytes = new byte[32];
        String answer = "thNnmggU2ex3L5XXeMNfxf8Wl8STcVZTxscSFEKSxa0=";

        String challenge = null;

        //when
        HS256Helper helper = new HS256Helper(secretBytes, challenge);
        String result = helper.answerAsString();

        //then
        assertThat(result).isEqualTo(answer);
    }


    private String figureResponse(byte[] secret, String challenge) {
        Mac hmac;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, HMACSHA256);

        try {
            hmac = Mac.getInstance(HMACSHA256);
            hmac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }

        byte[] output = hmac.doFinal(Base64.decode(challenge));
        return Base64.encode(output);
    }

}
