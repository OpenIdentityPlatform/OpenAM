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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring.impl.reaper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ReaperMonitorTest {

    private ReaperMonitor reaperMonitor;

    @BeforeMethod
    public void setUp() {
        reaperMonitor = new ReaperMonitor();
    }

    @Test
    public void shouldAddReaperRun() {

        //Given
        long startTime = 1000;
        long endTime = 2000;
        int numberOfDeletedSessions = 234;

        //When
        reaperMonitor.add(startTime, endTime, numberOfDeletedSessions);

        //Then
        assertEquals(reaperMonitor.getRateOfDeletion(), 234D);
    }

    @Test
    public void shouldGetZeroWhenNoRunsAdded() {

        //Given

        //When
        double result = reaperMonitor.getRateOfDeletion();

        //Then
        assertEquals(result, 0D);
    }

    @Test
    public void shouldGetAverageOfReaperRuns() {

        //Given
        long startTime = 1000;
        long endTime = 2000;

        reaperMonitor.add(startTime, endTime, 10);
        reaperMonitor.add(startTime, endTime, 15);

        //When
        double result = reaperMonitor.getRateOfDeletion();

        //Then
        assertEquals(result, 12.5D);
    }
}
