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

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import org.apache.commons.lang.ArrayUtils;
import org.mockito.BDDMockito;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenEncryptionTest {
    @Test
    public void shouldDecryptEncryptedToken() {
        // Given
        Token token = new Token("badger", TokenType.OAUTH);
        byte[] original = "blobbyblobbyblob".getBytes();
        byte[] copy = ArrayUtils.clone(original);
        token.setBlob(copy);

        TokenEncryption encryption = new TokenEncryption(BDDMockito.mock(Debug.class));

        // When / Then
        token = encryption.encrypt(token);
        assertNotSame(token.getBlob(), original);
        token = encryption.decrypt(token);
        assertEquals(token.getBlob(), original);
    }
}
