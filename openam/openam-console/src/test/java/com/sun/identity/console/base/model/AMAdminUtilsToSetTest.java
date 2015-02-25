/*
 * Copyright 2014 ForgeRock, AS.
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

package com.sun.identity.console.base.model;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

/**
 * Basic unit tests for {@link AMAdminUtils#toSet(Object...)} and {@link AMAdminUtils#toSetIgnoreEmpty(Object...)}.
 * Note: tests characterise the existing behaviour to avoid accidentally breaking anything, rather than specifying what
 * the behaviour should be.
 *
 * @since 12.0.0
 */
public class AMAdminUtilsToSetTest {

    @Test
    public void shouldReturnEmptySetForNullArray() {
        // Given
        Object[] args = null;

        // When
        Set<String> result = AMAdminUtils.toSet(args);

        // Then
        assertEquals(result, Collections.emptySet());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNPEForNullElements() {
        // Given
        Object[] args = { null };

        // When
        AMAdminUtils.toSet(args);

        // Then - exception
    }


    @Test
    public void shouldPreserveEmptyStringsNormally() {
        // Given
        String[] args = { "aaa", "", "ccc" };

        // When
        Set<String> result = AMAdminUtils.toSet(args);

        // Then
        assertEquals(result, new HashSet<String>(Arrays.asList(args)));
    }

    @Test
    public void shouldIgnoreEmptyStringsIfAsked() {
        // Given
        String[] args = { "aaa", "", "ccc" };

        // When
        Set<String> result = AMAdminUtils.toSetIgnoreEmpty(args);

        // Then
        assertThat(result).isNotNull()
                          .hasSize(2)
                          .contains("aaa", "ccc")
                          .excludes("");
    }

    @Test
    public void shouldTrimStrings() {
        // Given
        String[] args = { " foo ", " bar " };

        // When
        Set<String> result = AMAdminUtils.toSet(args);

        // Then
        assertEquals(result, new HashSet<String>(Arrays.asList("foo", "bar")));
    }

}
