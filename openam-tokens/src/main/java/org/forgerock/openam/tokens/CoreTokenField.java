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
    USER_ID("coreTokenUserId", String.class),
    TOKEN_TYPE("coreTokenType", null),
    TOKEN_ID("coreTokenId", String.class),
    EXPIRY_DATE("coreTokenExpirationDate", Calendar.class),
    BLOB("coreTokenObject", byte[].class),

    //LDAP optional attribute, added by OpenDJ/AD
    CREATE_TIMESTAMP("createTimestamp", Calendar.class),

    // Generic indexed String fields
    STRING_ONE("coreTokenString01", String.class),
    STRING_TWO("coreTokenString02", String.class),
    STRING_THREE("coreTokenString03", String.class),
    STRING_FOUR("coreTokenString04", String.class),
    STRING_FIVE("coreTokenString05", String.class),
    STRING_SIX("coreTokenString06", String.class),
    STRING_SEVEN("coreTokenString07", String.class),
    STRING_EIGHT("coreTokenString08", String.class),
    STRING_NINE("coreTokenString09", String.class),
    STRING_TEN("coreTokenString10", String.class),
    STRING_ELEVEN("coreTokenString11", String.class),
    STRING_TWELVE("coreTokenString12", String.class),
    STRING_THIRTEEN("coreTokenString13", String.class),
    STRING_FOURTEEN("coreTokenString14", String.class),
    STRING_FIFTEEN("coreTokenString15", String.class),

    // Generic indexed Integer fields
    INTEGER_ONE("coreTokenInteger01", Integer.class),
    INTEGER_TWO("coreTokenInteger02", Integer.class),
    INTEGER_THREE("coreTokenInteger03", Integer.class),
    INTEGER_FOUR("coreTokenInteger04", Integer.class),
    INTEGER_FIVE("coreTokenInteger05", Integer.class),
    INTEGER_SIX("coreTokenInteger06", Integer.class),
    INTEGER_SEVEN("coreTokenInteger07", Integer.class),
    INTEGER_EIGHT("coreTokenInteger08", Integer.class),
    INTEGER_NINE("coreTokenInteger09", Integer.class),
    INTEGER_TEN("coreTokenInteger10", Integer.class),

    // Generic indexed Date fields
    DATE_ONE("coreTokenDate01", Calendar.class),
    DATE_TWO("coreTokenDate02", Calendar.class),
    DATE_THREE("coreTokenDate03", Calendar.class),
    DATE_FOUR("coreTokenDate04", Calendar.class),
    DATE_FIVE("coreTokenDate05", Calendar.class);

    private final String ldapAttribute;
    private final Class<?> attributeType;

    /**
     * @param ldapAttribute The name of the field that this CoreTokenField is associated to.
     */
    private CoreTokenField(String ldapAttribute, Class<?> attributeType) {
        this.ldapAttribute = ldapAttribute;
        this.attributeType = attributeType;
    }

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
