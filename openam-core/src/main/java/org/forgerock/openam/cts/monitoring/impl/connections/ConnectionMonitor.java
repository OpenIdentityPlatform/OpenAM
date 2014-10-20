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

package org.forgerock.openam.cts.monitoring.impl.connections;

import java.util.concurrent.atomic.AtomicLong;
import org.forgerock.openam.shared.monitoring.RateTimer;
import org.forgerock.openam.shared.monitoring.RateWindow;

/**
 * This class maintains a cumulative count and rate for a CTS connections
 *
 * @since 12.0.0
 */
class ConnectionMonitor  {

    private static final long SAMPLE_RATE = 1000L;
    private static final int WINDOW_SIZE = 10;

    private final RateTimer timer;
    private final RateWindow rateWindow;

    private final AtomicLong count = new AtomicLong(0);

    public ConnectionMonitor() {
        this(new RateTimer());
    }

    /**
     * Constructs a new instance of the ConnectionMonitor.
     *
     * @param timer An instance of a Timer.
     */
    private ConnectionMonitor(final RateTimer timer) {
        this(timer, new RateWindow(timer, WINDOW_SIZE, SAMPLE_RATE));
    }

    ConnectionMonitor(final RateTimer timer, final RateWindow rateWindow) {
        this.timer = timer;
        this.rateWindow = rateWindow;
    }

    /**
     * Notifies the monitoring system that a rate tracker must be incremented, and the
     * rate information recalculated.
     */
    public void add() {
        count.incrementAndGet();
        rateWindow.incrementForTimestamp(timer.now());
    }

    /**
     * Returns the cumulative count of the number of connections made since server startup
     *
     * @return The cumulative count of connections
     */
    public long getCumulativeCount() {
        return count.longValue();
    }

    /**
     * Returns the minimum rate of connections made within the window
     *
     * @return The minimum rate at which connections are made
     */
    public long getMinimumRate() {
        return rateWindow.getMinRate();
    }

    /**
     * Returns the average rate of connections made within the window
     *
     * @return The average rate at which connections are made
     */
    public double getAverageRate() {
        return rateWindow.getAverageRate();
    }

    /**
     * Returns the maximum rate of connections made within the window
     *
     * @return The maximum rate at which connections are made
     */
    public long getMaximumRate() {
        return rateWindow.getMaxRate();
    }

}
