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
package org.forgerock.openam.cts.utils.blob.strategies;

import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class AttributeCompressionStrategyTest {

    private AttributeCompressionStrategy compression;
    private TokenBlobUtils blobUtils;

    @BeforeMethod
    public void setup() {
        blobUtils = new TokenBlobUtils();
        compression = new AttributeCompressionStrategy(blobUtils);
    }

    @Test
    public void shouldNotSelectAStaticFieldFromClass() {
        assertEquals(0, AttributeCompressionStrategy.getAllValidFields(StaticFieldTest.class).size());
    }

    @Test
    public void shouldSelectPrivateFields() {
        assertEquals(1, AttributeCompressionStrategy.getAllValidFields(PrivateFieldTest.class).size());
    }

    @Test
    public void shouldConvertFieldNameToInitials() {
        assertEquals("bW", AttributeCompressionStrategy.getInitials("badgerWoodland"));
        assertEquals("bWO", AttributeCompressionStrategy.getInitials("badgerWOodland"));
        assertEquals("bWOL", AttributeCompressionStrategy.getInitials("badgerWOodLand"));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotPerformWithNullData() throws TokenStrategyFailedException {
        compression.perform(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotReverseWithNullData() throws TokenStrategyFailedException {
        compression.reverse(null);
    }

    @Test
    public void shouldOnlyApplyToJSONlikeText() throws TokenStrategyFailedException, UnsupportedEncodingException {
        byte[] bytes = getBytes("badger");
        assertThat(compression.perform(bytes)).isEqualTo(bytes);
    }

    @Test
    public void shouldCompressBlobContents() throws TokenStrategyFailedException, UnsupportedEncodingException {
        byte[] first = getBytes("{\"sessionID\":badger}");
        assertThat(compression.perform(first).length).isLessThan(first.length);
    }

    @Test
    public void shouldBeSymmetrical() throws TokenStrategyFailedException, UnsupportedEncodingException {
        byte[] blob = getBytes("{\"sessionID\":badger}");
        assertThat(compression.reverse(compression.perform(blob))).isEqualTo(blob);
    }

    private byte[] getBytes(String s) {
        try {
            return blobUtils.fromUTF8(s);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Test Class which is examined via Reflection.
     */
    public static class StaticFieldTest {
        public static String field;
    }

    /**
     * Test Class which is examined via Reflection.
     */
    public static class PrivateFieldTest {
        private String field;
    }
}
