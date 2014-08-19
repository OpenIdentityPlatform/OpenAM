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
package org.forgerock.openam.sm.utils;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;

/**
 * Responsible for validating {@link ConnectionConfig} instances.
 */
public class ConfigurationValidator {
    /**
     * Indicates that the configuration passes a range of validation requirements
     * and should be considered appropriate for use by a Connection Pool.
     *
     * @throws InvalidConfigurationException If there was a problem with the configuration,
     * the detail of the exception will indicate the cause.
     */
    public void validate(ConnectionConfig config) throws InvalidConfigurationException {
        if (config.getLDAPURLs() == null || config.getLDAPURLs().isEmpty()) {
            throw new InvalidConfigurationException("Invalid host URL.");
        }

        for (LDAPURL url : config.getLDAPURLs()) {
            if (url.getPort() < 0 || url.getPort() > 65535) {
                throw new InvalidConfigurationException("Port outside of valid range.");
            }
        }

        if (config.getBindDN() == null) {
            throw new InvalidConfigurationException("Bind DN missing");
        }

        if (config.getBindPassword() == null) {
            throw new InvalidConfigurationException("Bind Password missing");
        }

        if (config.getMaxConnections() < 0) {
            throw new InvalidConfigurationException("Invalid maximum connections");
        }
    }
}
