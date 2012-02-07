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
 * $Id: HeadTaskRunnable.java,v 1.4 2008/06/25 05:52:51 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.Date;
import java.util.Set;

/**
 * HeadTaskRunnable is designed to be the head of the linked-list when
 * TaskRunnable needs to be linked together. Whenever there is an insertion or
 * deletion, HeadTaskRunnable must be locked to guarantee correct
 * synchronization. Besides, when last element of the linked-list is removed
 * (setNext(null)), HeadTaskRunnable should use Triggerable to remove or destroy
 * the linked-list.
 */

public class HeadTaskRunnable implements TaskRunnable {
    
    protected Date time;
    protected volatile TaskRunnable nextTask;
    protected volatile TaskRunnable tailTask;
    protected volatile boolean expired;
    protected volatile boolean timeout;
    protected int waitCount;
    protected int acquireCount;
    protected volatile Thread owner;
    protected Triggerable parent;
    
    /**
     * Constructor of HeadTaskRunnable.
     *
     * @param parent The Triggerable interface to be run when the linked-list is
     *        empty
     * @param nextTask The TaskRunnable next to this TaskRunnable
     * @param time The time this TaskRunnable is scheduled
     */
    
    public HeadTaskRunnable(Triggerable parent, TaskRunnable nextTask,
        Date time) throws IllegalArgumentException {
        if ((time == null) || (nextTask == null)) {
            throw new IllegalArgumentException();
        }
        this.time = time;
        this.owner = null;
        this.nextTask = nextTask;
        this.tailTask = nextTask;
        this.expired = false;
        this.timeout = false;
        this.waitCount = 0;
        this.acquireCount = 0;
        this.nextTask.setHeadTask(this);
        this.nextTask.setPrevious(this);
        this.parent = parent;
    }
    
    /**
     * Sets the status of the HeadTask to expired.
     */
    
    protected synchronized void expire() throws IllegalMonitorStateException {
        if (owner == Thread.currentThread()) {
            expired = true;
        } else {
            throw new IllegalMonitorStateException(
                "The calling thread is not the owner of the lock!");
        }
    }
    
    /**
     * Sets the status of the HeadTask to timeout.
     */
    
    protected synchronized void timeout() throws IllegalMonitorStateException {
        if (owner == Thread.currentThread()) {
            timeout = true;
        } else {
            throw new IllegalMonitorStateException(
                "The calling thread is not the owner of the lock!");
        }
    }
    
    /**
     * Returns a boolean to indicate whether the HeadTask is timeout already.
     *
     * @return a boolen to indicate whether the HeadTask is timeout.
     */
    
    public boolean isTimedOut() {
        return timeout;
    }
    
    /**
     * Returns a boolean to indicate whether the HeadTask is expired already.
     *
     * @return a boolean to indicate whether the HeadTask is expired.
     */
    
    public boolean isExpired() {
        return expired;
    }
    
    /**
     * Returns the thread which currently holding this lock.
     *
     * @return the thread which currently own the lock or null.
     */
    
    public Thread getCurrentOwner() {
        return owner;
    }
    
    /**
     * Tries to acquire a valid (non-expired) lock.
     *
     * @return a boolean to indicate whether it is succeed to acquire the lock.
     */
    
