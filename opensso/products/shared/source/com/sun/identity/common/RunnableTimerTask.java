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
 * $Id: RunnableTimerTask.java,v 1.2 2008/06/25 05:52:52 qcheng Exp $
 *
 */

package com.sun.identity.common;

/**
 * RunnableTimerTask is a TimerTask which will use Recoverable interface
 * whenever Exception or Error may stop the running Timer or make the Timer to
 * an unknown state.
 */

public class RunnableTimerTask extends RecoverableTimerTask {
    
    protected Runnable target;
    
    /**
     * Constructor of RunnableTimerTask
     *
     * @param target The runnable interface to be run
     */
    
    public RunnableTimerTask(Runnable target) {
        super();
        this.target = target;
    }
    
    /**
     * Assigns the runnable interface to this RunnableTimerTask
     *
     * @param target The runnable to assign
     */
    
    public synchronized void setRunnable(Runnable target) {
        this.target = target;
    }
    
    /**
     * Returns the runnable interface from this RunnableTimerTask
     *
     * @return Runnable interface assoicated with this RunnableTimerTask
     */
    
    public synchronized Runnable getRunnable() {
        return target;
    }
    
    /**
     * The run method with error handling and recovery.
     */
    
    public void run() {
        try {
            Runnable localTarget = null;
            synchronized (this) {
                localTarget = target;
            }
            if (localTarget != null) {
                localTarget.run();
            }
        } catch(RuntimeException ex) {
            // log something here
            synchronized (this) {
                if (recoverable != null) {
                    recoverable.recover();
                }
            }
        } catch(Exception ex) {
            // log something here
        } catch(Throwable t) {
            // log something here
            synchronized (this) {
                if (recoverable != null) {
                    recoverable.recover();
                }
            }
            throw new Error(t);
        }
    }    
} 
