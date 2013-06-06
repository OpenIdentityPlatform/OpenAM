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

import com.google.inject.Inject;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.OAuthTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import org.forgerock.json.fluent.JsonValue;

import java.util.Collection;
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
    // Injected
    private final TokenIdFactory tokenIdFactory;
    private final JSONSerialisation serialisation;
    private final OAuthValues oAuthValues;

    /**
     * Keyword used to store all OAuth specific oAuthValues within the JsonValue map.
     */
    public static final String VALUE = "value";

    /**
     * Default constructor with dependencies exposed.
     *
     * @param tokenIdFactory Non null.
     * @param serialisation
     * @param oAuthValues
     */
    @Inject
    public OAuthAdapter(TokenIdFactory tokenIdFactory, JSONSerialisation serialisation, OAuthValues oAuthValues) {
        this.tokenIdFactory = tokenIdFactory;
        this.serialisation = serialisation;
        this.oAuthValues = oAuthValues;
    }

    /**
     * Convert a JsonValue to a Token.
     *
     * The conversion assumes that the JsonValue contains a map which has an attribute called
     * 'value' which contains the OAuth Token values.
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
                    /**
                     * OAuthTokenField aware conversions.
                     *
                     * - Skip the ID as it is extracted by the TokenIdFactory.
                     * - Dates are formatted as milliseconds from epoch, and stored in Collections.
                     * - All other fields are stored in Collections which can be empty.
                     * - (just in case) If a field is not in a collection, assume it is the right type.
                     */
                    if (OAuthTokenField.ID.getOAuthField().equals(key)) {
                        continue;
                    }
                    if (OAuthTokenField.EXPIRY_TIME.getOAuthField().equals(key)) {
                        if (!Collection.class.isAssignableFrom(value.getClass())) {
                            throw new IllegalStateException("Date must be in a collection");
                        }
                        value = oAuthValues.getDateValue((Collection<String>) value);
                    } else if (value instanceof Collection) {
                        value = oAuthValues.getSingleValue((Collection<String>) value);
                    }
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
        String data = new String(token.getBlob());
        
        JsonValue r;
        try {
            r = new JsonValue(serialisation.deserialise(data, Map.class));
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
