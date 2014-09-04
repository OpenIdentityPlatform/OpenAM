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
package org.forgerock.openam.utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * A collection of static immutable time functions.
 *
 * Note: Unix time (aka POSIX time or Epoch time), is a system for describing
 * instants in time, defined as the number of seconds that have elapsed since
 * 00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970, not
 * counting leap seconds.
 */
public class TimeUtils {
    /**
     * Convert from a Java Calendar instance into a Unix Time stamp.
     *
     * Java Calendar instances contain timezone information. This will be lost
     * in the conversion and no attempt will be made to reconcile the local
     * timezone with the timezone information in the Calendar.
     *
     * @param timestamp Non null timestamp to convert.
     * @return A long representing the number of seconds from the epoch.
     */
    public static long toUnixTime(Calendar timestamp) {
        return timestamp.getTimeInMillis()/1000L;
    }

    /**
     * Converts Unix Time into Java Calendar time.
     *
     * Unix Time is a measure of seconds from the epoch. Therefore is devoid of
     * any timezone information. The current default timezone will be assumed
     * for this function.
     *
     * @param unixTime A Unix timestamp.
     * @return Non null Calendar representing this timestamp.
     */
    public static Calendar fromUnixTime(long unixTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        long millis = unixTime * 1000L;
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    /**
     * Generates the current timestamp in Unix time format.
     *
     * @return A long representing seconds from the epoch.
     */
    public static long currentUnixTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return toUnixTime(calendar);
    }
}
