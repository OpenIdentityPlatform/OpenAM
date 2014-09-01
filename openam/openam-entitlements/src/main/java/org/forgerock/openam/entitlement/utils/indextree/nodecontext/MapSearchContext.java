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

import java.util.HashMap;
import java.util.Map;

/**
 * Map backed implementation of the SearchContext.
 *
 * @author andrew.forrest@forgerock.com
 */
public class MapSearchContext implements SearchContext {

    private final Map<ContextKey<?>, Object> contextData;

    public MapSearchContext() {
        contextData = new HashMap<ContextKey<?>, Object>();
    }

    @Override
    public <T> void add(ContextKey<T> key, T value) {
        contextData.put(key, value);
    }

    @Override
    public <T> T get(ContextKey<T> key) {
        // Type safety is enforced by the use of the typed key.
        return key.getType().cast(contextData.get(key));
    }

    @Override
    public boolean has(ContextKey<?> key) {
        return contextData.containsKey(key);
    }

    @Override
    public void remove(ContextKey<?> key) {
        contextData.remove(key);
    }

}
