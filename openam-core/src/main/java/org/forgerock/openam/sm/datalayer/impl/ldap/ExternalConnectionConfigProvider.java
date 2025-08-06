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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.sm.datalayer.impl.ldap;

import jakarta.inject.Inject;

import org.forgerock.openam.sm.ConnectionConfig;

import com.google.inject.Provider;

/**
 * A guice provider for external connection config that is provided by an {@link LdapDataLayerConfiguration}
 * instance.
 */
public final class ExternalConnectionConfigProvider implements Provider<ConnectionConfig> {
    private final LdapDataLayerConfiguration configuration;
    private final ExternalLdapConfig externalConfig;

    @Inject
    public ExternalConnectionConfigProvider(ExternalLdapConfig externalConfig, LdapDataLayerConfiguration configuration) {
        this.externalConfig = externalConfig;
        this.configuration = configuration;
    }

    public ConnectionConfig get() {
        externalConfig.update(configuration);
        return externalConfig;
    }
}
