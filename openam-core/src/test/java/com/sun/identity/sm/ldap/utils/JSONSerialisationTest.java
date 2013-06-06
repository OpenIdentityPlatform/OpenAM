/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package com.sun.identity.sm.ldap.utils;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.TimedAction;
import com.sun.identity.sm.ldap.TokenTestUtils;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class JSONSerialisationTest {
    private static final Float ITERATIONS = new Float(5000);

    public void shouldSerialiseAString() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));

        String test = "Badger";
        // When
        String result = serialisation.deserialise(serialisation.serialise(test), String.class);
        // Then
        assertEquals(test, result);
    }

    public void shouldSerialiseAMap() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));

        Map<String, Object> test = new HashMap<String, Object>();
        test.put("badger", 1234);
        test.put("ferret", 4321);

        // When
        String text = serialisation.serialise(test);
        Map<String, Object> result = serialisation.deserialise(text, Map.class);
        // Then
        assertEquals(test, result);
    }

    public void shouldDeserialiseSerialisedToken() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        Token token = new Token("id", TokenType.OAUTH);

        // When
        Token result = serialisation.deserialise(serialisation.serialise(token), Token.class);

        // Then
        TokenTestUtils.compareTokens(result, token);
    }

    public void shouldImproveOnPerformance() {
        // Given
        TimedAction previous = new TimedAction(){
            @Override
            public void action() {
                try {
                    SessionUtils.encode(generateSession());
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        };

        TimedAction current = new TimedAction(){
            private JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
            @Override
            public void action() {
                serialisation.serialise(generateSession());
            }
        };

        // When
        float previousAvg = previous.go();
        float currentAvg = current.go();

        // Then
        String message = MessageFormat.format(
                "The previous method took: {1}\n" +
                "Current method took: {2}",
                ITERATIONS,
                previous,
                current);

        if (currentAvg > previousAvg) {
            throw new AssertionError(message);
        }
    }

    public void shouldSerialiseAndDeserialiseAToken() {
        // Given
        Token token = TokenTestUtils.generateToken();
        JSONSerialisation serialisation = new JSONSerialisation();
        // When
        String text = serialisation.serialise(token);
        Token result = serialisation.deserialise(text, Token.class);
        // Then
        TokenTestUtils.compareTokens(result, token);
    }

    /**
     * Generate an InternalSession with some fields set to random values.
     *
     * @return Non null.
     */
    public static InternalSession generateSession() {
        SessionID sid = new SessionID(randomString());
        InternalSession r = new InternalSession(sid, mock(SessionService.class), mock(Debug.class));
        // String
        r.setObject(randomString(), randomString());
        // Boolean
        r.setCookieMode(randomBoolean());
        r.setExpire(randomBoolean());
        r.setIsSessionUpgrade(randomBoolean());
        // Long
        r.setVersion(randomLong());
        return r;
    }

    private static String randomString() {
        return Double.toString(Math.random());
    }

    private static boolean randomBoolean() {
        return Math.random() > 0.5d;
    }

    private static long randomLong() {
        return Math.round(Math.random() * 1000);
    }
}
