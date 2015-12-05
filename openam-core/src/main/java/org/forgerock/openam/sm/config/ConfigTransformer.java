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

import java.util.Set;

/**
 * Provides a custom transformation to the attribute values.
 * <p/>
 * The transformed type should be the same type as that of parameter being passed via the setter.
 *
 * @since 13.0.0
 */
public interface ConfigTransformer<T> {

    /**
     * Transforms the set of strings into the parameter type required by the setter.
     *
     * @param values
     *         attribute values
     *
     * @return expected parameter type
     */
    T transform(Set<String> values, Class<?> parameterType);

}
