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
package org.forgerock.openam.selfservice.config;

/**
 * Responsible for handling the retrieval and notification of self service console configuration.
 *
 * @since 13.0.0
 */
public interface ConsoleConfigHandler {

    /**
     * Retrieves the config for the requested realm.
     *
     * @param realm
     *         the realm
     *
     * @return associated config
     */
    <C extends ConsoleConfig> C getConfig(String realm, ConsoleConfigExtractor<C> extractor);

    /**
     * Registers the passed listener for config changes.
     *
     * @param listener
     *         the listener
     */
    void registerListener(ConsoleConfigChangeListener listener);

}
