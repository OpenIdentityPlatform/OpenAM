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
package org.forgerock.openam.cts.adapters;

import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.SAMLTokenField;
import org.forgerock.openam.cts.api.tokens.SAMLToken;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.KeyConversion;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SAMLAdapterTest {

    private JSONSerialisation serialisation;
    private LDAPDataConversion dataConversion;
    private SAMLAdapter adapter;
    private TokenIdFactory tokenIdFactory;
    private TokenBlobUtils blobUtils;
    private KeyConversion encoding;

    @BeforeMethod
    public void setup() {
        serialisation = mock(JSONSerialisation.class);
        dataConversion = mock(LDAPDataConversion.class);
        tokenIdFactory = mock(TokenIdFactory.class);
        blobUtils = mock(TokenBlobUtils.class);
        encoding = mock(KeyConversion.class);

        adapter = new SAMLAdapter(tokenIdFactory, serialisation, dataConversion, blobUtils);
    }

    @Test
    public void shouldSerialiseAndDeserialiseToken() {
        // Given
        // Need real delegates for this test.
        serialisation = new JSONSerialisation();
        adapter = new SAMLAdapter(
                new TokenIdFactory(encoding),
                new JSONSerialisation(),
                new LDAPDataConversion(),
                new TokenBlobUtils());

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
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void shouldNotStoreSecondaryKeyIfNull() {
        // Given
        SAMLToken samlToken = new SAMLToken("primary", null, 12345, "");

        given(tokenIdFactory.toSAMLPrimaryTokenId(anyString())).willReturn("id");
        given(serialisation.serialise(anyObject())).willReturn("");

        // When
        Token token = adapter.toToken(samlToken);

        // Then
        assertThat(token.getValue(SAMLTokenField.SECONDARY_KEY.getField())).isNull();
    }
}
