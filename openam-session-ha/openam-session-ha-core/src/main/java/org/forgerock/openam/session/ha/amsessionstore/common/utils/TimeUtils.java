/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package org.forgerock.openam.session.ha.amsessionstore.common.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Provide Simple Static Utility to perform all Time Manipulations
 *
 * @author jeff.schenk@thought-matrix.com
 *         Date: Apr 30, 2010
 *         Time: 10:01:59 AM
 */
public class TimeUtils {
    /**
     * Non-accessible Constructor for Utility Class.
     */
    private TimeUtils() {
        // Make Constructor not Accessible for Static Utility Class.
    }

    /**
     * Get the Difference in time From a Date to Now.
     *
     * @param otherDate - The Date to be used to determine difference in Time between these two points.
     * @return long - The Number of Millisecond Difference between a Date and Now in Time.
     */
    public static long getTimeDifference(Date otherDate) {
        if (otherDate == null) {
            throw new IllegalArgumentException("Invalid Date Parameter!");
        }
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        Calendar aPointInTime = Calendar.getInstance();
        aPointInTime.setTime(otherDate);
        return aPointInTime.getTimeInMillis() - now.getTimeInMillis();
    }

    /**
     * Get the Difference in time From a Date to Another Date.
     *
     * @param thisDate  - A Date referencing a Point in Time.
     * @param otherDate - The Date to be used to determine difference in Time between these two points.
     * @return long - The Number of Millisecond Difference between a Date and Now in Time.
     */
    public static long getTimeDifference(Date thisDate, Date otherDate) {
        if ((thisDate == null) || (otherDate == null)) {
            throw new IllegalArgumentException("Invalid Date Parameter!");
        }
        Calendar aPointInTime = Calendar.getInstance();
        aPointInTime.setTime(thisDate);
        Calendar anotherPointInTime = Calendar.getInstance();
        anotherPointInTime.setTime(otherDate);
        return aPointInTime.getTimeInMillis() - anotherPointInTime.getTimeInMillis();
    }

    /**
     * Get the Difference in time From a Date to Now.
     *
     * @param aDate - A Date referencing a Point in Time.
     * @return String representing the Difference in Months, Days, Hour, Minutes.
     */
    public static String getTimeDifferenceFromNow(Date aDate) {
        long timeDifference = TimeUtils.getTimeDifference(aDate, new Date());
        return TimeDuration.getElapsedToTextString(timeDifference);
    }

    /**
     * Get the Difference in time From a Date to Now.
     *
     * @param aDate - A Date referencing a Point in Time.
     * @return Long
     */
    public static long getRawTimeDifferenceFromNow(Date aDate) {
        return TimeUtils.getTimeDifference(aDate, new Date());
    }

    /**
     * Get a date Object construct from a Format String and
     * the Value of the Date represented in the specified format.
     * <p/>
     * Here are the list of defined patterns that can be used to format the date taken from the Java class documentation.
     * Letter 	Date / Time Component 	Examples
     * G 	    Era designator      	AD
     * y 	    Year 	                1996; 96
     * M 	    Month in year 	        July; Jul; 07
     * w 	    Week in year 	        27
     * W 	    Week in month 	        2
     * D 	    Day in year 	        189
     * d 	    Day in month 	        10
     * F 	    Day of week in month 	2
     * E 	    Day in week 	        Tuesday; Tue
     * a 	    Am/pm marker 	        PM
     * H 	    Hour in day (0-23) 	    0
     * k 	    Hour in day (1-24) 	    24
     * K 	    Hour in am/pm (0-11) 	0
     * h 	    Hour in am/pm (1-12) 	12
     * m 	    Minute in hour 	        30
     * s 	    Second in minute 	    55
     * S 	    Millisecond 	        978
     * z 	    Time zone 	            Pacific Standard Time; PST; GMT-08:00
     * Z 	    Time zone 	            -0800
     *
     * @param format
     * @param value
     * @return Date - Representing Specified string Value.
     */
    public static Date getDate(String format, String value) {
        DateFormat df = new SimpleDateFormat(format);
        try {
            return df.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simple Date Helper to get the time a number of Minutes from Now.
     * To go back in time, simple supply a negative value.
     *
     * @return Date - xx Minutes from Now.
     */
    public static Date getDateMinutesFromNow(int minutes_offset) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, minutes_offset);
        return now.getTime();
    }

