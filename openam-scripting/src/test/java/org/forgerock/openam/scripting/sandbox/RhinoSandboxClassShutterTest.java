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

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

public class RhinoSandboxClassShutterTest {
    private static final List<Pattern> EMPTY_LIST = Collections.emptyList();

    private static final List<Pattern> SYSTEM = Collections.singletonList(Pattern.compile("java\\.lang\\.System"));

    private SecurityManager mockSecurityManager;

    private RhinoSandboxClassShutter sandboxClassShutter;

    @BeforeMethod
    public void setupTests() {
        mockSecurityManager = Mockito.mock(SecurityManager.class);

        sandboxClassShutter = new RhinoSandboxClassShutter(mockSecurityManager, SYSTEM, EMPTY_LIST);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullWhitelist() {
        new RhinoSandboxClassShutter(mockSecurityManager, null, EMPTY_LIST);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullBlacklist() {
        new RhinoSandboxClassShutter(mockSecurityManager, EMPTY_LIST, null);
    }

    @Test
    public void shouldAllowNullSecurityManager() {
        new RhinoSandboxClassShutter(null, EMPTY_LIST, EMPTY_LIST);
    }

    @Test
    public void shouldRespectSecurityManager() {
        // Given
        final String className = "java.lang.System";
        doThrow(new SecurityException()).when(mockSecurityManager).checkPackageAccess(className);

        // When
        final boolean visible = sandboxClassShutter.visibleToScripts(className);

        // Then
        assertThat(visible).isFalse();
    }

    @Test
    public void shouldAllowClassesThatMatchWhitelist() {
        // Given
        final String className = "java.lang.System";

        // When
        final boolean visible = sandboxClassShutter.visibleToScripts(className);

        // Then
        assertThat(visible).isTrue();
    }

    @Test
    public void shouldNotAllowClassesThatDoNotMatchWhitelist() {
        // Given
        final String className = "java.lang.Other";

        // When
        final boolean visible = sandboxClassShutter.visibleToScripts(className);

        // Then
        assertThat(visible).isFalse();
    }

    @Test
    public void shouldNotAllowClassesThatMatchBlackList() {
        // Given
        final List<Pattern> whiteList = Collections.singletonList(Pattern.compile("java\\.lang\\..*"));
        final List<Pattern> blackList = Collections.singletonList(Pattern.compile("java\\.lang\\.System"));
        sandboxClassShutter = new RhinoSandboxClassShutter(mockSecurityManager, whiteList, blackList);
        final String className = "java.lang.System";

        // When
        final boolean visible = sandboxClassShutter.visibleToScripts(className);

        // Then
        assertThat(visible).isFalse();
    }
}
