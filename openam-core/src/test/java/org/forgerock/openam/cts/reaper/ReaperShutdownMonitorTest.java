/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.reaper;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ScheduledFuture;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

public class ReaperShutdownMonitorTest {

    private ReaperShutdownMonitor monitor;
    private CoreGuiceModule.ShutdownManagerWrapper mockWrapper;
    private ShutdownListener listener;

    @BeforeMethod
    public void setUp() throws Exception {
        mockWrapper = mock(CoreGuiceModule.ShutdownManagerWrapper.class);
        ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
        doNothing().when(mockWrapper).addShutdownListener(captor.capture());

        monitor = new ReaperShutdownMonitor(mockWrapper, mock(Debug.class));
        listener = captor.getValue();
    }

    @Test
    public void shouldIndicateShutdownWhenListenerTriggered() {
        // Given
        monitor.setFuture(mock(ScheduledFuture.class));
        // When
        listener.shutdown();
        // Then
        assertThat(monitor.isShutdown()).isTrue();
    }

    @Test
    public void shouldCancelFutureWhenShutdown() {
        // Given
        ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        monitor.setFuture(mockFuture);
        // When
        listener.shutdown();
        // Then
        verify(mockFuture).cancel(anyBoolean());
    }

    @Test
    public void shouldMarkShutdownEvenIfFutureIsNull() {
        // Given
        monitor.setFuture(null);
        // When
        listener.shutdown();
        // Then
        assertThat(monitor.isShutdown()).isTrue();
    }
}
