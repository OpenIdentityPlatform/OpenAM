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
package com.sun.identity.sm.ldap.api.fields;

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
 * @see SAMLTokenField
 * @see OAuthTokenField
 *
 * @author robert.wapshott@forgerock.com
 */
public enum CoreTokenField {
    USER_ID("coreTokenUserId"),
    TOKEN_TYPE("coreTokenType"),
    TOKEN_ID("coreTokenId"),
    EXPIRY_DATE("coreTokenExpirationDate"),
    BLOB("coreTokenObject"),

    // Generic indexed String fields
    STRING_ONE("coreTokenString01"),
    STRING_TWO("coreTokenString02"),
    STRING_THREE("coreTokenString03"),
    STRING_FOUR("coreTokenString04"),
    STRING_FIVE("coreTokenString05"),
    STRING_SIX("coreTokenString06"),
    STRING_SEVEN("coreTokenString07"),
    STRING_EIGHT("coreTokenString08"),
    STRING_NINE("coreTokenString09"),
    STRING_TEN("coreTokenString10"),
    STRING_ELEVEN("coreTokenString11"),
    STRING_TWELVE("coreTokenString12"),
    STRING_THIRTEEN("coreTokenString13"),
    STRING_FOURTEEN("coreTokenString14"),
    STRING_FIFTEEN("coreTokenString15"),

    // Generic indexed Integer fields
    INTEGER_ONE("coreTokenInteger01"),
    INTEGER_TWO("coreTokenInteger02"),
    INTEGER_THREE("coreTokenInteger03"),
    INTEGER_FOUR("coreTokenInteger04"),
    INTEGER_FIVE("coreTokenInteger05"),
    INTEGER_SIX("coreTokenInteger06"),
    INTEGER_SEVEN("coreTokenInteger07"),
    INTEGER_EIGHT("coreTokenInteger08"),
    INTEGER_NINE("coreTokenInteger09"),
    INTEGER_TEN("coreTokenInteger10"),

    // Generic indexed Date fields
    DATE_ONE("coreTokenDate01"),
    DATE_TWO("coreTokenDate02"),
    DATE_THREE("coreTokenDate03"),
    DATE_FOUR("coreTokenDate04"),
    DATE_FIVE("coreTokenDate05");

    private final String ldapAttribute;

    /**
     * @param ldapAttribute The name of the field that this CoreTokenField is associated to.
     */
    private CoreTokenField(String ldapAttribute) {
        this.ldapAttribute = ldapAttribute;
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
