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

package org.forgerock.openam.scripting.sandbox;

import org.mozilla.javascript.ClassShutter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class GroovySandboxValueFilterTest {

    private ClassShutter mockClassShutter;
    private GroovySandboxValueFilter testFilter;

    @BeforeMethod
    public void setupTests() {
        mockClassShutter = mock(ClassShutter.class);
        testFilter = new GroovySandboxValueFilter(mockClassShutter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullClassShutter() {
        new GroovySandboxValueFilter(null);
    }

    @Test
    public void shouldAllowNullObjects() {
        // Given

        // When
        Object result = testFilter.filter(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldDelegateInstanceCallsToClassShutter() {
        // Given
        Object target = "a test object";
        given(mockClassShutter.visibleToScripts(target.getClass().getName())).willReturn(true);

        // When
        Object result = testFilter.filter(target);

        // Then
        assertThat(result).isSameAs(target);
    }

    @Test
    public void shouldDelegateStaticCallsToClassShutter() {
        // Given
        Class<?> target = String.class;
        given(mockClassShutter.visibleToScripts(target.getName())).willReturn(true);

        // When
        Object result = testFilter.filter(target);

        // Then
        assertThat(result).isSameAs(target);
    }

    @Test(expectedExceptions = SecurityException.class)
    public void shouldRejectObjectsThatDoNoMatchShutter() {
        // Given
        Object target = "a test object";
        given(mockClassShutter.visibleToScripts(target.getClass().getName())).willReturn(false);

        // When
        testFilter.filter(target);

        // Then - exception
    }
}