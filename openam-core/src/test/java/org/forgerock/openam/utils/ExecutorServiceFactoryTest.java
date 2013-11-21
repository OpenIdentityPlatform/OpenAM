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
package org.forgerock.openam.utils;

import com.sun.identity.common.ShutdownListener;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
@PrepareForTest(Executors.class)
public class ExecutorServiceFactoryTest extends PowerMockTestCase {

    private ExecutorServiceFactory factory;
    private CoreGuiceModule.ShutdownManagerWrapper mockWrapper;

    @BeforeMethod
    public void setup() {
        mockWrapper = mock(CoreGuiceModule.ShutdownManagerWrapper.class);
        factory = new ExecutorServiceFactory(mockWrapper);
    }

    // Don't understand why this test doesn't work
//    @Test
//    public void shouldUseExecutorService() {
//        // Given
//        PowerMockito.mockStatic(Executors.class);
//
//        // When
//        factory.createThreadPool(1);
//
//        // Then
//        PowerMockito.verifyStatic();
//        Executors.newFixedThreadPool(eq(1));
//    }

    @Test
    public void shouldRegisterWithShutdownManager() {
        factory.createThreadPool(1);
        verify(mockWrapper).addShutdownListener(any(ShutdownListener.class));
    }
}
