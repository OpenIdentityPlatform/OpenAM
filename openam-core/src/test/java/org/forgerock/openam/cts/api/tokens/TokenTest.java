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
package org.forgerock.openam.cts.api.tokens;

import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenTest {
    @Test
    public void shouldStoreString() {
        // Given
        CoreTokenField key = CoreTokenField.STRING_ONE;
        String value = "Badger";
        Token token = new Token("", TokenType.SESSION);
        // When
        token.setAttribute(key, value);
        // Then
        assertEquals(value, token.getValue(key));
    }

    @Test
    public void shouldStoreDate() {
        // Given
        Calendar now = Calendar.getInstance();
        CoreTokenField key = CoreTokenField.EXPIRY_DATE;
        Token token = new Token("", TokenType.SESSION);
        // When
        token.setAttribute(key, now);
        // Then
        Calendar result = token.getValue(key);
        assertEquals(now.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void shouldStoreInteger() {
        // Given
        Integer value = new Integer(12345);
        CoreTokenField key = CoreTokenField.INTEGER_EIGHT;
        Token token = new Token("", TokenType.SESSION);
        // When
        token.setAttribute(key, value);
        // Then
        assertEquals(value, token.getValue(key));
    }

    @Test
    public void shouldStoreByteData() {
        // Given
        byte[] data = "Badger".getBytes();
        CoreTokenField key = CoreTokenField.BLOB;
        Token token = new Token("", TokenType.SESSION);
        // When
        token.setAttribute(key, data);
        // Then
        assertArrayEquals(data, token.<byte[]>getValue(key));
    }

    @Test
    public void shouldReturnAttributeNamesOnlyForSetAttributes() {
        // Given
        Token token = new Token("id", TokenType.SESSION);

        // When
        Collection<CoreTokenField> fields = token.getAttributeNames();

        // Then
        assertEquals(fields.size(), 2);
    }

    @Test
    public void shouldReturnNotReturnAttributesForUnsetAttributes() {
        // Given
        Token token = new Token("ID", TokenType.SESSION);
        CoreTokenField field = CoreTokenField.STRING_ONE;

        // Set and clear an attribute
        token.setAttribute(field, "badger");
        token.clearAttribute(field);

        // When
        Collection<CoreTokenField> fields = token.getAttributeNames();

        // Then
        assertEquals(fields.size(), 2);
    }

    @Test
    public void shouldCopyToken() {
        // Given
        Token token = new Token("badger", TokenType.SAML2);
        token.setAttribute(CoreTokenField.INTEGER_ONE, 1234);
        token.setAttribute(CoreTokenField.STRING_ONE, "Weasel");
        token.setAttribute(CoreTokenField.DATE_ONE, Calendar.getInstance());

        // When
        Token result = new Token(token);

        // Then
        TokenTestUtils.compareTokens(result, token);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldRespectReadOnlyField() {
        // Given
        CoreTokenField key = CoreTokenField.TOKEN_ID;
        assertEquals(true, Token.isFieldReadOnly(key));

        Token token = new Token("", TokenType.SESSION);
        // When/Then
        token.setAttribute(key, "");
    }
}
