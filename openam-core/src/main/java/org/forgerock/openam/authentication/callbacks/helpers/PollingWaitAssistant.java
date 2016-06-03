package org.forgerock.openam.authentication.callbacks.helpers;
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

import org.forgerock.openam.utils.Time;
import java.util.concurrent.Future;

/**
 * The PollingWaitAssistant class is used by an authentication module to assist in the completion of the PollingWait
 * callback. It requires a Future object (Promise extends Future) to check if the waiting is over.  The
 * getPollingWaitState method getPollingWaitState returns true if the
 * future reports isDone as true and false otherwise.  If the timeout value is exceeded before the future returns
 * isDone as true then getPollingWaitState will throw an AuthLoginException.
 *
 * This class handles the incremental back off of the wait value and the spam checking.
 */
public class PollingWaitAssistant {

    private long timeoutInMilliSeconds;

    private long longElapsedThreshold;
    private long mediumElapsedThreshold;

    private PollingWaitSpamChecker spamChecker = new PollingWaitSpamChecker();

    private long startTime;

    private Future<?> finishFutureEvent;
    private boolean started = false;

    private long shortTimeout, medTimeout, longTimeout;

    /**
     * Create a new PollingWaitAssistant from the parent login module with a timeout value.
     *
     * @param timeoutInMilliSeconds the timeout period before throwing an exception
     * @param shortTimeout the period for which we are considered 'short' frequency polling.
     * @param medTimeout the period for which we are considered 'medium' frequency polling.
     * @param longTimeout the period for which we are considered 'long'frequency  polling.
     */
    public PollingWaitAssistant(final long timeoutInMilliSeconds, final long shortTimeout, final long medTimeout,
                                final long longTimeout) {
        this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        this.longElapsedThreshold = timeoutInMilliSeconds / 2;
        this.mediumElapsedThreshold = timeoutInMilliSeconds / 4;

        this.shortTimeout = shortTimeout;
        this.medTimeout = medTimeout;
        this.longTimeout = longTimeout;
    }

    /**
     * Create a new PollingWaitAssistant from the parent login module with a timeout value. Uses the default
     * polling timeout frequency within this timeout.
     *
     * @param timeoutInMilliSeconds the timeout period before throwing an exception
     */
    public PollingWaitAssistant(final long timeoutInMilliSeconds) {
        this(timeoutInMilliSeconds, 5000L, 4000L, 8000L);
    }

    /**
     * Starts this callback helper, setting up the future to watch and the startTime as now.
     * @param finishFutureEvent the future event that will trigger the completion of the callback.
     */
    public void start(Future<?> finishFutureEvent) {
        this.finishFutureEvent = finishFutureEvent;
        this.startTime = Time.currentTimeMillis();
        this.started = true;
        resetWait();
    }

    /**
     * This method checks to see if the wait is complete and if not sets the new wait timeout.
     *
     * @return true if the wait is over and false if it is not.
     */
    public PollingWaitState getPollingWaitState() {

        if (!started) {
            return PollingWaitState.NOT_STARTED;
        }
        if ( Time.currentTimeMillis() - startTime > timeoutInMilliSeconds) {
            return PollingWaitState.TIMEOUT;
        }
        if (!spamChecker.isWaitLongEnough()) {
            spamChecker.incrementSpamCheck();
            if (spamChecker.isSpammed()) {
                return PollingWaitState.SPAMMED;
            }
            return PollingWaitState.TOO_EARLY;
        }

        if (finishFutureEvent.isDone()) {
            return PollingWaitState.COMPLETE;
        }
        return PollingWaitState.WAITING;
    }

    /**
     * Gets the current wait period for the callback at this stage in the polling period.
     *
     * @return the current wait period in milliseconds
     */
    public long getWaitPeriod() {
        long elapsed = Time.currentTimeMillis() - startTime;
        if (elapsed > longElapsedThreshold) {
            return longTimeout;
        }
        if (elapsed > mediumElapsedThreshold) {
            return medTimeout;
        }
        return shortTimeout;
    }

    /**
     * Indicate that a new wait period has started and the spam checker should reset.
     */
    public void resetWait() {
        spamChecker.resetSpamCheck(getWaitPeriod());
    }

    /**
     * The polling wait is used to indicate the current wait situation.
     */
    public enum PollingWaitState {
        /** The Polling wait has not started. */
        NOT_STARTED,
        /** The Polling wait should continue waiting and send a new wait callback. */
        WAITING,
        /** The wait is complete. */
        COMPLETE,
        /** The wait has exceeded the maximum permitted timeout. */
        TIMEOUT,
        /** The spam check has detected that too many requests have occurred. */
        SPAMMED,
        /** The spam check has detected that it is too early to service a new request, but it has not had so many
         * requests that it should fail the authentication yet. */
        TOO_EARLY
    }
}
