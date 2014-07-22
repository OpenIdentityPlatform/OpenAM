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

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SandboxedGroovyScriptEngineTest {

    private GroovyEngineFactory engineFactory;
    private CompilableScriptEngine mockScriptEngine;
    private GroovyValueFilter mockValueFilter;

    private SandboxedGroovyScriptEngine testEngine;

    @BeforeMethod
    public void setupTests() {
        engineFactory = new GroovyEngineFactory();
        mockScriptEngine = mock(CompilableScriptEngine.class);
        mockValueFilter = mock(GroovyValueFilter.class);

        testEngine = new SandboxedGroovyScriptEngine(engineFactory, mockScriptEngine, mockValueFilter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullEngineFactory() {
        new SandboxedGroovyScriptEngine(null, mockScriptEngine, mockValueFilter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScriptEngine() {
        new SandboxedGroovyScriptEngine(engineFactory, null, mockValueFilter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullValueFilter() {
        new SandboxedGroovyScriptEngine(engineFactory, mockScriptEngine, null);
    }

    @Test
    public void shouldRegisterSandboxWhenEvaluatingScriptAsString() throws Exception {
        // Given
        String script = "1 + 1";
        ScriptContext context = new SimpleScriptContext();

        // When
        testEngine.eval(script, context);

        // Then
        verify(mockValueFilter).register();
        verify(mockScriptEngine).eval(script, context);
        verify(mockValueFilter).unregister();
    }

    @Test
    public void shouldRegisterSandboxWhenEvaluatingScriptAsReader() throws Exception {
        // Given
        String script = "1 + 1";
        Reader reader = new StringReader(script);
        ScriptContext context = new SimpleScriptContext();

        // When
        testEngine.eval(reader, context);

        // Then
        verify(mockValueFilter).register();
        verify(mockScriptEngine).eval(reader, context);
        verify(mockValueFilter).unregister();
    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldUnregisterEngineAfterException() throws Exception {
        // Given
        String script = "1 + 1";
        ScriptContext context = new SimpleScriptContext();
        given(mockScriptEngine.eval(script, context)).willThrow(new ScriptException("test"));

        // When
        try {
            testEngine.eval(script, context);
        } finally {
            // Then
            verify(mockValueFilter).unregister();
        }
    }

    @Test
    public void shouldApplySandboxToCompiledScripts() throws Exception {
        // Given
        String script = "1 + 1";
        ScriptContext context = new SimpleScriptContext();
        CompiledScript mockCompiledScript = mock(CompiledScript.class);
        given(mockScriptEngine.compile(script)).willReturn(mockCompiledScript);

        // When
        CompiledScript compiledScript = testEngine.compile(script);
        compiledScript.eval(context);

        // Then
        verify(mockValueFilter).register();
        verify(mockCompiledScript).eval(context);
        verify(mockValueFilter).unregister();
    }

    @Test
    public void shouldApplySandboxToCompiledReaderScripts() throws Exception {
        // Given
        String script = "1 + 1";
        Reader reader = new StringReader(script);
        ScriptContext context = new SimpleScriptContext();
        CompiledScript mockCompiledScript = mock(CompiledScript.class);
        given(mockScriptEngine.compile(reader)).willReturn(mockCompiledScript);

        // When
        CompiledScript compiledScript = testEngine.compile(reader);
        compiledScript.eval(context);

        // Then
        verify(mockValueFilter).register();
        verify(mockCompiledScript).eval(context);
        verify(mockValueFilter).unregister();
    }

    @Test
    public void shouldDelegateCreateBindingsToEngine() throws Exception {
        // Given

        // When
        testEngine.createBindings();

        // Then
        verify(mockScriptEngine).createBindings();
    }

    @Test
    public void shouldReturnCorrectEngineFactory() throws Exception {
        // Given

        // When
        ScriptEngineFactory result = testEngine.getFactory();

        // Then
        assertThat(result).isEqualTo(engineFactory);
    }

    /**
     * Combined interface to ensure mock implements both.
     */
    private static interface CompilableScriptEngine extends ScriptEngine, Compilable {

    }
}