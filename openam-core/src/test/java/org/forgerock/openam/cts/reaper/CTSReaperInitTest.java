package org.forgerock.openam.cts.reaper;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CTSReaperInitTest {

    private ThreadMonitor mockMonitor;
    private CTSReaperInit ctsReaperInit;

    @BeforeMethod
    public void setup() {
        mockMonitor = mock(ThreadMonitor.class);
        ctsReaperInit = new CTSReaperInit(
                mock(CTSReaper.class),
                mockMonitor,
                mock(CoreTokenConfig.class),
                mock(ScheduledExecutorService.class),
                mock(Debug.class));
    }

    @Test
    public void shouldUseMonitorToStartReaper() {
        ctsReaperInit.startReaper();
        verify(mockMonitor).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSubsequentStarts() {
        ctsReaperInit.startReaper();
        ctsReaperInit.startReaper();
    }
}