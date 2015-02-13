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
package org.forgerock.openam.cts.impl.queue.config;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.anyObject;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class CTSQueueConfigurationTest {

    private CTSQueueConfiguration config;
    private ConnectionConfigFactory mockConfigFactory;
    private ConnectionCount connectionCount;
    private ConnectionConfig mockConfig;

    @BeforeMethod
    public void setup() throws InvalidConfigurationException {
        mockConfigFactory = mock(ConnectionConfigFactory.class);
        mockConfig = mock(ConnectionConfig.class);
        given(mockConfigFactory.getConfig()).willReturn(mockConfig);
        LdapDataLayerConfiguration dataLayerConfiguration = mock(LdapDataLayerConfiguration.class);
        given(dataLayerConfiguration.getStoreMode()).willReturn(StoreMode.DEFAULT);
        Map<ConnectionType, LdapDataLayerConfiguration> configMap = mock(Map.class);
        given(configMap.get(anyObject())).willReturn(dataLayerConfiguration);
        connectionCount = new ConnectionCount(configMap);
        config = new CTSQueueConfiguration(mockConfigFactory, connectionCount, mock(Debug.class));
    }

    @Test
    public void shouldReturnConnectionsFromConfiguration() throws Exception {
        int count = 20;
        given(mockConfig.getMaxConnections()).willReturn(count);
        int result = config.getProcessors();
        assertThat(result).isLessThan(count);
    }

    @Test
    public void shouldThrowExceptionIfConnectionsTooLow() throws Exception {
        given(mockConfig.getMaxConnections()).willReturn(1);
        DataLayerException result = null;
        try {
            config.getProcessors();
        } catch (DataLayerException e) {
            result = e;
        }
        assertThat(result).isNotNull();
    }
}