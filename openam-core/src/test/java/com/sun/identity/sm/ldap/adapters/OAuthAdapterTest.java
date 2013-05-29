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
import com.sun.identity.sm.ldap.api.fields.OAuthTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.KeyConversion;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class OAuthAdapterTest {
    @Test
    public void shouldSerialiseAString() {
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        String badger = "badger";
        String serialised = serialisation.serialise(badger);
        String result = serialisation.deserialise(serialised, String.class);
        assertEquals(result, badger);
    }

    @Test
    public void shouldSerialiseAndDeserialiseASimpleString() {
        // Given
        String id = "badger";

        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        KeyConversion keyConversion = new KeyConversion();
        OAuthAdapter adapter = new OAuthAdapter(new TokenIdFactory(keyConversion), serialisation);

        // Populate the map with fields we know are queried during this conversion.
        Map<String, String> blob = new HashMap<String, String>();
        blob.put(OAuthTokenField.ID.getOAuthField(), id);
        String serialisedObject = serialisation.serialise(blob);

        Token token = new Token(id, TokenType.OAUTH);
        // Set the binary data fields
        token.setBlob(serialisedObject.getBytes());

        // When
        Token result = adapter.toToken(adapter.fromToken(token));

        // Then
        TokenTestUtils.compareTokens(result, token);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldVerifyThatObjectIsAMap() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        KeyConversion keyConversion = new KeyConversion();
        OAuthAdapter adapter = new OAuthAdapter(new TokenIdFactory(keyConversion), serialisation);

        JsonValue mockValue = mock(JsonValue.class);
        given(mockValue.getObject()).willReturn("badger");

        // When/Then
        adapter.toToken(mockValue);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotDeserialiseATokenWhichDoesntContainAMap() {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        KeyConversion keyConversion = new KeyConversion();
        OAuthAdapter adapter = new OAuthAdapter(new TokenIdFactory(keyConversion), serialisation);

        Token token = new Token("", TokenType.OAUTH);
        token.setBlob(serialisation.serialise("badger").getBytes());

        // When/Then
        adapter.fromToken(token);
    }

    @Test
    public void shouldExtractOAuthValuesFromJsonValue() {
        // Given
        String id = "badger";

        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        KeyConversion keyConversion = new KeyConversion();
        OAuthAdapter adapter = new OAuthAdapter(new TokenIdFactory(keyConversion), serialisation);

        Map<String, Object> blob = new HashMap<String, Object>();
        blob.put(OAuthTokenField.ID.getOAuthField(), id);

        Map<String, String> values = new HashMap<String, String>();
        values.put(OAuthTokenField.PARENT.getOAuthField(), "Badgers parent");
        values.put(OAuthTokenField.SCOPE.getOAuthField(), "Badgers scope");
        blob.put(OAuthAdapter.VALUE, values);

        String serialisedObject = serialisation.serialise(blob);

        Token token = new Token(id, TokenType.OAUTH);
        // Set the binary data fields
        token.setBlob(serialisedObject.getBytes());

        // When
        JsonValue value = adapter.fromToken(token);

        // Then
        Map<String, Object> resultMap = value.get(OAuthAdapter.VALUE).asMap();
        assertEquals(resultMap.size(), 2);
        List<String> fields = Arrays.asList(new String[]{
                OAuthTokenField.PARENT.getOAuthField(),
                OAuthTokenField.SCOPE.getOAuthField()});

        for (String field : fields) {
            assertEquals(resultMap.get(field), values.get(field));
        }
    }
}
