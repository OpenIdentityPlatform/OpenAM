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
 * Portions copyright 2026 3A Systems LLC.
 */

package org.forgerock.openam.scripting;

import com.google.inject.Module;
import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.openam.scripting.guice.ScriptingGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for GHSA-5hgj-6j53-p729: a Groovy compile-time AST transform sandbox escape. Submitting a script
 * for validation compiles it, and compile-time AST transforms such as {@code @groovy.transform.ASTTest} and
 * {@code @Grab} run arbitrary code during compilation, before the runtime sandbox applies. These tests assert that
 * such scripts are rejected at validation time and, crucially, that the transform code never runs.
 */
@GuiceModules({SharedGuiceModule.class, ScriptingGuiceModule.class})
public class GroovyAstTransformEscapeTest extends GuiceTestCase {

    /** System property the malicious payloads attempt to set; if it is ever non-null the escape executed. */
    private static final String CANARY = "openam.test.ghsa_5hgj_6j53_p729.executed";

    @BeforeMethod
    @Override
    public void setupGuiceModules() throws Exception {
        // Mirror GroovyValidatorTest: don't let other Guice modules on the classpath load.
        InjectorConfiguration.setGuiceModuleLoader(new GuiceModuleLoader() {
            @Override
            public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> aClass) {
                return new HashSet<>();
            }
        });
        super.setupGuiceModules();
        System.clearProperty(CANARY);
    }

    @AfterMethod
    public void clearCanary() {
        System.clearProperty(CANARY);
    }

    @Test
    public void shouldRejectFullyQualifiedAstTestAndNotExecuteIt() {
        // Given - the exact shape of the published proof-of-concept (using a harmless canary instead of Runtime.exec).
        ScriptObject script = groovy(
                "@groovy.transform.ASTTest(value={\n" +
                "    System.setProperty('" + CANARY + "', 'executed')\n" +
                "})\n" +
                "class Pwn {}");

        // When
        List<ScriptError> errors = script.validate();

        // Then
        assertThat(System.getProperty(CANARY)).as("AST transform must not execute").isNull();
        assertThat(errors).as("script must fail validation").isNotEmpty();
    }

    @Test
    public void shouldRejectImportedAstTestAndNotExecuteIt() {
        ScriptObject script = groovy(
                "import groovy.transform.ASTTest\n" +
                "@ASTTest(value={ System.setProperty('" + CANARY + "', 'executed') })\n" +
                "class Pwn {}");

        List<ScriptError> errors = script.validate();

        assertThat(System.getProperty(CANARY)).as("AST transform must not execute").isNull();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void shouldRejectAliasedAstTestAndNotExecuteIt() {
        ScriptObject script = groovy(
                "import groovy.transform.ASTTest as Sneaky\n" +
                "@Sneaky(value={ System.setProperty('" + CANARY + "', 'executed') })\n" +
                "class Pwn {}");

        List<ScriptError> errors = script.validate();

        assertThat(System.getProperty(CANARY)).as("aliased AST transform must not execute").isNull();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void shouldRejectGrabAnnotation() {
        ScriptObject script = groovy(
                "@Grab(group='commons-lang', module='commons-lang', version='2.6')\n" +
                "import org.apache.commons.lang.StringUtils\n" +
                "class Pwn {}");

        List<ScriptError> errors = script.validate();

        assertThat(errors).as("@Grab must be rejected").isNotEmpty();
    }

    @Test
    public void shouldRejectAnnotationCollectorMetaAnnotationBypass() {
        // @AnnotationCollector lets a meta-annotation (alias name is arbitrary) bundle @ASTTest. At CONVERSION only
        // @AnnotationCollector and the alias are visible; @ASTTest materializes later when the collector expands.
        ScriptObject script = groovy(
                "import groovy.transform.AnnotationCollector\n" +
                "import groovy.transform.ASTTest\n" +
                "\n" +
                "@AnnotationCollector([ASTTest])\n" +
                "@interface Foo {}\n" +
                "\n" +
                "@Foo(value={ System.setProperty('" + CANARY + "', 'pwn') })\n" +
                "class Pwn {}");

        List<ScriptError> errors = script.validate();

        assertThat(System.getProperty(CANARY)).as("collector-hidden AST transform must not execute").isNull();
        assertThat(errors).as("collector bypass must fail validation").isNotEmpty();
    }

    @Test
    public void shouldRejectAliasedAnnotationCollectorBypass() {
        ScriptObject script = groovy(
                "import groovy.transform.AnnotationCollector as AC\n" +
                "import groovy.transform.ASTTest\n" +
                "\n" +
                "@AC([ASTTest])\n" +
                "@interface Foo {}\n" +
                "\n" +
                "@Foo(value={ System.setProperty('" + CANARY + "', 'pwn') })\n" +
                "class Pwn {}");

        List<ScriptError> errors = script.validate();

        assertThat(System.getProperty(CANARY)).as("aliased collector bypass must not execute").isNull();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void shouldStillAllowLibraryCollectorAnnotation() {
        // @Canonical is a library AnnotationCollector; using it does not place @AnnotationCollector in the script, so
        // it must remain valid (no false positive from blocking @AnnotationCollector).
        ScriptObject script = groovy(
                "@groovy.transform.Canonical\n" +
                "class Point { int x; int y }");

        List<ScriptError> errors = script.validate();

        assertThat(errors).as("library collector annotations must still validate").isEmpty();
    }

    @Test
    public void shouldStillAllowLegitimateFieldTransform() {
        // @Field is a benign local AST transform commonly used in server-side scripts and must keep working.
        ScriptObject script = groovy(
                "import groovy.transform.Field\n" +
                "@Field String greeting = 'hello'\n" +
                "greeting");

        List<ScriptError> errors = script.validate();

        assertThat(errors).as("legitimate @Field transform must still validate").isEmpty();
    }

    @Test
    public void shouldStillAllowOrdinaryScript() {
        ScriptObject script = groovy("def text = 'This is a valid script';");

        List<ScriptError> errors = script.validate();

        assertThat(errors).isEmpty();
    }

    private ScriptObject groovy(String source) {
        return new ScriptObject("test script", source, SupportedScriptingLanguage.GROOVY, null);
    }
}
