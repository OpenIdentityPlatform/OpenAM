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
 * $Id: InstantGroupRunnable.java,v 1.2 2008/06/25 05:52:51 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Instant task (task will only be run once) which has handler separated
 * (share the same ScheduleableGroupAction) can be scheduled to Timer or
 * TimerPool by using InstantGroupRunnable. All the elements in the same
 * InstantGroupRunnable must have the same scheduled time.
 */

public class InstantGroupRunnable extends InstantRunnable implements
    ScheduleableGroupAction {
    
    protected ScheduleableGroupAction target;
    
    /**
     * Constructor of InstantGroupRunnable.
     *
     * @param target The ScheduleableGroupAction interface to be run on the
     *        objects when there is time
     * @param removeElementAfterAction Whether to remove the elements after
     *        running the ScheduleableGroupAction on the objects
     */
    
    public InstantGroupRunnable(ScheduleableGroupAction target,
        boolean removeElementAfterAction) {
        super(removeElementAfterAction);
        this.target = target;
    }
    
    /**
     * Implements for TaskRunnable. Run the function of ScheduleableGroupAction
     * on all the objects 1 by 1.
     */
    
    public void run() {        
        allowToChange = false;
        synchronized (actions) {
            if (!actions.isEmpty()) {
                for (Iterator iter = actions.iterator(); iter.hasNext();) {
                    doGroupAction(iter.next());
                    if (removeElementAfterAction) {
                        iter.remove();
                    }
                }
            }
        }
        reset();
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
}
