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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import org.forgerock.openam.shared.monitoring.RateTimer;
import org.forgerock.openam.shared.monitoring.RateWindow;

/**
 * This class maintains a cumulative count and rate of Policy evaluations.
 **/
class EvaluationMonitoringStore {

    private static final long SAMPLE_RATE = 1000L;
    private static final int WINDOW_SIZE = 10;

    private final RateTimer timerGetter;
    private final RateWindow rateWindow;

    private final AtomicLong count = new AtomicLong(0);

    /**
     * Constructs a new instance of the EvaluationMonitor. (Guice-powered).
     *
     * @param timer An instance of a Timer.
     */
    @Inject
    public EvaluationMonitoringStore(final RateTimer timer) {
        this(timer, new RateWindow(timer, WINDOW_SIZE, SAMPLE_RATE));
    }

    /**
     * Constructs a new instance of the EvaluationMonitor
     *
     * Default scope allows testing.
     *
     * @param timer An instance of a Timer.
     */
    EvaluationMonitoringStore(final RateTimer timer, final RateWindow rateWindow) {
        this.timerGetter = timer;
        this.rateWindow = rateWindow;
    }

    /**
     * Increments the cumulative count of evaluations and recalculates the rate.
     *
     * <br/>
     * Only synchronizes the count increment, NOT the whole method.
     */
    public void increment() {
        count.incrementAndGet();
        rateWindow.incrementForTimestamp(timerGetter.now());
    }
    /**
     * Returns the average rate at which evaluations have been performed.
     *
     * @return The average rate at which evaluations have been performed.
     */
    public double getAverageEvaluationsPerPeriod() {
        return rateWindow.getAverageRate();
    }

    /**
     * Returns the minimum rate at which evaluations have been performed.
     *
     * @return The minimum rate at which evaluations have been performed.
     */
    public long getMinimumEvaluationsPerPeriod() {
        return rateWindow.getMinRate();
    }

    /**
     * Returns the maximum rate at which evaluations have been performed.
     *
     * @return The maximum rate at which evaluations have been performed.
     */
    public long getMaximumEvaluationsPerPeriod() {
        return rateWindow.getMaxRate();
    }

    /**
     * Returns the cumulative count of evaluations, since server start up.
     *
     * @return The cumulative count of evaluations.
     */
    public long getEvaluationCumulativeCount() {
        return count.get();
    }

}
