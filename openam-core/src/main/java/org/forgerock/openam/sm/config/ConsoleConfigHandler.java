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

/**
 * Responsible for handling the retrieval and notification of console configuration.
 *
 * @since 13.0.0
 */
public interface ConsoleConfigHandler {

    /**
     * Gets a config bean for the passed realm using the builder.
     *
     * @param realm
     *         realm to retrieve config from
     * @param builderType
     *         console config builder type
     * @param <C>
     *         console config type
     *
     * @return new console config instance
     */
    <C> C getConfig(String realm, Class<? extends ConsoleConfigBuilder<C>> builderType);

    /**
     * Registers the passed listener for config changes associated with the passed builder type.
     *
     * @param listener
     *         the listener
     * @param builderType
     *         builder type
     */
    void registerListener(ConsoleConfigListener listener, Class<? extends ConsoleConfigBuilder<?>> builderType);

}
