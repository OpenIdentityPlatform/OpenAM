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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.utils;

import org.forgerock.util.Reject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility collection utility which maintains a the mappings for any given value.
 * At any point, the value can only be mapped to a single key.
 *
 * This class makes an implicit contract with the caller by maintaining only one key
 * mapped to a given value. If the caller changes which key should map to the value,
 * they do not need clean up the previous mapping, this class will perform this task
 * for them. This should reduce memory leak to orphaned key/value mappings.
 *
 * Thread Safety: Uses ConcurrentHashMap and limited synchronized to ensure thread-safety
 * and atomicity.
 *
 * Type K is the first value.
 * Type V is the second value.
 */
public class SingleValueMapper<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();
    private final ConcurrentHashMap<V, K> reverse = new ConcurrentHashMap<V, K>();

    /**
     * @param k The key to fetch a value for. May not be null.
     * @return A possibly null value for the given key.
     */
    public V get(K k) {
        Reject.ifNull(k);
        return map.get(k);
    }

    /**
     * @param v The value to get the key for. May not be null.
     * @return A possibly null key for the given value.
     */
    public K getValue(V v) {
        Reject.ifNull(v);
        return reverse.get(v);
    }

    /**
     * Store a mapping of key to value.
     *
     * Note: If there was a previous mapping for value, this will be cleaned up.
     *
     * Synchronized: to ensure both maps are updated in an atomic way.
     *
     * @param k The key to map against the value. May not be null.
     * @param v The value to map against the key. May not be null.
     */
    public synchronized void put(K k, V v) {
        Reject.ifNull(k, v);
        // Clean up previous mapping if present.
        K previousValue = reverse.get(v);
        if (previousValue != null) {
            map.remove(previousValue);
        }

        map.put(k, v);
        reverse.put(v, k);
    }

    /**
     * Removes the named key from the map.
     *
     * Synchronized: to ensure both maps are updated in an atomic way.
     *
     * @param k The key to remove the mapping for. May not be null.
     * @return A non null mapped value.
     */
    public synchronized V remove(K k) {
        Reject.ifNull(k);
        V remove = map.remove(k);
        reverse.remove(remove);
        return remove;
    }
}
