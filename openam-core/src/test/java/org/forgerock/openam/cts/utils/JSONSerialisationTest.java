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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils;

import com.iplanet.dpro.session.DNOrIPAddressListTokenRestriction;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class JSONSerialisationTest {

    private static final JSONSerialisation SERIALISATION = new JSONSerialisation();

    @Test
    public void shouldSerialiseAString() {
        // Given
        String test = "Badger";
        // When
        String result = SERIALISATION.deserialise(SERIALISATION.serialise(test), String.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldSerialiseAMap() {
        // Given
        Map<String, Object> test = new HashMap<String, Object>();
        test.put("badger", 1234);
        test.put("ferret", 4321);

        // When
        String text = SERIALISATION.serialise(test);
        Map<String, Object> result = SERIALISATION.deserialise(text, Map.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldDeserialiseSerialisedToken() {
        // Given
        Token token = new Token("id", TokenType.OAUTH);

        // When
        Token result = SERIALISATION.deserialise(SERIALISATION.serialise(token), Token.class);

        // Then
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void shouldSerialiseAndDeserialiseAToken() {
        // Given
        Token token = TokenTestUtils.generateToken();
        // When
        String text = SERIALISATION.serialise(token);
        Token result = SERIALISATION.deserialise(text, Token.class);
        // Then
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void basicSessionSerializationWorks() throws Exception {
        InternalSession is = new InternalSession();
        String serialised = SERIALISATION.serialise(is);
        assertThat(serialised).isNotNull().isEqualTo(getJSON("/json/basic-session.json"));
        assertThat(is).isNotNull();
    }

    @Test
    public void tokenRestrictionDeserialisationWithTypeWorks() throws Exception {
        InternalSession is = SERIALISATION.deserialise(getJSON("/json/basic-session-with-restriction.json"),
                InternalSession.class);
        assertThat(is).isNotNull();
        TokenRestriction restriction = is.getRestrictionForToken(new SessionID("AQIC5wM2LY4SfcyTLz6VjQ7nkFeDcEh8K5dXkIE"
                + "NpXlpg28.*AAJTSQACMDIAAlMxAAIwMQACU0sAEzc5ODIzMDM5MzQyNzU2MTg1NDQ.*"));
        assertThat(restriction).isNotNull().isInstanceOf(DNOrIPAddressListTokenRestriction.class);
        assertThat(restriction.toString().equals("Fzy2GsI/O1TsXhvlVuqjqIuTG2k="));
    }

    @DataProvider(name = "complex")
    public Object[][] getComplexJSONs() {
        return new Object[][]{
            {"/json/complex-session-with-restriction-v11.json"},
            {"/json/complex-session-with-restriction-v12.json"}
        };
    }

    @Test(dataProvider = "complex")
    public void internalSessionDeserialisationWorks(String path) throws Exception {
        InternalSession is = SERIALISATION.deserialise(getJSON(path), InternalSession.class);
        assertThat(is).isNotNull();
        assertThat(is.getID()).isNotNull();
        assertThat(Collections.list(is.getPropertyNames())).hasSize(23);
    }

    @Test(dataProvider = "complex")
    public void internalSessionDeserialisationDoesNotModifyMapTypes(String path) throws Exception {
        InternalSession is = SERIALISATION.deserialise(getJSON(path), InternalSession.class);
        assertThat(is).isNotNull();
        checkMapType(is, "sessionEventURLs");
        checkMapType(is, "restrictedTokensBySid");
        checkMapType(is, "restrictedTokensByRestriction");
    }

    @Test(dataProvider = "complex")
    public void complexInternalSessionSerializationWorks(String path) throws Exception {
        InternalSession is = SERIALISATION.deserialise(getJSON(path), InternalSession.class);
        assertThat(is).isNotNull();
        String serialised = SERIALISATION.serialise(is);
        assertThat(serialised).isNotNull().isNotEmpty();
        InternalSession is2 = SERIALISATION.deserialise(serialised, InternalSession.class);
        assertThat(is2).isNotNull().isNotSameAs(is);
        assertThat(is.getID()).isEqualTo(is2.getID());
    }

    @Test
    public void shouldChangeAttributeName() {
        String name = "badger";
        assertNotEquals(name, JSONSerialisation.jsonAttributeName(name));
    }

    private static String getJSON(String path) throws Exception {
        return IOUtils.getFileContentFromClassPath(path).replaceAll("\\s", "");
    }

    private static void checkMapType(InternalSession is, String fieldName) throws Exception {
        Field field = is.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object obj = field.get(is);
        assertThat(obj).isInstanceOf(ConcurrentHashMap.class);
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
                    SERIALISATION.serialise(generateSession());
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
