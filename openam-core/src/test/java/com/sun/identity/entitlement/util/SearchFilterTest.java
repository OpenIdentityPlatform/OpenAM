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
 * Portions copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SearchFilterTest {

    private static final SearchAttribute NAME_ATTR = new SearchAttribute("name", "ou");

    @Test
    public void shouldProduceValidFilterForNormalValue() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "myPolicy");
        String filter = f.getFilter();
        // Pin the exact output so the filter format cannot silently change for legitimate values.
        assertEquals(filter, "(ou=name=myPolicy)", "unexpected filter: " + filter);
    }

    @Test
    public void shouldEscapeAsteriskInValue() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "*");
        String filter = f.getFilter().toLowerCase();
        assertTrue(filter.contains("\\2a"), "asterisk should be escaped: " + filter);
        assertFalse(filter.contains("=*"), "raw asterisk should not appear as wildcard: " + filter);
    }

    @Test
    public void shouldEscapeParenthesesInValue() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "foo)(objectClass=*");
        String filter = f.getFilter().toLowerCase();
        assertTrue(filter.contains("\\29"), "closing paren should be escaped: " + filter);
        assertTrue(filter.contains("\\28"), "opening paren should be escaped: " + filter);
        assertFalse(filter.contains(")(objectclass="), "injected clause should not appear: " + filter);
    }

    @Test
    public void shouldEscapeBackslashInValue() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "foo\\bar");
        String filter = f.getFilter().toLowerCase();
        assertTrue(filter.contains("\\5c"), "backslash should be escaped: " + filter);
    }

    @Test
    public void shouldEscapeNullByteInValue() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "foo\0bar");
        String filter = f.getFilter().toLowerCase();
        assertTrue(filter.contains("\\00"), "null byte should be escaped: " + filter);
    }

    @Test
    public void shouldPreserveLdapFilterStructure() {
        SearchFilter f = new SearchFilter(NAME_ATTR, "myPolicy");
        String filter = f.getFilter();
        assertTrue(filter.startsWith("("), "filter should start with '(': " + filter);
        assertTrue(filter.endsWith(")"), "filter should end with ')': " + filter);
        assertTrue(filter.contains("ou="), "filter should contain LDAP attribute 'ou': " + filter);
    }

    @Test
    public void shouldEscapeMetacharactersInAttributeName() {
        // For an unknown attribute the name is attacker-controlled and becomes part of the
        // assertion value; its LDAP metacharacters must be escaped, not passed through.
        SearchAttribute injected = new SearchAttribute("foo)(uid=*", "ou");
        String filter = new SearchFilter(injected, "x").getFilter().toLowerCase();
        assertTrue(filter.contains("\\29"), "closing paren in attribute name should be escaped: " + filter);
        assertTrue(filter.contains("\\28"), "opening paren in attribute name should be escaped: " + filter);
        assertTrue(filter.contains("\\2a"), "asterisk in attribute name should be escaped: " + filter);
        assertFalse(filter.contains(")(uid="), "injected clause from attribute name must not appear: " + filter);
    }

    @Test
    public void shouldProduceExactFilterForNumericValue() {
        // The numeric/operator branch is unchanged by the fix (values are longs and cannot inject);
        // pin its output so the behaviour is documented and locked in.
        SearchFilter f = new SearchFilter(NAME_ATTR, 1234L, SearchFilter.Operator.EQUALS_OPERATOR);
        assertEquals(f.getFilter(), "(ou=1234=name)", "unexpected numeric filter: " + f.getFilter());
    }
}
