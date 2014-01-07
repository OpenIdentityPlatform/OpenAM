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

import javax.naming.NamingException;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WrappedHandlerFactoryTest {


    private CTSConnectionMonitoringStore monitoringStore;
    private WrappedHandlerFactory handlerFactory;

    @BeforeMethod
    public void setUp() {

        monitoringStore = mock(CTSConnectionMonitoringStore.class);
        handlerFactory = new WrappedHandlerFactory(monitoringStore);

    }

    @Test
    public void testFailureCallsAddConnection() throws NamingException {
        //given
        ResultHandler<Object> resultHandler = mock(ResultHandler.class);
        ErrorResultException error = mock(ErrorResultException.class);

        //when
        ResultHandler wrapped = handlerFactory.build(resultHandler);
        wrapped.handleErrorResult(error);

        //then
        verify(monitoringStore).addConnection(false);
    }

    @Test
    public void testSuccessCallsAddConnection() throws NamingException, ErrorResultException {
        //given
        ResultHandler<Object> resultHandler = mock(ResultHandler.class);
        Connection conn = mock(Connection.class);

        //when
        ResultHandler wrapped = handlerFactory.build(resultHandler);
        wrapped.handleResult(conn);

        //then
        verify(monitoringStore).addConnection(true);
    }

}
