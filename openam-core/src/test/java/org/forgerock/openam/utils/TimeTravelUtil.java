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

package org.forgerock.openam.utils;

import org.forgerock.util.time.TimeService;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for mocking the passing of time as seen by OpenAM classes through {@link Time}.
 */
public final class TimeTravelUtil {

    /**
     * Get the current {@link TimeService} used by {@link Time}.
     *
     * @return the current oracle of time as observed by OpenAM.
     */
    public static TimeService getBackingTimeService() {
        try {
            Field timeServiceField = Time.class.getDeclaredField("timeService");
            timeServiceField.setAccessible(true);
            return (TimeService) timeServiceField.get(Time.INSTANCE);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException("Unable to obtain time service");
        }
    }

    /**
     * Set the {@link TimeService} used by {@link Time}.
     *
     * @param timeService the oracle of time as observed by OpenAM.
     */
    public static void setBackingTimeService(final TimeService timeService) {
        try {
            Field timeServiceField = Time.class.getDeclaredField("timeService");
            timeServiceField.setAccessible(true);
            timeServiceField.set(Time.INSTANCE, timeService);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException("Unable to set time service");
        }
    }

    /**
     * Implementation of {@link TimeService} that allows a monotonically increasing offset to be applied
     * to the current system time.
     */
    public enum FastForwardTimeService implements TimeService {

        INSTANCE;

        private long offset;

        @Override
        public long now() {
            return System.currentTimeMillis() + offset;
        }

        @Override
        public long since(final long l) {
            return now() - l;
        }

        /**
         * Applies a monotonically increasing offset to the actual system time.
         * <p>
         * Multiple calls are cumulative.
         *
         * @param value the amount to add the current offset.
         * @param units the units of the amount to add to the current offset.
         */
        public void fastForward(final long value, final TimeUnit units) {
            this.offset += units.toMillis(value);
        }
    }

    /**
     * Implementation of {@link TimeService} that allows an arbitrary time to be set.
     * <p>
     * In its initial state, this {@link TimeService} fixes the "current time" to 0 milliseconds.
     * Tests making use of this {@link TimeService} should explicitly set "current time" through
     * calls to {@link #setCurrentTimeMillis} and {@link #fastForward}.
     */
    public enum FrozenTimeService implements TimeService {

        INSTANCE;

        private long currentTimeMillis = 0;

        @Override
        public long now() {
            return currentTimeMillis;
        }

        @Override
        public long since(final long l) {
            return currentTimeMillis - l;
        }

        /**
         * Sets the current time to the specified milliseconds.
         *
         * @param currentTimeMillis the current time.
         */
        public void setCurrentTimeMillis(final long currentTimeMillis) {
            this.currentTimeMillis = currentTimeMillis;
        }

        /**
         * Applies a monotonically increasing offset to the fixed time set by
         * the last call to {@link #setCurrentTimeMillis}.
         * <p>
         * Multiple calls are cumulative.
         *
         * @param value the amount to add the current offset.
         * @param units the units of the amount to add to the current offset.
         */
        public void fastForward(final long value, final TimeUnit units) {
            this.currentTimeMillis += units.toMillis(value);
        }

        /**
         * Calculates a time in the future by adding the specified interval to the
         * result of {@link #now()}.
         * <p>
         * Calling this method does not mutate this object.
         *
         * @param value the amount to add to the current time.
         * @param units the units of the amount to add.
         * @return the calculated future time.
         */
        public long plus(long value, TimeUnit units) {
            return currentTimeMillis + units.toMillis(value);
        }

    }

}