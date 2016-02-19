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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.TimeZone;

import org.forgerock.util.time.TimeService;
import org.joda.time.DateTimeUtils;
import org.slf4j.LoggerFactory;

/**
 * The source of all time-based information in OpenAM.
 * <p>
 * Use the methods on this class where you would have otherwise used {@code System.currentTimeMillis()},
 * {@code new Date()}, {@code Calendar.getInstance()} or the various constructors and {@code now} methods
 * in {@code org.joda.time.DateTime}.
 * <p>
 *
 */
public enum Time implements DateTimeUtils.MillisProvider {

    /** Singleton Instance. */
    INSTANCE;

    private final TimeService timeService;

    Time() {
        Iterator<TimeService> services = ServiceLoader.load(TimeService.class).iterator();
        if (services.hasNext()) {
            TimeService service = services.next();
            if (services.hasNext()) {
                LoggerFactory.getLogger(Time.class).
                        error("More than one TimeService configured in META-INF/services."
                                + " Defaulting to TimeService.SYSTEM");
                timeService = TimeService.SYSTEM;
            } else {
                timeService = service;
            }
        } else {
            timeService = TimeService.SYSTEM;
        }
        DateTimeUtils.setCurrentMillisProvider(this);
    }

    @Override
    public long getMillis() {
        return timeService.now();
    }

    /**
     * Get the current time in milliseconds from the {@code TimeService} - this replaces
     * {@code System.currentTimeMillis()}.
     * @return The current time since the epoch in milliseconds.
     * @see System#currentTimeMillis()
     */
    public static long currentTimeMillis() {
        return INSTANCE.timeService.now();
    }

    /**
     * Get a {@code Date} with current time in milliseconds from the {@code TimeService} - this replaces
     * {@code new Date()}.
     * @return A date with the current time since the epoch in milliseconds.
     * @see Date#Date()
     */
    public static Date newDate() {
        return new Date(currentTimeMillis());
    }

    /**
     * Get a {@code Calendar} with current time in milliseconds from the {@code TimeService} - this replaces
     * {@code Calendar.getInstance()}.
     * @return A date with the current time since the epoch in milliseconds.
     * @see Calendar#getInstance()
     */
    public static Calendar getCalendarInstance() {
        Calendar calendar = Calendar.getInstance();
        return setCalendarTime(calendar);
    }

    /**
     * Get a {@code Calendar} with current time in milliseconds from the {@code TimeService} - this replaces
     * {@code Calendar.getInstance(Locale)}.
     * @param locale The locale to use for the calendar instance.
     * @return A date with the current time since the epoch in milliseconds.
     * @see Calendar#getInstance(Locale)
     */
    public static Calendar getCalendarInstance(Locale locale) {
        Calendar calendar = Calendar.getInstance(locale);
        return setCalendarTime(calendar);
    }

    /**
     * Get a {@code Calendar} with current time in milliseconds from the {@code TimeService} - this replaces
     * {@code Calendar.getInstance(TimeZone)}.
     * @param tz The timezone to use for the calendar instance.
     * @return A date with the current time since the epoch in milliseconds.
     * @see Calendar#getInstance(TimeZone)
     */
    public static Calendar getCalendarInstance(TimeZone tz) {
        Calendar calendar = Calendar.getInstance(tz);
        return setCalendarTime(calendar);
    }

    /**
     * Get a {@code Calendar} with current time in milliseconds from the {@code TimeService} - this replaces
     * {@code Calendar.getInstance(TimeZone, Locale)}.
     * @param tz The timezone to use for the calendar instance.
     * @param locale The locale to use for the calendar instance.
     * @return A date with the current time since the epoch in milliseconds.
     * @see Calendar#getInstance(TimeZone, Locale)
     */
    public static Calendar getCalendarInstance(TimeZone tz, Locale locale) {
        Calendar calendar = Calendar.getInstance(tz, locale);
        return setCalendarTime(calendar);
    }

    private static Calendar setCalendarTime(Calendar calendar) {
        calendar.setTimeInMillis(currentTimeMillis());
        return calendar;
    }
}
