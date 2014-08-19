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
package org.forgerock.openam.cts.impl.queue.config;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class QueueConfigurationTest {

    private QueueConfiguration config;
    private ConnectionConfigFactory mockConfigFactory;
    private ConnectionCount connectionCount;
    private ConnectionConfig mockConfig;

    @BeforeMethod
    public void setup() throws InvalidConfigurationException {
        mockConfigFactory = mock(ConnectionConfigFactory.class);
        mockConfig = mock(ConnectionConfig.class);
        given(mockConfigFactory.getConfig(any(ConnectionType.class))).willReturn(mockConfig);

        connectionCount = new ConnectionCount(StoreMode.DEFAULT);
        config = new QueueConfiguration(mockConfigFactory, connectionCount, mock(Debug.class));
    }

    @Test
    public void shouldReturnConnectionsFromConfiguration() throws CoreTokenException {
        int count = 20;
        given(mockConfig.getMaxConnections()).willReturn(count);
        int result = config.getProcessors();
        assertThat(result).isLessThan(count);
    }

    @Test
    public void shouldThrowExceptionIfConnectionsTooLow() throws CoreTokenException {
        given(mockConfig.getMaxConnections()).willReturn(1);
        CoreTokenException result = null;
        try {
            config.getProcessors();
        } catch (CoreTokenException e) {
            result = e;
        }
        assertThat(result).isNotNull();
    }
}