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
package com.sun.identity.policy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the resource comparator validator, whose overlap check used to cost O(n<sup>3</sup>)
 * in the length of the two patterns it is given. It is reachable from the JAXRPC
 * {@code validateServiceAttributes} endpoint, where the caller names the validator and hands
 * it the values, so the cost of the check is the caller's to choose.
 */
public class ResourceComparatorValidatorTest {

    private final ResourceComparatorValidator validator = new ResourceComparatorValidator();

    /**
     * Builds one resource comparator value in the form the policy configuration schema
     * declares it, so that the patterns are read from the tokens they are really read from -
     * the attribute is {@code oneLevelWildcard}, not {@code one_level_wildcard}, and a value
     * naming the latter never reaches the overlap check at all.
     *
     * @param wildcard the wildcard pattern.
     * @param oneLevelWildcard the one level wildcard pattern.
     * @return the comparator value.
     */
    private static String comparator(String wildcard, String oneLevelWildcard) {
        return "serviceType=iPlanetAMWebAgentService"
                + "|class=com.sun.identity.policy.plugins.HttpURLResourceName"
                + "|wildcard=" + wildcard
                + "|oneLevelWildcard=" + oneLevelWildcard
                + "|delimiter=/|caseSensitive=false";
    }

    private boolean validate(String wildcard, String oneLevelWildcard) {
        return validator.validate(Collections.singleton(comparator(wildcard, oneLevelWildcard)));
    }

    // ------------------------------------------------------------------ the answers it gives

    @DataProvider(name = "patterns")
    public Object[][] patterns() {
        return new Object[][] {
            // the pair shipped in amPolicyConfig.xml: "-*-" contains "*", so the patterns are
            // nested and the overlap check is skipped
            { "*",    "-*-",  true,  "the shipped default" },
            { "*",    "*",    false, "two equal patterns" },
            { "ab*",  "*cd",  false, "a wildcard suffix that is a one level prefix" },
            { "*cd",  "ab*",  false, "a one level suffix that is a wildcard prefix" },
            { "*",    "-",    true,  "two patterns with nothing in common" },
            { "ab",   "cd",   true,  "two disjoint multi character patterns" },
        };
    }

    @Test(dataProvider = "patterns")
    public void answersForThePatternPair(String wildcard, String oneLevelWildcard,
            boolean expected, String description) {
        assertEquals(validate(wildcard, oneLevelWildcard), expected, description);
    }

    @Test
    public void acceptsAValueThatDeclaresNeitherPattern() {
        assertTrue(validator.validate(Collections.singleton(
                "serviceType=iPlanetAMWebAgentService|delimiter=/")));
    }

    @Test
    public void acceptsAnEmptySetOfValues() {
        assertTrue(validator.validate(Collections.<String>emptySet()));
    }

    // -------------------------------------------------------------------- and what they cost

    /**
     * The two patterns are of equal length and differ only in their last character, so they
     * are neither equal nor nested and the overlap check runs over both of them in full. The
     * check used to allocate a fresh suffix of each pattern for every pair of matching
     * characters, which is cubic: 8&nbsp;KB of caller supplied value cost tens of seconds of
     * CPU, and 20&nbsp;KB cost minutes, on a call any authenticated caller can make.
     */
    @Test
    public void boundsTheCostOfTheOverlapCheck() {
        String run = "a".repeat(6000);

        long startedAt = System.nanoTime();
        boolean answer = validate(run + "X", run + "Y");
        long elapsedMillis = (System.nanoTime() - startedAt) / 1000000L;

        assertTrue(answer, "the two patterns do not overlap");
        assertTrue(elapsedMillis < 5000,
                "the overlap check has to be bounded, it took " + elapsedMillis + " ms");
    }
}
