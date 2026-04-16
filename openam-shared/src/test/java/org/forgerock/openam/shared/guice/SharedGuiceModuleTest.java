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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.shared.guice;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class SharedGuiceModuleTest {
    @Test
    public void shouldCreatePoolWithFactory() {
        AMExecutorServiceFactory mockFactory = mock(AMExecutorServiceFactory.class);
        new SharedGuiceModule().provideThreadMonitor(mockFactory, mock(ShutdownManager.class), mock(Debug.class));
        verify(mockFactory).createCachedThreadPool(anyString());
    }
}