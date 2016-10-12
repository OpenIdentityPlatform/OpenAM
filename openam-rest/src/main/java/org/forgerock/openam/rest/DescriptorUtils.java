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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import org.forgerock.api.models.Schema;
import org.forgerock.api.models.TranslateJsonSchema;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Utility methods for dealing with API Descriptors.
 */
public class DescriptorUtils {
    private DescriptorUtils() {
        // utils class
    }

    /**
     * Create a {@code Schema} from a JSON file held as a classpath resource relative to the given type. All
     * {@code i18n:} prefixed values are replaced with {@code LocalizableString} instances with the classloader for the
     * given type.
     *
     * @param resource The name of the resource, relative to the type.
     * @param relativeType The type to resolve from.
     * @return The {@code Schema} instance.
     */
    public static Schema fromResource(String resource, Class<?> relativeType) {
        return Schema.newBuilder().schema(
                JsonValueBuilder.fromResource(relativeType, resource)
                        .as(new TranslateJsonSchema(relativeType.getClassLoader()))).build();
    }
}
