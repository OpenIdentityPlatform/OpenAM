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
package org.forgerock.openam.sm.datalayer.providers;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class DataLayerConnectionFactoryCacheTest {

    private ConnectionFactory mockFactory;
    private ShutdownManager mockShutdown;
    private DataLayerConnectionFactoryCache provider;
    private ArgumentCaptor<ShutdownListener> listenerCaptor;
    private DataLayerConnectionFactoryProvider mockFactoryProvider;

    @BeforeMethod
    public void setup() throws InvalidConfigurationException {
        mockShutdown = mock(ShutdownManager.class);
        listenerCaptor = ArgumentCaptor.forClass(ShutdownListener.class);
        doNothing().when(mockShutdown).addShutdownListener(listenerCaptor.capture());

        mockFactory = mock(ConnectionFactory.class);
        mockFactoryProvider = mock(DataLayerConnectionFactoryProvider.class);
        given(mockFactoryProvider.createFactory(any(ConnectionType.class))).willReturn(mockFactory);

        provider = new DataLayerConnectionFactoryCache(mockShutdown, mockFactoryProvider, mock(Debug.class));
    }

    @Test
    public void shouldRespondToShutdownSignal() throws InvalidConfigurationException {
        // Given
        listenerCaptor.getValue().shutdown();

        // When
        IllegalStateException result = null;
        try {
            provider.createFactory(ConnectionType.DATA_LAYER);
        } catch (IllegalStateException e) {
            result = e;
        }

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    public void shouldShutdownFactoryDuringShutdown() throws InvalidConfigurationException {
        // Given
        provider.createFactory(ConnectionType.DATA_LAYER);

        // When
        listenerCaptor.getValue().shutdown();

        // Then
        verify(mockFactory).close();
    }

    @Test
    public void shouldOnlyCallCreateFactoryOncePerType() throws InvalidConfigurationException {
        // Given
        ConnectionType type = ConnectionType.DATA_LAYER;
        provider.createFactory(type);

        // When
        provider.createFactory(type);

        // Then
        verify(mockFactoryProvider, times(1)).createFactory(any(ConnectionType.class));
    }
}