/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DateUtils.java,v 1.4 2008/06/25 05:42:25 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class provides utility to perform date conversion.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.DateUtils}
 */
public class DateUtils {

    private static final String UTC_DATE_Z_FORMAT = "{0}-{1}-{2}T{3}:{4}:{5}Z";

    private static final String UTC_DATE_FORMAT = "{0}-{1}-{2}T{3}:{4}:{5}";

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Returns <code>yyyy-MM-dd HH:mm:ss</code> String representation of a
     * date.
     * 
     * @param date
     *            Date object.
     */
    public static String dateToString(Date date) {
        return dateToString(date, UTC_DATE_FORMAT);
    }

    /**
     * Returns UTC String representation of a date. For instance,
     * 2004-03-20T05:53:32Z.
     * 
     * @param date
     *            Date object.
     */
    public static String toUTCDateFormat(Date date) {
        return dateToString(date, UTC_DATE_Z_FORMAT);
    }

    private static String dateToString(Date date, String format) {
        GregorianCalendar cal = new GregorianCalendar(UTC_TIME_ZONE);
        cal.setTime(date);
        String[] params = new String[6];

        params[0] = formatInteger(cal.get(Calendar.YEAR), 4);
        params[1] = formatInteger(cal.get(Calendar.MONTH) + 1, 2);
        params[2] = formatInteger(cal.get(Calendar.DAY_OF_MONTH), 2);
        params[3] = formatInteger(cal.get(Calendar.HOUR_OF_DAY), 2);
        params[4] = formatInteger(cal.get(Calendar.MINUTE), 2);
        params[5] = formatInteger(cal.get(Calendar.SECOND), 2);

        return MessageFormat.format(format, (Object[])params);
    }

    private static String formatInteger(int value, int length) {
        String val = Integer.toString(value);
        int diff = length - val.length();

        for (int i = 0; i < diff; i++) {
            val = "0" + val;
        }

        return val;
    }

    /**
     * Returns date that is represented by a string. It uses the following
     * representation of date. yyyy-MM-DD'T'hh:mm:ss based on the following
     * definition of "dateTime" attribute in XML schema which can be found at
     * http://www.w3.org/TR/xmlschema-2/#dateTime. A single lexical
     * representation, which is a subset of the lexical representations allowed
     * by [ISO 8601], is allowed for dateTime. This lexical representation is
     * the [ISO 8601] extended format CCYY-MM-DDThh:mm:ss where "CC" represents
     * the century, "YY" the year, "MM" the month and "DD" the day, preceded by
     * an optional leading "-" sign to indicate a negative number. If the sign
     * is omitted, "+" is assumed. The letter "T" is the date/time separator and
     * "hh", "mm", "ss" represent hour, minute and second respectively.
     * Additional digits can be used to increase the precision of fractional
     * seconds if desired i.e the format ss.ss... with any number of digits
     * after the decimal point is supported. The fractional seconds part is
     * optional; other parts of the lexical form are not optional. To
     * accommodate year values greater than 9999 additional digits can be added
     * to the left of this representation. Leading zeros are required if the
     * year value would otherwise have fewer than four digits; otherwise they
     * are forbidden. The year 0000 is prohibited. The CCYY field must have at
     * least four digits, the MM, DD, SS, hh, mm and ss fields exactly two
     * digits each (not counting fractional seconds); leading zeroes must be
     * used if the field would otherwise have too few digits.
     * 
     * This representation may be immediately followed by a "Z" to indicate
     * Coordinated Universal Time (UTC) or, to indicate the time zone, i.e. the
     * difference between the local time and Coordinated Universal Time,
     * immediately followed by a sign, + or -, followed by the difference from
     * UTC represented as hh:mm (note: the minutes part is required). See ISO
     * 8601 Date and Time Formats ('D) for details about legal values in the
     * various fields. If the time zone is included, both hours and minutes must
     * be present.
     * 
     * For example, to indicate 1:20 pm on May the 31st, 1999 for Eastern
     * Standard Time which is 5 hours behind Coordinated Universal Time (UTC),
     * one would write: 1999-05-31T13:20:00-05:00.
     * 
     * @param strDate
     *            String representation of date.
     * @throws ParseException
     *             if <code>strDate</code> is in an invalid format.
     */
    public static Date stringToDate(String strDate) throws ParseException {
        int[] diffTime = null;
        boolean plusTime = true;

        // get time differences (if any)
        int idxT = strDate.indexOf('T');
        if (idxT == -1) {
            throw new ParseException("Invalid Date Format", 0);
        }

        int idxDiffUTC = strDate.indexOf('-', idxT);
        if (idxDiffUTC == -1) {
            idxDiffUTC = strDate.indexOf('+', idxT);
            plusTime = false;
        }

        if (idxDiffUTC != -1) {
            diffTime = getDiffTime(strDate, idxDiffUTC);
            strDate = strDate.substring(0, idxDiffUTC);
        }

        int idxMilliSec = strDate.indexOf('.');
        if (idxMilliSec != -1) {
            strDate = strDate.substring(0, idxMilliSec);
        } else {
            // remove the trailing z/Z character
            char lastChar = strDate.charAt(strDate.length() - 1);
            if ((lastChar == 'z') || (lastChar == 'Z')) {
                strDate = strDate.substring(0, strDate.length() - 1);
            }
        }

        return createDate(strDate, diffTime, plusTime);
    }

