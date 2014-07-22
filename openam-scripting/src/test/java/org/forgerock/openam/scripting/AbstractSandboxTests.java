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

import org.mozilla.javascript.Wrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.fail;

/**
 * Tests that verify that the sandbox is functioning correctly. This test suite is abstract and expects individual
 * language implementations to sub-class and provide the script engine. It further assumes that each script engine
 * supports basic Java-style syntax for constructing objects and calling methods (true for Javascript and Groovy).
 * <p/>
 * <strong>Note:</strong> just because these tests pass, does not mean the sandbox is watertight! For example, if you
 * white-list the java.lang.reflect.* classes then script authors have pretty much a free hand. So either only
 * white-list exactly those classes that the script should have access to (and never white-list reflection or
 * java.lang.Class) or run OpenAM with a SecurityManager enabled and an appropriate security policy in place.
 */
public abstract class AbstractSandboxTests {

    private ScriptEngine scriptEngine;

    @BeforeMethod
    public void setupEngine() {
        StandardScriptEngineManager scriptEngineManager = new StandardScriptEngineManager();
        // Set up very permissive whitelist, and then blacklist our one class
        scriptEngineManager.setConfiguration(ScriptEngineConfiguration.builder()
                .withWhiteList(Arrays.asList(Pattern.compile(".*")))
                .withBlackList(Arrays.asList(Pattern.compile(Pattern.quote(ForbiddenFruit.class.getName()))))
                .build());

        scriptEngine = getEngine(scriptEngineManager);
    }

    protected abstract ScriptEngine getEngine(ScriptEngineManager manager);

    /**
     * Convenience wrapper that constructs a program from the given lines of code and then evaluates it in the sandbox.
     */
    @SuppressWarnings("unchecked")
    private <T> T eval(Bindings bindings, String...script) throws ScriptException {
        Object result = scriptEngine.eval(scriptEngine.getFactory().getProgram(script), bindings);
        while (result instanceof Wrapper) {
            result = ((Wrapper) result).unwrap();
        }
        return (T) result;
    }

    private <T> T eval(String...script) throws ScriptException {
        return eval(new SimpleBindings(), script);
    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldNotBeAbleToInstantiateBlackListedClasses() throws Exception {
        eval("new " + ForbiddenFruit.class.getName() + "();");
    }

    @Test
    public void shouldBeAbleToInstantiateWhiteListedClasses() throws Exception {
        Allowed result = eval("new " + Allowed.class.getName() + "();");
        assertThat(result).isNotNull();
    }

    @Test
    public void shouldBeAllowedToAccessWhiteListedInstance() throws Exception {
        // Given
        Allowed allowed = new Allowed();
        Bindings bindings = new SimpleBindings();
        bindings.put("allowed", allowed);

        // When
        eval(bindings, "allowed.setDirty()");

        // Then
        assertThat(allowed.dirty).isTrue();
    }

    @Test
    public void shouldNotBeAllowedToAccessBlackListedMember() throws Exception {
        // Given
        Allowed allowed = new Allowed();
        Bindings bindings = new SimpleBindings();
        bindings.put("allowed", allowed);

        // When
        try {
            eval(bindings, "allowed.forbiddenFruit.setDirty()");
            fail("Sandbox failed to protect access to black-listed member");
        } catch (ScriptException ex) {
            // Then
            assertThat(allowed.fruit.dirty).isFalse();
        }
    }

    @Test(expectedExceptions = ScriptException.class)
    public void shouldPreventInstantiationViaReflection() throws Exception {
        eval("java.lang.Class.forName('" + ForbiddenFruit.class.getName() + "').newInstance();");
    }

    @Test
    public void shouldPreventCallingStaticMethodsOnForbiddenClasses() throws Exception {
        try {
            eval(ForbiddenFruit.class.getName() + ".dangerous();");
            fail("Static method calls to black-listed classes should be forbidden.");
        } catch (ScriptException ex) {
            assertThat(ForbiddenFruit.danger).isFalse();
        }
    }

    public static class Allowed {
        private final ForbiddenFruit fruit = new ForbiddenFruit();
        private boolean dirty = false;

        public ForbiddenFruit getForbiddenFruit() {
            return fruit;
        }

        public void setDirty() {
            dirty = true;
        }
    }

    public static class ForbiddenFruit {
        private static boolean danger = false;
        private boolean dirty = false;

        public void setDirty() {
            dirty = true;
        }

        public static void dangerous() {
            danger = true;
        }
    }
}
