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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.api.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.util.Reject;

/**
 * Represents a partial CTS {@link Token}. Used to represent the result of a query
 * where only selective attributes of the Token have been requested.
 *
 * Importantly, this is not a full Token and cannot be used as such.
 *
 * The main use case of this PartialToken is an optimisation when requesting data from
 * the CTS and not all fields are required.
 *
 * @see CTSPersistentStore#attributeQuery(TokenFilter)
 * @see TokenFilter#addReturnAttribute(CoreTokenField)
 * @see Token
 */
public class PartialToken {
    private final Map<CoreTokenField, Object> entry;

    /**
     * Initialise the PartialToken with the specific fields returned from the query.
     * @param entry Non null, possibly empty collection.
     */
    public PartialToken(Map<CoreTokenField, Object> entry) {
        Reject.ifNull(entry);
        this.entry = entry;
    }

    /**
     * Copy constructor allowing the caller to modify a field.
     *
     * @param token The PartialToken to copy. Non null.
     * @param field The field to modify, non null.
     * @param value The value of the field to modify, non null.
     */
    public PartialToken(PartialToken token, CoreTokenField field, Object value) {
        Reject.ifNull(token, field, value);
        entry = new HashMap<>(token.entry);
        entry.put(field, value);
    }

    /**
     * The fields that were included in this query.
     *
     * @return A unmodifiable collection of fields.
     */
    public Collection<CoreTokenField> getFields() {
        return Collections.unmodifiableSet(entry.keySet());
    }

    /**
     * @param field The field to return.
     * @param <T> The return type, simplifies
     * @return The value that was stored or null if this PartialToken does not contain the requested field.
     */
    public <T> T getValue(CoreTokenField field) {
        if (!entry.containsKey(field)) {
            throw new NullPointerException(field.toString() + " not assigned");
        }
        return (T)entry.get(field);
    }

    /**
     * Check if this {@link PartialToken} can be converted into a {@link Token}.
     *
     * @return true if this {@code PartialToken} contains {@link CoreTokenField#TOKEN_ID} and
     *         {@link CoreTokenField#TOKEN_TYPE}.
     * @see #toToken()
     * @since 14.0.0
     */
    public boolean canConvertToToken() {
        return entry.containsKey(CoreTokenField.TOKEN_ID) && entry.containsKey(CoreTokenField.TOKEN_TYPE);
    }

    /**
     * Converts this {@code PartialToken} into a {@link Token} containing all of the populated fields.
     * <p>
     * Callers should ensure that this {@code PartialToken} can be converted by calling {@link #canConvertToToken()}
     * before calling this method.
     *
     * @return A {@code Token}.
     * @throws IllegalStateException if this {@code PartialToken} does not contain {@link CoreTokenField#TOKEN_ID} and
     *         {@link CoreTokenField#TOKEN_TYPE}.
     * @see #canConvertToToken()
     * @since 14.0.0
     */
    public Token toToken() {
        Reject.rejectStateIfTrue(!entry.containsKey(CoreTokenField.TOKEN_ID), "Token requires token ID");
        Reject.rejectStateIfTrue(!entry.containsKey(CoreTokenField.TOKEN_TYPE), "Token requires token type");

        String tokenId = getValue(CoreTokenField.TOKEN_ID);
        TokenType tokenType = getValue(CoreTokenField.TOKEN_TYPE);
        Token token = new Token(tokenId, tokenType);

        for (final Map.Entry<CoreTokenField, Object> field : entry.entrySet()) {
            if (field.getKey() == CoreTokenField.TOKEN_ID || field.getKey() == CoreTokenField.TOKEN_TYPE) {
                continue;
            }
            token.setAttribute(field.getKey(), field.getValue());
        }
        return token;
    }
}