    /**
     * Returns the difference portion of a date string. Array of integer with
     * the first element defining the hour difference; and second element
     * defining the minute difference
     * 
     * @param strDate
     *            Date String.
     * @param idx
     *            index of the +/- character.
     * @returns the difference portion of a date string.
     * @throws ParseException
     *             if <code>strDate</code> is in an invalid format.
     */
    private static int[] getDiffTime(String strDate, int idx)
            throws ParseException {
        // discard the plus/minus char and trailing z char.
        String strDiff = strDate.substring(idx + 1, strDate.length() - 1);
        int[] diffArray = new int[2];
        int colonIdx = strDiff.indexOf(':');

        if (colonIdx == -1) {
            throw new ParseException("Invalid Date Format", 0);
        }

        try {
            diffArray[0] = Integer.parseInt(strDiff.substring(0, colonIdx));
            diffArray[1] = Integer.parseInt(strDiff.substring(colonIdx + 1));
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid Date Format", 0);
        }

        return diffArray;
    }

    /**
     * Returns a date with a string of yyyy-MM-ssThh:mm:ss or yyyy-MM-ssThh:mm.
     * 
     * @param strDate
     *            string representation of a date.
     * @param timeDiff
     *            time differences
     * @param plusDiff
     *            time differences (plus/minus). true indicates do a plus to the
     *            computed date. Ignore this value if <code>timeDiff</code> is
     *            null.
     */
    private static Date createDate(String strDate, int[] timeDiff,
            boolean plusDiff) throws ParseException {
        try {
            int year = Integer.parseInt(strDate.substring(0, 4));
            if (strDate.charAt(4) != '-') {
                throw new ParseException("Invalid Date Format", 0);
            }

            int month = Integer.parseInt(strDate.substring(5, 7)) - 1;
            if (strDate.charAt(7) != '-') {
                throw new ParseException("Invalid Date Format", 0);
            }

            int day = Integer.parseInt(strDate.substring(8, 10));
            if (strDate.charAt(10) != 'T') {
                throw new ParseException("Invalid Date Format", 0);
            }

            int hour = Integer.parseInt(strDate.substring(11, 13));
            if (strDate.charAt(13) != ':') {
                throw new ParseException("Invalid Date Format", 0);
            }

            int minute = Integer.parseInt(strDate.substring(14, 16));
            int second = 0;

            if (strDate.length() > 17) {
                if (strDate.charAt(16) != ':') {
                    throw new ParseException("Invalid Date Format", 0);
                }

                second = Integer.parseInt(strDate.substring(17, 19));
            }

            GregorianCalendar cal = new GregorianCalendar(year, month, day,
                    hour, minute, second);
            cal.setTimeZone(UTC_TIME_ZONE);

            if (timeDiff != null) {
                int hourDiff = (plusDiff) ? timeDiff[0] : (-1 * timeDiff[0]);
                int minuteDiff = (plusDiff) ? timeDiff[1] : (-1 * timeDiff[1]);
                cal.add(Calendar.HOUR, hourDiff);
                cal.add(Calendar.MINUTE, minuteDiff);
            }

            return cal.getTime();
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid Date Format", 0);
        }
    }

    /*
     * public static void main(String[] argv) { try {
     * System.out.println(toUTCDateFormat(new Date()));
     * System.out.println(stringToDate("2004-12-02T12:00:01z"));
     * System.out.println(stringToDate("2004-12-02T12:01:02.3z"));
     * System.out.println(stringToDate("2004-12-02T12:02:03.3Z"));
     * System.out.println(stringToDate("2004-12-02T12:03:04Z"));
     * System.out.println(stringToDate("2004-12-02T12:04Z"));
     * System.out.println(stringToDate("2004-12-02T12:05:00+05:10Z"));
     * System.out.println(stringToDate("2004-12-02T12:06:00-05:00Z"));
     * System.out.println(stringToDate("2004-12-02T12:07:00.0Z"));
     * System.out.println(stringToDate("2004-12-02T12:08:00.0-05:00Z")); } catch
     * (ParseException pe) { System.err.println(pe); } }
     */
}
