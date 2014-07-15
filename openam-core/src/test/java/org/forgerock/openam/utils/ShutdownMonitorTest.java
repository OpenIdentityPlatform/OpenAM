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
package org.forgerock.openam.utils;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManagerWrapper;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

public class ShutdownMonitorTest {

    private ShutdownMonitor monitor;
    private ShutdownListener listener;

    @BeforeTest
    public void setup() {
        ShutdownManagerWrapper shutdownManagerWrapper = mock(ShutdownManagerWrapper.class);
        ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
        monitor = new ShutdownMonitor(shutdownManagerWrapper);
        verify(shutdownManagerWrapper).addShutdownListener(captor.capture());
        listener = captor.getValue();
    }

    @Test
    public void shouldNotSignalShutdownWhenNot() {
        assertThat(monitor.hasShutdown()).isFalse();
    }

    @Test
    public void shouldSignalShutdownWhenShutdown() {
        listener.shutdown();
        assertThat(monitor.hasShutdown()).isTrue();
    }
}
