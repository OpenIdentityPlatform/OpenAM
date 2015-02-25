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

import org.forgerock.util.Reject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Rhino scope that is backed by the given ScriptContext instance. Variables are looked up in the given JSR 223
 * script context configured scopes in the scope order defined by the context.
 */
final class ScriptContextScope implements Scriptable {
    private static final int SCOPE_NOT_FOUND = -1;
    private final ScriptContext scriptContext;
    private Scriptable prototype;
    private Scriptable parentScope;

    /**
     * Constructs the scope backed by the given JSR 223 script context.
     *
     * @param scriptContext the JSR 223 script context. May not be null.
     */
    ScriptContextScope(final ScriptContext scriptContext) {
        Reject.ifNull(scriptContext);
        this.scriptContext = scriptContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassName() {
        return "ScriptContextScope";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(String name, Scriptable start) {
        return scriptContext.getAttributesScope(name) != SCOPE_NOT_FOUND;
    }

    /**
     * Indexed variables are not supported by this scope.
     */
    @Override
    public boolean has(final int index, final Scriptable start) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(String name, Scriptable start) {
        int scope = scriptContext.getAttributesScope(name);
        if (scope != SCOPE_NOT_FOUND) {
            final Object result = scriptContext.getAttribute(name, scope);
            return Context.javaToJS(result, this);
        } else {
            return NOT_FOUND;
        }
    }

    /**
     * Indexed variables are not supported by this scope.
     */
    @Override
    public Object get(final int index, final Scriptable start) {
        return NOT_FOUND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        int scope = scriptContext.getAttributesScope(name);
        if (scope == SCOPE_NOT_FOUND) {
            scope = ScriptContext.ENGINE_SCOPE;
        }
        if (value instanceof Wrapper) {
            value = ((Wrapper) value).unwrap();
        }
        scriptContext.setAttribute(name, value, scope);
    }

    /**
     * Indexed variables are not supported by this scope.
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value) {
        // Ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String name) {
        int scope = scriptContext.getAttributesScope(name);
        if (scope != SCOPE_NOT_FOUND) {
            scriptContext.removeAttribute(name, scope);
        }
    }

    /**
     * Indexed variables are not supported by this scope.
     */
    @Override
    public void delete(final int index) {
        // Ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrototype(final Scriptable prototype) {
        this.prototype = prototype;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getParentScope() {
        return parentScope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentScope(final Scriptable parent) {
        this.parentScope = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getIds() {
        final List<Object> keys = new ArrayList<Object>();

        for (int scope : scriptContext.getScopes()) {
            final Bindings bindings = scriptContext.getBindings(scope);
            keys.addAll(bindings.keySet());
        }

        return keys.toArray();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.mozilla.javascript.NativeObject#getDefaultValue(org.mozilla.javascript.Scriptable, Class)
     */
    @Override
    public Object getDefaultValue(final Class<?> hint) {
        return NativeObject.getDefaultValue(this, hint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInstance(final Scriptable instance) {
        Scriptable prototype = instance.getPrototype();
        while (prototype != null) {
            if (prototype.equals(this)) {
                return true;
            }
            prototype = prototype.getPrototype();
        }
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ScriptContextScope that = (ScriptContextScope) o;

        if (parentScope != null ? !parentScope.equals(that.parentScope) : that.parentScope != null) {
            return false;
        }
        if (prototype != null ? !prototype.equals(that.prototype) : that.prototype != null) {
            return false;
        }
        if (!scriptContext.equals(that.scriptContext)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = scriptContext.hashCode();
        result = 31 * result + (prototype != null ? prototype.hashCode() : 0);
        result = 31 * result + (parentScope != null ? parentScope.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScriptContextScope{" +
                "scriptContext=" + scriptContext +
                ", prototype=" + prototype +
                ", parentScope=" + parentScope +
                '}';
    }
}
