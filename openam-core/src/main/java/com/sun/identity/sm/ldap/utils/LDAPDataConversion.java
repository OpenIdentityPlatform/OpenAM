/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package com.sun.identity.sm.ldap.utils;

import org.forgerock.opendj.ldap.GeneralizedTime;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Responsible for converting data types to and from compatible LDAP data types.
 *
 * @author robert.wapshott@forgerock.com
 */
public class LDAPDataConversion {
    /**
     * Convert the 'seconds since the epoch' value that is common in OpenAM to a Calendar.
     *
     * Note: The implication of this call is that the timestamp should be set to the local
     * timezone on the system this conversion is performed.
     *
     * @param epochedTimeInSeconds Number of seconds since the epoch.
     *
     * @return A non null Calendar representing the timestamp.
     */
    public Calendar fromEpochedSeconds(long epochedTimeInSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        long millis = epochedTimeInSeconds * 1000L;
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    /**
     * Convert a Calendar to the 'seconds from epoch' timestamp.
     *
     * Note: This function strips off the timezone information. Use with care.
     *
     * @param date A non null Calendar object.
     * @return A positive long.
     */
    public long toEpochedSeconds(Calendar date) {
        return date.getTimeInMillis()/1000L;
    }

    /**
     * Convert a Date to an LDAP date string.
     *
     * @param calendar Non null Calendar which contains both the timestamp and timezone information.
     *
     * @return A non null String formatted for LDAP.
     */
    public String toLDAPDate(Calendar calendar) {
        return GeneralizedTime.valueOf(calendar).toString();
    }

    /**
     * Parses an LDAP date string and converts this to a Java Date object.
     *
     * @param ldapDate The date to parse.
     * @return Null if there was a parsing error, otherwise a non null Date.
     */
    public Calendar fromLDAPDate(String ldapDate) {
        return GeneralizedTime.valueOf(ldapDate).toCalendar();
    }
}
