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

package org.forgerock.openam.oauth2.extensions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.guava.common.collect.Lists;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.guice.core.InjectorHolder;

/**
 * A class for retrieving extension filters, through the Java
 * {@link ServiceLoader} framework, that will be invoked as extension points in
 * the various flows.
 *
 * @since 13.0.0
 */
public class ExtensionFilterManager {

    private final Map<Class<?>, Collection<?>> cache = new HashMap<>();

    /**
     * Gets all of the registered filters of a particular class type.
     *
     * @param clazz The filter class.
     * @param <T> The filter class type.
     * @return A collection of filters.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Collection<T> getFilters(Class<T> clazz) {
        if (!cache.containsKey(clazz)) {
            ServiceLoader<T> extensionFilters = ServiceLoader.load(clazz);
            Set<T> filters = new TreeSet<>(Lists.newArrayList(extensionFilters));
            for (T filter : filters) {
                InjectorHolder.injectMembers(filter);
            }
            cache.put(clazz, filters);
            return filters;
        } else {
            return (Collection) cache.get(clazz);
        }
    }
}
