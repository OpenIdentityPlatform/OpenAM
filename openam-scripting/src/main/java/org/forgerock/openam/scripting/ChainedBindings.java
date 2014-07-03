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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom Bindings implementation that allows scopes to be chained from parent to child. Variables are looked up in
 * the current scope first, and then in the parent scope. Setting a variable always does so in the current scope.
 * Each scope bindings object is held as a reference to the original bindings objects passed to the constructor.
 * Therefore any changes made to a variable will be reflected in the current scope.
 * <p/>
 * Note: chained scopes effectively act as a linked list. Therefore performance for getting/setting variables will
 * decrease roughly linearly with the number of chained scopes (assuming constant access times within each individual
 * scope).
 */
public final class ChainedBindings implements Bindings {

    private final Bindings current;
    private final Bindings parent;

    /**
     * Constructs a chained bindings set with the given parent scope and current scope objects.
     * @param parent the parent scope.
     * @param current the current scope.
     */
    public ChainedBindings(final Bindings parent, final Bindings current) {
        Reject.ifNull(parent, current);
        this.parent = parent;
        this.current = current;
    }

    /**
     * Sets the given variable to the given value. The variable will be set in <em>both</em> the current scope and the
     * parent scope, in case both already had a binding for this variable.
     *
     * @param varName the name of the variable to set.
     * @param value the value to set the variable to.
     */
    public Object put(final String varName, final Object value) {
        final Object currentResult = current.put(varName, value);
        final Object parentResult = parent.put(varName, value);
        return currentResult != null ? currentResult : parentResult;
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends String, ? extends Object> map) {
        current.putAll(map);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        current.clear();
        parent.clear();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> keySet() {
        final Set<String> keys = new HashSet<String>(current.keySet());
        keys.addAll(parent.keySet());
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Object> values() {
        final List<Object> values = new ArrayList<Object>(current.values());
        values.addAll(parent.values());
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(current.entrySet());
        entries.addAll(parent.entrySet());
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return current.size() + parent.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return current.isEmpty() && parent.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(final Object o) {
        return current.containsKey(o) || parent.containsKey(o);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object o) {
        return current.containsValue(o) || parent.containsValue(o);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(final Object o) {
        Object result = current.get(o);
        if (result == null) {
            result = parent.get(o);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(final Object o) {
        Object result = current.remove(o);
        if (result == null) {
            result = parent.remove(o);
        }
        return result;
    }
}
