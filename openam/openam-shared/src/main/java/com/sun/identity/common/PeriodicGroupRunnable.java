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
 * $Id: PeriodicGroupRunnable.java,v 1.2 2008/06/25 05:52:51 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Periodic task (task will be run periodically) which has handler separated
 * (share the same ScheduleableGroupAction) can be scheduled to Timer or
 * TimerPool by using PeriodicGroupRunnable. All the elements in the same
 * PeriodicGroupRunnable must have the same timeout period and run period.
 * Elements will be grouped by using the time they enter PeriodicGroupRunnable.
 * ScheduleableGroupAction will only go through the elements which entered
 * PeriodicGroupRunnable excess the timeout time.
 */

public class PeriodicGroupRunnable extends PeriodicRunnable implements
    ScheduleableGroupAction {
    
    protected ScheduleableGroupAction target;
    
    /**
     * Constructor of PeriodicGroupRunnable.
     *
     * @param target The ScheduleableGroupAction interface to be run on the
     *        objects when there is time
     * @param runPeriod Run period in ms
     * @param timeoutPeriod timeout period in ms
     * @param removeElementAfterAction Whether to remove the elements after
     *        running the ScheduleableGroupAction on the objects
     */
    
    public PeriodicGroupRunnable(ScheduleableGroupAction target,
        long runPeriod, long timeoutPeriod, boolean removeElementAfterAction)
        throws IllegalArgumentException {   
        super(runPeriod, timeoutPeriod, removeElementAfterAction);
        this.target = target;
    }
    
    /**
     * The function to be run on the objects when there is time.
     *
     * @param obj The object the function act on
     */
    
    public void doGroupAction(Object obj) {
        if (target != null) {
            target.doGroupAction(obj);
        }
    }
    
    /**
     * Implements for TaskRunnable. Run the function of ScheduleableGroupAction
     * on the objects in thisTurn 1 by 1, and interchange thisTurn and nextTurn.
     */
    
    public void run() {
        synchronized (thisTurn) {
            if (!thisTurn.isEmpty()) {
                for (Iterator iter = thisTurn.iterator(); iter.hasNext();) {
                    Object obj = iter.next();
                    doGroupAction(obj);
                    iter.remove();
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
