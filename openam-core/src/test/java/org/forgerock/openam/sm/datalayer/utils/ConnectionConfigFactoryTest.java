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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.utils;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.openam.sm.utils.ConfigurationValidator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConnectionConfigFactoryTest {

    private ConnectionConfig mockDataLayerConfig;
    private ConnectionConfig mockExternalCTSConfig;
    private ConfigurationValidator mockConfigurationValidator;
    private LdapDataLayerConfiguration mockDataLayerConfiguration;

    @BeforeMethod
    public void setup() {
        mockDataLayerConfig = mock(ConnectionConfig.class);
        mockExternalCTSConfig = mock(ConnectionConfig.class);
        mockConfigurationValidator = mock(ConfigurationValidator.class);
        mockDataLayerConfiguration = mock(LdapDataLayerConfiguration.class);
    }

    @Test
    public void shouldReturnConfigForDefaultStoreMode() throws InvalidConfigurationException {
        // given
        ConnectionConfigFactory factory = new ConnectionConfigFactory(
                mockDataLayerConfig, mockExternalCTSConfig, mockDataLayerConfiguration, mockConfigurationValidator);
        when(mockDataLayerConfiguration.getStoreMode()).thenReturn(StoreMode.DEFAULT);

        // when
        ConnectionConfig config = factory.getConfig();

        // then
        assertThat(config).isSameAs(mockDataLayerConfig);
    }

    @Test
    public void shouldReturnConfigForExternalStoreMode() throws InvalidConfigurationException {
        // given
        ConnectionConfigFactory factory = new ConnectionConfigFactory(
                mockDataLayerConfig, mockExternalCTSConfig, mockDataLayerConfiguration, mockConfigurationValidator);
        when(mockDataLayerConfiguration.getStoreMode()).thenReturn(StoreMode.EXTERNAL);

        // when
        ConnectionConfig config = factory.getConfig();

        // then
        assertThat(config).isSameAs(mockExternalCTSConfig);
    }
}