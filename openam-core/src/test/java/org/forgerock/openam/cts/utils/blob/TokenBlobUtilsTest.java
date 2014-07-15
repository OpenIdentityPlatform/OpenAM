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
package org.forgerock.openam.cts.utils.blob;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

public class TokenBlobUtilsTest {

    private TokenBlobUtils utils;
    private Token mockToken;

    @BeforeMethod
    public void setup() {
        utils = new TokenBlobUtils();
        mockToken = mock(Token.class);
    }

    @Test
    public void shouldUseTokenWhenStoringStringAsBlob() throws CoreTokenException {
        utils.setBlobFromString(mockToken, "badger");
        verify(mockToken).setBlob(any(byte[].class));
    }

    @Test
    public void shouldUseTokenForBlobAsString() throws CoreTokenException, UnsupportedEncodingException {
        // Given
        given(mockToken.getBlob()).willReturn("badger".getBytes(TokenBlobUtils.ENCODING));

        // When
        utils.getBlobAsString(mockToken);

        // Then
        verify(mockToken).getBlob();
    }

    @Test
    public void shouldDecodeEncoding() throws CoreTokenException {
        // Given
        String key = "badger";
        Token token = new Token("id", TokenType.SESSION);

        // When
        utils.setBlobFromString(token, key);
        String result = utils.getBlobAsString(token);

        // Then
        assertThat(key).isEqualTo(result);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullToConvert() throws UnsupportedEncodingException {
        utils.toUTF8(null);
    }

    @Test
    public void shouldReturnNullIfTokenHasNoBinaryDataAssigned() {
        given(mockToken.getBlob()).willReturn(null);
        assertThat(utils.getBlobAsString(mockToken)).isNull();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllNullContentsFromUTF8() throws UnsupportedEncodingException {
        utils.fromUTF8(null);
    }
}
