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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.temper;

import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.SystemTimerPool;
import org.forgerock.util.time.TimeService;

/**
 * The time service that permits time travel. This implementation has an offset that can be set from services exposed
 * by {@link TimeTravelRouteProvider}, and the implementation then adds that offset (which could in theory be negative)
 * to the time provided by {@link TimeService#SYSTEM}.
 */
public class TimeTravelTimeService implements TimeService {

    private static long OFFSET = 0;

    @Override
    public long now() {
        return TimeService.SYSTEM.now() + OFFSET;
    }

    @Override
    public long since(long l) {
        return now() - l;
    }

    public static long getOffset() {
        return OFFSET;
    }

    public static void setOffset(long offset) {
        OFFSET = offset;
        notifyDependencies();
    }

    /**
     * The local time has changed, so we need to wake up the timer pools.
     */
    private static void notifyDependencies() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(SystemTimerPool.SCHEDULER_NAME) || t.getName().equals(SystemTimer.SCHEDULER_NAME)) {
                synchronized (t) {
                    t.notify();
                }
            }
        }
    }

}
