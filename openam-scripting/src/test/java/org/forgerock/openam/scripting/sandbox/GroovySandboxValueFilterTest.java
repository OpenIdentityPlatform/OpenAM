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
 * Portions copyright 2026 3A Systems,LLC
 * Portions Copyrighted 2026 OSSTech Corporation
 */

package org.forgerock.openam.scripting.sandbox;

import org.kohsuke.groovy.sandbox.GroovyInterceptor.Invoker;
import org.mozilla.javascript.ClassShutter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Test(expectedExceptions = SecurityException.class)
    public void shouldBlockProcessExecuteOnList() throws Throwable {
        Invoker invoker = mock(Invoker.class);
        try {
            testFilter.onMethodCall(invoker, Arrays.asList("sh", "-c", "id"), "execute");
        } finally {
            verify(invoker, never()).call(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
        }
    }

    @Test(expectedExceptions = SecurityException.class)
    public void shouldBlockProcessExecuteOnString() throws Throwable {
        Invoker invoker = mock(Invoker.class);
        try {
            testFilter.onMethodCall(invoker, "id", "execute");
        } finally {
            verify(invoker, never()).call(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
        }
    }

    @Test(expectedExceptions = SecurityException.class)
    public void shouldBlockProcessExecuteOnStringArray() throws Throwable {
        Invoker invoker = mock(Invoker.class);
        String[] cmd = {"sh", "-c", "id"};
        try {
            testFilter.onMethodCall(invoker, cmd, "execute");
        } finally {
            verify(invoker, never()).call(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
        }
    }

    @Test(expectedExceptions = SecurityException.class)
    public void shouldBlockProcessExecuteViaInvokeMethod() throws Throwable {
        Invoker invoker = mock(Invoker.class);
        try {
            testFilter.onMethodCall(invoker, Arrays.asList("sh", "-c", "id"),
                    "invokeMethod", "execute", new Object[0]);
        } finally {
            verify(invoker, never()).call(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.<Object[]>any());
        }
    }
}