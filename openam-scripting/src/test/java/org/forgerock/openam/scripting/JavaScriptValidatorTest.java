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
import org.forgerock.openam.scripting.guice.ScriptingGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@GuiceModules({SharedGuiceModule.class, ScriptingGuiceModule.class})
public class JavaScriptValidatorTest extends GuiceTestCase {

    /**
     * Have the injector built from no modules before any test of the suite runs, for the reasons and with the limits
     * given on {@link GroovyValidatorTest#setupGuiceModuleLoader()}. This class needs its own copy: when it runs alone,
     * as with {@code -Dtest=JavaScriptValidatorTest} or from an IDE, the other classes' {@code @BeforeSuite} methods do
     * not run.
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
        SupportedScriptingLanguage.JAVASCRIPT.getScriptValidator().validateScript(null);
    }

    @Test
    public void shouldAllowEmptyScript() {
        // Given
        ScriptObject testScript = getJavascript("");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isZero();
    }

    @Test
    public void shouldProduceValidationErrors() {
        // Given
        ScriptObject testScript = getJavascript("This is not a valid script");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isGreaterThan(0);
    }

    @Test
    public void shouldNotProduceValidationErrors() {
        // Given
        ScriptObject testScript = getJavascript("var text = 'This is a valid script';");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isZero();
    }

    @Test
    public void shouldReportValidationErrorOnCorrectLine() {
        // Given
        ScriptObject testScript = getJavascript("var valid = 'This line has no errors';\n" +
                                                "notavar invalid = 'This line contains an error';");

        // When
        List<ScriptError> errors = testScript.validate();

        // Then
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.get(0).getLineNumber()).isEqualTo(2);
    }

    private ScriptObject getJavascript(String script) {
        final ScriptingLanguage language = SupportedScriptingLanguage.JAVASCRIPT;
        final String name = "test script";

        return new ScriptObject(name, script, language, null);
    }

}
