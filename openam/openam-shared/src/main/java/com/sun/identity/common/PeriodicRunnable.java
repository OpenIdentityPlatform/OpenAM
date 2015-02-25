/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PeriodicRunnable.java,v 1.2 2008/06/25 05:52:51 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Periodic task (task will be run periodically) which has handler integrated
 * (implements ScheduleableAction) can be scheduled to Timer or TimerPool by
 * using PeriodicRunnable. All the elements in the same PeriodicRunnable must
 * have the same timeout period and run period. Elements will be grouped by
 * using the time they enter PeriodicRunnable. Elements which entered
 * PeriodicRunnable excess the timeout time will be checked 1 by 1 and
 * ScheduleableAction (doAction()) will be invoked.
 */

public class PeriodicRunnable extends GeneralTaskRunnable {
    
    protected Set thisTurn;
    protected Set[] nextTurn;
    protected long runPeriod;
    protected long timeoutPeriod;
    protected int containerNeeded;
    protected boolean removeElementAfterAction;
    
    /**
     * Constructor of PeriodicRunnable.
     *
     * @param runPeriod The period of running this PeriodicRunnable
     * @param timeoutPeriod The timeout period of the objects in this
     *        PeriodicRunnable
     * @param removeElementAfterAction Whether to remove the elements after
     *        running the ScheduleableAction on the objects
     */
    
    public PeriodicRunnable(long runPeriod, long timeoutPeriod,
        boolean removeElementAfterAction) throws
        IllegalArgumentException {   
        if ((runPeriod < 0) || (timeoutPeriod < 0)){
            throw new IllegalArgumentException();
        }
        containerNeeded = (int) (timeoutPeriod / runPeriod);
        if ((timeoutPeriod % runPeriod) > 0) {
            containerNeeded++;
        }
        this.runPeriod = runPeriod;
        this.timeoutPeriod = timeoutPeriod;
        this.removeElementAfterAction = removeElementAfterAction;
        thisTurn = new HashSet();
        nextTurn = new HashSet[containerNeeded];
        for (int i = 0; i < containerNeeded; i++) {
            nextTurn[i] = new HashSet();
        }
    }
    
    /**
     * Adds an element to this PeriodicRunnable.
     *
     * @param obj Element to be added to this PeriodicRunnable
     * @return a boolean to indicate whether the add success
     */
    
    public boolean addElement(Object obj) {
        synchronized (nextTurn[containerNeeded - 1]) {
            return nextTurn[containerNeeded - 1].add(obj);
        }
    }
    
    /**
     * Removes an element from this PeriodicRunnable.
     *
     * @param obj Element to be removed from this PeriodicRunnable
     * @return A boolean to indicate whether the remove success
     */
    
    public boolean removeElement(Object obj) {
        // if the item is in groupNextTurn, don't lock groupThisTurn that won't
        // block if the cleanup is in process
        synchronized (nextTurn[containerNeeded - 1]) {
            if (!nextTurn[containerNeeded - 1].remove(obj)) {
                for (int i = (containerNeeded - 2); i >= 0 ; i--) {
                    if (nextTurn[i].remove(obj)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        synchronized (thisTurn) {
            return thisTurn.remove(obj);
        }
    }
    
    /**
     * Returns the timeout period of this PeriodicRunnable.
     *
     * @return A long value to indicate the timeout period
     */
    
    public long getTimeoutPeriod() {
        return timeoutPeriod;
    }
    
    /**
     * Indicates whether this PeriodicRunnable is empty.
     *
     * @return A boolean to indicate whether this PeriodicRunnable is empty
     */
    
    public boolean isEmpty() {
        return false;
    }
    
    /**
     * Returns the run period of this PeriodicRunnable.
     *
     * @return A long value to indicate the run period
     */
    
    public long getRunPeriod() {
        return runPeriod;
    }
    
    /**
     * Implements for TaskRunnable. Run the function of ScheduleableAction on
     * all the objects in thisTurn 1 by 1, and interchange thisTurn and
     * nextTurn.
     */
    
    public void run() {
        synchronized (thisTurn) {
            if (!thisTurn.isEmpty()) {
                for (Iterator iter = thisTurn.iterator(); iter.hasNext();) {
                    ScheduleableAction action = (ScheduleableAction)
                        iter.next();
                    action.doAction();
                    if (removeElementAfterAction) {
                        iter.remove();
                    }
                }
            }
        }
        synchronized (nextTurn[containerNeeded - 1]) {
            Set tempSet = thisTurn;
            for (int i = 0; i < containerNeeded + 1; i++) {
                if (i == 0) {
                    thisTurn = nextTurn[0];
                } else {
                    if (i == containerNeeded) {
                        nextTurn[containerNeeded - 1] = tempSet;
                    } else {
                        nextTurn[i - 1] = nextTurn[i];
                    }
                }
            }
        }
    }
} 
