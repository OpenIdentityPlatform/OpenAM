/**
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
package com.sun.identity.shared.timeservice;

import org.forgerock.util.time.TimeService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An accelerate timeservice for testing time sensitive features
 * It accelerates the time from a date reference called initial time.
 * <p/>
 * <p/>
 * Example :
 * <p/>
 * TimeService timeservice = new AccelerateTimeService(System.currentTimeMillis());
 */
public class AccelerateTimeService implements TimeService {

    /*
        The clock is incremented each time the "now" function is called.
        By this way, we aren't dependent to the system clock anymore.
        16 is a value fixed by experience and generate log files with a size of 886k, with approximately 3750 logs in
        each file.
     */
    private static final int INCR_TIME_MS = 16;

    private AtomicLong clock;

    /**
     * Constructor
     *
     * @param initTime when the time acceleration should started, in MS from epoch
     */
    public AccelerateTimeService(long initTime) {

        this.clock = new AtomicLong(initTime);
    }

    @Override
    public long now() {

        // Increment the clock
        return incrementTime(INCR_TIME_MS);
    }

    /**
     * Increment the accelerate clock
     * @param deltaTime
     */
    public long incrementTime(long deltaTime) {
        return clock.addAndGet(deltaTime);
    }

    @Override
    public long since(long l) {
        return now() - l;
    }

}
