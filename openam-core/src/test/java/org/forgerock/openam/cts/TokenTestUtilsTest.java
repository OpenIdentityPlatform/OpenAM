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
package org.forgerock.openam.cts;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.LDAPDataConversionTest;
import org.testng.annotations.Test;

import java.util.Calendar;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenTestUtilsTest {

    @Test (expectedExceptions = AssertionError.class)
    public void shouldCompareValue() {
        // Given
        Token expected = new Token("", TokenType.SESSION);
        Token result = new Token("", TokenType.SESSION);

        CoreTokenField field = CoreTokenField.STRING_ONE;

        expected.setAttribute(field, "badger");
        result.setAttribute(field, "ferret");

        // When / Then
        TokenTestUtils.compareTokens(result, expected);
    }

    @Test (expectedExceptions = AssertionError.class)
    public void shouldCompareDateWithMilliseconds() {
        // Given
        Token expected = new Token("", TokenType.SESSION);
        expected.setExpiryTimestamp(Calendar.getInstance());

        Token result = new Token("", TokenType.SESSION);
        Calendar resultCal = Calendar.getInstance();
        resultCal.add(Calendar.MILLISECOND, 1);
        result.setExpiryTimestamp(resultCal);

        // When / Then
        TokenTestUtils.compareTokens(result, expected);
    }

    @Test (expectedExceptions = AssertionError.class)
    public void shouldFailBecauseTokenTimestampsAreDifferentTimeZones() {
        // Given
        Token expected = new Token("", TokenType.SESSION);
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeZone(LDAPDataConversionTest.CHICAGO);
        expected.setExpiryTimestamp(expectedCal);

        Token result = new Token("", TokenType.SESSION);
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeZone(LDAPDataConversionTest.BERLIN);
        result.setExpiryTimestamp(resultCal);

        // When / Then
        TokenTestUtils.compareTokens(result, expected);
    }
}