    public synchronized boolean acquireValidLock() {
        while (!expired) {
            if (owner == null) {
                owner = Thread.currentThread();
                acquireCount = 1;
                return true;
            } else {
                if (owner != Thread.currentThread()) {
                    try {
                        waitCount++;
                        this.wait();
                        waitCount--;
                    } catch (InterruptedException ex){
                        //ignored
                    }
                } else {
                    acquireCount++;
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Releases the currently holding lock.
     */
    
    public synchronized void releaseLockAndNotify() throws
        IllegalMonitorStateException {
        if (owner == Thread.currentThread()) {
            if (acquireCount > 1) {
                acquireCount--;
            } else {
                owner = null;
                acquireCount = 0;
                if (waitCount > 0) {
                    if (expired) {
                        this.notifyAll();
                    } else {
                        this.notify();
                    }
                }
            }
        } else {
            throw new IllegalMonitorStateException(
                "The calling thread is not the owner of the lock!");
        }
    }
    
    /**
     * Implements for TaskRunnable interface, no actual use for HeadTaskRunnable.
     */
    
    public void setHeadTask(HeadTaskRunnable headTask) {
    }
    
    /**
     * Implements for TaskRunnable interface, no actual use for HeadTaskRunnable.
     */
    
    public HeadTaskRunnable getHeadTask() {
        return null;
    }
    
    /**
     * Implements for TaskRunnable interface, always return false.
     *
     * @return false means nothing can be added to this TaskRunnable
     */
    
    public boolean addElement(Object key) {
        return false;
    }
    
    /**
     * Implements for TaskRunnable interface, always return false.
     *
     * @return false means nothing can be removed from this TaskRunnable
     */
    
    public boolean removeElement(Object key) {
        return false;
    }
    
    /**
     * Implements for TaskRunnable interface, always return false.
     *
     * @return true means this TaskRunnable is always empty
     */
    
    public boolean isEmpty() {
        return true;
    }
    
    /** 
     * Sets the TaskRunnable next to this TaskRunnable in the linked-list.
     *
     * @param task The next TaskRunnable
     */
    
    public void setNext(TaskRunnable task) {
        if (task == null) {
            synchronized (this) {
                if (parent != null) {
                    parent.trigger(time);
                }
            }
            nextTask = null;
        } else {
            nextTask = task;
        }
    }
    
    /**
     * Implements for TaskRunnable interface, There is no previous element for
     * HeadTaskRunnable.
     */
    
    public void setPrevious(TaskRunnable task) {
    }
    
    /**
     * Returns the TaskRunnable next to this TaskRunnable in the linked-list.
     *
     * @return next TaskRunnable object or null if it is not set
     */
    
    public TaskRunnable next() {
        return nextTask;
    }
    
    /**
     * Implements for TaskRunnable interface, there is no previous element for
     * HeadTaskRunnable.
     *
     * @return null means there is no previous element
     */
    
    public TaskRunnable previous() {
        return null;
    }
    
    /**
     * Implements for TaskRunnable interface, HeadTaskRunnable doesn't have a
     * run period.
     *
     * @return -1 means the task only will be run once
     */
    
    public long getRunPeriod() {
        return -1;
    }
    
    /**
     * Implements for TaskRunnable interface.
     */
    
    public void cancel() {
        if (acquireValidLock()) {
            try {
                synchronized (this) {
                    if (parent != null) {
                        parent.trigger(time);
                    }
                }
            } finally {
                releaseLockAndNotify();
            }
        }
    }
    
    /**
     * Sets the Triggerable interface which will be run when the linked-list is
     * empty.
     *
     * @param parent The Triggerable interface to be run when the linked-list is
     *        empty
     */
    
    public void setTrigger(Triggerable parent) {
        synchronized (this) {
            this.parent = parent;
        }
    }
    
    /**
     * Sets the task which is the tail of the list.
     *
     * It is for internal use only.
     *
     * @param task The task which is at the tail of the list.
     */
    
    public void setTail(TaskRunnable task) {        
        tailTask = task;
    }
    
    /**
     * Returns the Task which is at the tail of the list.
     *
     * @return The task which is at the tail of the list.
     */
    
    public TaskRunnable tail() {
        return tailTask;
    }
    
    /**
     * Returns the time which this HeadTaskRunnable is scheduled.
     *
     * @return The long value which represents the time this task is scheduled
     */
    
    public long scheduledExecutionTime() {
        if (expired) {
            return -1;
        } else {
            return time.getTime();
        }
    }
    
    /**
     * Implements for TaskRunnable interface, just run the next TaskRunnable.
     */
    
    public void run() {    
        TaskRunnable taskToRun = next();
        do {
            taskToRun.run();
        } while ((taskToRun = taskToRun.next()) != null);
    }
    
}
