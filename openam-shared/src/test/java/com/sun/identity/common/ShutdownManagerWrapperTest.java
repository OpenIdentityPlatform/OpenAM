package com.sun.identity.common;

import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;

public class ShutdownManagerWrapperTest {
    @Test
    public void shouldLockAndReleaseManager() {
        // Given
        ShutdownManager mockManager = mock(ShutdownManager.class);
        given(mockManager.acquireValidLock()).willReturn(true);

        // When
        new ShutdownManagerWrapper(mockManager).addShutdownListener(mock(ShutdownListener.class));

        // Then
        verify(mockManager).acquireValidLock();
        verify(mockManager).releaseLockAndNotify();
    }
}