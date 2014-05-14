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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    private final LinkedHashMap<Long, AtomicLong> window;
    private final Set<AtomicLong> minMaxRate = new TreeSet<AtomicLong>(new Comparator<AtomicLong>() {
        public int compare(AtomicLong rate, AtomicLong rate2) {
            return (int) (rate.get() - rate2.get());
        }
    });

    private long currentIndex = 0L;

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
        this.window = new LinkedHashMap<Long, AtomicLong>(size);
    }

    /**
     * Re-calculates the rate.
     *
     * @param timestamp The millisecond timestamp of the event.
     */
    public synchronized void recalculate(final long timestamp) {
        long sample = toSampleRate(timestamp);

        if (sample < currentIndex) {
            // timestamp is after current index so is in the past so just update a previous rate
            updatePastRate(sample, currentIndex);
            return;
        }

        if (isAtCurrentIndex(sample)) {
            AtomicLong rate = window.get(currentIndex);
            rate.incrementAndGet();
            return;
        }

        // Otherwise sample is ahead of currentIndex
        addNextSlot();
        recalculate(timestamp);
    }

    /**
     * Given a sample that is behind the current index, this method will traverse backwards to find the right slot in
     * the window to update the rate for.
     * <br/>
     * If the window has moved past the slot for the sample then the sample is dropped without further action.
     *
     * @param sample The sample rate index.
     * @param index The index in the window.
     */
    private void updatePastRate(final long sample, final long index) {
        final long i = index - 1;
        if (window.size() == 0 || window.get(i) == null) {
            // Window has passed the sample time so nothing to do
            return;
        }

        if (i == sample) {
            AtomicLong rate = window.get(i);
            rate.incrementAndGet();
            return;
        }

        updatePastRate(sample, i);
    }

    /**
     * Updates the min and max figures when the window moves forwards.
     *
     * @param currentIndex The current index in the window.
     * @param oldestIndex The oldest index in the window.
     */
    private void updateMinAndMax(final long currentIndex, final long oldestIndex) {
        final AtomicLong oldestRate = window.get(oldestIndex);
        if (!minMaxRate.isEmpty() && oldestRate != null) {
            minMaxRate.remove(oldestRate);
        }

        final AtomicLong currentRate = window.get(currentIndex);
        minMaxRate.add(currentRate);
    }

    /**
     * Adds the next window slot to the window.
     */
    private void addNextSlot() {
        long nextIndex = getNextIndex();
        final long oldestIndex = getPastIndex(nextIndex);
        updateMinAndMax(currentIndex, oldestIndex);
        currentIndex = nextIndex;
        window.put(nextIndex, new AtomicLong(0));
        // Each time we add a new index, we move the window along by removing the oldest index.
        window.remove(oldestIndex);
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

        if(window.size()  == 0) {
            return 0D;
        }

        double averageRate = 0;
        final long now = toSampleRate(timer.now());
        for (Map.Entry<Long, AtomicLong> entry : window.entrySet()) {
            if (isAtCurrentIndex(now) && entry.getKey().equals(currentIndex)) {
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
    public synchronized long getMinRate() {
        if (minMaxRate.isEmpty()) {
            return 0L;
        }
        if (isAtCurrentIndex(toSampleRate(timer.now()))) {
            addNextSlot();
        }
        return new ArrayList<AtomicLong>(minMaxRate).get(0).get();
    }

    /**
     * Gets the maximum rate.
     *
     * @return The maximum event rate.
     */
    public synchronized long getMaxRate() {
        if (minMaxRate.isEmpty()) {
            return 0L;
        }
        if (isAtCurrentIndex(toSampleRate(timer.now()))) {
            addNextSlot();
        }
        List<AtomicLong> maxRate = new ArrayList<AtomicLong>(minMaxRate);
        return maxRate.get(maxRate.size() - 1).get();
    }

    /**
     * Converts the millisecond timestamp into a normalised sample rate.
     *
     * @param timestamp The millisecond timestamp.
     * @return The sample rate.
     */
    private long toSampleRate(final long timestamp) {
        return timestamp / sampleRate;
    }

    /**
     * Determines if the given sample rate index is at the current index.
     * <br/>
     * Will set up the window if not done yet.
     *
     * @param sample The sample rate index.
     * @return Whether the given sample rate index is the current index.
     */
    private boolean isAtCurrentIndex(final long sample) {
        // If is first time the window has been used we need to first set the currentIndex to the given sample.
        if (currentIndex == 0L) {
            currentIndex = sample;
            window.put(currentIndex, new AtomicLong(0));
            return true;
        }

        return currentIndex == sample;
    }

    /**
     * Calculates the next window index.
     *
     * @return The next window sample index.
     */
    private long getNextIndex() {
        return currentIndex + 1;
    }

    /**
     * Calculates the oldest sample index of the window.
     *
     * @param nextIndex The upcoming next sample index of the window.
     * @return The oldest sample index.
     */
    private long getPastIndex(final long nextIndex) {
        return nextIndex - size;
    }

}
