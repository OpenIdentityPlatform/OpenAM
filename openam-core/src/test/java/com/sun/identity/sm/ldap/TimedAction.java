package com.sun.identity.sm.ldap;

import org.apache.commons.lang.time.StopWatch;

/**
 * A simple timing class for timing an action.
 *
 * @author robert.wapshott@forgerock.com
 */
public abstract class TimedAction {

    public static final float ITERATIONS = 1000;
    private float micros;

    /**
     * Perform the action and time the results.
     *
     * @return The number of microseconds the operation took to complete.
     */
    public float go() {
        StopWatch watch = new StopWatch();
        watch.start();
        for (int ii = 0; ii < ITERATIONS; ii++) {
            action();
        }
        watch.stop();
        // Stopwatch times to milliseconds, so divide 1000 to microseconds.
        micros = (float) watch.getTime() / ITERATIONS;
        return micros;
    }

    /**
     * Action to perform under timed conditions.
     */
    public abstract void action();

    @Override
    public String toString() {
        return micros + " microseconds";
    }
}