/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
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
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package org.forgerock.openam.utils;

/**
 * Simple Class to Provide Time Duration Calculation for certain
 * events within the OpenAM framework.
 *
 * @author jeff.schenk@forgerock.com
 */
public class TimeDuration {
    /**
     * Start Time in Milliseconds.
     */
    private long start = 0;
    /**
     * End Time in Milliseconds.
     */
    private long end = 0;
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
    } // End of start Method.

    /**
     * Set Current Time as End Time.
     */
    public void stop() {
        end = System.currentTimeMillis();
    } // End of stop Method.

    /**
     * Reset all Counters.
     */
    public void reset() {
        start = 0;
        end = 0;
    } // End of Reset Method.

    /**
     * Get current Duration.
     *
     * @return a long.
     */
    public long getCurrentDuration() {
        this.stop();
        return (end - start);
    } // End of getCurrentDuration Method.

    /**
     * Obtain data in String form.
     *
     * @return String representation of TimDuration.
     */
    public String toString() {
        return (getDurationToString());
    }

    /**
     * Get Elapsed Timing in String Form.
     *
     * @return String of Duration.
     */
    public String getDurationToString() {
        return (getDurationToString(getCurrentDuration()));
    }
    /**
     * Get Elapsed Timing in String Form.
     *
     * @param _duration long primitive.
     * @return String of Duration.
     */
    public static String getDurationToString(long _duration) {
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
    } // End of getDurationToString Method.

}
