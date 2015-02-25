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
package org.forgerock.openam.cts.monitoring.impl.connections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConnectionStoreTest {


    private ConnectionStore connectionStore;
    private ConnectionMonitor failureMonitor;
    private ConnectionMonitor successMonitor;

    @BeforeMethod
    public void setUp() {

        failureMonitor = mock(ConnectionMonitor.class);
        successMonitor = mock(ConnectionMonitor.class);

        connectionStore = new ConnectionStore(successMonitor, failureMonitor);
    }

    @Test
    public void shouldAddOperation() {

        //Given
        boolean connectionSuccess = true;

        //When
        connectionStore.addConnection(connectionSuccess);

        //Then
        verify(successMonitor).add();
    }

    @Test
    public void shouldGetMinimumRate() {

        //Given
        boolean connectionSuccess = true;

        //When
        connectionStore.getMinimumOperationsPerPeriod(connectionSuccess);

        //Then
        verify(successMonitor).getMinimumRate();
    }

    @Test
    public void shouldGetMaximumRate() {

        //Given
        boolean connectionSuccess = true;

        //When
        connectionStore.getMaximumOperationsPerPeriod(connectionSuccess);

        //Then
        verify(successMonitor).getMaximumRate();
    }

    @Test
    public void shouldGetAverageRate() {

        //Given
        boolean connectionSuccess = true;

        //When
        connectionStore.getAverageConnectionsPerPeriod(connectionSuccess);

        //Then
        verify(successMonitor).getAverageRate();
    }

}
