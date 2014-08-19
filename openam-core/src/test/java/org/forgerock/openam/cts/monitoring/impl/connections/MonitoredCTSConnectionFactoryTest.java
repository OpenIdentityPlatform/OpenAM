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

import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;

public class MonitoredCTSConnectionFactoryTest {

    private CTSConnectionMonitoringStore monitoringStore;

    private ConnectionFactory connectionFactory;
    private MonitoredCTSConnectionFactory monitoredConnectionFactory;
    private WrappedHandlerFactory handlerFactory;

    @BeforeMethod
    public void setUp() {

        connectionFactory = mock(ConnectionFactory.class);
        monitoringStore = mock(CTSConnectionMonitoringStore.class);
        handlerFactory = mock(WrappedHandlerFactory.class);

        monitoredConnectionFactory = new MonitoredCTSConnectionFactory(connectionFactory, monitoringStore, handlerFactory);

    }

    @Test
    public void shouldAddToFailedConnectionOnError() throws ErrorResultException {
        //given
        boolean success = false;
        boolean errorCaught = false;

        doThrow(mock(ErrorResultException.class)).when(connectionFactory).getConnection();

        //when
        try {
            monitoredConnectionFactory.getConnection();
        } catch (ErrorResultException e) {
            errorCaught = true;
        }

        //then
        verify(monitoringStore).addConnection(success);
        assertTrue(errorCaught);
    }

    @Test
    public void shouldAddToSuccessfulConnection() throws ErrorResultException {

        //given
        boolean success = true;

        //when
        monitoredConnectionFactory.getConnection();

        //then
        verify(monitoringStore).addConnection(success);
    }

    @Test
    public void shouldWrapHandlerWhenCalledAsync() {
        //given
        ResultHandler resultHandler = mock(ResultHandler.class);
        ResultHandler wrappedHandler = mock(ResultHandler.class);

        given(handlerFactory.build(resultHandler)).willReturn(wrappedHandler);

        //when
        monitoredConnectionFactory.getConnectionAsync(resultHandler);

        //then
        verify(connectionFactory).getConnectionAsync(wrappedHandler);
    }

}
