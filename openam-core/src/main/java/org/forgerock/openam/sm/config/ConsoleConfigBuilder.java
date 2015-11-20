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

import java.util.Map;
import java.util.Set;

/**
 * Builder to construct the config object represented by {@link C}.
 * <p/>
 * The builder needs to declare which service configuration sources to pull on using
 * {@link ConfigSource} and each setter needs to declare which attribute to be
 * populated with using {@link ConfigAttribute}.
 *
 * @param <C>
 *         Config object type
 *
 * @since 13.0.0
 */
public interface ConsoleConfigBuilder<C> {

    /**
     * Builds a new instance of {@link C}.
     *
     * @param attributes
     *         all retrieved attributes
     *
     * @return new instance of {@link C}
     */
    C build(Map<String, Set<String>> attributes);

}
