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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.utils.indextree.nodecontext;

/**
 * Search context provides a shared data space for a given search.
 * This allows tree nodes to share information.
 *
 * @author andrew.forrest@forgerock.com
 */
public interface SearchContext {

    /**
     * Add data to the context.
     *
     * @param key
     *         The key.
     * @param value
     *         The value.
     * @param <T>
     *         The value type.
     */
    public <T> void add(ContextKey<T> key, T value);

    /**
     * Get data from the context.
     *
     * @param key
     *         The key for which to extract data.
     * @param <T>
     *         The value type.
     * @return The value associated with the key.
     */
    public <T> T get(ContextKey<T> key);

    /**
     * Whether the context contains data with the associated with the key.
     *
     * @param key
     *         The key.
     * @return Whether data exists for the given key.
     */
    public boolean has(ContextKey<?> key);

    /**
     * Removes data from the context.
     *
     * @param key
     *         The key.
     */
    public void remove(ContextKey<?> key);

}
