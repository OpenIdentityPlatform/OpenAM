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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @since 12.0.0
 */
public class UtilsTest {

    @Test
    public void shouldSplitResponseType() {

        //Given
        final String responseType = "a b c";

        //When
        final Set<String> s = Utils.splitResponseType(responseType);

        //Then
        assertEquals(s, new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"})));
    }

    @Test
    public void shouldSplitScope() {

        //Given
        final String scope = "a b c";

        //When
        final Set<String> s = Utils.splitScope(scope);

        //Then
        assertEquals(s, new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"})));
    }

    @Test
    public void shouldCreateSetFromNullString() {

        //Given
        final String s = null;

        //When
        final Set<String> set = Utils.stringToSet(s);

        //Then
        assertEquals(set, Collections.emptySet());
    }

    @Test
    public void shouldCreateSetFromEmptyString() {

        //Given
        final String s = "";

        //When
        final Set<String> set = Utils.stringToSet(s);

        //Then
        assertEquals(set, Collections.emptySet());
    }

    @Test
    public void shouldCreateSetFromSpaceDelimitedString() {

        //Given
        final String s = "a b c";

        //When
        final Set<String> set = Utils.stringToSet(s);

        //Then
        assertEquals(set, new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"})));
    }

    @Test
    public void shouldJoinScope() {

        //Given
        final Set<String> scope = new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"}));

        //When
        final String s = Utils.joinScope(scope);

        //Then
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenStringIsNull() {

        //Given
        final String s = null;

        //When
        final boolean empty = Utils.isEmpty(s);

        //Then
        assertTrue(empty);
    }

    @Test
    public void isEmptyShouldReturnTrueWhenStringIsEmpty() {

        //Given
        final String s = "";

        //When
        final boolean empty = Utils.isEmpty(s);

        //Then
        assertTrue(empty);
    }

    @Test
    public void isEmptyShouldReturnFalseWhenStringIsNotEmpty() {

        //Given
        final String s = "A";

        //When
        final boolean empty = Utils.isEmpty(s);

        //Then
        assertFalse(empty);
    }
}
