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
 * Copyright 2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.share;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class SessionInfoTest {

    @Test
    public void shouldNotOverflowTimeLeft() {
        // Given
        SessionInfo info = new SessionInfo();

        // When
        info.setTimeLeft(Long.MAX_VALUE);

        // Then
        assertEquals(info.getExpiryTime(), Long.MAX_VALUE);
    }

    @Test
    public void shouldRespectNeverExpiringFlag() {
        // Given
        SessionInfo info = new SessionInfo();
        info.setTimeLeft(300L);
        info.setTimeIdle(100L);
        info.setNeverExpiring(true);

        // When
        long timeLeft = info.getTimeLeft();
        long timeIdle = info.getTimeIdle();

        // Then
        assertEquals(timeLeft, Long.MAX_VALUE);
        assertEquals(timeIdle, 0);
    }
}