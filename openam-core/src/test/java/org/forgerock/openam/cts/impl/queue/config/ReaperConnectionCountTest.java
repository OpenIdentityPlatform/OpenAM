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
import static org.mockito.BDDMockito.mock;

public class ReaperConnectionCountTest {

    private CTSConnectionCount mockConnectionCount;
    private ReaperConnectionCount count;

    @BeforeMethod
    public void setup() {
        mockConnectionCount = mock(CTSConnectionCount.class);
        count = new ReaperConnectionCount(mockConnectionCount, mock(Debug.class));
    }

    @Test
    public void shouldUsePowerOfTwoConnectionsIfLessThanConnectionCount() throws CoreTokenException {
        given(mockConnectionCount.getProcessorCount()).willReturn(65);
        assertThat(count.getProcessorCount()).isEqualTo(64);
    }

    @Test
    public void shouldSelectNextAppropriatePowerOfTwoForConnectionCount() throws CoreTokenException {
        given(mockConnectionCount.getProcessorCount()).willReturn(63);
        assertThat(count.getProcessorCount()).isEqualTo(32);
    }
}