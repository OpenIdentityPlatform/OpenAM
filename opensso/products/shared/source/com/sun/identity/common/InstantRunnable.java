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
 * $Id: InstantRunnable.java,v 1.3 2008/06/25 05:52:51 qcheng Exp $
 *
 */

package com.sun.identity.common;

import com.sun.identity.common.HeadTaskRunnable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Instant task (task will only be run once) which has handler integrated
 * (implements ScheduleableAction) can be scheduled to Timer or TimerPool by
 * using InstantRunnable. All the elements in the same InstantRunnable must have
 * the same scheduled time.
 */

public class InstantRunnable extends GeneralTaskRunnable {
    
    protected Set actions;
    protected boolean removeElementAfterAction;
    protected volatile boolean allowToChange;
    
    /**
     * Constructor of InstantRunnable.
     *
     * @param removeElementAfterAction Whether to remove the elements after
     *        running the ScheduleableAction on the objects
     */
    
    public InstantRunnable(boolean removeElementAfterAction) {
        this.actions = new HashSet();
        this.allowToChange = true;
        this.removeElementAfterAction = removeElementAfterAction;
    }
    
    /**
     * Implements for TaskRunnable. Run the function of ScheduleableAction on
     * all the objects 1 by 1.
     */
    
    public void run() {        
        allowToChange = false;
        synchronized (actions) {
            if (!actions.isEmpty()) {
                for (Iterator iter = actions.iterator(); iter.hasNext();) {
                    ((ScheduleableAction)iter.next()).doAction();
                    if (removeElementAfterAction) {
                        iter.remove();
                    }
                }
            }
        }
        reset();
    }
    
    /**
     * Adds an element to this InstantRunnable.
     *
     * @param obj Element to be added to this InstantRunnable
     * @return a boolean to indicate whether the add success
     */
    
    public boolean addElement(Object obj) {
        // use a boolean variable to stop the element to be added after run()
        // has gone through the Set since InstantTask will not be rerun.
        synchronized (actions) {
            if (allowToChange) {
                return actions.add(obj);
            }
        }
        return false;
    }
    
    /**
     * Removes an element from this InstantRunnable.
     *
     * @param obj Element to be removed from this InstantRunnable
     * @return A boolean to indicate whether the remove success
     */
    
    public boolean removeElement(Object obj) {
        synchronized (actions) {
            if (allowToChange) {
                boolean result = actions.remove(obj);
                if (actions.isEmpty()) {
                    if (headTask != null) {
                        cancel();
                    }
                }
                return result;
            }
        }
        return false;
    }
    
    /**
     * Indicates whether this InstantRunnable is empty.
     *
     * @return A boolean to indicate whether this InstantRunnable is empty
     */
    
    public boolean isEmpty() {
        synchronized (actions) {
            if (allowToChange) {
                return actions.isEmpty();
            }
        }
        return true;
    }
    
    /**
     * Returns the run period of this InstantRunnable.
     *
     * @return -1 means this task doesn't have a run period
     */
    
    public long getRunPeriod() {
        return -1;
    }
    
    /**
     * Resets this InstantRunnable to allow elements to be added.
     */
    
    public void reset() {
        allowToChange = true;
    }
}
