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
package org.forgerock.openam.cts.utils;

import javax.inject.Inject;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responsible for the conversion to and from LDAP Entry and Token.
 *
 * This class handles all the detail around this process and the appropriate class
 * casting required.
 *
 * Note: This class manages the objectClass of an LDAP Entry as part of its operations.
 *
 * @see CoreTokenConstants#OBJECT_CLASS
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenAttributeConversion {
    /**
     * Empty Strings cannot be stored in LDAP, so they are replaced by a keyword which
     * will be handled accordingly.
     */
    static final String EMPTY = "-empty-";

    // Injected
    private final LDAPConfig constants;
    private final LDAPDataConversion conversion;

    @Inject
    public TokenAttributeConversion(LDAPConfig constants, LDAPDataConversion conversion) {
        this.constants = constants;
        this.conversion = conversion;
    }

    /**
     * Generate an Entry based on the given Token.
     *
     * @param token Non null Token to base the Entry on.
     *
     * @return An Entry suitable for LDAP operations. Includes the Object Class.
     */
    public Entry getEntry(Token token) {
        Entry entry = new LinkedHashMapEntry(generateTokenDN(token));
        addObjectClass(entry);

        for (CoreTokenField field : token.getAttributeNames()) {

            String key = field.toString();

            // Token Type special case is an Enum
            if (CoreTokenField.TOKEN_TYPE.equals(field)) {
                TokenType type = token.getValue(field);
                entry.addAttribute(key, type.name());
                continue;
            }

            if (CoreTokenFieldTypes.isCalendar(field)) {
                Calendar calendar = token.getValue(field);
                String dateString = conversion.toLDAPDate(calendar);
                entry.addAttribute(key, dateString);
            } else if (CoreTokenFieldTypes.isByteArray(field)) {
                byte[] array = token.getValue(field);
                entry.addAttribute(key, ByteString.valueOf(array));
            } else if (CoreTokenFieldTypes.isInteger(field)) {
                Integer value = token.getValue(field);
                entry.addAttribute(key, ByteString.valueOf(value));
            } else if (CoreTokenFieldTypes.isString(field)) {
                String value = token.getValue(field);
                if (value.isEmpty()) {
                    value = EMPTY;
                }
                entry.addAttribute(key, ByteString.valueOf(value));
            } else {
                throw new IllegalStateException();
            }
        }
        return entry;
    }

    /**
     * Convert an Entry into a more convenient Mapping of CoreTokenField to Object.
     *
     * This function is important because no every operation with LDAP needs to return a
     * fully initialised Token. Instead users may be interested in only certain
     * attributes of the Token, and choose to query just those as a performance enhancement.
     *
     * @param entry Non null entry to convert.
     *
     * @return A mapping of zero or more CoreTokenFields to Objects.
     */
    public Map<CoreTokenField, Object> mapFromEntry(Entry entry) {
        stripObjectClass(entry);

        Map<CoreTokenField, Object> r = new LinkedHashMap<CoreTokenField, Object>();

        for (Attribute a : entry.getAllAttributes()) {
            AttributeDescription description = a.getAttributeDescription();
            CoreTokenField field = CoreTokenField.fromLDAPAttribute(description.toString());

            // Special case for Token Type
            if (CoreTokenField.TOKEN_TYPE.equals(field)) {
                String value = entry.parseAttribute(description).asString();
                r.put(field, TokenType.valueOf(value));
                continue;
            }

            if (CoreTokenFieldTypes.isCalendar(field)) {
                String dateString = entry.parseAttribute(description).asString();
                Calendar calendar = conversion.fromLDAPDate(dateString);
                r.put(field, calendar);
            } else if (CoreTokenFieldTypes.isString(field)) {
                String value = entry.parseAttribute(description).asString();
                if (EMPTY.equals(value)) {
                    value = "";
                }
                r.put(field, value);
            } else if (CoreTokenFieldTypes.isInteger(field)) {
                Integer value = entry.parseAttribute(description).asInteger();
                r.put(field, value);
            } else if (CoreTokenFieldTypes.isByteArray(field)) {
                byte[] data = entry.parseAttribute(description).asByteString().toByteArray();
                r.put(field, data);
            } else {
                throw new IllegalStateException();
            }
        }

        return r;
    }

    /**
     * Convert an Entry into a Token.
     *
     * The implication of function is that the Entry contains all the attributes of the
     * Token. If any required attributes are missing, this operation will fail.
     *
     * @param entry A non null Entry.
     *
     * @return A non null Token initialised with the contents of the Entry.
     *
     * @see #mapFromEntry(org.forgerock.opendj.ldap.Entry)
     */
    public Token tokenFromEntry(Entry entry) {
        Map<CoreTokenField, Object> map = mapFromEntry(entry);

        String tokenId = (String) map.get(CoreTokenField.TOKEN_ID);
        TokenType type = (TokenType) map.get(CoreTokenField.TOKEN_TYPE);

        Token token = new Token(tokenId, type);

        for (Map.Entry<CoreTokenField, Object> e : map.entrySet()) {
            CoreTokenField key = e.getKey();
            Object value = e.getValue();

            if (Token.isFieldReadOnly(key)) {
                continue;
            }

            token.setAttribute(key, value);
        }
        return token;
    }

    /**
     * Only adds the ObjectClass if it hasn't already been added.
     *
     * @param entry Adds the ObjectClass attribute to this Entry.
     *
     * @return The updated Entry.
     */
    public static Entry addObjectClass(Entry entry) {
        Attribute attribute = entry.getAttribute(CoreTokenConstants.OBJECT_CLASS);
        if (attribute == null) {
            entry.addAttribute(CoreTokenConstants.OBJECT_CLASS, CoreTokenConstants.FR_CORE_TOKEN);
        }
        return entry;
    }

    /**
     * Only strips out the ObjectClass if it is present.
     *
     * @param entry Non null Entry to process.
     *
     * @return The Entry reference passed in.
     */
    public static Entry stripObjectClass(Entry entry) {
        Attribute attribute = entry.getAttribute(CoreTokenConstants.OBJECT_CLASS);
        if (attribute != null) {
            AttributeDescription description = attribute.getAttributeDescription();
            entry.removeAttribute(description);
        }
        return entry;
    }

    /**
     * @param token Token which has a TokenId to base the DN on.
     * @return Non null DN.
     */
    public DN generateTokenDN(Token token) {
        return generateTokenDN(token.getTokenId());
    }

    /**
     * @param tokenId Token Id to base the DN on.
     * @return Non null DN.
     */
    public DN generateTokenDN(String tokenId) {
        DN rootDN = constants.getTokenStoreRootSuffix();
        return rootDN.child(CoreTokenField.TOKEN_ID.toString(), tokenId);
    }
}
