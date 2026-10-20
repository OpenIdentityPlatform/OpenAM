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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.scripting;

import com.google.inject.Module;
import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.scripting.guice.ScriptingGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Groovy validation tests.
 */
@GuiceModules({SharedGuiceModule.class, ScriptingGuiceModule.class})
public class GroovyValidatorTest extends GuiceTestCase {

    /**
     * Before each test, {@link GuiceTestCase#setupGuiceModules()} asks {@link InjectorHolder} for its injector. The
     * holder builds the first injector of the JVM from the module loader in place at that moment and never consults the
     * loader again. The default loader collects every Guice module on the test classpath, Guice cannot build an
     * injector from those, and the holder class then stays broken for every later test in the same JVM. Ask for an
     * injector built from no modules instead, before any test of the suite runs: a {@code @BeforeMethod} or
     * {@code @BeforeClass} comes too late whenever surefire happens to run another class that touches the holder ahead
     * of this one.
     *
     * Two limits remain. TestNG creates the instances of all test classes before any {@code @BeforeSuite} method runs,
     * so a constructor or field initializer that reaches the holder still goes first. And this method covers other
     * classes only while the whole suite runs in one JVM ({@code forkCount=1}, {@code reuseForks=true}).
     */
    @BeforeSuite(alwaysRun = true)
    public void setupGuiceModuleLoader() {
        InjectorConfiguration.setGuiceModuleLoader(new GuiceModuleLoader() {
            @Override
            public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> aClass) {
                return new HashSet<>();
            }
        });
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullScript() {
        SupportedScriptingLanguage.GROOVY.getScriptValidator().validateScript(null);
    }

    @Test
    public void shouldAllowEmptyScript() {
        // Given
        ScriptObject testScript = getGroovyScript("");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isZero();
    }

    @Test
    public void shouldProduceValidationErrors() {
        // Given
        ScriptObject testScript = getGroovyScript("*&^*&^");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isGreaterThan(0);
    }

    @Test
    public void shouldNotProduceValidationErrors() {
        // Given
        ScriptObject testScript = getGroovyScript("def text = 'This is a valid script';");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isZero();
    }

    @Test
    public void shouldReportValidationErrorOnCorrectLine() {
        // Given
        ScriptObject testScript = getGroovyScript("def valid = 'This line has no errors';\n" +
                                                  "def randomtext invalid = 'This line contains an error';");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.get(0).getLineNumber()).isEqualTo(2);
    }

    // Groovy does not provide any programmatic way to get hold of line numbers short of parsing the error message

    private ScriptObject getGroovyScript(String script) {
        final ScriptingLanguage language = SupportedScriptingLanguage.GROOVY;
        final String name = "test script";

        return new ScriptObject(name, script, language, null);
    }
}
