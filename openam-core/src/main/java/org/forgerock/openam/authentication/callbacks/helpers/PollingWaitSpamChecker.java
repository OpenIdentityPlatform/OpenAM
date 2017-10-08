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

/**
 * The PollingWaitSpamChecker is a tool for verifying that the client using the PollingWait callback is polling at
 * an acceptable interval.
 */
public class PollingWaitSpamChecker {

    private static final int DEFAULT_SPAM_GRACE_PERIOD = 2000;
    private static final int DEFAULT_MAX_SPAM_REQUEST = 3;

    private long lastSendTime = 0;
    private long currentWaitPeriod = 3000;
    private int spamStrike = 0;
    private long spamGracePeriod;
    private int maxSpamRequest;

    /**
     * Constructor for the spam checker, setting a custom gr`ce period and strike count.
     * @param spamGracePeriod the time within which we call the return call 'close enough' to the current wait period
     *                        to be considered not spam
     * @param maxSpamRequest the number of early request we will tolerate before we throw an exception.
     */
    public PollingWaitSpamChecker(long spamGracePeriod, int maxSpamRequest) {
        this.spamGracePeriod = spamGracePeriod;
        this.maxSpamRequest = maxSpamRequest;
    }

    /**
     * Default Constructor for the spam checker.
     */
    public PollingWaitSpamChecker() {
        this(DEFAULT_SPAM_GRACE_PERIOD, DEFAULT_MAX_SPAM_REQUEST);
    }

    /**
     * increments teh number of hits on this spamChecker
     */
    public void incrementSpamCheck() {
        spamStrike++;
    }

    /**
     * Checks for spam requests.  If the caller is following protocol this method will return true.
     * However if the call has been made too many times (but not so many that it triggers a network level DOS block)
     * then it will throw an exception and the authentication module will fail.  To avoid this failure pay attention
     * to the wait timeout period returned in the callback object and ensure that this spam checker is
     *
     * @return true if the spam check has detected a spam attempt
     */
    public boolean isSpammed() {
        return !isWaitLongEnough() && spamStrike > maxSpamRequest;
    }

    /**
     * Determines if the wait period has been long enough to service te request.
     * @return true if the wait request has waited long enough.
     */
    public boolean isWaitLongEnough() {
        long elapsed = Time.currentTimeMillis() - lastSendTime;
        if (elapsed > currentWaitPeriod - spamGracePeriod) {
            return true;
        }
        return false;
    }

    /**
     * Sets the wait period to base the spamming detection from.
     * @param currentWaitPeriod the time period in milliseconds that this spam checker it working against.
     */
    public void resetSpamCheck(long currentWaitPeriod) {
        this.currentWaitPeriod = currentWaitPeriod;
        this.lastSendTime = Time.currentTimeMillis();
        spamStrike = 0;
    }
}

