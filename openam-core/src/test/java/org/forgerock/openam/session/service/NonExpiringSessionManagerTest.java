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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.session.service;

import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.service.NonExpiringSessionManager;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

public class NonExpiringSessionManagerTest {

    private NonExpiringSessionManager nonExpiringSessionManager;
    private Runnable runnable;
    @Mock private SessionAccessManager sessionAccessManager;
    @Mock private ScheduledExecutorService scheduledExecutorService;
    @Mock private ThreadMonitor threadMonitor;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockInternalSession;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doAnswer((new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                runnable = (Runnable) invocationOnMock.getArguments()[1];
                return null;
            }
        })).when(threadMonitor).watchScheduledThread(
                any(ScheduledExecutorService.class), any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        given(mockInternalSession.getID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);

        nonExpiringSessionManager = new NonExpiringSessionManager(sessionAccessManager, scheduledExecutorService, threadMonitor);
    }

    @Test
    public void shouldBeThreadMonitoredAndScheduled() {
        verify(threadMonitor).watchScheduledThread(scheduledExecutorService, runnable, 0, 5, TimeUnit.MINUTES);
    }

    @Test
    public void shouldSetUpSessionCorrectly() {
        nonExpiringSessionManager.addNonExpiringSession(mockInternalSession);
        verify(mockInternalSession).setMaxSessionTime(SessionConstants.NON_EXPIRING_SESSION_LENGTH_MINUTES);
        verify(mockInternalSession).setMaxIdleTime(5*10);
        verify(mockInternalSession).setLatestAccessTime();
    }

    @Test
    public void shouldUpdateSessionOnRefresh() {
        nonExpiringSessionManager.addNonExpiringSession(mockInternalSession);
        verify(mockInternalSession, times(1)).setLatestAccessTime();

        runnable.run();
        verify(mockInternalSession, times(2)).setLatestAccessTime();
    }

}
