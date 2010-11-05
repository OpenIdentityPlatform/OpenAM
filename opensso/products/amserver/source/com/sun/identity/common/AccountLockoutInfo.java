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
 * $Id: AccountLockoutInfo.java,v 1.3 2008/06/25 05:42:24 qcheng Exp $
 *
 */

package com.sun.identity.common;

/**
 * <code>AccountLockoutInfo</code> defines a set of methods that are required
 * by the common account lockout class.
 */
public class AccountLockoutInfo {
    private int failCount = 0;

    private long lastFailTime = 0;

    private long lockoutAt = 0;

    private boolean locked = false;

    private int userWarningCount = 0;

    private String userToken = null;

    private long actualLockoutDuration=0;

    /**
     * Returns the current failure count stored in this object.
     * 
     * @return the current failure count stored in this object.
     */
    public int getFailCount() {
        return failCount;
    }

    /**
     * Returns the last fail time stored in this object.
     * 
     * @return the last fail time stored in this object.
     */
    public long getLastFailTime() {
        return lastFailTime;
    }

    /**
     * Returns the lockout time stored in this object.
     * 
     * @return the lockout time stored in this object.
     */
    public long getLockoutAt() {
        return lockoutAt;
    }
    
    /**
     * Returns the actual time the user needs be locked out.
     *
     * @return the actual time the user needs be locked out.
     */
     public long getActualLockoutDuration() {
         return actualLockoutDuration;
     }

    /**
     * Returns true if user was locked out
     * 
     * @return true if user was locked out
     */
    public boolean isLockout() {
        return locked;
    }

    /**
     * Sets the lockout failure count.
     * 
     * @param count
     *            lockout failure count
     */
    public void setFailCount(int count) {
        failCount = count;
    }

    /**
     * Sets the last fail time.
     * 
     * @param now
     *            time of failure
     */
    public void setLastFailTime(long now) {
        lastFailTime = now;
    }

    /**
     * Sets the lockout time.
     * 
     * @param now
     *            time that user is lockout
     */
    public void setLockoutAt(long now) {
        lockoutAt = now;
    }

    /**
     * Sets the actual Lockout Duration
     *
     * @param aActualLockoutDuration actul current lockout duration*/
    public void setActualLockoutDuration(long aActualLockoutDuration) {
        actualLockoutDuration = aActualLockoutDuration;
    }

    /**
     * Sets the lockout status.
     * 
     * @param locked
     *            lockout flag
     */
    public void setLockout(boolean locked) {
        this.locked = locked;
    }

    /**
     * Sets the warning count, this is the number of allowed failed attempts
     * after which account deactivation warning message will be displayed to the
     * user.
     * 
     * @param userWarningCount
     *            is the warning count
     */
    public void setWarningCount(int userWarningCount) {
        this.userWarningCount = userWarningCount;
    }

    /**
     * Returns the warning count.
     * 
     * @return the warning count.
     */
    public int getWarningCount() {
        return userWarningCount;
    }

    /**
     * Sets the user token.
     * 
     */
    public void setUserToken(String token) {
        userToken = token;
    }

    /**
     * Returns the user token.
     * 
     * @return the user token.
     */
    public String getUserToken() {
        return userToken;
    }
}
