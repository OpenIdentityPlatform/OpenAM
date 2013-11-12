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
 * $Id: TaskRunnable.java,v 1.3 2008/06/25 05:52:52 qcheng Exp $
 *
 */

package com.sun.identity.common;

/**
 * TaskRunnable is the basic scheduleable unit which defines the necessary
 * functions for TimerPool and RunnableTimerTask.
 */

public interface TaskRunnable extends Runnable {
    
    /**
     * Adds an element to this TaskRunnable.
     *
     * @param key Element to be added to this TaskRunnable
     * @return a boolean to indicate whether the add success
     */
    
    public boolean addElement(Object key);
    
    /**
     * Removes an element from this TaskRunnable.
     *
     * @param key Element to be removed from this TaskRunnable
     * @return A boolean to indicate whether the remove success
     */
    
    public boolean removeElement(Object key);
    
    /**
     * Indicates whether this TaskRunnable is empty.
     *
     * @return A boolean to indicate whether this TaskRunnable is empty
     */
    
    public boolean isEmpty();
    
    /** 
     * Sets the TaskRunnable next to this TaskRunnable in the linked-list.
     * 
     * It is for internal use only.
     *
     * @param task The next TaskRunnable
     */
    
    public void setNext(TaskRunnable task);
    
    /**
     * Sets the TaskRunnable previous to this TaskRunnable in the linked-list.
     * 
     * It is for internal use only.
     *
     * @param task The previous TaskRunnable
     */
    
    public void setPrevious(TaskRunnable task);
    
    /**
     * Sets the head task for this linkable TaskRunnable.
     *
     * The head task of this TaskRunnable will be set. HeadTask works as a lock
     * when the elements of the linked task is going to be changed. It is for
     * internal use only.
     *
     * @param headTask The HeadTaskRunnable
     */
    
    public void setHeadTask(HeadTaskRunnable headTask);
    
    /**
     * Returns the head task for this linkable TaskRunnable.
     *
     * The head task of this TaskRunnable will be returned or null if it doesn't
     * have one. HeadTask works as a lock when the elements of the linked task
     * is going to be changed.
     *
     * @return The head task of this linkable TaskRunnable
     */
    
    public HeadTaskRunnable getHeadTask();
    
    /**
     * Returns the run period of this TaskRunnable.
     *
     * @return A long value to indicate the run period
     */
    
    public long getRunPeriod();
    
    /**
     * Returns the TaskRunnable next to this TaskRunnable in the linked-list.
     *
     * @return next TaskRunnable object or null if it is not set
     */
    
    public TaskRunnable next();
    
    /**
     * Returns the TaskRunnable previous to this TaskRunnable in the
     * linked-list.
     *
     * @return previous TaskRunnable object or null if it is not set
     */
    
    public TaskRunnable previous();
    
    /**
     * Returns the scheduled time of this TaskRunnable.
     *
     * @return A long value indicate the time this TaskRunnable is scheduled,
     *         or -1 if it is not scheduled yet
     */
    
    public long scheduledExecutionTime();
    
    /**
     * Cancel the task from scheduled Timer.
     */
    
    public void cancel();
} 
