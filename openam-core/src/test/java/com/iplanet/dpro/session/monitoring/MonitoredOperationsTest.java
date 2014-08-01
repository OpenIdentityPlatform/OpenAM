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
package com.iplanet.dpro.session.monitoring;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.operations.SessionOperations;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Verifies that each monitored operation calls the "real" operation along with
 * a check to set a value in the monitoring store.
 */
public class MonitoredOperationsTest {

    private MonitoredOperations testMoniteredOperations;

    private SessionOperations mockSessionOperations;
    private SessionMonitorType type;
    private SessionMonitoringStore mockStore;

    @BeforeTest
    public void setup() {
        mockSessionOperations = mock(SessionOperations.class);
        type = SessionMonitorType.LOCAL;
        mockStore = mock(SessionMonitoringStore.class);

        testMoniteredOperations = new MonitoredOperations(mockSessionOperations, type, mockStore);
    }

    @Test
    public void refreshTest() throws SessionException {
        //given
        Session mockSession = mock(Session.class);
        boolean reset = true;

        //when
        testMoniteredOperations.refresh(mockSession, reset);

        //then
        verify(mockSessionOperations).refresh(mockSession, reset);
        verify(mockStore).storeRefreshTime(anyLong(), any(SessionMonitorType.class));
    }

    @Test
    public void destroyTest() throws SessionException {
        //given
        Session mockRequester = mock(Session.class);
        Session mockSession = mock(Session.class);

        //when
        testMoniteredOperations.destroy(mockRequester, mockSession);

        //then

        verify(mockSessionOperations, times(1)).destroy(mockRequester, mockSession);
        verify(mockStore).storeDestroyTime(anyLong(), any(SessionMonitorType.class));
    }

    @Test
    public void logoutTest() throws SessionException {
        //given
        Session mockSession = mock(Session.class);

        //when
        testMoniteredOperations.logout(mockSession);

        //then

        verify(mockSessionOperations, times(1)).logout(mockSession);
        verify(mockStore).storeLogoutTime(anyLong(), any(SessionMonitorType.class));

    }

    @Test
    public void setPropertyTest() throws SessionException {
        //given
        Session mockSession = mock(Session.class);
        String name = "name";
        String value = "value";

        //when
        testMoniteredOperations.setProperty(mockSession, name, value);

        //then
        verify(mockSessionOperations, times(1)).setProperty(mockSession, name, value);
        verify(mockStore).storeSetPropertyTime(anyLong(), any(SessionMonitorType.class));
    }

}
