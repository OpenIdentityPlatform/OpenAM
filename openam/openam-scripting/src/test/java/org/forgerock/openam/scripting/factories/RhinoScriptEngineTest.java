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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Basic tests of the rhino script engine. The engine is more fully tested by the
 * {@link org.forgerock.openam.scripting.StandardScriptEvaluatorTest}.
 */
public class RhinoScriptEngineTest {

    private RhinoScriptEngine testEngine;

    @BeforeMethod
    public void setupTests() {
        // It is not possible to mock the ContextFactory stuff effectively as it relies on thread-local variables, so
        // we instead use a real script engine factory.
        RhinoScriptEngineFactory factory = new RhinoScriptEngineFactory();
        testEngine = new RhinoScriptEngine(factory);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScriptEngineFactory() {
        new RhinoScriptEngine(null);
    }

    @Test
    public void shouldReturnNonNullBindings() {
        assertThat(testEngine.createBindings()).isNotNull();
    }

    @Test
    public void shouldEvaluateSimpleScripts() throws Exception {
        // Given
        String script = "3 * 4";

        // When
        Number result = (Number) testEngine.eval(script);

        // Then
        assertThat(result.intValue()).isEqualTo(12);
    }

    @Test
    public void shouldReportErrorsAccurately() throws Exception {
        // Given
        String script = "var x = 1;\nvarxxxx y = 2;\nx+y";
        String filename = "test.js";


        // When
        String source = null;
        int line = -1;
        int col = -1;

        try {
            testEngine.put(ScriptEngine.FILENAME, filename);
            testEngine.eval(script);
        } catch (ScriptException ex) {
            source = ex.getFileName();
            line = ex.getLineNumber();
            col = ex.getColumnNumber();
        }

        // Then
        assertThat(source).isEqualTo(filename);
        assertThat(line).isEqualTo(2);
        assertThat(col).isGreaterThanOrEqualTo(0); // Just make sure not -1
    }

    @Test
    public void shouldSupportJSONParsing() throws Exception {
        // Given
        String script = "var x = JSON.parse('{\"a\" : 12}'); x['a']";

        // When
        Object result = testEngine.eval(script);

        // Then
        assertThat(result).isEqualTo(12);
    }
}