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
package org.forgerock.openam.cts.impl.query.reaper;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.fail;

public class ReaperConnectionTest {

    private ConnectionFactory mockFactory;
    private ReaperImpl mockImpl;
    private ReaperConnection connection;
    private Connection mockConnection;

    @BeforeMethod
    public void setup() {
        mockFactory = mock(ConnectionFactory.class);
        mockImpl = mock(ReaperImpl.class);
        connection = new ReaperConnection(mockFactory, mockImpl);

        mockConnection = mock(Connection.class);
    }

    @AfterMethod
    public void teardown() {
        // Clear interrupt status
        Thread.interrupted();
    }

    @Test
    public void shouldRespondToInterrupt() throws CoreTokenException {
        Thread.currentThread().interrupt();
        connection.nextPage();
        verify(mockImpl, times(0)).nextPage();
    }

    @Test
    public void shouldOpenConnectionOnFirstCall() throws CoreTokenException, ErrorResultException {
        given(mockFactory.getConnection()).willReturn(mockConnection);
        connection.nextPage();
        verify(mockImpl).setConnection(eq(mockConnection));
    }

    @Test
    public void shouldCloseConnectionOnLastCall() throws CoreTokenException, ErrorResultException {
        given(mockFactory.getConnection()).willReturn(mockConnection);
        given(mockImpl.nextPage()).willReturn(null);
        connection.nextPage();
        verify(mockConnection).close();
    }

    @Test
    public void shouldCloseConnectionOnException() throws CoreTokenException, ErrorResultException {
        given(mockFactory.getConnection()).willReturn(mockConnection);
        given(mockImpl.nextPage()).willThrow(new CoreTokenException(""));
        try {
            connection.nextPage();
            fail();
        } catch (CoreTokenException e) {
            verify(mockConnection).close();
        }
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotReturnFurtherPagesOnceFailed() throws CoreTokenException, ErrorResultException {
        given(mockFactory.getConnection()).willReturn(mockConnection);
        given(mockImpl.nextPage()).willThrow(new CoreTokenException(""));
        try {
            connection.nextPage();
        } catch (CoreTokenException e) {}
        connection.nextPage();
    }
}