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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.sm;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Set;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.openam.sm.utils.ConfigurationValidator;

/**
 * The factory used for acquiring the configuration used by the Service Manager data layer.
 *
 * The LDAP Connection configuration depends on a number of factors which this class
 * encapsulates:
 *
 * - What the caller intends to do with the connection.
 * - Which mode the CTS is in.
 */
public class ConnectionConfigFactory {
    private final ConnectionConfig externalTokenConfig;
    private final ConnectionConfig smsConfiguration;
    private final ConfigurationValidator validator;
    private final LdapDataLayerConfiguration dataLayerConfiguration;

    /**
     * Guice initialised constructor.
     *
     * @param datalayerConfig Non null default configuration.
     * @param externalTokenConfig Non null External CTS configuration.
     * @param validator Required for validation.
     */
    @Inject
    public ConnectionConfigFactory(@Named(DataLayerConstants.SERVICE_MANAGER_CONFIG) ConnectionConfig datalayerConfig,
            @Named(DataLayerConstants.EXTERNAL_CONFIG) ConnectionConfig externalTokenConfig,
            LdapDataLayerConfiguration dataLayerConfiguration,
            ConfigurationValidator validator) {
        this.smsConfiguration = datalayerConfig;
        this.externalTokenConfig = externalTokenConfig;
        this.validator = validator;
        this.dataLayerConfiguration = dataLayerConfiguration;
    }

    /**
     * Get an instance of the configuration to be used for generating a ConnectionPool.
     *
     * The connection configuration will be validated before returning to the caller
     * to ensure there are no obvious errors with the configuration.
     *
     * @param connectionType The connection type.
     *
     * @return Non null configuration for that configuration type.
     *
     * @throws InvalidConfigurationException If there was a validation error with the configuration.
     */
    public ConnectionConfig getConfig(ConnectionType connectionType) throws InvalidConfigurationException {
        ConnectionConfig configuration;
        switch (dataLayerConfiguration.getStoreMode()) {
            case DEFAULT:
                configuration = getDefaultConfiguration();
                break;
            case EXTERNAL:
                configuration = externalTokenConfig;
                break;
            default:
                throw new IllegalStateException();
        }
        if (isCtsWorkerConnectionType(connectionType)) {
            configuration = wrapCtsReaperConfiguration(configuration);
        }
        validator.validate(configuration);
        return configuration;
    }

    private boolean isCtsWorkerConnectionType(ConnectionType connectionType) {
        return ConnectionType.CTS_EXPIRY_DATE_WORKER.equals(connectionType)
                || ConnectionType.CTS_MAX_SESSION_TIMEOUT_WORKER.equals(connectionType)
                || ConnectionType.CTS_SESSION_IDLE_TIMEOUT_WORKER.equals(connectionType);
    }

    private ConnectionConfig getDefaultConfiguration() {
        return new DelegatingConnectionConfig(smsConfiguration) {
            @Override
            public int getMaxConnections() {
                int max = externalTokenConfig.getMaxConnections();
                if (max < 0) {
                    max = smsConfiguration.getMaxConnections();
                }
                return max;
            }
        };
    }

    private ConnectionConfig wrapCtsReaperConfiguration(ConnectionConfig configuration) {
        return new DelegatingConnectionConfig(configuration) {
            @Override
            public int getMaxConnections() {
                return 1;
            }

            @Override
            public boolean isAffinityEnabled() {
                return false;
            }
        };
    }

    private static abstract class DelegatingConnectionConfig implements ConnectionConfig {

        private final ConnectionConfig delegateConnectionConfig;

        private DelegatingConnectionConfig(ConnectionConfig delegateConnectionConfig) {
            this.delegateConnectionConfig = delegateConnectionConfig;
        }

        @Override
        public final Set<LDAPURL> getLDAPURLs() {
            return delegateConnectionConfig.getLDAPURLs();
        }

        @Override
        public final String getBindDN() {
            return delegateConnectionConfig.getBindDN();
        }

        @Override
        public final char[] getBindPassword() {
            return delegateConnectionConfig.getBindPassword();
        }

        @Override
        public final int getLdapHeartbeat() {
            return delegateConnectionConfig.getLdapHeartbeat();
        }

        @Override
        public boolean isAffinityEnabled() {
            return delegateConnectionConfig.isAffinityEnabled();
        }
    }
}
