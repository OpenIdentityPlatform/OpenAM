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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionMonitoringStoreTest {

    private SessionMonitoringTimingStoreFactory mockFactory;
    private ExecutorService service = new CallerRunsExecutor();
    private SessionMonitoringStore testSessionMonitoringStore;

    private ConcurrentHashMap refreshStore = mock(ConcurrentHashMap.class);
    private ConcurrentHashMap destroyStore = mock(ConcurrentHashMap.class);
    private ConcurrentHashMap logoutStore = mock(ConcurrentHashMap.class);
    private ConcurrentHashMap propertyStore = mock(ConcurrentHashMap.class);

    @BeforeMethod
    public void setUp() {
        mockFactory = mock(SessionMonitoringTimingStoreFactory.class);
        testSessionMonitoringStore = new SessionMonitoringStore(service, mockFactory, refreshStore,
                propertyStore, destroyStore, logoutStore);
    }

    @Test
    public void testStoreRefreshTimeCreatesEntryIfNoneExists() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(refreshStore.containsKey(type)).willReturn(false);
        given(refreshStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeRefreshTime(duration, type);

        //then
        verify(mockFactory, times(1)).createSessionMonitoringTimingStore();
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }

    @Test
    public void testStoreRefreshTimeCreatesEntry() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(refreshStore.containsKey(type)).willReturn(true);
        given(refreshStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeRefreshTime(duration, type);

        //then
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }


    @Test
    public void testStoreDestroyTimeCreatesEntryIfNoneExists() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(destroyStore.containsKey(type)).willReturn(false);
        given(destroyStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeDestroyTime(duration, type);

        //then
        verify(mockFactory, times(1)).createSessionMonitoringTimingStore();
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }

    @Test
    public void testStoreDestroyTimeCreatesEntry() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(destroyStore.containsKey(type)).willReturn(true);
        given(destroyStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeDestroyTime(duration, type);

        //then
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }


    @Test
    public void testStorePropertyTimeCreatesEntryIfNoneExists() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(propertyStore.containsKey(type)).willReturn(false);
        given(propertyStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeSetPropertyTime(duration, type);

        //then
        verify(mockFactory, times(1)).createSessionMonitoringTimingStore();
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }

    @Test
    public void testStorePropertyTimeCreatesEntry() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(propertyStore.containsKey(type)).willReturn(true);
        given(propertyStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeSetPropertyTime(duration, type);

        //then
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }


    @Test
    public void testStoreLogoutTimeCreatesEntryIfNoneExists() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(logoutStore.containsKey(type)).willReturn(false);
        given(logoutStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeLogoutTime(duration, type);

        //then
        verify(mockFactory, times(1)).createSessionMonitoringTimingStore();
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }

    @Test
    public void testStoreLogoutTimeCreatesEntry() {
        //given
        long duration = 0L;
        SessionMonitorType type = SessionMonitorType.LOCAL;

        SessionMonitoringTimingStore timingStore = mock(SessionMonitoringTimingStore.class);

        given(logoutStore.containsKey(type)).willReturn(true);
        given(logoutStore.get(type)).willReturn(timingStore);

        //when
        testSessionMonitoringStore.storeLogoutTime(duration, type);

        //then
        verify(timingStore, times(1)).addTimingEntry(anyLong());
    }

    // Executor that runs everything in the calling thread without a pool
    private class CallerRunsExecutor extends AbstractExecutorService {

        private volatile boolean shutdown;

        public void shutdown() {
            shutdown = true;
        }

        public List<Runnable> shutdownNow() {
            return null;
        }

        public boolean isShutdown() {
            return shutdown;
        }

        public boolean isTerminated() {
            return shutdown;
        }

        public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void execute(Runnable runnable) {
            runnable.run();
        }

    }

}
