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

/**
 * Provides Simple Time Duration Object for calculating during of task or other
 * invocations.
 *
 * @author jeff.schenk@forgerock.com
 * @version $Id: $
 */
public class TimeDuration {

    private long start = 0; // Start Time in Milliseconds.
    private long end = 0; // End Time in Milliseconds.
    private long duration = 0; // Duration Time in Milliseconds.

    /**
     * Provides Default Constructor.
     */
    public TimeDuration() {
    } // End of Constructor.

    /**
     * Set Current Time as Start Time.
     */
    public void start() {
        start = System.currentTimeMillis();
        end = start;
        duration = 0;
    } // End of start Method.

    /**
     * Set Current Time as End Time.
     */
    public void stop() {
        end = System.currentTimeMillis();
        duration = (end - start);
    } // End of stop Method.

    /**
     * Reset all Counters.
     */
    public void reset() {
        start = 0;
        end = 0;
        duration = 0;
    } // End of Reset Method.

    /**
     * Get current Duration.
     *
     * @return a long.
     */
    public long getCurrentDuration() {
//        stop();
        return (duration);
    } // End of getCurrentDuration Method.

    /**
     * Obtain data in String form.
     *
     * @return String representation of TimDuration.
     */
    public String toString() {
        return (getElapsedtoString());
    } // End of toString Override method.

    /**
     * Get Elapsed Timing in String Form.
     *
     * @return String of Duration.
     */
    public String getElapsedtoString() {
        return (getElapsedtoString(duration));
    } // End of getElaspedtoString with no Parameters.

    /**
     * Get Elapsed Timing in String Form.
     *
     * @param _duration long primitive.
     * @return String of Duration.
     */
    public static String getElapsedtoString(long _duration) {
        long days;
        long hours;
        long minutes;
        long seconds;
        long milliseconds;

        // **************************************
        // First Convert Duration into Seconds.
        long timeInSeconds = _duration / 1000;

        if (timeInSeconds <= 0) {
            return (_duration + "ms");
        }

        // *****************************
        // Save our Milliseconds.
        milliseconds = (_duration - (timeInSeconds * 1000));

        // *****************************
        // Now Convert the seconds.
        days = timeInSeconds / (3600 * 24);
        timeInSeconds = timeInSeconds - (days * (3600 * 24));
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;
        if (days > 0) {
            return (days + "d:" + hours + "h:" + minutes + "m:" + seconds + "s:"
                    + milliseconds + "ms");
        } else if (hours > 0) {
            return (hours + "h:" + minutes + "m:" + seconds + "s:"
                    + milliseconds + "ms");
        } else if (minutes > 0) {
            return (minutes + "m:" + seconds + "s:" + milliseconds + "ms");
        } else {
            return (seconds + "s:" + milliseconds + "ms");
        }
    } // End of getElapsedtoString Method.

    /**
     * Get Elapsed Timing in String Form.
     *
     * @param _duration long primitive.
     * @return String of Duration.
     */
    public static String getElapsedToTextString(long _duration) {
        long days;
        long hours;
        long minutes;
        long seconds;
        long milliseconds;

        // **************************************
        // First Convert Duration into Seconds.
        long timeInSeconds = _duration / 1000;

        if (timeInSeconds <= 0) {
            return "Now";
        }

        // *****************************
        // Save our Milliseconds.
        milliseconds = (_duration - (timeInSeconds * 1000));

        // *****************************
        // Now Convert the seconds.
        days = timeInSeconds / (3600 * 24);
        timeInSeconds = timeInSeconds - (days * (3600 * 24));
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;
        if (days > 0) {
            return (days + " Day" + checkPlurels(days) + ", "
                    + hours + " Hour" + checkPlurels(hours) + ", "
                    + minutes + " Minute" + checkPlurels(minutes) + ", "
                    + seconds + " Second" + checkPlurels(seconds));
        } else if (hours > 0) {
            return ( hours + " Hour" + checkPlurels(hours) + ", "
                    + minutes + " Minute" + checkPlurels(minutes) + ", "
                    + seconds + " Second" + checkPlurels(seconds));
        } else if (minutes > 0) {
            return (minutes + " Minute" + checkPlurels(minutes) + ", "
                    + seconds + " Second" + checkPlurels(seconds));
        } else {
            return (seconds + " Second" + checkPlurels(seconds));
        }
    } // End of getElapsedToTextString Method.

    /**
     * Simple Private Helper to determine if the
     * Time Designation should be plurel or not.
     *
     * @param number
     * @return String or either "" or "s".
     */
    protected static String checkPlurels(long number) {
        if (number > 1) {
            return "s";
        } else {
            return "";
        }
    }

} // /:>~ End of TimeDuration Class.
