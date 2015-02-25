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
package org.forgerock.openam.sm;

import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.openam.sm.utils.ConfigurationValidator;

import javax.inject.Inject;
import javax.inject.Named;

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
    private final StoreMode storeMode;
    private final ConfigurationValidator validator;

    /**
     * Guice initialised constructor.
     *
     * @param datalayerConfig Non null default configuration.
     * @param externalCTSConfig Non null External CTS configuration.
     * @param storeMode Required to indicate the mode of the CTS configuration.
     * @param validator Required for validation.
     */
    @Inject
    public ConnectionConfigFactory(@Named(DataLayerConstants.SERVICE_MANAGER_CONFIG) ConnectionConfig datalayerConfig,
                                   @Named(DataLayerConstants.EXTERNAL_CTS_CONFIG) ConnectionConfig externalCTSConfig,
                                   StoreMode storeMode,
                                   ConfigurationValidator validator) {
        this.smsConfiguration = datalayerConfig;
        this.externalTokenConfig = externalCTSConfig;
        this.storeMode = storeMode;
        this.validator = validator;
    }

    /**
     * Get an instance of the configuration to be used for generating a ConnectionPool.
     *
     * The connection configuration will be validated before returning to the caller
     * to ensure there are no obvious errors with the configuration.
     *
     * @param type Non null type of the connection required.
     * @return Non null configuration for that configuration type.
     *
     * @throws InvalidConfigurationException If there was a validation error with the configuration.
     */
    public ConnectionConfig getConfig(ConnectionType type) throws InvalidConfigurationException {
        ConnectionConfig configuration;
        switch (storeMode) {
            case DEFAULT:
                configuration = smsConfiguration;
                break;
            case EXTERNAL:
                if (type == ConnectionType.DATA_LAYER) {
                    configuration = smsConfiguration;
                } else {
                    configuration = externalTokenConfig;
                }
                break;
            default:
                throw new IllegalStateException();
        }
        validator.validate(configuration);
        return configuration;
    }
}
