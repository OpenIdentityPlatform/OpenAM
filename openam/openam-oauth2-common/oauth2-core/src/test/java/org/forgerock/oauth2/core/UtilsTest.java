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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * @since 12.0.0
 */
public class UtilsTest {

    @Test
    public void isEmptyShouldReturnTrueWhenStringIsNull() {
        assertTrue(Utils.isEmpty((String) null));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenStringIsEmpty() {
        assertTrue(Utils.isEmpty(""));
    }

    @Test
    public void isEmptyShouldReturnFalseWhenStringIsNotEmpty() {
        assertFalse(Utils.isEmpty("WHATEVER"));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenCollectionIsNull() {
        assertTrue(Utils.isEmpty((Collection<?>) null));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenCollectionIsEmpty() {
        assertTrue(Utils.isEmpty(Collections.emptyList()));
    }

    @Test
    public void isEmptyShouldReturnFalseWhenCollectionIsNotEmpty() {
        assertFalse(Utils.isEmpty(Collections.singleton("WHATEVER")));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenMapIsNull() {
        assertTrue(Utils.isEmpty((Map<?, ?>) null));
    }

    @Test
    public void isEmptyShouldReturnTrueWhenMapIsEmpty() {
        assertTrue(Utils.isEmpty(Collections.emptyMap()));
    }

    @Test
    public void isEmptyShouldReturnFalseWhenMapIsNotEmpty() {
        assertFalse(Utils.isEmpty(Collections.singletonMap("WHAT", "EVER")));
    }

    @Test
    public void shouldSplitResponseTypeWhenResponseTypesIsNull() {
        assertEquals(Utils.splitResponseType(null), Collections.emptySet());
    }

    @Test
    public void shouldSplitResponseTypeWhenResponseTypesIsEmpty() {
        assertEquals(Utils.splitResponseType(""), Collections.emptySet());
    }

    @Test
    public void shouldSplitResponseType() {
        Set<String> expectedResponseTypes = new HashSet<String>();
        expectedResponseTypes.add("a");
        expectedResponseTypes.add("b");
        expectedResponseTypes.add("c");
        assertEquals(Utils.splitResponseType("a b c"), expectedResponseTypes);
    }

    @Test
    public void shouldSplitScopeWhenScopeIsNull() {
        assertEquals(Utils.splitScope(null), Collections.emptySet());
    }

    @Test
    public void shouldSplitScopeWhenScopeIsEmpty() {
        assertEquals(Utils.splitScope(""), Collections.emptySet());
    }

    @Test
    public void shouldSplitScope() {
        Set<String> expectedScope = new HashSet<String>();
        expectedScope.add("a");
        expectedScope.add("b");
        expectedScope.add("c");
        assertEquals(Utils.splitScope("a b c"), expectedScope);
    }

    @Test
    public void shouldJoinScopeWhenScopeIsNull() {
        assertEquals(Utils.joinScope(null), "");
    }

    @Test
    public void shouldJoinScopeWhenScopeIsEmpty() {
        assertEquals(Utils.joinScope(Collections.<String>emptySet()), "");
    }

    @Test
    public void shouldJoinScope() {

        //Given
        Set<String> scope = new LinkedHashSet<String>();
        scope.add("a");
        scope.add("b");
        scope.add("c");

        //When/Then
        assertEquals(Utils.joinScope(scope), "a b c");
    }

    @Test
    public void shouldConvertStringToSetWhenStringIsNull() {
        assertEquals(Utils.stringToSet(null), Collections.emptySet());
    }

    @Test
    public void shouldConvertStringToSetWhenStringIsEmpty() {
        assertEquals(Utils.stringToSet(""), Collections.emptySet());
    }

    @Test
    public void shouldConvertStringToSet() {
        Set<String> expectedResponseTypes = new LinkedHashSet<String>();
        expectedResponseTypes.add("a");
        expectedResponseTypes.add("b");
        expectedResponseTypes.add("c");
        assertEquals(Utils.stringToSet("a b c"), expectedResponseTypes);
    }

    @Test
    public void shouldSortCollectionIntoSet() {
        final String result = "abdejz";

        List<String> sortedList = Utils.asSortedList(Utils.stringToSet("d e a z j b b"), new TestComparator());
        StringBuilder sb = new StringBuilder();

        for (String s : sortedList) {
            sb.append(s);
        }

        assertEquals(sb.toString(), result);
    }

    class TestComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

}
