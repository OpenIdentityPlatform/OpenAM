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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.util.Pair;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the parser that reads back the map values the map validators accept. It carries its own
 * copy of the validators' key expression - two adjacent quantifiers competing for the same
 * characters - so a key that is never closed costs it the same quadratic backtracking, on
 * values it reads from the stored configuration rather than from a request.
 */
public class MapValueParserTest {

    private final MapValueParser parser = new MapValueParser();

    @DataProvider(name = "entries")
    public Object[][] entries() {
        return new Object[][] {
            { "[asdf]=ALL",         "asdf",     "ALL" },
            { "[asdf]=",            "asdf",     "" },
            // whitespace is allowed everywhere, which is the whole reason the key is written
            // as one quantifier that requires a non whitespace character followed by one that
            // does not - both halves have to keep matching
            { "[a b]=v",            "a b",      "v" },
            { "[ a b ] = v ",       "a b",      "v" },
            { "[a ]=v",             "a",        "v" },
            { "[ a]=v",             "a",        "v" },
            { "[a=b]=v",            "a=b",      "v" },
            { "[unterminated",      null,       null },
            { "[]=v",               null,       null },
            { "no brackets=v",      null,       null },
        };
    }

    @Test(dataProvider = "entries")
    public void parsesTheEntry(String entry, String name, String value) {
        Pair<String, String> parsed = parser.parse(entry);

        if (name == null) {
            assertThat(parsed).as("%s is not a map entry", entry).isNull();
        } else {
            assertThat(parsed).as("%s is a map entry", entry).isNotNull();
            assertThat(parsed.getFirst()).isEqualTo(name);
            assertThat(parsed.getSecond()).isEqualTo(value);
        }
    }

    /**
     * The same bound the validators carry. This parser reads stored values, so reaching it
     * needs a value to have been written past the validators - through an import, or through
     * an attribute whose schema declares no validator at all - but the expression is the
     * validators' own and has to hold the same way.
     */
    @Test
    public void boundsTheCostOfAnUnterminatedKey() {
        String entry = "[" + "a".repeat(64000);

        long startedAt = System.nanoTime();
        Pair<String, String> parsed = parser.parse(entry);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1000000L;

        assertThat(parsed).isNull();
        assertThat(elapsedMillis).as("matching an unterminated key has to be bounded")
                .isLessThan(5000L);
    }
}