    /**
     * Simple Date Helper to get the time a number of hours from Now.
     * To go back in time, simple supply a negative value.
     *
     * @return Date -  xx Hours from Now.
     */
    public static Date getDateHoursFromNow(int hours_offset) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.HOUR, hours_offset);
        return now.getTime();
    }

    /**
     * Simple Date Helper to get the time a number of Days from Now.
     * To go back in time, simple supply a negative value.
     *
     * @return
     */
    public static Date getDateDaysFromNow(int days_offset) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.HOUR, (days_offset*24));
        return now.getTime();
    }

    /**
     * Simple Date Helper to get the time a number of Days from some point in time.
     * To go back in time, simple supply a negative value.
     *
     * @return
     */
    public static Date getDateDaysFromSomeTime(int days_offset, Date someTime) {
        Calendar origin = Calendar.getInstance();
        origin.setTime(someTime);
        origin.add(Calendar.HOUR, (days_offset*24));
        return origin.getTime();
    }

    /**
     * Return current Time in Milliseconds.
     * @return long Time in Milliseconds.
     */
    public static long now() {
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Return current Time in Seconds.
     * @return long Time in Seconds.
     */
    public static long nowInSeconds() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    /**
     * Return current Time in Milliseconds.
     * @return long Time in Milliseconds.
     */
    public static Date getNow() {
        return Calendar.getInstance().getTime();
    }

    /**
     * Return specified Milliseconds in a Date Object.
     *
     * @param milliseconds
     * @return Date
     */
    public static Date getDate(long milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        return cal.getTime();
    }


    /**
     * Takes a time and returns it rounded-off to the nearest previous value
     *
     * @param timeMillisSinceEpoch time in milliseconds since the Epoch
     * @param roundOffMillis round off period in milliseconds - e.g. 1000 * 60 * 15 would be 15 minutes
     * @return a Date representing the rounded-off time - e.g. 16:57:43 -> 16:45:00
     */
    public static Date getRoundedOffTime(long timeMillisSinceEpoch, long roundOffMillis) {
        timeMillisSinceEpoch -= timeMillisSinceEpoch % roundOffMillis;
        return new Date(timeMillisSinceEpoch);
    }

    /**
     * Main to Provide Command Line Interface to Utility.
     *
     * @param args
     */
    public static void main(String[] args) {
        Calendar live = Calendar.getInstance();
        for (String arg : args) {

        }
        live.setTime(new Date());
        live.add(Calendar.MINUTE, 100);

        long timeDifference = TimeUtils.getTimeDifference(live.getTime());

        System.out.println("Time Difference:[" + TimeDuration.getElapsedtoString(timeDifference) + "]");

        timeDifference = TimeUtils.getTimeDifference(live.getTime(), live.getTime());

        System.out.println("Time Difference:[" + TimeDuration.getElapsedtoString(timeDifference) + "]");

        live.add(Calendar.MINUTE, -100);
        timeDifference = TimeUtils.getTimeDifference(live.getTime());
        System.out.println("Time Difference:[" + TimeDuration.getElapsedtoString(timeDifference) + "]");

        live.setTime(new Date());
        System.out.println("Time Difference:[" + TimeUtils.getTimeDifferenceFromNow(live.getTime()) + "]");

        live.add(Calendar.MINUTE, 100);
        System.out.println("Time Difference:[" + TimeUtils.getTimeDifferenceFromNow(live.getTime()) + "]");

        live.setTime(new Date());
        System.out.println("Original Time:[" + live.getTime() + "] - Rounded-off Time:[" +
                TimeUtils.getRoundedOffTime(live.getTimeInMillis(), (1000 * 60 * 15)) +"]");
    }


}

