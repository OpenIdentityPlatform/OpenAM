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

import org.forgerock.util.Reject;

import javax.script.Bindings;
import java.util.List;

/**
 * Representation of a script in some scripting language.
 *
 * NB: We use 'ScriptObject' to avoid clashes with the JSR 223 'Script' class.
 *
 * @since 12.0.0
 */
public final class ScriptObject {
    private final String name;
    private final String script;
    private final ScriptingLanguage language;
    private final Bindings bindings;

    /**
     * Constructs a script object with the given name, script body, language and variable bindings.
     *
     * @param name the name of the script.
     * @param script the script itself.
     * @param language the language that the script is written in.
     * @param bindings the bindings used for the script.
     */
    public ScriptObject(final String name, final String script, final ScriptingLanguage language,
                        final Bindings bindings) {
        Reject.ifNull(name, script, language);
        this.name = name;
        this.script = script;
        this.language = language;
        this.bindings = bindings;
    }

    /**
     * The name of the script.
     */
    public String getName() {
        return name;
    }

    /**
     * The contents of the script in the given scripting language.
     */
    public String getScript() {
        return script;
    }

    /**
     * The language that the script is written in.
     */
    public ScriptingLanguage getLanguage() {
        return language;
    }

    /**
     * Variable bindings that should be set when executing this script.
     */
    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScriptObject)) {
            return false;
        }

        final ScriptObject that = (ScriptObject) o;

        return !(bindings != null ? !bindings.equals(that.getBindings()) : that.getBindings() != null)
                && language.equals(that.getLanguage())
                && name.equals(that.getName())
                && script.equals(that.getScript());
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + script.hashCode();
        result = 31 * result + language.hashCode();
        result = 31 * result + (bindings != null ? bindings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScriptObject{name='" + name + '\'' + ", language=" + language + ", bindings=" + bindings + '}';
    }

    /**
     * Validate this script for the particular language rules and produces a list of
     * {@link ScriptError} instances if any validation errors occurred.
     *
     * @return a list of validation errors if validation failed and an empty list if validation passed.
     * @throws java.lang.NullPointerException if script is empty.
     */
    public List<ScriptError> validate() {
        return language.getScriptValidator().validateScript(this);
    }
}
