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
package org.forgerock.openam.cts.impl.queue.config;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class QueueConfigurationTest {

    private AsyncProcessorCount mockCount;
    private QueueConfiguration config;

    @BeforeMethod
    public void setup() {
        mockCount = mock(AsyncProcessorCount.class);
        config = new QueueConfiguration(
                mockCount,
                mock(Debug.class));
    }

    @Test
    public void shouldUseConnectionCountForProcessors() throws CoreTokenException {
        given(mockCount.getProcessorCount()).willReturn(2);
        config.getProcessors();
        verify(mockCount).getProcessorCount();
    }

    @Test
    public void shouldReturnCountWhenPowerOfTwo() throws CoreTokenException {
        int count = 2;
        given(mockCount.getProcessorCount()).willReturn(count);
        assertThat(config.getProcessors()).isEqualTo(count);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldRejectANegativeValue() throws CoreTokenException {
        given(mockCount.getProcessorCount()).willReturn(-1);
        config.getProcessors();
    }
}