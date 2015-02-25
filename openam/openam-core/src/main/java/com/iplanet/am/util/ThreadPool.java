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
 * $Id: ThreadPool.java,v 1.10 2008/10/04 00:11:46 arviranga Exp $
 *
 */

package com.iplanet.am.util;

import com.sun.identity.shared.debug.Debug;

/**
 * <p>
 * This thread pool maintains a number of threads that run the tasks from a task
 * queue one by one. The tasks are handled in asynchronous mode, which means it
 * will not block the main thread to proceed while the task is being processed
 * by the thread pool.
 * <p>
 * This thread pool has a fixed size of threads. It maintains all the tasks to
 * be executed in a task queue. Each thread then in turn gets a task from the
 * queue to execute. If the tasks in the task queue reaches a certain number(the
 * threshold value), it will log an error message and ignore the new incoming
 * tasks until the number of un-executed tasks is less than the threshold value.
 * This guarantees the thread pool will not use up the system resources under
 * heavy load.
 * @supported.all.api
 */
public class ThreadPool {

    private int poolSize;
    private int threshold;
    private String poolName;
    private Debug debug;
    private java.util.ArrayList taskList;
    private int busyThreadCount;
    private int currentThreadCount;
    private boolean shutdownThePool;
    private boolean daemon;
    private WorkerThread[] threads;

    /**
     * Constructs a thread pool with given parameters.
     * 
     * @param name
     *            name of the thread pool.
     * @param poolSize
     *            the thread pool size, indicates how many threads are created
     *            in the pool.
     * @param threshold
     *            the maximum size of the task queue in the thread pool.
     * @param daemon
     *            set the threads as daemon if true; otherwise if not.
     * @param debug
     *            Debug object to send debugging message to.
     */
    public ThreadPool(String name, int poolSize, int threshold, boolean daemon,
        Debug debug) {
	this.debug = debug;
	this.poolSize = poolSize;
	this.threshold = threshold;
        this.poolName = name;
        if (threshold > 0) {
            // initialize the size of the ArrayList, it doesn't need to expand
            // during runtime.
            this.taskList = new java.util.ArrayList(threshold);
        } else {
            this.taskList = new java.util.ArrayList();
        }
        this.busyThreadCount = 0;
        this.currentThreadCount = 0;
        this.daemon = daemon;
        this.shutdownThePool = false;
        this.threads = new WorkerThread[poolSize];
	if (debug.messageEnabled()) {
            debug.message("Initiating login thread pool size = "
                    + this.poolSize + "\nThreshold = " + threshold);
        }
        synchronized (this) {
            createThreads(poolSize);
        }
    }
    
    /**
     * Create thread for the pool.
     *
     * @param threadsToCreate number of threads of the pool after creation
     */
    protected void createThreads(int threadsToCreate) {
        if (threadsToCreate > poolSize) {
            threadsToCreate = poolSize;
        }
        for (int i = currentThreadCount; i < threadsToCreate; i++) {
            threads[i - busyThreadCount] = new WorkerThread(poolName, this);
            threads[i - busyThreadCount].setDaemon(daemon);
            threads[i - busyThreadCount].start();
        }
        currentThreadCount = threadsToCreate;
    }
    
    private WorkerThread getAvailableThread() {
        WorkerThread t = null;
        synchronized (this) {
            if (currentThreadCount == busyThreadCount) {
                createThreads(poolSize);
            }
            // get threads from the end of the array
            t = threads[currentThreadCount - busyThreadCount - 1];
            threads[currentThreadCount - busyThreadCount - 1] = null;
            busyThreadCount++;
        }
        return t;
    }

    /**
     * Runs a user defined task.
     * 
     * @param task
     *            user defined task.
     * @throws ThreadPoolException
     */
    public final void run(Runnable task) throws ThreadPoolException 
    {
        WorkerThread t = null;
        synchronized (this) {
            if (shutdownThePool) {
                // No more tasks will be accepted
                throw new ThreadPoolException(poolName +
                    " thread pool's being shutdown.");
            }
            if (busyThreadCount == poolSize) {
                if ((threshold > 0) && (taskList.size() >= threshold)) {
                    throw new ThreadPoolException(poolName + 
                        " thread pool's task queue is full.");
                } else {
                    taskList.add(task);
                }
            }
            else{
                t = getAvailableThread();
            }
        }
        if ((t != null) && (task != null)) {
            t.runTask(task);
        }
    }

    protected synchronized void deductCurrentThreadCount(){
        currentThreadCount--;
        busyThreadCount--;
        if (!taskList.isEmpty()) {
            WorkerThread t = getAvailableThread();
            t.runTask((Runnable)taskList.remove(0));
        } else {
            if (shutdownThePool && (busyThreadCount == 0)) {
                notify();
            }
        }
    }
    
