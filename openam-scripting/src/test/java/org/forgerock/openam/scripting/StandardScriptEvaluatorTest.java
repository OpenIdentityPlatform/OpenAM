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

package org.forgerock.openam.scripting;

import com.sun.phobos.script.javascript.RhinoScriptEngineFactory;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.openam.scripting.timeouts.ContextFactoryWrapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StandardScriptEvaluatorTest {

    private StandardScriptEvaluator testEvaluator;
    private StandardScriptEngineManager scriptEngineManager;
    private ContextFactoryWrapper factoryWrapper = mock(ContextFactoryWrapper.class);
    private Future mockFuture = mock(Future.class);

    @BeforeMethod
    public void createTestEvaluator() {
        scriptEngineManager = new StandardScriptEngineManager(factoryWrapper);
        // Use our bundled Rhino engine for tests
        scriptEngineManager.registerEngineName(SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME,
                new RhinoScriptEngineFactory());
        testEvaluator = new StandardScriptEvaluator(scriptEngineManager);
        try {
            StandardScriptEvaluator.configureThreadPool(1, 1);
        } catch (IllegalStateException ise) { //ignore

        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScriptEngineManager() {
        new StandardScriptEvaluator(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullVariableNames() {
        testEvaluator.bindVariableInGlobalScope(null, "value");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullVariableValues() {
        testEvaluator.bindVariableInGlobalScope("test", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyVariableNames() {
        testEvaluator.bindVariableInGlobalScope("", "value");
    }

    @Test
    public void shouldBindVariablesInGlobalScope() {
        // Given
        String name = "aTestVariable";
        Object value = "Some test object";

        // When
        testEvaluator.bindVariableInGlobalScope(name, value);

        // Then
        Object result = scriptEngineManager.get(name);
        assertThat(result).isNotNull().isEqualTo(value);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldFailEvaluationOfSimpleScriptBeforeThreadPoolConfigured() throws Exception {
        // Given
        ScriptObject script = getGroovyScript("3 * 4");
        StandardScriptEvaluator myTestEvaluator = new NoThreadStandardScriptEvaluator(scriptEngineManager);

        // When
        myTestEvaluator.evaluateScript(script, null);

        // Then - exception
    }

    @Test
    public void shouldEvaluateSimpleScripts() throws Exception {
        // Given
        ScriptObject script = getGroovyScript("3 * 4");

        // When
        Number result = testEvaluator.evaluateScript(script, null);

        // Then
        assertThat(result.intValue()).isEqualTo(12);
    }

    @Test
    public void shouldExposeGlobalVariables() throws Exception {
        // Given
        String varName = "testVar";
        String value = "a test value";
        ScriptObject script = getJavascript(varName);

        // When
        testEvaluator.bindVariableInGlobalScope(varName, value);
        String result = testEvaluator.evaluateScript(script, null);

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    public void shouldExposeScriptVariables() throws Exception {
        // Given
        String varName = "scriptVar";
        String value = "a script value";
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, value);
        ScriptObject script = getJavascript(varName, bindings);

        // When
        String result = testEvaluator.evaluateScript(script, null);

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    public void shouldExposeParameterVariables() throws Exception {
        // Given
        String varName = "paramVar";
        String value = "a parameter value";
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, value);
        ScriptObject script = getJavascript(varName);

        // When
        String result = testEvaluator.evaluateScript(script, bindings);

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    public void shouldOverrideGlobalVariablesWithScriptVariables() throws Exception {
        // Given
        String varName = "testVar";
        String globalValue = "global value";
        String scriptValue = "script value";
        testEvaluator.bindVariableInGlobalScope(varName, globalValue);
        Bindings bindings = new SimpleBindings();
        bindings.put(varName, scriptValue);
        ScriptObject script = getJavascript(varName, bindings);

        // When
        String result = testEvaluator.evaluateScript(script, null);

        // Then
        assertThat(result).isEqualTo(scriptValue);
    }

    @Test
    public void shouldOverrideScriptVariablesWithParameterVariables() throws Exception {
        // Given
        String varName = "testVar";
        String paramValue = "param value";
        String scriptValue = "script value";
        Bindings scriptBindings = new SimpleBindings();
        scriptBindings.put(varName, scriptValue);
        ScriptObject script = getJavascript(varName, scriptBindings);
        Bindings paramBindings = new SimpleBindings();
        paramBindings.put(varName, paramValue);

        // When
        String result = testEvaluator.evaluateScript(script, paramBindings);

        // Then
        assertThat(result).isEqualTo(paramValue);

    }

    /**
     * Ensure that binding scopes are passed by reference to the script engine so that any changes made by the script
     * are reflected in the final state of the bindings passed in.
     */
    @Test
    public void shouldPassBindingsByReference() throws Exception {
        // Given
        String varName = "state";
        Bindings scope = new SimpleBindings();
        scope.put(varName, "initial");
        String expected = "expected";
        ScriptObject script = getJavascript(varName + " = '" + expected + "'");

        // When
        testEvaluator.evaluateScript(script, scope);

        // Then
        assertThat(scope.get(varName)).isEqualTo(expected);
    }

    @Test
    public void shouldSupportGroovyScripts() throws Exception {
        // Given
        String varName = "state";
        String expected = "expected";
        ScriptObject groovyScript = getGroovyScript(varName + " = \"" + expected + "\"");
        Bindings scope = new SimpleBindings();
        scope.put(varName, "initial");

        // When
        testEvaluator.evaluateScript(groovyScript, scope);

        // Then
        assertThat(scope.get(varName)).isEqualTo(expected);
    }

    @Test
    public void shouldStopJavaScriptExecutionWhenTimeoutReached() throws InterruptedException, ExecutionException, TimeoutException, ScriptException {
        //given
        mockFuture = mock(Future.class);
        StandardScriptEvaluator myTestEvaluator = new FakeFutureStandardScriptEvaluator(scriptEngineManager);
        ScriptObject loopScript = getJavascript("while(true) { }");
        myTestEvaluator.getEngineManager().configureTimeout(1);

        //when
        myTestEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldNotStopJavaScriptExecutionWhenNoTimeoutConfigured() throws InterruptedException, ExecutionException, ScriptException {
        //given
        mockFuture = mock(Future.class);
        StandardScriptEvaluator myTestEvaluator = new FakeFutureStandardScriptEvaluator(scriptEngineManager);
        ScriptObject loopScript = getJavascript("while(true) { }");
        myTestEvaluator.getEngineManager().configureTimeout(0);

        //when
        myTestEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get();
    }

    @Test
    public void shouldNotStopGroovyScriptExecutionWhenTimeoutReached() throws ExecutionException, InterruptedException, ScriptException {
        //given
        mockFuture = mock(Future.class);
        StandardScriptEvaluator myTestEvaluator = new FakeFutureStandardScriptEvaluator(scriptEngineManager);
        ScriptObject loopScript = getGroovyScript("while(true) { }");
        myTestEvaluator.getEngineManager().configureTimeout(0);

        //when
        myTestEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get();
    }

    @Test
    public void shouldStopGroovyScriptExecutionWhenTimeoutReached() throws InterruptedException, ExecutionException, TimeoutException, ScriptException {
        //given
        mockFuture = mock(Future.class);
        StandardScriptEvaluator myTestEvaluator = new FakeFutureStandardScriptEvaluator(scriptEngineManager);
        ScriptObject loopScript = getGroovyScript("while(true) { }");
        myTestEvaluator.getEngineManager().configureTimeout(1);

        //when
        myTestEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get(1000, TimeUnit.MILLISECONDS);
    }

    private ScriptObject getJavascript(String script) {
        return getJavascript(script, null);
    }

    private ScriptObject getJavascript(String script, Bindings bindings) {
        final ScriptingLanguage language = SupportedScriptingLanguage.JAVASCRIPT;
        final String name = "test script";

        return new ScriptObject(name, script, language, bindings);
    }

    private ScriptObject getGroovyScript(String script) {
        return new ScriptObject("groovyTest", script, SupportedScriptingLanguage.GROOVY, null);
    }

    // Executor that runs everything in the calling thread without a pool and returns a fake future when submit called
    private class FakeFutureExecutor extends AbstractExecutorService {

        public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> tCallable) {
            return mockFuture;
        }

        private volatile boolean shutdown;

        public void shutdown() {
            shutdown = true;
        }

        public List<Runnable> shutdownNow() {
            return null;
        }

        public boolean isShutdown() {
            return shutdown;
        }

        public boolean isTerminated() {
            return shutdown;
        }

        public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void execute(Runnable runnable) {
            return;
        }

    }

    private class FakeFutureStandardScriptEvaluator extends StandardScriptEvaluator {

        /**
         * Constructs the script evaluator using the given JSR 223 script engine manager instance.
         *
         * @param scriptEngineManager the script engine manager to use for creating script engines. May not be null.
         */
        public FakeFutureStandardScriptEvaluator(StandardScriptEngineManager scriptEngineManager) {
            super(scriptEngineManager);
        }

        @Override
        ExecutorService getThreadPool() {
            return new FakeFutureExecutor();
        }
    }

    private class NoThreadStandardScriptEvaluator extends StandardScriptEvaluator {

        /**
         * Constructs the script evaluator using the given JSR 223 script engine manager instance.
         *
         * @param scriptEngineManager the script engine manager to use for creating script engines. May not be null.
         */
        public NoThreadStandardScriptEvaluator(StandardScriptEngineManager scriptEngineManager) {
            super(scriptEngineManager);
        }

        @Override
        ExecutorService getThreadPool() {
            return null;
        }
    }


}
