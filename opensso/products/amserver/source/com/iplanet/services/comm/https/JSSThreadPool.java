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
 * $Id: JSSThreadPool.java,v 1.3 2008/06/25 05:41:34 qcheng Exp $
 *
 */
package com.iplanet.services.comm.https;

import com.sun.identity.shared.debug.Debug;

public class JSSThreadPool {
    public static final int DEFAULT_THREAD_POOL_SIZE = 20;
    private static Debug debug;
    private ReaderWriterClear[] readerWriter;
    private int poolSize;
    private String poolName;
    private java.util.LinkedList taskList;
    private int busyThreadCount;
    private int currentThreadCount;
    private int busyReaderWriterCount;
    private int currentReaderWriterCount;
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
     * @param daemon
     *            set the threads as daemon if true; otherwise if not.
     * @param debug
     *            Debug object to send debugging message to.
     */
    public JSSThreadPool(String name, int poolSize, boolean daemon, Debug debug) {
	this.debug = debug;
	this.poolSize = poolSize;
        this.poolName = name;
        // initialize the size of the ArrayList, it doesn't need to expand
        // during runtime.
        this.taskList = new java.util.LinkedList();
        this.busyThreadCount = 0;
        this.currentThreadCount = 0;
        this.busyReaderWriterCount = 0;
        this.currentReaderWriterCount = 0;
        this.daemon = daemon;
        this.shutdownThePool = false;
        this.threads = new WorkerThread[poolSize];
        this.readerWriter = new ReaderWriterClear[poolSize];
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
    public void run(Runnable task) {
        WorkerThread t = null;
        synchronized (this) {
            if (busyThreadCount == poolSize) {
                taskList.addLast(task);
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
            t.runTask((Runnable)taskList.removeFirst());
        }
    }
    
    // return the thread to the thread pool
    protected synchronized void returnThread(WorkerThread t) {
        if(shutdownThePool) {
            t.terminate();
            // notify the thread pool when all threads are backed
            // need to discuss whether the thread pool need to wait until all
            // threads are terminated.  For stand alone application, the answer
            // is yes, however, our application is run under web container.
            // The reason why we need shutdown because it has a parameter daemon
            // in the constructor, if it is set to false, the old implementation
            // has no way to stop the running threads.  For the new
            // implementation, if daemon is set to false, it is necessary to
            // call shutdown.  If daemon is set to true, it is nice to call it
            // because the thread pool has better knownledge than the web
            // container to stop the threads in the pool.
            t.setNeedReturn(false);
            busyThreadCount--;
            if(busyThreadCount == 0){
                notify();
            }
            return;
        }
        if (!taskList.isEmpty()){
            t.runTask((Runnable)taskList.remove(0));
        }
        else{
            busyThreadCount--;
            // return threads from the end of array
            threads[currentThreadCount - busyThreadCount - 1] = t;
        }
    }
    
    // terminate all the threads since the pass-in parameter of daemon may be
    // false
    public synchronized void shutdown() {
        if(!shutdownThePool) {
            shutdownThePool = true;
            for(int i = 0; i < currentThreadCount - busyThreadCount; i++) {
                // terminate the thread from the beginning of the array
                threads[i].terminate();
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
        private JSSThreadPool pool;
        private boolean needReturn;
        private boolean shouldTerminate;
        
        public WorkerThread(String name, JSSThreadPool pool) {
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
                        if ((task == null) && (!shouldTerminate)){
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
            this.notify();
        }
        
        public synchronized void setNeedReturn(boolean value){
            // this will be set by ThreadPool.returnThread when shutdown is
            // called.
            needReturn = value;
        }
    }
    
}

