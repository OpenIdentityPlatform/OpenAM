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

import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.OAuthTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import org.forgerock.json.fluent.JsonValue;

import java.util.Map;

/**
 * OAuth TokenAdapter provides conversion to and from OAuth JsonValue tokens.
 *
 * Note: The JsonValue in particular is unable to parse its own output. Therefore
 * it is less suitable as a container for JSON than one would like. Instead the
 * serialisation is based on the object that is contained within the JSON value.
 *
 * Note: The object contained within the JsonValue must be a map. This seems to be
 * the implication of the way it was being used previously by the OAuth sections
 * of the CTS.
 *
 * @author robert.wapshott@forgerock.com
 */
public class OAuthAdapter implements TokenAdapter<JsonValue> {

    /**
     * Keyword used to store all OAuth specific values within the JsonValue map.
     */
    public static final String VALUE = "value";

    private final TokenIdFactory tokenIdFactory;
    private final JSONSerialisation serialisation;

    /**
     * Default constructor with dependencies exposed.
     *
     * @param tokenIdFactory Non null.
     * @param serialisation
     */
    public OAuthAdapter(TokenIdFactory tokenIdFactory, JSONSerialisation serialisation) {
        this.tokenIdFactory = tokenIdFactory;
        this.serialisation = serialisation;
    }

    /**
     * Covert to Token.
     *
     * The TokenIdFactory is responsible for resolving the primary Id of the Token.
     *
     * Note: OAuth tokens don't have an expiry or user concepts.
     *
     * @param request Non null.
     *
     * @return Non null Token.
     *
     * @throws IllegalArgumentException If the object wrapped inside the JsonValue
     * was not an instance of a Map.
     */
    public Token toToken(JsonValue request) {
        assertObjectIsAMap(request);

        String id = tokenIdFactory.toOAuthTokenId(request);
        Token token = new Token(id, TokenType.OAUTH);

        // For each OAuth attribute, assign it to the token.
        Map<String,Object> values = request.get(VALUE).asMap();
        if (values != null) {
            for (OAuthTokenField field : OAuthTokenField.values()) {
                String key = field.getOAuthField();
                if (values.containsKey(key)) {
                    Object value = values.get(key);
                    token.setAttribute(field.getField(), value);
                }
            }
        }

        /**
         * Binary Data
         * The JsonValue class is unable to parse its own output, therefore we need
         * a suitable mechanism to work around this. In this case we will serialise
         * the object contained within the JsonValue which we know to be a map.
         */
        Object objectToStore = request.getObject();
        String serialisedObject = serialisation.serialise(objectToStore);
        token.setBlob(serialisedObject.getBytes());

        return token;
    }

    /**
     * Convert from a Token using the serialised JSON blob to generate the JsonValue.
     *
     * @param token Token to be converted back to its original format.
     * @return Non null JsonValue.
     * @throws IllegalArgumentException If the object wrapped inside the Token
     * was not an instance of a Map.
     */
    public JsonValue fromToken(Token token) {
        String objectToDeserialise = new String(token.getBlob());

        JsonValue r;
        try {
            Object obj = serialisation.deserialise(objectToDeserialise, Map.class);
            r = new JsonValue(obj);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "The CTS usage of the JsonValue depends on the value being of " +
                    "type Map.");
        }

        return r;
    }

    /**
     * Verify that the object in the JsonValue is actually a map.
     * @param value Non null.
     * @throws IllegalArgumentException If this is not the case.
     */
    private void assertObjectIsAMap(JsonValue value) {
        if (Map.class.isAssignableFrom(value.getObject().getClass())) {
            return;
        }
        throw new IllegalArgumentException(
                "Only Map instances are permitted in the OAuth token.");
    }
}
