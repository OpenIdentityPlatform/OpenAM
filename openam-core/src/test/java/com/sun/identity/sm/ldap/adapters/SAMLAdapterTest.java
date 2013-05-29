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
package com.sun.identity.sm.ldap.adapters;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.TokenTestUtils;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.SAMLTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.KeyConversion;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.eq;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SAMLAdapterTest {
    @Test
    public void shouldSerialiseAndDeserialiseToken() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        KeyConversion encoding = mock(KeyConversion.class);

        SAMLAdapter adapter = new SAMLAdapter(
                new TokenIdFactory(encoding),
                serialisation,
                dataConversion);

        String tokenId = "badger";
        Token token = new Token(tokenId, TokenType.SAML2);

        // SAML tokens only store time to seconds resolution
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        token.setExpiryTimestamp(now);

        // SAML implementation detail around stored object
        String blob = "woodland forrest";
        token.setBlob(serialisation.serialise(blob).getBytes());
        token.setAttribute(SAMLTokenField.OBJECT_CLASS.getField(), String.class.getName());

        // SAML mocking for primary key
        given(encoding.encodeKey(eq(tokenId))).willReturn(tokenId);

        // SAML detail for secondary key
        String secondaryKey = "weasel";
        token.setAttribute(SAMLTokenField.SECONDARY_KEY.getField(), secondaryKey);
        given(encoding.encodeKey(eq(secondaryKey))).willReturn(secondaryKey);

        // When
        Token result = adapter.toToken(adapter.fromToken(token));

        // Then
        TokenTestUtils.compareTokens(result, token);
    }
}
