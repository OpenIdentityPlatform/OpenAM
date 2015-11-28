/**
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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.tokens;

import java.util.Calendar;

/**
 * CoreTokenField contains a mapping from the Java enumeration and the defined
 * attributes present in the LDAP Schema for the Core Token Service.
 *
 * Note: These enumerations are backed by LDAP attributes and as such are the
 * only attributes available to populate for any Token that is to be stored.
 *
 * Other enumerations may exist for the convenience of development, however
 * they must link to these enumerated values.
 *
 * @see org.forgerock.openam.cts.api.fields.SAMLTokenField
 * @see org.forgerock.openam.cts.api.fields.OAuthTokenField
 */
public enum CoreTokenField {
    /**
     * Token User Id field name.
     */
    USER_ID("coreTokenUserId", String.class),
    /**
     * Token type field name.
     */
    TOKEN_TYPE("coreTokenType", null),
    /**
     * Token Id field name.
     */
    TOKEN_ID("coreTokenId", String.class),
    /**
     * Token expiry date field name.
     */
    EXPIRY_DATE("coreTokenExpirationDate", Calendar.class),
    /**
     * Token blob field name.
     */
    BLOB("coreTokenObject", byte[].class),

    //LDAP optional attribute, added by OpenDJ/AD
    /**
     * Token creation timestamp field.
     */
    CREATE_TIMESTAMP("createTimestamp", Calendar.class),

    // Generic indexed String fields
    /**
     * Generic token string one field name.
     */
    STRING_ONE("coreTokenString01", String.class),
    /**
     * Generic token string two field name.
     */
    STRING_TWO("coreTokenString02", String.class),
    /**
     * Generic token string three field name.
     */
    STRING_THREE("coreTokenString03", String.class),
    /**
     * Generic token string four field name.
     */
    STRING_FOUR("coreTokenString04", String.class),
    /**
     * Generic token string five field name.
     */
    STRING_FIVE("coreTokenString05", String.class),
    /**
     * Generic token string six field name.
     */
    STRING_SIX("coreTokenString06", String.class),
    /**
     * Generic token string seven field name.
     */
    STRING_SEVEN("coreTokenString07", String.class),
    /**
     * Generic token string eight field name.
     */
    STRING_EIGHT("coreTokenString08", String.class),
    /**
     * Generic token string nine field name.
     */
    STRING_NINE("coreTokenString09", String.class),
    /**
     * Generic token string ten field name.
     */
    STRING_TEN("coreTokenString10", String.class),
    /**
     * Generic token string eleven field name.
     */
    STRING_ELEVEN("coreTokenString11", String.class),
    /**
     * Generic token string twelve field name.
     */
    STRING_TWELVE("coreTokenString12", String.class),
    /**
     * Generic token string thirteen field name.
     */
    STRING_THIRTEEN("coreTokenString13", String.class),
    /**
     * Generic token string fourteen field name.
     */
    STRING_FOURTEEN("coreTokenString14", String.class),
    /**
     * Generic token string fifteen field name.
     */
    STRING_FIFTEEN("coreTokenString15", String.class),

    // Generic indexed Integer fields
    /**
     * Generic token integer one field name.
     */
    INTEGER_ONE("coreTokenInteger01", Integer.class),
    /**
     * Generic token integer two field name.
     */
    INTEGER_TWO("coreTokenInteger02", Integer.class),
    /**
     * Generic token integer three field name.
     */
    INTEGER_THREE("coreTokenInteger03", Integer.class),
    /**
     * Generic token integer four field name.
     */
    INTEGER_FOUR("coreTokenInteger04", Integer.class),
    /**
     * Generic token integer five field name.
     */
    INTEGER_FIVE("coreTokenInteger05", Integer.class),
    /**
     * Generic token integer six field name.
     */
    INTEGER_SIX("coreTokenInteger06", Integer.class),
    /**
     * Generic token integer seven field name.
     */
    INTEGER_SEVEN("coreTokenInteger07", Integer.class),
    /**
     * Generic token integer eight field name.
     */
    INTEGER_EIGHT("coreTokenInteger08", Integer.class),
    /**
     * Generic token integer nine field name.
     */
    INTEGER_NINE("coreTokenInteger09", Integer.class),
    /**
     * Generic token integer ten field name.
     */
    INTEGER_TEN("coreTokenInteger10", Integer.class),

    // Generic indexed Date fields
    /**
     * Generic token date one field name.
     */
    DATE_ONE("coreTokenDate01", Calendar.class),
    /**
     * Generic token date two field name.
     */
    DATE_TWO("coreTokenDate02", Calendar.class),
    /**
     * Generic token date three field name.
     */
    DATE_THREE("coreTokenDate03", Calendar.class),
    /**
     * Generic token date four field name.
     */
    DATE_FOUR("coreTokenDate04", Calendar.class),
    /**
     * Generic token date five field name.
     */
    DATE_FIVE("coreTokenDate05", Calendar.class);

    private final String ldapAttribute;
    private final Class<?> attributeType;

    /**
     * Constructs a CoreTokenField for the specified LDAP attribute and attribute type.
     *
     * @param ldapAttribute The name of the field that this CoreTokenField is associated to.
     */
    CoreTokenField(String ldapAttribute, Class<?> attributeType) {
        this.ldapAttribute = ldapAttribute;
        this.attributeType = attributeType;
    }

    /**
     * Gets the core token field attribute type.
     *
     * @return The attribute type.
     */
    public Class<?> getAttributeType() {
        return attributeType;
    }

    /**
     * Convert the field name into a CoreTokenField enumeration.
     *
     * This is the reverse of calling toString on this enum.
     *
     * @param value The String representation of a CoreTokenField.
     * @return Non null CoreTokenField if the String provided matches a CoreTokenField.
     * @throws IllegalArgumentException If the value provided did not match a CoreTokenField.
     */
    public static CoreTokenField fromLDAPAttribute(String value) {
        for (CoreTokenField field : values()) {
            if (field.toString().equals(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Invalid CoreTokenField value: " + value);
    }

    /**
     * Convert the enumeration into its LDAP attribute representation.
     *
     * @return The name of the LDAP Attribute.
     */
    @Override
    public String toString() {
        return ldapAttribute;
    }
}