    // return the thread to the thread pool
    protected synchronized void returnThread(WorkerThread t) {
        if (!taskList.isEmpty()){
            t.runTask((Runnable)taskList.remove(0));
        }
        else{
            if(shutdownThePool) {
                t.terminate();
                // notify the thread pool when all threads are backed
                // need to discuss whether the thread pool need to wait until
                // all threads are terminated.  For stand alone application, the
                // answer is yes, however, our application is run under web
                // container. The reason why we need shutdown because it has a
                // parameter daemon in the constructor, if it is set to false,
                // the old implementation has no way to stop the running
                // threads. For the new implementation, if daemon is set to
                // false, it is necessary to call shutdown.  If daemon is set to
                // true, it is nice to call it because the thread pool has
                // better knownledge than the web container to stop the threads
                // in the pool.
                busyThreadCount--;
                currentThreadCount--;
                if(busyThreadCount == 0){
                    notify();
                }
            } else {
                busyThreadCount--;
                // return threads from the end of array
                threads[currentThreadCount - busyThreadCount - 1] = t;
            }
        }
    }
    
    // terminate all the threads since the pass-in parameter of daemon may be
    // false
    public synchronized void shutdown() {
        if(!shutdownThePool) {
            shutdownThePool = true;
            // If daemon thread, discard the remaining tasks
            // else, wait for all tasks to be completed
            if (daemon) {
                taskList.clear();
            } else {
                while (!taskList.isEmpty()) {
                    try {
                        // wait if there are tasks & threads to be executed
                        wait();
                    } catch (Exception ex) {
                        debug.error("ThreadPool.shutdown Excetion while " +
                            "waiting for tasks/threads to complete", ex);
                    }
                }
            }
            for(int i = 0; i < currentThreadCount - busyThreadCount; i++) {
                // terminate the thread from the beginning of the array
                if (threads[i] != null) {
                    threads[i].terminate();
                }
            }
            while(busyThreadCount != 0){
                try{
                    // wait if there are threads running, it will be notified
                    // when they all back.
                    wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            currentThreadCount = busyThreadCount = 0;
            threads = null;
        }
    }
    
    // for test only
    public synchronized int getCurrentThreadCount() {
        return currentThreadCount;
    }
    
    /*
     * Returns the size of the task list.
     */
    public int getCurrentSize() {
        return taskList.size();
    }
    
    // private thread class that fetches tasks from the task queue and
    // executes them.
    private class WorkerThread extends Thread {
        
        private Runnable task = null;
        private ThreadPool pool;
        private boolean needReturn;
        private boolean shouldTerminate;
        
        public WorkerThread(String name, ThreadPool pool) {
	    setName(name);
            this.pool = pool;
            this.shouldTerminate = false;
            this.needReturn = true;
        }
 
	/**
	 * Starts the thread pool.
	 */
        public void run() {
            boolean localShouldTerminate = false;
            Runnable localTask = null;
            WorkerThread t = this;
            while (true) {
                try{
                    synchronized (this) {
                        while ((task == null) && (!shouldTerminate)){
                            this.wait();
                        }
                        // need a local copy because they may be changed after
                        // leaving synchronized block.
                        localShouldTerminate = shouldTerminate;
                        localTask = task;
                        task = null;
                    }
                    if (localShouldTerminate) {
                        // we may need to log something here!
                        break;
                    }
                    if(localTask != null){
                        localTask.run();
                    }
                } catch (RuntimeException ex) {
                    debug.error("Running task " + task, ex);
                    // decide what to log here
                    pool.deductCurrentThreadCount();
                    localShouldTerminate = true;
                    needReturn = false;
                } catch (Exception ex) {
                    // don't need to rethrow
                    debug.error("Running task " + task, ex);
	        } catch (Throwable e) {
		    debug.error("Running task " + task, e);
                    // decide what to log here
                    pool.deductCurrentThreadCount();
                    localShouldTerminate = true;
                    needReturn = false;
                    // rethrow Error here
                    throw new Error(e);
	        } finally {
                    // the thread may has returned already if shutdown is
                    // called.
                    if (needReturn) {
                        pool.returnThread(t);
                    }
                }
                if (localShouldTerminate) {
                    // we may need to log something here!
                    break;
                }
	    }
        }
    
        public synchronized void runTask(Runnable toRun) {
            this.task = toRun;
            // Although the thread may not in wait state when this function
            // is called (the taskList is not empty), it doesn't hurt to
            // call it.  getState method can check whether the Thread is
            // waiting, but it is available in jdk1.5 or newer.
            this.notify();
        }
        
        // terminate the thread pool when daemon is set to false
        // it is better to have a way to terminate the thread pool
        public synchronized void terminate() {
            shouldTerminate = true;
            needReturn = false;
            this.notify();
        }
    }
}
