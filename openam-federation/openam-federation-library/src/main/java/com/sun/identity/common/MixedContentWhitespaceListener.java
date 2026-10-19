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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.xml.bind.Unmarshaller;

/**
 * Removes the whitespace-only text between the children of mixed-content types. The JAXB 3 runtime
 * keeps that text, where the JAXB 1 runtime dropped it, and a formatted marshaller indents around it
 * again, so every unmarshal and formatted marshal of the same XML would add blank lines.
 * <p>
 * Register only the types whose text is formatting. Once its types are registered, a listener can be
 * shared by unmarshallers in any thread.
 */
public final class MixedContentWhitespaceListener extends Unmarshaller.Listener {

    private final Map<Class<?>, Function<Object, List<Object>>> contentGetters = new HashMap<>();

    /**
     * Registers a mixed-content type.
     *
     * @param type the mixed-content type
     * @param content returns the content list of an instance of the type
     * @param <T> the mixed-content type
     * @return this listener
     */
    public <T> MixedContentWhitespaceListener add(Class<T> type, Function<T, List<Object>> content) {
        contentGetters.put(type, target -> content.apply(type.cast(target)));
        return this;
    }

    @Override
    public void afterUnmarshal(Object target, Object parent) {
        Function<Object, List<Object>> content = contentGetters.get(target.getClass());
        if (content != null) {
            content.apply(target).removeIf(o -> o instanceof String && ((String) o).trim().isEmpty());
        }
    }
}
