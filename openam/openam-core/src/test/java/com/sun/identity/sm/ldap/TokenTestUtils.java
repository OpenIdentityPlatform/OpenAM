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
package com.sun.identity.sm.ldap;

import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.fields.CoreTokenFieldTypes;
import com.sun.identity.sm.ldap.api.tokens.Token;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Provides CTS based Token testing functionality, specifically for unit testing.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenTestUtils {

    /**
     * Logic for comparing two tokens. It might be useful to move this to a .equals method at some point.
     *
     * @param result Non null.
     * @param expected Non null.
     */
    public static void compareTokens(Token result, Token expected) {
        assertEquals(result.getAttributeNames().size(), expected.getAttributeNames().size());
        for (CoreTokenField field : result.getAttributeNames()) {

            if (CoreTokenFieldTypes.isCalendar(field)) {
                Calendar resultCal = result.getValue(field);
                Calendar expectedCal = expected.getValue(field);

                if (resultCal.getTimeInMillis() != expectedCal.getTimeInMillis()) {
                    throw new AssertionError(MessageFormat.format(
                            "Milliseconds did not match for date field {0}:\n" +
                                    "Expected: {1}\n" +
                                    "  Result: {2}",
                            field.toString(),
                            expectedCal.getTimeInMillis(),
                            resultCal.getTimeInMillis()));
                }

                int resultOffset = getTotalTimeZoneOffset(resultCal.getTimeZone());
                int expectedOffset = getTotalTimeZoneOffset(expectedCal.getTimeZone());

                if (resultOffset != expectedOffset) {
                    throw new AssertionError(MessageFormat.format(
                            "TimeZone offset did not match for date field {0}:\n" +
                            "Expected: {1}\n" +
                            "  Result: {2}",
                            field.toString(),
                            expectedOffset,
                            resultOffset));
                }
            } else if (CoreTokenFieldTypes.isByteArray(field)) {

                byte[] resultValue = result.getValue(field);
                byte[] expectedValue = expected.getValue(field);

                if (!ArrayUtils.isEquals(resultValue, expectedValue)) {
                    throw new AssertionError(MessageFormat.format(
                            "Value did not match for byte[] field {0}:\n" +
                                    "Expected: {1} bytes\n" +
                                    "  Result: {2} bytes",
                            field.toString(),
                            expectedValue.length,
                            resultValue.length));
                }

            } else {
                Object resultValue = result.getValue(field);
                Object expectedValue = expected.getValue(field);

                if (!compareValue(resultValue, expectedValue)) {
                    throw new AssertionError(MessageFormat.format(
                            "Value did not match for field {0}:\n" +
                                    "Expected: {1}\n" +
                                    "  Result: {2}",
                            field.toString(),
                            expectedValue,
                            resultValue));
                }
            }
        }
    }

    /**
     * Required to correctly account for all factors when dealing with TimeZones.
     *
     * @param zone Non null.
     * @return
     */
    private static int getTotalTimeZoneOffset(TimeZone zone) {
        int r = zone.getRawOffset();
        if (zone.useDaylightTime()) {
            r += zone.getDSTSavings();
        }
        return r;
    }

    /**
     * This method is needed because the TestNG assertEquals method didn't seem to work for the
     * enum TokenType.
     *
     * @param first Non null.
     * @param second Non null.
     * @return True if the objects compare.
     */
    private static boolean compareValue(Object first, Object second) {
        if (first.equals(second)) {
            return true;
        }

        if (first.toString().equals(second.toString())) {
            return true;
        }

        return false;
    }

    public static Token generateToken() {
        String id = RandomStringUtils.randomAlphabetic(20);
        Token token = new Token(id, TokenType.SESSION);

        // Set to expire now.
        token.setExpiryTimestamp(Calendar.getInstance());

        // Some extra data
        token.setAttribute(CoreTokenField.STRING_ONE, RandomStringUtils.randomAlphabetic(20));
        token.setAttribute(CoreTokenField.STRING_TWO, RandomStringUtils.randomAlphabetic(20));
        token.setAttribute(CoreTokenField.STRING_THREE, RandomStringUtils.randomAlphabetic(20));
        token.setAttribute(CoreTokenField.STRING_FOUR, RandomStringUtils.randomAlphabetic(20));
        token.setAttribute(CoreTokenField.STRING_FIVE, RandomStringUtils.randomAlphabetic(20));

        // Some binary data
        byte[] data = RandomStringUtils.randomAlphabetic(100).getBytes();
        token.setBlob(data);

        return token;
    }
}
