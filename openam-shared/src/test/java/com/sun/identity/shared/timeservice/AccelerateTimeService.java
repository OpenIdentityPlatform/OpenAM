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

/**
 * An accelerate timeservice for testing time sensitive features
 * It accelerate the time from a date reference called initial time.
 * Every MS after this reference will be multiplied by a factor
 * </p>
 * </p>
 * Example :
 * </p>
 * //Multiply the time by two from now (16/05/2014 16:20:30)
 * TimeService timeservice = new AccelerateTimeService(System.currentTimeMillis(), 2);
 * </p>
 * ... 10 secs later
 * //return a value equivalent to 16/05/2014 16:20:50
 * timeservice.now()
 */
public class AccelerateTimeService implements TimeService {

    private int factor;
    private long initTime;
    private long systemTimeAtInitialization;

    /**
     * Constructor
     *
     * @param initTime when the time acceleration should started, in MS from epoch
     * @param factor   acceleration factor
     */
    public AccelerateTimeService(long initTime, int factor) {
        this.initTime = initTime;
        this.factor = factor;
        this.systemTimeAtInitialization = System.currentTimeMillis();
    }

    @Override
    public long now() {

        //elapsed time since the beginning
        long deltaTimeFromInitTime = System.currentTimeMillis() - systemTimeAtInitialization;

        //We only accelerate the elapsed time with the factor requested
        return deltaTimeFromInitTime * factor + initTime;
    }

    @Override
    public long since(long l) {
        return now() - l;
    }

}
