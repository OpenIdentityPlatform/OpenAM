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
package com.sun.identity.sm.ldap.api.tokens;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.fields.CoreTokenFieldTypes;
import com.sun.identity.sm.ldap.exceptions.OperationFailedException;
import edu.emory.mathcs.backport.java.util.Collections;
import org.forgerock.opendj.ldap.GeneralizedTime;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple domain value responsible for modelling a Core Token Service Token. This container is intended
 * to be generic and has little knowledge of the data it is storing.
 *
 * The Token is backed by the CoreTokenField enumeration which contains the known fields that can be
 * written to. Every value being stored must be mapped to one of these CoreTokenFields.
 *
 * There are some read only fields which can only be set during initialisation of the Token.
 *
 * The Token models the data stored in two ways. Firstly the primary fields of the Token are accessible
 * via getters and setters. All other fields are accessed in a more generic way.
 *
 * @author robert.wapshott@forgerock.com
 */
public class Token {

    /**
     * Note: This map stores all data for the Token. It is intentionally using a String to Object mapping
     * rather than a CoreTokenField based key because this works better with Jackson based JSON
     * serialisation.
     */
    private Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    /**
     * Private constructor performs some initialisation to ensure the main fields appear
     * in a consistent manner when performing a toString on this Token.
     */
    private Token() {
        put(CoreTokenField.TOKEN_ID, null);
        put(CoreTokenField.TOKEN_TYPE, null);
        put(CoreTokenField.USER_ID, null);
        put(CoreTokenField.EXPIRY_DATE, null);
        put(CoreTokenField.BLOB, null);
    }

    /**
     * Create an instance of the Token.
     *
     * @param tokenId Required field which cannot be null.
     * @param type Required to define the type of Token.
     */
    public Token(String tokenId, TokenType type) {
        this();

        if (tokenId == null || type == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }

        put(CoreTokenField.TOKEN_ID, tokenId);
        put(CoreTokenField.TOKEN_TYPE, type);
    }

    /**
     * Copy constructor will create a shallow-copy of the provided Token.
     *
     * @param copy Non null Token to copy.
     */
    public Token(Token copy) {
        for (CoreTokenField field : copy.getAttributeNames()) {
            Object value = copy.get(field);
            put(field, value);
        }
    }

    /**
     * @return The unique id for this Token.
     */
    public String getTokenId() {
        return (String) get(CoreTokenField.TOKEN_ID);
    }

    /**
     * @return The TokenType of the Token.
     */
    public TokenType getType() {
        return (TokenType) get(CoreTokenField.TOKEN_TYPE);
    }

    /**
     * @return The unique id for the user.
     */
    public String getUserId() {
        return (String) get(CoreTokenField.USER_ID);
    }

    /**
     * @param userId The unique id for the user.
     */
    public void setUserId(String userId) {
        put(CoreTokenField.USER_ID, userId);
    }

    /**
     * @return The timestamp (with timezone information) when the Token is due to expire.
     */
    public Calendar getExpiryTimestamp() {
        return (Calendar) get(CoreTokenField.EXPIRY_DATE);
    }

    /**
     * @param expiryDate Assign the timestamp of when the Token will expire.
     */
    public void setExpiryTimestamp(Calendar expiryDate) {
        put(CoreTokenField.EXPIRY_DATE, expiryDate);
    }

    /**
     * A binary representation of the Token being stored can be placed in the Token.
     *
     * This ensures that attributes that are not modelled at the Token level can still be
     * persisted.
     *
     * @return The the serialised binary object for this Token.
     */
    public byte[] getBlob() {
        return (byte[]) get(CoreTokenField.BLOB);
    }

    /**
     * @param data Assign the binary data that represents the object being stored in the Token.
     */
    public void setBlob(byte[] data) {
        put(CoreTokenField.BLOB, data);
    }

