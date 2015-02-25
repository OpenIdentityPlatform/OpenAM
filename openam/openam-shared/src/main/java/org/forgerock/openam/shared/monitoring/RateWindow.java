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
package org.forgerock.openam.shared.monitoring;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class maintains the rate of an event for a sample rate over a window of a particular size.
 * <br/>
 * The window is constructed with a given size and sample rate and the window will move continuously at interval
 * defined by the sample rate. The min, max and average will always be of the current window position, ie as the window
 * moves and sample are not covered by the window the sample will not be included in the  min, max and average
 * calculations.
 *
 * @since 12.0.0
 */
public class RateWindow {

    private final RateTimer timer;
    private final int size;
    private final long sampleRate;
    private final ConcurrentSkipListMap<Long, AtomicLong> window = new ConcurrentSkipListMap<Long, AtomicLong>();
    private final Comparator<AtomicLong> atomicLongComparator = new Comparator<AtomicLong>() {

        @Override
        public int compare(AtomicLong rate, AtomicLong rate2) {
            long x = rate.get();
            long y = rate2.get();
            return x > y ? 1 : x < y ? -1 : 0;
        }
    };

    /**
     * Constructs a new instance of the RateWindow.
     *
     * @param timer An instance of a Timer.
     * @param size The size of the window.
     * @param sampleRate The sample rate for the window.
     */
    public RateWindow(final RateTimer timer, final int size, final long sampleRate) {
        this.timer = timer;
        this.size = size;
        this.sampleRate = sampleRate;
    }

    /**
     * Re-calculates the rate.
     *
     * @param timestamp The millisecond timestamp of the event.
     */
    public void incrementForTimestamp(final long timestamp) {
        long index = getIndexForTimestamp(timestamp);

        if (isWithinWindow(index)) {
            AtomicLong rate = window.get(index);
            if (rate == null) {
                //fill in the RateWindow until the current index
                fillInWindow(index - 1);
                rate = new AtomicLong(0);
                AtomicLong previousValue = window.putIfAbsent(index, rate);
                if (previousValue == null) {
                    //this is a new entry, hence we should clear out old entries to prevent memory leak
                    window.headMap(window.lastKey() - size, true).clear();
                } else {
                    rate = previousValue;
                }
            }
            rate.incrementAndGet();
        }
    }

    /**
     * Fills in the windows with 0 values until the index provided. This ensures that there are no empty spots between
     * the indexes, so the information stored in the window actually represents a rolling window of data.
     *
     * @param index The index until which the window should be filled. The entry corresponding to the provided index
     * will be also initialized
     */
    private void fillInWindow(long index) {
        if (!window.isEmpty()) {
            Long lastKey = window.lastKey();
            for (lastKey = lastKey + 1; lastKey <= index; lastKey++) {
                window.putIfAbsent(lastKey, new AtomicLong(0));
            }
            window.headMap(window.lastKey() - size, true).clear();
        }
    }

    /**
     * Gets the average rate for the sample rate averaged across the whole window.
     * <br/>
     * Does not include the latest window slot if time has not passed beyond it yet as otherwise could skew the average
     * as that time slot has not yet completed and may get more events made in it.
     *
     * @return The average event rate.
     */
    public synchronized double getAverageRate() {
        if (window.isEmpty()) {
            return 0D;
        }

        fillInWindow(getCurrentIndex());
        double averageRate = 0;
        for (Map.Entry<Long, AtomicLong> entry : window.entrySet()) {
            if (entry.getKey().equals(getCurrentIndex())) {
                /*
                 * If this is true then the latest window slot has not completed so the rate in it will not be
                 * accurate so skip it.
                 */
                continue;
            }
            averageRate += entry.getValue().get();
        }

        return averageRate / window.size();

    }

    /**
     * Gets the minimum rate.
     *
     * @return The minimum event rate.
     */
    public long getMinRate() {
        if (window.isEmpty()) {
            return 0L;
        }

        fillInWindow(getCurrentIndex());
        return Collections.min(window.values(), atomicLongComparator).get();
    }

    /**
     * Gets the maximum rate.
     *
     * @return The maximum event rate.
     */
    public long getMaxRate() {
        if (window.isEmpty()) {
            return 0L;
        }

        fillInWindow(getCurrentIndex());
        return Collections.max(window.values(), atomicLongComparator).get();
    }

    /**
     * Converts the millisecond timestamp into a normalised sample rate.
     *
     * @param timestamp The millisecond timestamp.
     * @return The sample rate.
     */
    private long getIndexForTimestamp(final long timestamp) {
        return timestamp / sampleRate;
    }

    private long getCurrentIndex() {
        return getIndexForTimestamp(timer.now());
    }

    private boolean isWithinWindow(final long index) {
        return getCurrentIndex() - size < index;
    }
}
