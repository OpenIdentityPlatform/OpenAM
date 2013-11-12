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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.filter;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class QueryParameterMatcherTest {

    @Test
    public void shouldMatch() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        boolean matches = queryParameterMatcher.match("A=1&KeY=VALuE&-fdw=23nd");

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchOnKey() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        boolean matches = queryParameterMatcher.match("A=1&KEY1=VALUE&-fdw=23nd");

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldNotMatchOnValue() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        boolean matches = queryParameterMatcher.match("A=1&KEY=VALUE1&-fdw=23nd");

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldNotMatchWhenQueryStringIsNull() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        boolean matches = queryParameterMatcher.match(null);

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldNotMatchWhenQueryStringIsEmpty() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        boolean matches = queryParameterMatcher.match("");

        //Then
        assertFalse(matches);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenMissingEqualsInQueryParamKeyValue() {

        //Given
        QueryParameterMatcher queryParameterMatcher = new QueryParameterMatcher("KEY", "VALUE");

        //When
        queryParameterMatcher.match("KEYVALUE");

        //Then
        fail();
    }
}
