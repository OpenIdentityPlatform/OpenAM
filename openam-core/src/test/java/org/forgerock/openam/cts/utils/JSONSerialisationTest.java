/**
 * Copyright 2013 ForgeRock, AS.
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
package org.forgerock.openam.cts.utils;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class JSONSerialisationTest {
    @Test
    public void shouldSerialiseAString() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation();

        String test = "Badger";
        // When
        String result = serialisation.deserialise(serialisation.serialise(test), String.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldSerialiseAMap() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation();

        Map<String, Object> test = new HashMap<String, Object>();
        test.put("badger", 1234);
        test.put("ferret", 4321);

        // When
        String text = serialisation.serialise(test);
        Map<String, Object> result = serialisation.deserialise(text, Map.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldDeserialiseSerialisedToken() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation();
        Token token = new Token("id", TokenType.OAUTH);

        // When
        Token result = serialisation.deserialise(serialisation.serialise(token), Token.class);

        // Then
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void shouldSerialiseAndDeserialiseAToken() {
        // Given
        Token token = TokenTestUtils.generateToken();
        JSONSerialisation serialisation = new JSONSerialisation();
        // When
        String text = serialisation.serialise(token);
        Token result = serialisation.deserialise(text, Token.class);
        // Then
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void shouldChangeAttributeName() {
        String name = "badger";
        assertNotEquals(name, JSONSerialisation.jsonAttributeName(name));
    }

    /**
     * Generate an InternalSession with some fields set to random values.
     *
     * @return Non null.
     */
    private static InternalSession generateSession() {
        SessionID sid = new SessionID(randomString());
        InternalSession r = new InternalSession(sid, mock(SessionService.class), mock(Debug.class));
        // String
        r.setObject(randomString(), randomString());
        // Boolean
        r.setCookieMode(randomBoolean());
        r.setExpire(randomBoolean());
        r.setIsSessionUpgrade(randomBoolean());
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

    /**
     * Demonstrates that neither serialisation mechanism is consistent with the time it takes
     * to serialise a session. This is not a completely fair test because the Internal Session
     * class being serialised is awkward at best to serialise and deserialise.
     *
     * @param args none.
     */
    public static void main(String[] args) throws InterruptedException {
        final ExecutorService service = Executors.newFixedThreadPool(8);
        final long iterations = 1000;
        final JSONSerialisation serialisation = new JSONSerialisation();

        final Runnable command = new Runnable() {
            public void run() {

                /**
                 * Time the previous mechanism
                 */
                long start = System.currentTimeMillis();
                for (int ii = 0; ii < iterations; ii++) {
                    try {
                        SessionUtils.encode(generateSession());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
                long delta = System.currentTimeMillis() - start;
                float previous = (float)delta / iterations;

                /**
                 * Time the current mechanism
                 */
                start = System.currentTimeMillis();
                for (int ii = 0; ii < iterations; ii++) {
                    serialisation.serialise(generateSession());
                }
                delta = System.currentTimeMillis() - start;
                float current = (float)delta / iterations;

                String msg = MessageFormat.format(
                        "Object {0}ms vs JSON: {1}ms",
                        previous,
                        current);
                (current <= previous ? System.out : System.err).println(msg);

                if (!service.isShutdown()) {
                    service.execute(this);
                }
            }
        };

        System.out.println("Serialisation Test");
        for (int ii = 0; ii < 8; ii++) {
            service.execute(command);
        }
        Thread.sleep(3000);
        service.shutdown();
        service.awaitTermination(1, TimeUnit.SECONDS);
        System.exit(0);
    }
}
