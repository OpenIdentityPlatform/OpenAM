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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryFilters;
import org.testng.annotations.Test;

public class JsonValueQueryFilterVisitorTest {

    private static final JsonValue JSON_VALUE = json(object(
            field("a", "abc"),
            field("b", "def"),
            field("c", array("one", "two", "three")),
            field("d", 5L)
    ));

    private JsonValueQueryFilterVisitor visitor = new JsonValueQueryFilterVisitor();

    @Test
    public void testEquals() throws Exception {
        assertThat(QueryFilters.parse("a eq \"abc\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("c eq \"One\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("d eq 5").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("c eq \"four\"").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test
    public void testContains() throws Exception {
        assertThat(QueryFilters.parse("a co \"bc\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("c co \"On\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("c co \"f\"").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test
    public void testStartsWith() throws Exception {
        assertThat(QueryFilters.parse("a sw \"ab\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("c sw \"tW\"").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("a sw \"bc\"").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test
    public void testGreaterThan() throws Exception {
        assertThat(QueryFilters.parse("d gt 4").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("d gt 5").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test
    public void testGreaterThanEqualTo() throws Exception {
        assertThat(QueryFilters.parse("d ge 4").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("d ge 5").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("d ge 6").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test
    public void testLessThan() throws Exception {
        assertThat(QueryFilters.parse("d lt 5").accept(visitor, JSON_VALUE)).isFalse();
        assertThat(QueryFilters.parse("d lt 6").accept(visitor, JSON_VALUE)).isTrue();
    }

    @Test
    public void testLessThanEqualTo() throws Exception {
        assertThat(QueryFilters.parse("d le 4").accept(visitor, JSON_VALUE)).isFalse();
        assertThat(QueryFilters.parse("d le 5").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("d le 6").accept(visitor, JSON_VALUE)).isTrue();
    }

    @Test
    public void testPresent() throws Exception {
        assertThat(QueryFilters.parse("e pr").accept(visitor, JSON_VALUE)).isFalse();
        assertThat(QueryFilters.parse("d pr").accept(visitor, JSON_VALUE)).isTrue();
    }

    @Test
    public void testAnd() throws Exception {
        assertThat(QueryFilters.parse("e pr and d pr").accept(visitor, JSON_VALUE)).isFalse();
        assertThat(QueryFilters.parse("d pr and d eq 5").accept(visitor, JSON_VALUE)).isTrue();
    }

    @Test
    public void testOr() throws Exception {
        assertThat(QueryFilters.parse("e pr or f pr").accept(visitor, JSON_VALUE)).isFalse();
        assertThat(QueryFilters.parse("d pr or e pr").accept(visitor, JSON_VALUE)).isTrue();
    }

    @Test
    public void testNot() throws Exception {
        assertThat(QueryFilters.parse("! e pr").accept(visitor, JSON_VALUE)).isTrue();
        assertThat(QueryFilters.parse("! d pr").accept(visitor, JSON_VALUE)).isFalse();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testExtended() throws Exception {
        QueryFilters.parse("d fred 5").accept(visitor, JSON_VALUE);
    }

}