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

import org.forgerock.openam.scripting.factories.GroovyEngineFactory;
import org.forgerock.openam.scripting.factories.RhinoScriptEngineFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;

public class StandardScriptEvaluatorTest {

    private static final ScriptEngineConfiguration CONFIGURATION =
            ScriptEngineConfiguration.builder()
                // Allow any java.* classes for the unit tests
                .withWhiteList(Arrays.asList(Pattern.compile("java\\..*"), Pattern.compile("groovy\\..*")))
                // But deny access to reflection
                .withBlackList(Arrays.asList(Pattern.compile("java\\.lang\\.Class"),
                        Pattern.compile("java\\.lang\\.reflect\\..*")))
                .build();

    private static final RhinoScriptEngineFactory RHINO = new RhinoScriptEngineFactory();
    private static final GroovyEngineFactory GROOVY = new GroovyEngineFactory();

    private StandardScriptEvaluator testEvaluator;
    private StandardScriptEngineManager scriptEngineManager;

    @BeforeMethod
    public void createTestEvaluator() {
        scriptEngineManager = new StandardScriptEngineManager();
        scriptEngineManager.setConfiguration(CONFIGURATION);
        testEvaluator = new StandardScriptEvaluator(scriptEngineManager);
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
        ScriptObject script = getJavascript(bindings, varName);

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
        ScriptObject script = getJavascript(bindings, varName);

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
        ScriptObject script = getJavascript(scriptBindings, varName);
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
    public void shouldSupportJSONParsing() throws Exception {
        ScriptObject script = getJavascript("var json = JSON.parse(x)",  "json['a']");
        Bindings scope = new SimpleBindings();
        scope.put("x", "{\"a\" : 12}");

        Object result = testEvaluator.evaluateScript(script, scope);

        assertThat(result).isEqualTo(12);
    }

    @Test
    public void shouldSupportJSONParsingInGroovy() throws Exception {
        ScriptObject script = getGroovyScript("import groovy.json.JsonSlurper",
                                              "def slurper = new JsonSlurper()",
                                              "def json = slurper.parseText(x)",
                                              "json.a");
        Bindings scope = new SimpleBindings();
        scope.put("x", "{\"a\" : 12}");

        Object result = testEvaluator.evaluateScript(script, scope);

        assertThat(result).isEqualTo(12);
    }

    @Test(expectedExceptions = ScriptException.class,
            expectedExceptionsMessageRegExp = ".*Access to Java class .*? is prohibited.*")
    public void shouldForbidChangingIntegerCache() throws Exception {
        // Given
        // This script attempts to use reflection to alter the shared integer cache, making 1 == 2 in boxed Integer
        // contexts.
        ScriptObject evil = getJavascript(
                "var value = new java.lang.Integer(0).getClass().getDeclaredField('value')",
                 "value.setAccessible(true)",
                 "value.set(java.lang.Integer.valueOf(1), java.lang.Integer.valueOf(2))",
                 "java.lang.Integer.valueOf(1)"); // Would be 2 if allowed

        // When
        testEvaluator.evaluateScript(evil, null);

        // Then - sandbox should abort script
    }

    @Test(expectedExceptions = ScriptException.class,
            expectedExceptionsMessageRegExp = ".*Access to Java class .*? is prohibited.*")
    public void shouldForbidChangingIntegerCacheFromGroovy() throws Exception {
        // Given
        // This script attempts to use reflection to alter the shared integer cache, making 1 == 2 in boxed Integer
        // contexts.
        ScriptObject evil = getGroovyScript(
                "def value = new Integer(0).getClass().getDeclaredField('value')",
                "value.setAccessible(true)",
                "value.set(Integer.valueOf(1), Integer.valueOf(2))",
                "Integer.valueOf(1)"); // Would be 2 if allowed

        // When
        testEvaluator.evaluateScript(evil, null);

        // Then - sandbox should abort script

    }


    static ScriptObject getJavascript(String... script) {
        return getJavascript(null, script);
    }

    static ScriptObject getJavascript(Bindings bindings, String... script) {
        final ScriptingLanguage language = SupportedScriptingLanguage.JAVASCRIPT;
        final String name = "test script";


        return new ScriptObject(name, RHINO.getProgram(script), language, bindings);
    }

    static ScriptObject getGroovyScript(String... lines) {
        return new ScriptObject("groovyTest", GROOVY.getProgram(lines), SupportedScriptingLanguage.GROOVY, null);
    }


}
