package org.forgerock.openam.shared.guice;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SharedGuiceModuleTest {
    @Test
    public void shouldCreatePoolWithFactory() {
        ExecutorServiceFactory mockFactory = mock(ExecutorServiceFactory.class);
        new SharedGuiceModule().provideThreadMonitor(mockFactory, mock(ShutdownManager.class), mock(Debug.class));
        verify(mockFactory).createCachedThreadPool(anyString());
    }
}