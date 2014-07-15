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
import org.forgerock.openam.cts.ExternalTokenConfig;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.ServerGroupConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CTSConnectionCountTest {

    private ServerGroupConfiguration mockServerConfig;
    private ExternalTokenConfig mockExternalConfig;
    private CTSConnectionCount count;

    @BeforeMethod
    public void setup() {
        mockServerConfig = mock(ServerGroupConfiguration.class);
        mockExternalConfig = mock(ExternalTokenConfig.class);
        count = new CTSConnectionCount(mockServerConfig, mockExternalConfig, mock(Debug.class));
    }

    @Test
    public void shouldUseServerConfigInDefaultMode() throws CoreTokenException {
        int internal = 10;
        given(mockExternalConfig.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.DEFAULT);
        given(mockServerConfig.getMaxConnections()).willReturn(internal);
        assertThat(count.getProcessorCount()).isEqualTo(5);
    }

    @Test
    public void shouldReturnExternalConnectionsInExternalMode() throws CoreTokenException {
        int external = 10;
        given(mockExternalConfig.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternalConfig.getMaxConnections()).willReturn(Integer.toString(external));
        assertThat(count.getProcessorCount()).isEqualTo(external);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldFailIfExternalMaxConnectionsIsNotANumber() throws CoreTokenException {
        given(mockExternalConfig.getStoreMode()).willReturn(ExternalTokenConfig.StoreMode.EXTERNAL);
        given(mockExternalConfig.getMaxConnections()).willReturn("hobbits");
        count.getProcessorCount();
    }
}