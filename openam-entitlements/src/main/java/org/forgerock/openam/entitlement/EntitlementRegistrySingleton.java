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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement;

import org.forgerock.guice.core.InjectorHolder;

/**
 * Singleton holder for the {@link EntitlementRegistry} instance.
 *
 * @since 12.0.0
 */
public enum EntitlementRegistrySingleton {

    /**
     * Singleton instance.
     */
    INSTANCE;

    private EntitlementRegistry registry;

    /**
     * Gets the {@link EntitlementRegistry} instance.
     *
     * @return The instance of the {@code EntitlementRegistry}.
     */
    public EntitlementRegistry getRegistry() {
        if (registry == null) {
            return InjectorHolder.getInstance(EntitlementRegistry.class);
        } else {
            return registry;
        }
    }

    /**
     * <p>Sets the {@link EntitlementRegistry} instance.</p>
     *
     * <p>Used by tests to avoid instantiating Guice.</p>
     *
     * @param registry The {@code EntitlementRegistry} instance.
     */
    public void setRegistry(EntitlementRegistry registry) {
        this.registry = registry.load();
    }
}
