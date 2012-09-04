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

import java.util.Calendar;

/**
 * Simple TimeStamp Object to provide various timestamp formats
 *
 * @author Jeff.Schenk@forgerock.com
 * @version $Id: $
 */
public class TimeStamp {
    /**
     * <p>getTimeStamp</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public static final String getTimeStamp() {
        return getTimeStampForFileName(Calendar.getInstance());
    } // End of getTimestamp Method.

    /**
     * <p>getTimeStampForFileName</p>
     *
     * @param current_calendar a {@link java.util.Calendar} object.
     * @return a {@link java.lang.String} object.
     */
    public static final String getTimeStampForFileName(Calendar current_calendar) {
        StringBuffer sb = new StringBuffer();
        sb.append( formatDigit(current_calendar.get(Calendar.YEAR)) );
        sb.append( formatDigit(current_calendar.get(Calendar.MONTH)+1) );
        sb.append( formatDigit(current_calendar.get(Calendar.DAY_OF_MONTH)) );
        sb.append( formatDigit(current_calendar.get(Calendar.HOUR)) );
        sb.append( formatDigit(current_calendar.get(Calendar.MINUTE)) );
        sb.append( formatDigit(current_calendar.get(Calendar.SECOND)) );
        sb.append( current_calendar.get(Calendar.MILLISECOND) );

        // ***********************
        // Return the String.
        return sb.toString();
    } // End of getTimestamp Method.

    /**
     * <p>getGenerationTimeStamp</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public static final String getGenerationTimeStamp() {
        return getTimeStampForReadableText(Calendar.getInstance());
    } // End of getTimestamp Method.

    /**
     * <p>getTimeStampForReadableText</p>
     *
     * @param current_calendar a {@link java.util.Calendar} object.
     * @return a {@link java.lang.String} object.
     */
    public static final String getTimeStampForReadableText(Calendar current_calendar) {
        StringBuffer sb = new StringBuffer();
        sb.append( formatDigit(current_calendar.get(Calendar.YEAR))+"-" );
        sb.append( formatDigit(current_calendar.get(Calendar.MONTH)+1)+"-" );
        sb.append( formatDigit(current_calendar.get(Calendar.DAY_OF_MONTH))+" " );
        sb.append( formatDigit(current_calendar.get(Calendar.HOUR_OF_DAY))+":" );
        sb.append( formatDigit(current_calendar.get(Calendar.MINUTE))+":" );
        sb.append( formatDigit(current_calendar.get(Calendar.SECOND))+"." );
        sb.append( current_calendar.get(Calendar.MILLISECOND) );

        // ***********************
        // Return the String.
        return sb.toString();
    } // End of getTimestamp Method.

    /**
     * Private Helper Method to Format Digits.
     */
    private static String formatDigit(int number) {
        if (number > 9)
        { return Integer.toString(number); }
        else { return  "0" + (Integer.toString(number)); }
    } // End of private Helper Method.
} ///:>~ End of TimeStamp Public Utility Class.
