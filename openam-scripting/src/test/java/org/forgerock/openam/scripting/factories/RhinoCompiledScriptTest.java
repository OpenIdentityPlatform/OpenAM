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

import org.mozilla.javascript.Script;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RhinoCompiledScriptTest {

    private RhinoScriptEngine mockEngine;
    private Script mockScript;
    private RhinoCompiledScript testScript;

    @BeforeMethod
    public void setupTest() {
        mockEngine = mock(RhinoScriptEngine.class);
        mockScript = mock(Script.class);
        testScript = new RhinoCompiledScript(mockEngine, mockScript);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScriptEngine() {
        new RhinoCompiledScript(null, mockScript);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScript() {
        new RhinoCompiledScript(mockEngine, null);
    }

    @Test
    public void shouldReturnCorrectEngine() {
        assertThat(testScript.getEngine()).isEqualTo(mockEngine);
    }

    @Test
    public void shouldDelegateToScriptEngine() throws ScriptException {
        // Given
        ScriptContext context = mock(ScriptContext.class);
        Object expectedResult = "a result";
        given(mockEngine.evalCompiled(mockScript, context)).willReturn(expectedResult);

        // When
        Object result = testScript.eval(context);

        // Then
        verify(mockEngine).evalCompiled(mockScript, context);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldPropagateExceptions() throws ScriptException {
        // Given
        ScriptContext context = mock(ScriptContext.class);
        given(mockEngine.evalCompiled(mockScript, context)).willThrow(new ScriptException("test exception"));

        // When
        testScript.eval(context);

        // Then - exception
    }
}