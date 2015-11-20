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

package org.forgerock.openam.sm.config;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides the default transformations if a specific transformation class is not defined.
 * <p/>
 * Default transformations handles common primitive types and set and list. In the case
 * of set and list, the expectation is that the parameter type is a set or list of strings;
 * anything other will need a custom transformation.
 *
 * @since 13.0.0
 */
@Singleton
final class DefaultConfigTransformer implements ConfigTransformer<Object> {

    @Override
    public Object transform(Set<String> values, Class<?> parameterType) {
        if (parameterType == Set.class) {
            // Making the assumption that the type is Set<String>.
            return values;
        }
        if (parameterType == List.class) {
            // Making the assumption that the type is List<String>.
            return new ArrayList<>(values);
        }

        String value = values.iterator().next();

        if (parameterType == String.class) {
            return value;
        }
        if (parameterType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (parameterType == int.class) {
            return Integer.parseInt(value);
        }
        if (parameterType == long.class) {
            return Long.parseLong(value);
        }
        if (parameterType == double.class) {
            return Double.parseDouble(value);
        }

        throw new IllegalArgumentException("Unsupported parameter type " + parameterType);
    }

}