    /**
     * The Token supports being accessed in a generic way allowing a caller to iterate
     * over all fields that are assigned in this token.
     *
     * @return A non null, non modifiable list of the CoreTokenField fields currently assigned.
     */
    public Collection<CoreTokenField> getAttributeNames() {
        Set<CoreTokenField> fields = new HashSet<CoreTokenField>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            CoreTokenField field = CoreTokenField.fromLDAPAttribute(entry.getKey());
            if (entry.getValue() == null) {
                continue;
            }
            fields.add(field);
        }
        return Collections.unmodifiableSet(fields);
    }

    /**
     * Accessor for the non-primary CoreTokenField attributes.
     *
     * @param field The CoreTokenField to request the value for.
     * @return The value assigned which may be null.
     */
    public <T> T getValue(CoreTokenField field) {
        return (T) get(field);
    }

    /**
     * Mutator for the non-primary fields of the Token.
     *
     * @param field The CoreTokenField field to store the value against.
     * @param value The possibly null value to store in this Token.
     */
    public <T> void setAttribute(CoreTokenField field, T value) {
        if (isFieldReadOnly(field)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Token Field {0} is read only and cannot be set.",
                    field.toString()));
        }

        // Validate that the type matches the field they are assigning.
        try {
            CoreTokenFieldTypes.validateType(field, value);
        } catch (OperationFailedException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        put(field, value);
    }

    /**
     * Clear a set attribute.
     *
     * Regardless of whether the attribute was set, it will be cleared as a result of this call.
     *
     * @param field Non null field to clear.
     */
    public void clearAttribute(CoreTokenField field) {
        remove(field);
    }

    /**
     * Trying to change a read-only field will trigger a runtime exception.
     *
     * @param field The CoreTokenField to check.
     * @return True if the field is read only, and cannot be changed once assigned.
     */
    public static boolean isFieldReadOnly(CoreTokenField field) {
        if (field.equals(CoreTokenField.TOKEN_TYPE)) {
            return true;
        }
        if (field.equals(CoreTokenField.TOKEN_ID)) {
            return true;
        }
        return false;
    }

    /**
     * Store a value in the map.
     *
     * Note: We are performing conversions based on the field because it has been found that
     * the Jackson based JSON encoding doesn't handle all data types equally well.
     *
     * @param field Non null
     * @param value May be null
     */
    private void put(CoreTokenField field, Object value) {
        String s;
        if (value == null) {
            s = null;
        } else if (CoreTokenField.TOKEN_TYPE.equals(field)) {
            s = ((TokenType)value).name();
        } else if (CoreTokenFieldTypes.isCalendar(field)) {
            s = GeneralizedTime.valueOf((Calendar) value).toString();
        } else if (CoreTokenFieldTypes.isByteArray(field)) {
            s = Base64.encode((byte[]) value);
        } else if (CoreTokenFieldTypes.isInteger(field)) {
            s = Integer.toString((Integer) value);
        } else {
            s = value.toString();
        }
        attributes.put(field.toString(), s);
    }

    /**
     * Get a value in the map.
     *
     * Note: We are performing conversions based on the field because it has been found that
     * the Jackson based JSON encoding doesn't handle all data types equally well.
     *
     * @param field Non null
     * @return May be null
     */
    private Object get(CoreTokenField field) {
        String s = (String) attributes.get(field.toString());
        if (s == null) {
            return null;
        } else if (CoreTokenField.TOKEN_TYPE.equals(field)) {
            return TokenType.valueOf(s);
        } else if (CoreTokenFieldTypes.isCalendar(field)) {
            return GeneralizedTime.valueOf(s).toCalendar();
        } else if (CoreTokenFieldTypes.isByteArray(field)) {
            return Base64.decode(s);
        } else if (CoreTokenFieldTypes.isInteger(field)) {
            return Integer.parseInt(s);
        } else {
            return s;
        }
    }

    private void remove(CoreTokenField field) {
        attributes.remove(field.toString());
    }

    /**
     * Returns a formatted version fo the Token which is intended to be human readable.
     * Some of the field values have been formatted to ensure that their contents are more
     * meaningful.
     *
     * Note: This is not a machine readable/parsable format. This function is only intended
     * for debugging use only as its conversion logic will not be performant.
     *
     * @return Non null string which describes the Token.
     */
    @Override
    public String toString() {
        String r = this.getClass().getName() + "\n";
        String format = "\t{0} = {1}\n";

        for (CoreTokenField field : getAttributeNames()) {
            Object value;
            if (field.equals(CoreTokenField.EXPIRY_DATE)) {
                DateFormat dateFormat = DateFormat.getInstance();
                Date date = getExpiryTimestamp().getTime();
                value = dateFormat.format(date);
            } else if (field.equals(CoreTokenField.BLOB)) {
                value = Long.toString(getBlob().length) + " bytes";
            } else {
                value = getValue(field);
            }

            if (value == null) {
                value = "[null]";
            }

            r += MessageFormat.format(format, field.toString(), value);
        }

        return r;
    }
}
