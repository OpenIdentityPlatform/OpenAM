/**
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.cts.utils.blob.strategies;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.openam.cts.utils.blob.strategies.AttributeCompressionStrategy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author robert.wapshott@forgerock.com
 */
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

    @Test
    public void shouldNotUpdateNonSessionTokens() throws TokenStrategyFailedException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockToken.getType()).willReturn(TokenType.OAUTH);

        // When
        compression.perform(mockToken);

        // Then
        verify(mockToken, times(0)).setBlob(any(byte[].class));
    }

    @Test
    public void shouldNotUpdateWithNullTokenBlob() throws TokenStrategyFailedException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockToken.getType()).willReturn(TokenType.SESSION);
        given(mockToken.getBlob()).willReturn(null);

        // When
        compression.perform(mockToken);

        // Then
        verify(mockToken, times(0)).setBlob(any(byte[].class));
    }

    @Test
    public void shouldLookForCurlyBracketsInJSONBlob() throws TokenStrategyFailedException, UnsupportedEncodingException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockToken.getType()).willReturn(TokenType.SESSION);
        given(mockToken.getBlob()).willReturn("badger".getBytes(TokenBlobUtils.ENCODING));

        // When
        compression.perform(mockToken);

        // Then
        verify(mockToken, atLeastOnce()).getBlob();
        verify(mockToken, times(0)).setBlob(any(byte[].class));
    }

    @Test
    public void shouldUpdateBlobContents() throws TokenStrategyFailedException, UnsupportedEncodingException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockToken.getType()).willReturn(TokenType.SESSION);
        given(mockToken.getBlob()).willReturn("{\"sessionID\":badger}".getBytes(TokenBlobUtils.ENCODING));

        // When
        compression.perform(mockToken);

        // Then
        verify(mockToken).setBlob(any(byte[].class));
    }

    @Test
    public void shouldCompressSessionBlob() throws TokenStrategyFailedException, UnsupportedEncodingException {
        // Given
        Token token = new Token("Badger", TokenType.SESSION);
        String blob = "{\"sessionID\":badger}";
        token.setBlob(blob.getBytes(TokenBlobUtils.ENCODING));

        // When
        compression.perform(token);

        // Then
        int previous = blob.getBytes().length;
        int updated = token.getBlob().length;
        assertTrue(updated < previous);
    }

    @Test
    public void shouldBeSymmetrical() throws TokenStrategyFailedException, UnsupportedEncodingException {
        // Given
        Token token = new Token("Badger", TokenType.SESSION);
        String blob = "{\"sessionID\":badger}";
        token.setBlob(blob.getBytes(TokenBlobUtils.ENCODING));

        // When
        compression.perform(token);
        compression.reverse(token);

        // Then
        String currentBlob = new String(token.getBlob());
        assertEquals(blob, currentBlob);
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
