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

package org.forgerock.openam.scripting.factories;

import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.ScriptEngine;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GroovyEngineFactoryTest {

    private GroovyEngineFactory testFactory;
    private GroovyValueFilter mockFilter;

    @BeforeMethod
    public void setupFactory() {
        mockFilter = mock(GroovyValueFilter.class);
        testFactory = new GroovyEngineFactory();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldErrorIfNoSandboxConfigured() {
        testFactory.getScriptEngine();
    }

    @Test
    public void shouldUseConfiguredSandbox() {
        // Given
        testFactory.setSandbox(mockFilter);

        // When
        ScriptEngine engine = testFactory.getScriptEngine();

        // Then
        assertThat(engine).isInstanceOf(SandboxedGroovyScriptEngine.class);
        SandboxedGroovyScriptEngine sandboxedGroovyScriptEngine = (SandboxedGroovyScriptEngine) engine;
        assertThat(sandboxedGroovyScriptEngine.getSandbox()).isEqualTo(mockFilter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullSandbox() {
        testFactory.setSandbox(null);
    }
}