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
package com.sun.identity.sm.ldap.utils.blob;

import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenBlobUtilsTest {
    @Test
    public void shouldUseTokenWhenStoringStringAsBlob() throws CoreTokenException {
        // Given
        TokenBlobUtils utils = new TokenBlobUtils();
        Token mockToken = mock(Token.class);

        // When
        utils.setBlobFromString(mockToken, "badger");

        // Then
        verify(mockToken).setBlob(any(byte[].class));
    }

    @Test
    public void shouldUseTokenForBlobAsString() throws CoreTokenException, UnsupportedEncodingException {
        // Given
        TokenBlobUtils utils = new TokenBlobUtils();
        Token mockToken = mock(Token.class);
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
        TokenBlobUtils utils = new TokenBlobUtils();
        Token token = new Token("id", TokenType.SESSION);

        // When
        utils.setBlobFromString(token, key);
        String result = utils.getBlobAsString(token);

        // Then
        assertThat(key).isEqualTo(result);
    }
}
