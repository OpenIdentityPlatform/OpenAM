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
package org.forgerock.openam.cts.monitoring.impl.connections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.verifyZeroInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.util.promise.PromiseImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MonitoredCTSConnectionFactoryTest {

    private CTSConnectionMonitoringStore monitoringStore;

    private ConnectionFactory<Object> connectionFactory;
    private MonitoredCTSConnectionFactory monitoredConnectionFactory;

    @BeforeMethod
    public void setUp() {

        connectionFactory = mock(ConnectionFactory.class);
        monitoringStore = mock(CTSConnectionMonitoringStore.class);

        monitoredConnectionFactory = new MonitoredCTSConnectionFactory(connectionFactory, monitoringStore);

    }

    @Test
    public void shouldAddToFailedConnectionOnError() throws Exception {
        //given
        doThrow(Exception.class).when(connectionFactory).create();

        //when
        try {
            monitoredConnectionFactory.create();
            fail("Should throw exception");
        } catch (Exception e) {
            // expected
        }

        //then
        verify(monitoringStore).addConnection(false);
    }

    @Test
    public void shouldAddToSuccessfulConnection() throws Exception {

        //given
        boolean success = true;

        //when
        monitoredConnectionFactory.create();

        //then
        verify(monitoringStore).addConnection(success);
    }

    @Test
    public void shouldWrapHandlerWhenCalledAsync() {
        //given
        PromiseImpl<Object, DataLayerException> promise = PromiseImpl.create();
        given(connectionFactory.createAsync()).willReturn(promise);

        //when
        monitoredConnectionFactory.createAsync();

        //then
        verify(connectionFactory).createAsync();
        verifyZeroInteractions(monitoringStore);
        promise.handleError(new DataLayerException("reason"));
        verify(monitoringStore).addConnection(false);
    }

}
