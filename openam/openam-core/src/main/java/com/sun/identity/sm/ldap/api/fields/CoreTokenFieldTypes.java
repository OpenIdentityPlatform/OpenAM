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

import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.exceptions.OperationFailedException;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Map;

/**
 * Provides the mapping between CoreTokenFields and the type of the value that is associated to
 * that field.
 *
 * There are currently a number of uses for the type information of a Core Token Field:
 * - Manipulating a Token via its generic fields.
 * - Persisting a Token to LDAP
 *
 * Both of these cases need to know the type of the value stored in the Tokens map.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenFieldTypes {
    /**
     * Validate a collection of key/value mappings.
     *
     * @param types A mapping of CoreTokenField to value. Non null, may be empty.
     * @throws com.sun.identity.sm.ldap.exceptions.OperationFailedException If one of the values was invalid for the CoreTokenField field.
     */
    public static void validateTypes(Map<CoreTokenField, Object> types) throws OperationFailedException {
        for (Map.Entry<CoreTokenField, Object> entry : types.entrySet()) {
            validateType(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Validate the value matches the expected type for the given key.
     *
     * @param field The CoreTokenField to validate against.
     * @param value The value to verify. Non null.
     * @throws OperationFailedException
     */
    public static void validateType(CoreTokenField field, Object value) throws OperationFailedException {
        if (value == null) {
            throw new OperationFailedException(MessageFormat.format(
                    "\n" +
                            CoreTokenConstants.DEBUG_HEADER +
                            "Value field cannot be null!" +
                            "Key: {0}:{1}",
                    CoreTokenField.class.getSimpleName(),
                    field.name()));
        }

        try {
            if (isString(field)) {
                assertClass(value, String.class);
            } else if (isInteger(field)) {
                assertClass(value, Integer.class);
            } else if (isCalendar(field)) {
                assertClass(value, Calendar.class);
            } else if (isByteArray(field)) {
                assertClass(value, byte[].class);
            } else {
                throw new IllegalStateException("Unknown field: " + field.name());
            }
        } catch (OperationFailedException e) {
            throw new OperationFailedException(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER +
                    "Value was not the correct type:\n" +
                    "           Key: {0}:{1}\n" +
                    "Required Class: {2}" +
                    "  Actual Class: {3}",
                    CoreTokenField.class.getSimpleName(),
                    field.name(),
                    e.getMessage(),
                    value.getClass().getName()),
                    e);
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a Date.
     */
    public static boolean isCalendar(CoreTokenField field) {
        // Intentional fall-through
        switch (field) {
            case EXPIRY_DATE:
            case DATE_ONE:
            case DATE_TWO:
            case DATE_THREE:
            case DATE_FOUR:
            case DATE_FIVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is an Integer.
     */
    public static boolean isInteger(CoreTokenField field) {
        // Intentional fall-through
        switch (field) {
            case INTEGER_ONE:
            case INTEGER_TWO:
            case INTEGER_THREE:
            case INTEGER_FOUR:
            case INTEGER_FIVE:
            case INTEGER_SIX:
            case INTEGER_SEVEN:
            case INTEGER_EIGHT:
            case INTEGER_NINE:
            case INTEGER_TEN:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a String.
     */
    public static boolean isString(CoreTokenField field) {
        switch (field) {
            case TOKEN_ID:
            case USER_ID:
            case STRING_ONE:
            case STRING_TWO:
            case STRING_THREE:
            case STRING_FOUR:
            case STRING_FIVE:
            case STRING_SIX:
            case STRING_SEVEN:
            case STRING_EIGHT:
            case STRING_NINE:
            case STRING_TEN:
            case STRING_ELEVEN:
            case STRING_TWELVE:
            case STRING_THIRTEEN:
            case STRING_FOURTEEN:
            case STRING_FIFTEEN:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a binary field.
     */
    public static boolean isByteArray(CoreTokenField field) {
        return CoreTokenField.BLOB.equals(field);
    }

    /**
     * Perform a simple class assertion.
     * @param value Non null value to assert.
     * @param clazz Non null class to assert against.
     * @throws OperationFailedException If the value was not assignable from the clazz.
     */
    private static void assertClass(Object value, Class clazz) throws OperationFailedException {
        if (!clazz.isAssignableFrom(value.getClass())) {
            throw new OperationFailedException(clazz.getName());
        }
    }
}
